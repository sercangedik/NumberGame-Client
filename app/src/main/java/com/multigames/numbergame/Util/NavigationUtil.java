package com.multigames.numbergame.Util;

import android.app.Fragment;

import com.multigames.numbergame.Fragments.EndScreen;
import com.multigames.numbergame.Fragments.GameScreen;
import com.multigames.numbergame.Fragments.HomeScreen;
import com.multigames.numbergame.Model.GameDataModel;
import com.multigames.numbergame.Model.GameModel;
import com.multigames.numbergame.Network.MatchmakingImpl;
import com.multigames.numbergame.Network.MatchmakingService;
import com.multigames.numbergame.NumberGameActivity;
import com.pusher.client.Pusher;

public class NavigationUtil {
    public static final String HOME_SCREEN = "HOME_SCREEN";
    public static final String GAME_SCREEN = "GAME_SCREEN";
    public static final String END_SCREEN = "END_SCREEN";
    public String currentScreen;

    private NumberGameActivity numberGameActivity;
    private LoadingWidgetManager loadingWidgetManager;

    public NavigationUtil(NumberGameActivity numberGameActivity){
        this.numberGameActivity = numberGameActivity;
        loadingWidgetManager = new LoadingWidgetManager(numberGameActivity);
    }

    public void switchToGameScreenWithNew(String socketName, Pusher pusher, MatchmakingImpl matchmaking){
        GameScreen gameScreen = new GameScreen(socketName, pusher, matchmaking);
        setScreen(gameScreen, GAME_SCREEN);
    }

    public void switchToGameScreenWithResume(GameModel gameModel, Pusher pusher, MatchmakingImpl matchmaking){
        GameScreen gameScreen = new GameScreen(gameModel, pusher, matchmaking);
        setScreen(gameScreen, GAME_SCREEN);
    }

    public void switchToHomeScreen(MatchmakingImpl matchmakingService){
        HomeScreen homeScreen = new HomeScreen(matchmakingService);
        setScreen(homeScreen, HOME_SCREEN);
    }

    public void switchToEndScreen(String opponentNumber, boolean isWin, String cause){
        EndScreen endScreen = new EndScreen(opponentNumber, isWin, cause);
        setScreen(endScreen, END_SCREEN);
    }

    public void setScreen(Fragment fragment, String tag) {
        currentScreen = tag;
        numberGameActivity.getFragmentManager()
                .beginTransaction()
                .replace(numberGameActivity.getFragmentContainerId(), fragment, tag)
                .commitAllowingStateLoss();
    }

    public String getCurrentScreenName() {
        return currentScreen;
    }

    public LoadingWidgetManager getLoadingWidgetManager() {
        return loadingWidgetManager;
    }
}
