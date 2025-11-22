package com.example.rc;

import android.util.Log;

import com.example.rc.models.ChessMove;
import com.google.firebase.database.*;
import com.example.rc.models.User;
import com.example.rc.models.King;
import com.example.rc.models.GameSession;
import java.util.HashMap;
import java.util.Map;

public class FirebaseManager {
    private static FirebaseManager instance;
    private DatabaseReference database;

    private static final String FIREBASE_URL = "";

    private static final String MATCHMAKING_NODE = "matchmaking";
    private static final String GAME_SESSIONS_NODE = "game_sessions";
    private static final String USERS_NODE = "users";
    private static final String MOVES_NODE = "moves";

    public static FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    private FirebaseManager() {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance(FIREBASE_URL);
        database = firebaseDatabase.getReference();

        FirebaseDatabase.getInstance().setLogLevel(Logger.Level.DEBUG);
    }

    public static class MatchmakingRequest {
        public String userId;
        public String username;
        public String color;
        public int timeMinutes;
        public String kingType;
        public long timestamp;
        public String matchmakingId;

        public MatchmakingRequest() {
        }

        public MatchmakingRequest(User user, String matchmakingId, String color, int timeMinutes, King king) {
            this.userId = user.getOnlineId();
            this.username = user.getUsername();
            this.color = color;
            this.timeMinutes = timeMinutes;
            this.kingType = getKingTypeFromFaction(king.getFaction());
            this.timestamp = System.currentTimeMillis();
            this.matchmakingId = matchmakingId;
        }
    }

    public void addToMatchmaking(MatchmakingRequest request, DatabaseReference.CompletionListener listener) {
        database.child(MATCHMAKING_NODE).child(request.matchmakingId).setValue(request, listener);
    }

    public void removeFromMatchmaking(String matchmakingId) {
        database.child(MATCHMAKING_NODE).child(matchmakingId).removeValue();
    }

    public void createGameSession(GameSession session, DatabaseReference.CompletionListener listener) {
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("sessionId", session.getSessionId());
        sessionData.put("player1Id", session.getPlayer1Id());
        sessionData.put("player2Id", session.getPlayer2Id());
        sessionData.put("player1Username", session.getPlayer1Username());
        sessionData.put("player2Username", session.getPlayer2Username());
        sessionData.put("player1KingType", session.getPlayer1KingType());
        sessionData.put("player2KingType", session.getPlayer2KingType());
        sessionData.put("player1Color", session.getPlayer1Color());
        sessionData.put("player2Color", session.getPlayer2Color());
        sessionData.put("timeMinutes", session.getTimeMinutes());
        sessionData.put("status", session.getStatus());
        sessionData.put("createdAt", session.getCreatedAt());
        sessionData.put("currentFen", session.getCurrentFen());
        sessionData.put("isWhiteTurn", session.isWhiteTurn());

        database.child(GAME_SESSIONS_NODE).child(session.getSessionId()).setValue(sessionData, listener);
    }

    public void listenForMatches(String currentUserId, ValueEventListener listener) {
        database.child(MATCHMAKING_NODE).orderByChild("timestamp").addValueEventListener(listener);
    }

    public void stopListeningForMatches(ValueEventListener listener) {
        database.child(MATCHMAKING_NODE).removeEventListener(listener);
    }

    public void getGameSession(String sessionId, ValueEventListener listener) {
        database.child(GAME_SESSIONS_NODE).child(sessionId).addListenerForSingleValueEvent(listener);
    }

    private static String getKingTypeFromFaction(String faction) {
        switch (faction) {
            case "Люди":
                return "human";
            case "Драконы":
                return "dragon";
            case "Эльфы":
                return "elf";
            case "Гномы":
                return "gnome";
            default:
                return "human";
        }

    }

    public void saveUserInfo(User user) {
        DatabaseReference userRef = database.child(USERS_NODE).child(user.getOnlineId());

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", user.getUsername());
        userInfo.put("lastSeen", System.currentTimeMillis());

        userRef.setValue(userInfo);
    }

    public void getUserInfo(String onlineId, ValueEventListener listener) {
        database.child(USERS_NODE).child(onlineId).addListenerForSingleValueEvent(listener);
    }

    public void getOpponentInfo(String sessionId, String currentUserId, ValueEventListener listener) {
        database.child(GAME_SESSIONS_NODE).child(sessionId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String opponentId = null;
                    String player1Id = dataSnapshot.child("player1Id").getValue(String.class);
                    String player2Id = dataSnapshot.child("player2Id").getValue(String.class);

                    if (currentUserId.equals(player1Id)) {
                        opponentId = player2Id;
                    } else if (currentUserId.equals(player2Id)) {
                        opponentId = player1Id;
                    }

                    if (opponentId != null) {
                        getUserInfo(opponentId, listener);
                    } else {
                        listener.onCancelled(DatabaseError.fromException(new Exception("Opponent not found")));
                    }
                } else {
                    listener.onCancelled(DatabaseError.fromException(new Exception("Session not found")));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onCancelled(databaseError);
            }
        });
    }

    public void reportSurrender(String sessionId, String surrenderingPlayerId) {
        DatabaseReference sessionRef = database.child(GAME_SESSIONS_NODE).child(sessionId);

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("status", "finished");
        //updateData.put("winner", surrenderingPlayerId.equals(getPlayer1Id(sessionId)) ? "player2" : "player1");
        updateData.put("surrender", true);
        updateData.put("endTime", System.currentTimeMillis());

        sessionRef.updateChildren(updateData);
    }

    public void sendMove(String sessionId, Map<String, Object> moveData) {
        database.child(GAME_SESSIONS_NODE).child(sessionId).child("moves").push().setValue(moveData);
    }

    public void listenForMoves(String sessionId, ChildEventListener listener) {
        database.child(GAME_SESSIONS_NODE).child(sessionId).child(MOVES_NODE).addChildEventListener(listener);
    }

    public void updateBoardState(String sessionId, String fen, boolean isWhiteTurn) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("currentFen", fen);
        updateData.put("isWhiteTurn", isWhiteTurn);
        updateData.put("lastUpdate", System.currentTimeMillis());

        database.child(GAME_SESSIONS_NODE).child(sessionId).updateChildren(updateData);
    }

    public void listenForBoardState(String sessionId, ValueEventListener listener) {
        database.child(GAME_SESSIONS_NODE).child(sessionId).addValueEventListener(listener);
    }

    public void sendMove(String sessionId, ChessMove move) {
        Map<String, Object> moveData = new HashMap<>();
        moveData.put("playerId", move.getPlayerId());
        moveData.put("fromRow", move.getFromRow());
        moveData.put("fromCol", move.getFromCol());
        moveData.put("toRow", move.getToRow());
        moveData.put("toCol", move.getToCol());
        moveData.put("promotionType", move.getPromotionType());
        moveData.put("moveType", move.getMoveType());
        moveData.put("timestamp", move.getTimestamp());

        database.child(GAME_SESSIONS_NODE).child(sessionId).child(MOVES_NODE).push().setValue(moveData);
    }

    public void updateGameSessionStatus(String sessionId, String status, String loserId, int movesCount) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("status", status);
        updateData.put("loserId", loserId);
        updateData.put("endTime", System.currentTimeMillis());
        updateData.put("totalMoves", movesCount);
        updateData.put("winnerId", getOpponentId(sessionId, loserId));

        database.child(GAME_SESSIONS_NODE).child(sessionId).updateChildren(updateData)
                .addOnSuccessListener(aVoid -> Log.d("FirebaseManager", "Game session status updated to: " + status))
                .addOnFailureListener(e -> Log.e("FirebaseManager", "Failed to update game session status: " + e.getMessage()));
    }

    private String getOpponentId(String sessionId, String loserId) {
        return "opponent_of_" + loserId;
    }
    public void clearSessionData(String sessionId) {
        Map<String, Object> clearData = new HashMap<>();
        clearData.put("playerExit", null);
        clearData.put("abilityActivations", null);
        clearData.put("pieceTransformations", null);
        clearData.put("dragonFireShots", null);

        database.child(GAME_SESSIONS_NODE).child(sessionId).updateChildren(clearData);
    }

    public void deleteGameSession(String sessionId) {
        database.child(GAME_SESSIONS_NODE).child(sessionId).removeValue()
                .addOnSuccessListener(aVoid -> Log.d("FirebaseManager", "Game session deleted: " + sessionId))
                .addOnFailureListener(e -> Log.e("FirebaseManager", "Failed to delete game session: " + e.getMessage()));
    }

    public void sendPlayerExitNotification(String sessionId, Map<String, Object> exitData) {
        database.child(GAME_SESSIONS_NODE).child(sessionId).child("playerExit").setValue(exitData);
    }

    public void listenForPlayerExit(String sessionId, ValueEventListener listener) {
        database.child(GAME_SESSIONS_NODE).child(sessionId).child("playerExit").addValueEventListener(listener);
    }

    private long getSessionStartTime(String sessionId) {
        return System.currentTimeMillis() - (10 * 60 * 1000);
    }

    public void sendAbilityActivation(String sessionId, Map<String, Object> abilityData) {
        database.child(GAME_SESSIONS_NODE).child(sessionId).child("abilityActivations").push().setValue(abilityData);
    }

    public void sendPieceTransformation(String sessionId, Map<String, Object> transformData) {
        database.child(GAME_SESSIONS_NODE).child(sessionId).child("pieceTransformations").push().setValue(transformData);
    }

    public void stopListeningForMoves(String sessionId, ChildEventListener listener) {
        database.child(GAME_SESSIONS_NODE).child(sessionId).child(MOVES_NODE).removeEventListener(listener);
    }

    public void listenForAbilityActivations(String sessionId, ChildEventListener listener) {
        database.child(GAME_SESSIONS_NODE).child(sessionId).child("abilityActivations").addChildEventListener(listener);
    }

    public void listenForPieceTransformations(String sessionId, ChildEventListener listener) {
        database.child(GAME_SESSIONS_NODE).child(sessionId).child("pieceTransformations").addChildEventListener(listener);
    }
    public void stopListeningForBoardState(String sessionId, ValueEventListener listener) {
        database.child(GAME_SESSIONS_NODE).child(sessionId).removeEventListener(listener);
    }

    public void sendDragonFireShot(String sessionId, Map<String, Object> fireData) {
        database.child(GAME_SESSIONS_NODE).child(sessionId).child("dragonFireShots").push().setValue(fireData);
    }

    public void listenForDragonFireShots(String sessionId, ChildEventListener listener) {
        database.child(GAME_SESSIONS_NODE).child(sessionId).child("dragonFireShots").addChildEventListener(listener);
    }

    public void clearPlayerExitNotification(String sessionId) {
        database.child(GAME_SESSIONS_NODE).child(sessionId).child("playerExit").removeValue();
    }

    public void stopListeningForPlayerExit(String sessionId, ValueEventListener listener) {
        database.child(GAME_SESSIONS_NODE).child(sessionId).child("playerExit").removeEventListener(listener);
    }

    public void stopListeningForAbilityActivations(String sessionId, ChildEventListener listener) {
        database.child(GAME_SESSIONS_NODE).child(sessionId).child("abilityActivations").removeEventListener(listener);
    }

    public void stopListeningForPieceTransformations(String sessionId, ChildEventListener listener) {
        database.child(GAME_SESSIONS_NODE).child(sessionId).child("pieceTransformations").removeEventListener(listener);
    }

    public void stopListeningForDragonFireShots(String sessionId, ChildEventListener listener) {
        database.child(GAME_SESSIONS_NODE).child(sessionId).child("dragonFireShots").removeEventListener(listener);
    }

}