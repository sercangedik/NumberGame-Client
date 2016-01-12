package com.multigames.numbergame.Fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import com.multigames.numbergame.Controllers.HomeScreenManager;
import com.multigames.numbergame.Model.ResponseGameModel;
import com.multigames.numbergame.Network.MatchmakingImpl;
import com.multigames.numbergame.NumberGameActivity;
import com.multigames.numbergame.R;
import com.multigames.numbergame.Util.LoadingWidgetManager;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;
import tyrantgit.explosionfield.ExplosionField;

public class HomeScreen extends Fragment implements NumberGameActivity.PusherListener{

    @Bind(R.id.newGame) Button newGame;
    private String deviceId;
    private boolean isAnyMatch = true;
    private boolean isAnyMatchFlowStarted = false;
    private HomeScreenManager manager;
    private LoadingWidgetManager loadingWidget;
    private NumberGameActivity activity;
    private ExplosionField mExplosionField;

    public HomeScreen(MatchmakingImpl matchmakingService) {
        manager = new HomeScreenManager(matchmakingService);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home_screen, container, false);
        ButterKnife.bind(this, view);
        prepareProperties();
        activity.setPusherListener(this);
        mExplosionField = ExplosionField.attach2Window(getActivity());
        newGame.setText(getResources().getString(R.string.new_game));
        return view;
    }

    private void checkAvailableMatch() {
        if(!isAnyMatchFlowStarted) {
            anyAvailableMatch();
        }
    }

    private void prepareProperties() {
        deviceId = Settings.Secure.getString(getActivity().getContentResolver(),
                Settings.Secure.ANDROID_ID);
        activity = ((NumberGameActivity) getActivity());
        loadingWidget = activity.getLoadingWidget();
    }

    @OnClick(R.id.newGame)
    void newGame(){
        if(!activity.isConnected()) {
            Toast.makeText(getActivity(), getString(R.string.connection_not_yet), Toast.LENGTH_SHORT).show();
            return;
        } else if(!isAnyMatch){
            Toast.makeText(getActivity(), getString(R.string.game_check), Toast.LENGTH_SHORT).show();
            return;
        }

        mExplosionField.explode(getActivity().findViewById(R.id.newGame));
        callNewGameRequest();
    }

    private void callNewGameRequest() {
        loadingWidget.start();
        manager.newGame(deviceId).enqueue(new Callback<ResponseGameModel>() {
            @Override
            public void onResponse(Response<ResponseGameModel> response, Retrofit retrofit) {
                activity.setSocketName(response.body().getGameModels().getSocketName());
                activity.getNavigationUtil().switchToGameScreenWithNew(activity.getSocketName(), activity.getPusher(), activity.getMatchmakingService());
            }

            @Override
            public void onFailure(Throwable t) {
                stopLoadingWidget();
                Toast.makeText(getActivity(), t.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void stopLoadingWidget() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadingWidget.stop();
            }
        });
    }

    private void startLoadingWidget() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadingWidget.start();
            }
        });
    }

    public void anyAvailableMatch(){
        isAnyMatchFlowStarted = true;
        startLoadingWidget();

        manager.anyAvailableMatch(deviceId).enqueue(new Callback<ResponseGameModel>() {
            @Override
            public void onResponse(Response<ResponseGameModel> response, Retrofit retrofit) {
                if (response.body().getStat().equals("fail")) {
                    isAnyMatch = true;
                    Toast.makeText(getActivity(), getString(R.string.game_check_fail), Toast.LENGTH_SHORT).show();
                    stopLoadingWidget();
                } else {
                    activity.setSocketName(response.body().getGameModels().getSocketName());
                    ((NumberGameActivity) getActivity()).getNavigationUtil().switchToGameScreenWithResume(response.body().getGameModels(), activity.getPusher(), activity.getMatchmakingService());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                isAnyMatch = true;
                stopLoadingWidget();
                Toast.makeText(getActivity(), t.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void connected() {
        checkAvailableMatch();
    }
}
