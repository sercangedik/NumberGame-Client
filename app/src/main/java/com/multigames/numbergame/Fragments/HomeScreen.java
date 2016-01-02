package com.multigames.numbergame.Fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.multigames.numbergame.Controllers.HomeScreenManager;
import com.multigames.numbergame.Model.ResponseGameModel;
import com.multigames.numbergame.Network.MatchmakingImpl;
import com.multigames.numbergame.NumberGameActivity;
import com.multigames.numbergame.R;
import com.multigames.numbergame.Util.LoadingWidgetManager;

import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

public class HomeScreen extends Fragment implements NumberGameActivity.PusherListener{

    private String deviceId;
    private boolean isAnyMatch = true;
    private boolean isAnyMatchFlowStarted = false;
    private HomeScreenManager manager;
    private LoadingWidgetManager loadingWidget;
    private NumberGameActivity activity;

    public HomeScreen(MatchmakingImpl matchmakingService) {
        manager = new HomeScreenManager(matchmakingService);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home_screen, container, false);
        ButterKnife.bind(this, view);
        prepareProperties();
        checkAvailableMatch();
        activity.setPusherListener(this);
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
            Toast.makeText(getActivity(), "Henuz baglantiniz gerceklesmedi.", Toast.LENGTH_SHORT).show();
            return;
        } else if(!isAnyMatch){
            Toast.makeText(getActivity(), "Devam eden oyununuz kontrol ediliyor.", Toast.LENGTH_SHORT).show();
            return;
        } else if(!isAnyMatchFlowStarted) {
            anyAvailableMatch();
            return;
        }

        callNewGameRequest();
    }

    private void callNewGameRequest() {
        loadingWidget.startWithTimeOut(10);
        manager.newGame(deviceId).enqueue(new Callback<ResponseGameModel>() {
            @Override
            public void onResponse(Response<ResponseGameModel> response, Retrofit retrofit) {
                stopLoadingWidget();
                activity.setSocketName(response.body().getGameModels().getSocketName());
                activity.getNavigationUtil().switchToGameScreenWithNew(activity.getSocketName(), activity.getPusher(), activity.getMatchmakingService());
            }

            @Override
            public void onFailure(Throwable t) {
                stopLoadingWidget();
                Toast.makeText(getActivity(), t.toString(), Toast.LENGTH_SHORT).show();
                Log.d("Sercan", "Failure : " + t);
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
                loadingWidget.startWithTimeOut(10);
            }
        });
    }

    public void anyAvailableMatch(){
        isAnyMatchFlowStarted = true;
        startLoadingWidget();

        manager.anyAvailableMatch(deviceId).enqueue(new Callback<ResponseGameModel>() {
            @Override
            public void onResponse(Response<ResponseGameModel> response, Retrofit retrofit) {
                stopLoadingWidget();
                if (response.body().getStat().equals("fail")) {
                    isAnyMatch = true;
                    Toast.makeText(getActivity(), "Devam eden oyununuz bulunmamaktadir.", Toast.LENGTH_SHORT).show();
                } else {
                    activity.setSocketName(response.body().getGameModels().getSocketName());
                    ((NumberGameActivity) getActivity()).getNavigationUtil().switchToGameScreenWithResume(response.body().getGameModels(), activity.getPusher(), activity.getMatchmakingService());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                stopLoadingWidget();
                Toast.makeText(getActivity(), t.toString(), Toast.LENGTH_SHORT).show();
                Log.d("Sercan", "Failure : " + t);
            }
        });
    }

    @Override
    public void connected() {
        checkAvailableMatch();
    }
}
