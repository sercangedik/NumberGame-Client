package com.multigames.numbergame.Fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.akexorcist.roundcornerprogressbar.RoundCornerProgressBar;
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
import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;


public class GameScreen extends Fragment implements PrivateChannelEventListener {

    public static final String BLANK_GUESS = "xxxx";
    @Bind(R.id.yourNumber)      TextView yourNumber;
    @Bind(R.id.timer)           RoundCornerProgressBar timerView;
    @Bind(R.id.myGuess)         EditText myGuessEditText;
    @Bind(R.id.numPad)          View     numPad;
    @Bind(R.id.turnDetailText)  TextView turnDetailText;

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
    private final long MY_MOVE_TIME = 60000;

    public GameScreen(GameModel gameModel, Pusher pusher, MatchmakingImpl matchmaking) {
        this.gameModel = gameModel;
        myNumber = gameModel.getGameDataModel().getMyNumber();
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

    public void setTurnDetail(final boolean isMyTurn) {
        if(getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    turnDetailText.setText(isMyTurn ? "Sira Sizde" : "Sira Karsida");
                }
            });
        }
    }

    public void startTimer(final long countDown) {
        long time = countDown;
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
                        timerView.setProgress(remainingTime);
                    }

                    @Override
                    public void onFinish() {
                        if(timerView.getProgress() <= 1) {
                            if(!manager.isMineMove()) {

                                manager.endMatch(deviceId).enqueue(new Callback<ResponseModel>() {
                                    @Override
                                    public void onResponse(Response<ResponseModel> response, Retrofit retrofit) {
                                        manager.endGame(myNumber, false);
                                        cancelTimer();
                                        activity.getNavigationUtil().switchToEndScreen(response.body().getData(), true, "Rakibiniz zamaninda hamlesini yapmadi. Oyunuz kazandiniz.");
                                    }

                                    @Override
                                    public void onFailure(Throwable t) {

                                    }
                                });
                            } else {
                                addMove(BLANK_GUESS);
                            }
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
        stopLoadingWidget();
        prepareQuitDialogBox();
        timerView.setMax(60);
        timerView.setRadius(5);
        timerView.setProgressBackgroundColor(getResources().getColor(R.color.progressBackground));
        timerView.setProgressColor(getResources().getColor(R.color.progressMain));
        List<View> touchables = numPad.getTouchables();
        for (View touchable : touchables)
        {
            touchable.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onButtonClick(v);
                }
            });
        }
    }

    public void onButtonClick(View view)
    {
        Button button = (Button) view;
        String btnStr = button.getText().toString();
        if (btnStr.equals("GONDER")) {
            clickSend();
        }
        else if (btnStr.equals(getString(R.string.button_backspace))) {
            String myGuessString = myGuessEditText.getText().toString();
            if(myGuessString.length() > 0) {
                myGuessEditText.setText(myGuessString.substring(0, myGuessString.length() - 1));
            }
        }
        else {
            if(myGuessEditText.length() < 4) {
                myGuessEditText.setText(myGuessEditText.getText().toString() + btnStr);
            }
        }
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
                                    cancelTimer();
                                    activity.getNavigationUtil().switchToEndScreen(response.body().getData(), false, "Oyundan kendi isteginizle ciktiginiz icin kaybettiniz.");
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

        myGuessEditText.setEnabled(false);
    }

    private void updateListWithMoves() {
        if(gameModel.getGameDataModel().getMyMoves() != null && gameModel.getGameDataModel().getPlayerMoveResults() != null) {
            updateList(gameModel.getGameDataModel().getMyMoves().split(","), gameModel.getGameDataModel().getPlayerMoveResults().split(","));
        }
    }

    private void prepareWhoIsMove() {
        setMove(deviceId.equals(gameModel.getGameDataModel().getCurrentMoveId()));
    }

    private void prepareTime() {
        String moveEndTime = gameModel.getGameDataModel().getMoveEndTime();
        if(moveEndTime == null) {
            return;
        }
        long countDown = Long.parseLong(moveEndTime) - (System.currentTimeMillis() / 1000);
        if(countDown < 0) {
            addMove(BLANK_GUESS);
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
        activity.getLoadingWidget().start();
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

        final EditText textView = (EditText)chooseNumberDialog.findViewById(R.id.text);
        textView.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager keyboard = (InputMethodManager)
                        getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.showSoftInput(textView, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);

    }

    public void showToastMessage(final String message) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroy() {
        manager.unsubscribeChannel();
        super.onDestroy();
    }

    private void clickSend(){
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

        myGuessEditText.setText("");
        addMove(myGuessSending);
    }

    private void addMove(final String myGuess) {
        startLoadingWidget();
        manager.sendMyGuessToOpponent(myGuess);
        manager.addMove(myGuess, deviceId).enqueue(new Callback<ResponseModel>() {
            @Override
            public void onResponse(Response<ResponseModel> response, Retrofit retrofit) {

                stopLoadingWidget();
                if (response.body().getData().split(",")[0].equals("finished")) {
                    manager.endGame(myNumber, false);
                    cancelTimer();
                    activity.getNavigationUtil().switchToEndScreen(response.body().getData().split(",")[1], true, "Sayiyi Bildiniz !");
                } else {
                    setMove(false);
                    manager.nextMove();
                    String[] myMoveResult = response.body().getData().split(",");
                    String myMove = "Tahmin : " + myGuess + " Cevap : +" + myMoveResult[0] + " ______ " + " -" + myMoveResult[1];
                    updateList(myMove);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                stopLoadingWidget();
                setMove(true);
                showToastMessage("Islem gerceklesmedi. Lutfen tekrar deneyiniz.");
            }
        });
        startTimer(NEXT_MOVE_TIME);
    }

    private void setMove(boolean isMyMove) {
        setTurnDetail(isMyMove);
        manager.setIsMineMove(isMyMove);
    }

    @Override
    public void onAuthenticationFailure(String s, Exception e) {
        stopLoadingWidget();
        Log.d("Sercan", "onAuthenticationFailure : " + s + " Exception : " + e.toString());
    }

    @Override
    public void onSubscriptionSucceeded(String s) {
        isConnectionCompleted = true;
        manager.opponentConnected();
        if(!isResumingGame) {
            Call<ResponseModel> objectCall = activity.getMatchmakingService().startGame(activity.getSocketName(), deviceId, myNumber);
            objectCall.enqueue(new Callback<ResponseModel>() {
                @Override
                public void onResponse(Response<ResponseModel> response, Retrofit retrofit) {
                    if (response.body().getData() == null) {
                        showToastMessage("Sayi gonderilemedi. Tekrar deneyin.");
                        chooseNumberDialog.show();
                    } else if (response.body().getData().equals("Start")) {
                        startTimer(NEXT_MOVE_TIME);
                        manager.setIsConnectedOpponent(true);
                        setMove(false);
                        showToastMessage("Oyun basladi. Sira karsi oyuncuda.");
                        manager.startGame();
                    } else if (response.body().getData().equals("Ok")) {
                        showToastMessage("Sayiniz gonderildi. Karsidaki oyuncunun katilmasi bekleniyor.");
                    }
                    stopLoadingWidget();
                }

                @Override
                public void onFailure(Throwable t) {
                    showToastMessage("Sayi gonderilemedi. Tekrar deneyin.");
                    stopLoadingWidget();
                    if(myNumber.length() == 0) {
                        chooseNumberDialog.show();
                    }
                }
            });
        } else {
            prepareTime();
            stopLoadingWidget();
            manager.reconnect();
        }
    }

    private void stopLoadingWidget() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.getLoadingWidget().stop();
            }
        });
    }

    private void startLoadingWidget() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.getLoadingWidget().start();
            }
        });
    }

    @Override
    public void onEvent(String s, String s1, String s2) {
        Log.d("Sercan", "onEvent : " + s2);
        JSONObject event;
        try {
            event = new JSONObject(s2);

            if(event.has("startGame") && event.getString("startGame").equals("start")){
                manager.setIsConnectedOpponent(true);
                setMove(true);
                showToastMessage("Sira sizde. Simdi baslayabilirsiniz.");
                startTimer(MY_MOVE_TIME);
            } else if(event.has("nextMove") && event.getString("nextMove").equals("move")){
                startTimer(MY_MOVE_TIME);
                showToastMessage("Sira sizde. Lutfen hamlenizi yapin.");
                setMove(true);
            } else if(event.has("endGame") && event.getString("endGame").equals("end")){
                manager.unsubscribeChannel();
                cancelTimer();
                activity.getNavigationUtil().switchToEndScreen(event.getString("opponentNumber"), event.getBoolean("isWin"), null);
            } else if(event.has("opponentConnected")) {
                showToastMessage("Rakibiniz baglandi.");
            } else if(event.has("guessNumber")) {
                showToastMessage("Rakibinizin hamlesi : " + event.getString("guessNumber"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void updateList(String myMove) {
        if(moveList != null) {
            moveList.add(myMove);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    moveListView.invalidateViews();
                }
            });
        }
    }

    public void updateList(String[] myMoves, String[] myMovesResults) {
        for (int i = 1; i < myMoves.length; i++) {
            String myMove = "Tahmin : " + myMoves[i] + " Cevap : +" + myMovesResults[(i*2) - 1] + " ______ " + " -" + myMovesResults[i*2];
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