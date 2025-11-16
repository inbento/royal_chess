package com.example.rc;
import com.example.rc.models.User;
import com.example.rc.models.King;
import com.example.rc.models.GameSession;
public class OnlineGameManager {
    private static OnlineGameManager instance;

    public static OnlineGameManager getInstance() {
        if (instance == null) {
            instance = new OnlineGameManager();
        }
        return instance;
    }

    public interface MatchmakingListener {
        void onMatchFound(GameSession session);
        void onMatchmakingUpdate(int playersInQueue);
        void onError(String error);
    }

    public void findMatch(User user, String matchId, String color, int timeMinutes, King king, MatchmakingListener listener) {

    }


    public void cancelMatchmaking(User user) {

    }
}