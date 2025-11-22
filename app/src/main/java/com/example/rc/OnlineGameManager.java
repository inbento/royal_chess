package com.example.rc;

import android.util.Log;

import com.google.firebase.database.*;
import com.example.rc.models.User;
import com.example.rc.models.King;
import com.example.rc.models.GameSession;
import com.example.rc.FirebaseManager.MatchmakingRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class OnlineGameManager {
    private static OnlineGameManager instance;
    private FirebaseManager firebaseManager;
    private ValueEventListener matchmakingListener;
    private String currentMatchmakingId;
    private MatchmakingListener userMatchmakingListener;

    public static OnlineGameManager getInstance() {
        if (instance == null) {
            instance = new OnlineGameManager();
        }
        return instance;
    }

    private OnlineGameManager() {
        firebaseManager = FirebaseManager.getInstance();
    }

    public interface MatchmakingListener {
        void onMatchFound(GameSession session);
        void onMatchmakingUpdate(int playersInQueue);
        void onError(String error);
    }

    public void findMatch(User user, String matchmakingId, String color, int timeMinutes, King king, MatchmakingListener listener) {
        this.currentMatchmakingId = matchmakingId;
        this.userMatchmakingListener = listener;

        MatchmakingRequest request = new MatchmakingRequest(user, matchmakingId, color, timeMinutes, king);

        firebaseManager.addToMatchmaking(request, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError error, DatabaseReference ref) {
                if (error != null) {
                    listener.onError(error.getMessage());
                    return;
                }

                startListeningForMatches(user, color, timeMinutes, king);
            }
        });
    }

    private void startListeningForMatches(User currentUser, String preferredColor, int timeMinutes, King king) {
        matchmakingListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<MatchmakingRequest> availablePlayers = new ArrayList<>();
                int totalPlayers = 0;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    MatchmakingRequest request = snapshot.getValue(MatchmakingRequest.class);
                    if (request != null && !request.matchmakingId.equals(currentMatchmakingId)) {
                        availablePlayers.add(request);
                    }
                    totalPlayers++;
                }

                if (userMatchmakingListener != null) {
                    userMatchmakingListener.onMatchmakingUpdate(totalPlayers);
                }

                findSuitableOpponent(currentUser, availablePlayers, preferredColor, timeMinutes, king);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                if (userMatchmakingListener != null) {
                    userMatchmakingListener.onError(databaseError.getMessage());
                }
            }
        };

        firebaseManager.listenForMatches(currentUser.getOnlineId(), matchmakingListener);
    }

    private void findSuitableOpponent(User currentUser, List<MatchmakingRequest> opponents,
                                      String preferredColor, int timeMinutes, King king) {
        if (opponents.isEmpty()) return;

        for (MatchmakingRequest opponent : opponents) {
            if (isCompatibleMatch(currentUser, opponent, preferredColor, timeMinutes)) {
                String potentialSessionId = generateSessionId(currentUser, opponent, timeMinutes);

                checkIfSessionExists(potentialSessionId, (sessionExists) -> {
                    if (sessionExists) {
                        connectToExistingSession(potentialSessionId, currentUser);
                    } else {
                        // Создаем новую сессию
                        createGameSession(currentUser, opponent, preferredColor, timeMinutes, king);
                    }
                });
                break;
            }
        }
    }

    private boolean isCompatibleMatch(User currentUser, MatchmakingRequest opponent,
                                      String preferredColor, int timeMinutes) {
        if (opponent.timeMinutes != timeMinutes) {
            return false;
        }

        if (preferredColor.equals(opponent.color)) {
            return false;
        }

        return true;
    }

    private void createGameSession(User currentUser, MatchmakingRequest opponent,
                                   String preferredColor, int timeMinutes, King king) {
        String player1Color = determineColors(preferredColor, opponent.color);
        String player2Color = player1Color.equals("white") ? "black" : "white";

        String sessionId = generateSessionId(currentUser, opponent, timeMinutes);
        clearOldSessionData(sessionId);
        GameSession session = new GameSession(sessionId);

        session.setPlayer1Id(currentUser.getOnlineId());
        session.setPlayer2Id(opponent.userId);
        session.setPlayer1Color(player1Color);
        session.setPlayer2Color(player2Color);
        session.setTimeMinutes(timeMinutes);

        session.setPlayer1Username(currentUser.getUsername());
        session.setPlayer2Username(opponent.username);
        session.setPlayer1KingType(getKingTypeFromFaction(king.getFaction()));
        session.setPlayer2KingType(opponent.kingType);

        Log.d("OnlineGameManager", "Creating session:" +
                "\nPlayer1: " + currentUser.getUsername() + " (" + player1Color + ", " + getKingTypeFromFaction(king.getFaction()) + ")" +
                "\nPlayer2: " + opponent.username + " (" + player2Color + ", " + opponent.kingType + ")" +
                "\nTime: " + timeMinutes + " minutes");

        firebaseManager.createGameSession(session, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError error, DatabaseReference ref) {
                if (error != null) {
                    Log.e("OnlineGameManager", "Error creating session: " + error.getMessage());
                    if (userMatchmakingListener != null) {
                        userMatchmakingListener.onError(error.getMessage());
                    }
                    return;
                }

                firebaseManager.removeFromMatchmaking(currentMatchmakingId);
                firebaseManager.removeFromMatchmaking(opponent.matchmakingId);

                if (userMatchmakingListener != null) {
                    userMatchmakingListener.onMatchFound(session);
                }

                stopMatchmaking();
            }
        });
    }

    private String determineColors(String color1, String color2) {
        if ("white".equals(color1)) return "white";
        if ("white".equals(color2)) return "black";
        if ("black".equals(color1)) return "black";
        if ("black".equals(color2)) return "white";

        return "white";
    }

    public void cancelMatchmaking(User user) {
        if (currentMatchmakingId != null) {
            firebaseManager.removeFromMatchmaking(currentMatchmakingId);
        }
        stopMatchmaking();
    }

    private void stopMatchmaking() {
        if (matchmakingListener != null) {
            firebaseManager.stopListeningForMatches(matchmakingListener);
            matchmakingListener = null;
        }
        currentMatchmakingId = null;
        userMatchmakingListener = null;
    }

    public void getGameSession(String sessionId, ValueEventListener listener) {
        firebaseManager.getGameSession(sessionId, listener);
    }

    private String getKingTypeFromFaction(String faction) {
        switch (faction) {
            case "Люди": return "human";
            case "Драконы": return "dragon";
            case "Эльфы": return "elf";
            case "Гномы": return "gnome";
            default: return "human";
        }
    }

    private String generateSessionId(User player1, MatchmakingRequest player2, int timeMinutes) {
        String[] ids = {player1.getOnlineId(), player2.userId};
        Arrays.sort(ids);
        return "session_" + ids[0] + "_" + ids[1] + "_" + timeMinutes;
    }

    private void checkIfSessionExists(String sessionId, SessionExistsCallback callback) {
        FirebaseManager.getInstance().getGameSession(sessionId, new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                callback.onResult(dataSnapshot.exists());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onResult(false);
            }
        });
    }

    private void connectToExistingSession(String sessionId, User currentUser) {
        FirebaseManager.getInstance().getGameSession(sessionId, new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    GameSession session = new GameSession();
                    session.setSessionId(dataSnapshot.child("sessionId").getValue(String.class));
                    session.setPlayer1Id(dataSnapshot.child("player1Id").getValue(String.class));
                    session.setPlayer2Id(dataSnapshot.child("player2Id").getValue(String.class));
                    session.setPlayer1Username(dataSnapshot.child("player1Username").getValue(String.class));
                    session.setPlayer2Username(dataSnapshot.child("player2Username").getValue(String.class));
                    session.setPlayer1KingType(dataSnapshot.child("player1KingType").getValue(String.class));
                    session.setPlayer2KingType(dataSnapshot.child("player2KingType").getValue(String.class));
                    session.setPlayer1Color(dataSnapshot.child("player1Color").getValue(String.class));
                    session.setPlayer2Color(dataSnapshot.child("player2Color").getValue(String.class));
                    session.setTimeMinutes(dataSnapshot.child("timeMinutes").getValue(Integer.class));

                    if (session != null && userMatchmakingListener != null) {
                        firebaseManager.removeFromMatchmaking(currentMatchmakingId);
                        userMatchmakingListener.onMatchFound(session);
                        stopMatchmaking();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("OnlineGameManager", "Error connecting to existing session: " + databaseError.getMessage());
            }
        });
    }

    interface SessionExistsCallback {
        void onResult(boolean sessionExists);
    }

    private void clearOldSessionData(String sessionId) {
        FirebaseManager.getInstance().clearSessionData(sessionId);
    }

}