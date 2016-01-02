package com.multigames.numbergame.Fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.multigames.numbergame.Controllers.GameScreenManager;
import com.multigames.numbergame.Model.GameModel;
import com.multigames.numbergame.Model.ResponseModel;
import com.multigames.numbergame.Network.MatchmakingImpl;
import com.multigames.numbergame.NumberGameActivity;
import com.multigames.numbergame.R;
import com.multigames.numbergame.Util.MoveAdapter;
import com.pusher.client.Pusher;
import com.pusher.client.channel.PrivateChannelEventListener;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;


public class GameScreen extends Fragment implements PrivateChannelEventListener {

    @Bind(R.id.yourNumber)  TextView yourNumber;
    @Bind(R.id.timer)       TextView timerView;
    @Bind(R.id.myGuess)     EditText myGuessEditText;

    private ListView moveListView;
    private List<String> moveList;

    private Dialog chooseNumberDialog;
    private String myNumber;
    private String deviceId;
    private final GameScreenManager manager;
    private final GameModel gameModel;
    private boolean isResumingGame;
    private CountDownTimer timer;
    private NumberGameActivity activity;
    private boolean isConnectionCompleted;
    private DialogInterface.OnClickListener dialogQuitListener;

    private final long NEXT_MOVE_TIME = 65000;

    public GameScreen(GameModel gameModel, Pusher pusher, MatchmakingImpl matchmaking) {
        this.gameModel = gameModel;
        manager = new GameScreenManager(matchmaking, gameModel.getSocketName(), pusher);
        isResumingGame = true;
        if(gameModel.getGameState().equals("3")) {
            manager.setIsConnectedOpponent(true);
        }
    }

    public GameScreen(String socketName, Pusher pusher, MatchmakingImpl matchmaking) {
        manager = new GameScreenManager(matchmaking, socketName, pusher);
        this.gameModel = null;
        isResumingGame = false;
    }

    public void startTimer(final long countDown) {
        cancelTimer();
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                timer = new CountDownTimer(countDown, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        long remainingTime = (millisUntilFinished / 1000);

                        if(!manager.isMineMove()) {
                            remainingTime = remainingTime - 5;
                        }

                        if(remainingTime < 0) {
                            remainingTime = 0;
                        }
                        timerView.setText("Kalan Sure : " + String.valueOf(remainingTime));
                    }

                    @Override
                    public void onFinish() {
                        if(!manager.isMineMove()) {

                            manager.endMatch(deviceId).enqueue(new Callback<ResponseModel>() {
                                @Override
                                public void onResponse(Response<ResponseModel> response, Retrofit retrofit) {
                                    showToastMessage("Rakibiniz zamaninda hamlesini yapmadi. Oyunuz kazandiniz.");
                                    manager.endGame(myNumber, false);
                                    cancelTimer();
                                    activity.getNavigationUtil().switchToEndScreen(response.message(), true);
                                }

                                @Override
                                public void onFailure(Throwable t) {

                                }
                            });
                        } else if(countDown <= 1) {
                            Log.d("sercan", String.valueOf(countDown));
                            addMove("");
                        }
                    }
                }.start();
            }
        });
    }

    private void cancelTimer() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(timer != null) {
                    timer.cancel();
                    timer = null;
                }
            }
        });
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.game_screen, container, false);
        ButterKnife.bind(this, view);
        initializeDeviceId();
        initializeMoveList(view);
        prepareGame();
        activity = (NumberGameActivity)getActivity();
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prepareQuitDialogBox();
    }

    private void prepareQuitDialogBox() {
        dialogQuitListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        if(manager.isConnectedOpponent()) {
                            manager.endMatch(deviceId).enqueue(new Callback<ResponseModel>() {
                                @Override
                                public void onResponse(Response<ResponseModel> response, Retrofit retrofit) {
                                    manager.endGame(myNumber, true);
                                    showToastMessage("Oyundan kendi isteginizle ciktiginiz icin kaybettiniz.");
                                    cancelTimer();
                                    activity.getNavigationUtil().switchToEndScreen(response.body().getData(), false);
                                }

                                @Override
                                public void onFailure(Throwable t) {

                                }
                            });
                        } else {
                            manager.cancelGame(deviceId).enqueue(new Callback<ResponseModel>() {
                                @Override
                                public void onResponse(Response<ResponseModel> response, Retrofit retrofit) {
                                    manager.cancelGame(deviceId);
                                    showToastMessage("Oyununuz iptal edildi. Yeni oyuna baslayabilirsiniz.");
                                    cancelTimer();
                                    activity.getNavigationUtil().switchToHomeScreen(activity.getMatchmakingService());
                                }

                                @Override
                                public void onFailure(Throwable t) {

                                }
                            });
                        }
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };
    }

    public void showQuitDialogBox() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Oyundan cikmak istiyor musunuz ?").setPositiveButton("Evet", dialogQuitListener)
                .setNegativeButton("Hayir", dialogQuitListener).show();

    }

    private void prepareGame() {
        if(isResumingGame) {
            yourNumber.setText(gameModel.getGameDataModel().getMyNumber());
            manager.subscribeChannel(this);
            updateListWithMoves();
            prepareWhoIsMove();
        } else {
            createAndShowNumber();
            initializeDialogButton();
        }
    }

    private void updateListWithMoves() {
        if(gameModel.getGameDataModel().getMyMoves() != null && gameModel.getGameDataModel().getPlayerMoveResults() != null) {
            updateList(gameModel.getGameDataModel().getMyMoves().split(","), gameModel.getGameDataModel().getPlayerMoveResults().split(","));
        }
    }

    private void prepareWhoIsMove() {
        if(deviceId.equals(gameModel.getGameDataModel().getCurrentMoveId())){
            manager.setIsMineMove(true);
        } else {
            manager.setIsMineMove(false);
        }
    }

    private void prepareTime() {
        String moveEndTime = gameModel.getGameDataModel().getMoveEndTime();
        if(moveEndTime == null) {
            return;
        }
        long countDown = Long.parseLong(moveEndTime) - (System.currentTimeMillis() / 1000);
        if(countDown < 0) {
            addMove("");
        } else {
            startTimer(countDown);
        }
    }

    private void initializeDeviceId() {
        deviceId = Settings.Secure.getString(getActivity().getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    private void initializeMoveList(View view) {
        moveList = new ArrayList<>();
        moveListView = (ListView) view.findViewById(R.id.yourMoveList);
        MoveAdapter moveAdapter = new MoveAdapter(getActivity(), moveList);
        moveListView.setAdapter(moveAdapter);
    }

    private void initializeDialogButton() {
        Button dialogButton = (Button) chooseNumberDialog.findViewById(R.id.dialogButtonOK);
        final EditText dialogText = (EditText) chooseNumberDialog.findViewById(R.id.text);
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!manager.checkNumber(dialogText.getText().toString())) {
                    showToastMessage("Lutfen kurallara uygun bir sayi seciniz");
                    return;
                }

                yourNumber.setText(dialogText.getText());
                myNumber = yourNumber.getText().toString();
                subscribeChannel();
                chooseNumberDialog.dismiss();
                dialogText.getText().clear();
            }
        });
    }

    private void subscribeChannel() {
        activity.getLoadingWidget().startWithTimeOut(10);
        manager.subscribeChannel(this);
    }

    private void createAndShowNumber() {
        chooseNumberDialog = new Dialog(getActivity());
        chooseNumberDialog.setContentView(R.layout.select_number_popup);
        chooseNumberDialog.setTitle("Lutfen Sayinizi Belirleyin");
        chooseNumberDialog.setCanceledOnTouchOutside(false);
        chooseNumberDialog.setOnKeyListener(new Dialog.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                return keyCode == KeyEvent.KEYCODE_BACK;
            }
        });
        chooseNumberDialog.show();
    }

    public void showToastMessage(final String message) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void hideLoadingWidget() {
        ((NumberGameActivity)getActivity()).getLoadingWidget().stop();
        showToastMessage("Sayi gonderilemedi. Tekrar deneyin.");
    }

    @Override
    public void onDestroy() {
        manager.destroy();
        super.onDestroy();
    }

    @OnClick(R.id.buttonSend) void clickSend(){
        String myGuessSending = myGuessEditText.getText().toString();
        if(!manager.isConnectedOpponent()){
            showToastMessage("Rakibiniz bekleniyor...");
            return;
        } else if(!manager.isMineMove()) {
            showToastMessage("Sira sizde degil. Lutfen bekleyiniz.");
            return;
        } else if(!isConnectionCompleted) {
            showToastMessage("Baglantiniz henuz tamamlanmadi. Lutfen bekleyiniz.");
            return;
        } else if(!manager.checkNumber(myGuessSending)) {
            showToastMessage("Lutfen kurallara uygun bir sayi secin.");
            return;
        }

        addMove(myGuessSending);
    }

    private void addMove(final String myGuess) {
        manager.addMove(myGuess, deviceId).enqueue(new Callback<ResponseModel>() {
            @Override
            public void onResponse(Response<ResponseModel> response, Retrofit retrofit) {
                if (response.body().getData().split(",")[0].equals("finished")) {
                    manager.endGame(myNumber, false);
                    cancelTimer();
                    activity.getNavigationUtil().switchToEndScreen(response.body().getData().split(",")[1], true);
                } else {
                    manager.setIsMineMove(false);
                    manager.nextMove();
                    String[] myMoveResult = response.body().getData().split(",");
                    String myMove = "Number : " + myGuess + " | +" + myMoveResult[0] + " & " + " - " + myMoveResult[1];
                    updateList(myMove);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                manager.setIsMineMove(true);
                showToastMessage("Islem gerceklesmedi. Lutfen tekrar deneyiniz.");
            }
        });
        startTimer(NEXT_MOVE_TIME);
    }


    @Override
    public void onAuthenticationFailure(String s, Exception e) {
        hideLoadingWidget();
        Log.d("Sercan", "onAuthenticationFailure : " + s + " Exception : " + e.toString());
    }

    @Override
    public void onSubscriptionSucceeded(String s) {
        isConnectionCompleted = true;
        if(!isResumingGame) {
            Call<ResponseModel> objectCall = activity.getMatchmakingService().startGame(activity.getSocketName(), deviceId, myNumber);
            objectCall.enqueue(new Callback<ResponseModel>() {
                @Override
                public void onResponse(Response<ResponseModel> response, Retrofit retrofit) {
                    if (response.body().getData() == null) {
                        showToastMessage("Sayi gonderilemedi. Tekrar deneyin.");
                    } else if (response.body().getData().equals("Start")) {
                        startTimer(NEXT_MOVE_TIME);
                        manager.setIsConnectedOpponent(true);
                        showToastMessage("Oyun basladi. Sira karsi oyuncuda.");
                        manager.startGame();
                    }
                    stopLoadingWidget();
                }

                @Override
                public void onFailure(Throwable t) {
                    hideLoadingWidget();
                }
            });
        } else {
            prepareTime();
            stopLoadingWidget();
            manager.reconnect();
        }
    }

    private void stopLoadingWidget() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.getLoadingWidget().stop();
            }
        });
    }

    @Override
    public void onEvent(String s, String s1, String s2) {
        Log.d("Sercan", "onEvent : " + s2);
        JSONObject event;
        try {
            event = new JSONObject(s2);
            long MY_MOVE_TIME = 60000;
            if(event.has("startGame") && event.getString("startGame").equals("start")){
                manager.setIsConnectedOpponent(true);
                manager.setIsMineMove(true);
                showToastMessage("Rakibiniz baglandi. Simdi baslayabilirsiniz.");
                startTimer(MY_MOVE_TIME);
            } else if(event.has("nextMove") && event.getString("nextMove").equals("move")){
                startTimer(MY_MOVE_TIME);
                showToastMessage("Sira sizde. Lutfen hamlenizi yapin.");
                manager.setIsMineMove(true);
            } else if(event.has("endGame") && event.getString("endGame").equals("end")){
                manager.unsubscribeChannel();
                cancelTimer();
                activity.getNavigationUtil().switchToEndScreen(event.getString("opponentNumber"), event.getBoolean("isWin"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void updateList(String myMove) {
        moveList.add(myMove);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                moveListView.invalidateViews();
            }
        });
    }

    public void updateList(String[] myMoves, String[] myMovesResults) {
        for (int i = 1; i < myMoves.length; i++) {
            String myMove = "Number " + myMoves[i] + " | +" + myMovesResults[(i*2) - 1] + " & " + " - " + myMovesResults[i*2];
            moveList.add(myMove);
        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                moveListView.invalidateViews();
            }
        });
    }
}