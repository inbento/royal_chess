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

    public void findMatch(User user, String color, int timeMinutes, King king, MatchmakingListener listener) {
        // TODO: Реализовать логику поиска матча через выбранный сервер
        // 1. Отправляем запрос на сервер с параметрами
        // 2. Сервер ищет подходящего соперника
        // 3. При нахождении создает GameSession
        // 4. Возвращает session обоим игрокам
    }

    public void cancelMatchmaking(User user) {
        // TODO: Отменить поиск если пользователь вышел
    }
}