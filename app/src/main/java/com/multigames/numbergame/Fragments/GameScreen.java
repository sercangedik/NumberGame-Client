package com.multigames.numbergame.Fragments;

import android.app.Dialog;
import android.app.Fragment;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.multigames.numbergame.Controllers.GameScreenManager;
import com.multigames.numbergame.Model.GameDataModel;
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
    @Bind(R.id.textFirst)   TextView textFirst;
    @Bind(R.id.textSecond)  TextView textSecond;
    @Bind(R.id.textThird)   TextView textThird;
    @Bind(R.id.textFourth)  TextView textFourth;
    @Bind(R.id.timer)       TextView timerView;

    private int firstNumber;
    private int secondNumber;
    private int thirdNumber;
    private int fourthNumber;

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

    public GameScreen(GameModel gameModel, Pusher pusher, MatchmakingImpl matchmaking) {
        this.gameModel = gameModel;
        manager = new GameScreenManager(matchmaking, gameModel.getSocketName(), pusher);
        isResumingGame = true;
        manager.setIsConnectedOpponent(true);
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
                        timerView.setText("Kalan Sure : " + String.valueOf(millisUntilFinished / 1000));
                    }

                    @Override
                    public void onFinish() {
                        Log.d("sercan", String.valueOf(countDown));
                        manager.addMove("", deviceId).enqueue(new Callback<ResponseModel>() {
                            @Override
                            public void onResponse(Response<ResponseModel> response, Retrofit retrofit) {
                                if (response.body().getData().equals("finished")) {
                                    manager.endGame();
                                    showToastMessage("Oyunuz kazandiniz.");
                                    //TODO oyunu kazandim
                                } else {
                                    manager.nextMove();
                                    String[] myMoveResult = response.body().getData().split(",");
                                    String myMove = "Number : " + "" + " | +" + myMoveResult[0] + " & " + " - " + myMoveResult[1];
                                    updateList(myMove);
                                }
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                manager.setIsMineMove(true);
                                showToastMessage("Islem gerceklesmedi. Lutfen tekrar deneyiniz.");
                            }
                        });
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

    private void prepareGame() {
        if(isResumingGame) {
            manager.subscribeChannel(this);
            yourNumber.setText(gameModel.getGameDataModel().getMyNumber());
            prepareTime();
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
        long countDown = Long.parseLong(gameModel.getGameDataModel().getMoveEndTime()) - (System.currentTimeMillis() / 1000);
        startTimer(countDown);
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

    public boolean isResumingGame() {
        return isResumingGame;
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
        if(!manager.isConnectedOpponent()){
            showToastMessage("Rakibiniz bekleniyor...");
            return;
        } else if(!manager.isMineMove()) {
            showToastMessage("Sira sizde degil. Lutfen bekleyiniz.");
            return;
        } else if(!isConnectionCompleted) {
            showToastMessage("Baglantiniz henuz tamamlanmadi. Lutfen bekleyiniz.");
            return;
        }

        final String myGuess = String.valueOf(firstNumber) +  String.valueOf(secondNumber) + String.valueOf(thirdNumber) + String.valueOf(fourthNumber);
        manager.addMove(myGuess, deviceId).enqueue(new Callback<ResponseModel>() {
            @Override
            public void onResponse(Response<ResponseModel> response, Retrofit retrofit) {
                if (response.body().getData().equals("finished")) {
                    manager.endGame();
                    showToastMessage("Oyunuz kazandiniz.");
                    //TODO oyunu kazandim
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
        startTimer(62000);
    }

    @OnClick(R.id.buttonFirstUp) void clickButtonFirstUp(){
        firstNumber++;
        calculateFirstNumberForUp();

        if (firstNumber > 9) {
            firstNumber = firstNumber % 10;
        }

        calculateFirstNumberForUp();
        textFirst.setText(String.valueOf(firstNumber));
    }

    private void calculateFirstNumberForUp() {
        while (secondNumber == firstNumber || thirdNumber == firstNumber || fourthNumber == firstNumber) {
            firstNumber++;
            if (firstNumber > 9) {
                firstNumber = firstNumber % 10;
            }
        }
    }

    @OnClick(R.id.buttonFirstDown) void clickButtonFirstDown(){
        firstNumber--;

        if (firstNumber < 0) {
            firstNumber = 9;
        }

        calculateFirstNumberForDown();
        textFirst.setText(String.valueOf(firstNumber));
    }

    private void calculateFirstNumberForDown() {
        while (secondNumber == firstNumber || thirdNumber == firstNumber || fourthNumber == firstNumber) {
            firstNumber--;
            if (firstNumber < 0) {
                firstNumber = 9;
            }
        }
    }

    @OnClick(R.id.buttonSecondUp) void clickButtonSecondUp(){
        secondNumber++;
        calculateSecondNumberForUp();

        if (secondNumber > 9) {
            secondNumber = secondNumber % 10;
        }

        calculateSecondNumberForUp();
        textSecond.setText(String.valueOf(secondNumber));
    }

    private void calculateSecondNumberForUp() {
        while (secondNumber == firstNumber || thirdNumber == secondNumber || fourthNumber == secondNumber) {
            secondNumber++;
            if (secondNumber > 9) {
                secondNumber = secondNumber % 10;
            }
        }
    }

    @OnClick(R.id.buttonSecondDown) void clickButtonSecondDown(){
        secondNumber--;

        if (secondNumber < 0) {
            secondNumber = 9;
        }

        calculateSecondNumberForDown();
        textSecond.setText(String.valueOf(secondNumber));
    }

    private void calculateSecondNumberForDown() {
        while (secondNumber == firstNumber || thirdNumber == secondNumber || fourthNumber == secondNumber) {
            secondNumber--;
            if (secondNumber < 0) {
                secondNumber = 9;
            }
        }
    }

    @OnClick(R.id.buttonThirdUp) void clickButtonThirdUp(){
        thirdNumber++;
        calculateThirdNumberForUp();

        if (thirdNumber > 9) {
            thirdNumber = thirdNumber % 10;
        }

        calculateThirdNumberForUp();
        textThird.setText(String.valueOf(thirdNumber));
    }

    private void calculateThirdNumberForUp() {
        while (secondNumber == thirdNumber || thirdNumber == firstNumber || fourthNumber == thirdNumber) {
            thirdNumber++;
            if (thirdNumber > 9) {
                thirdNumber = thirdNumber % 10;
            }
        }
    }

    @OnClick(R.id.buttonThirdDown) void clickButtonThirdDown(){
        thirdNumber--;

        if (thirdNumber < 0) {
            thirdNumber = 9;
        }

        calculateThirdNumberForDown();
        textThird.setText(String.valueOf(thirdNumber));
    }

    private void calculateThirdNumberForDown() {
        while (secondNumber == thirdNumber || thirdNumber == firstNumber || fourthNumber == thirdNumber) {
            thirdNumber--;
            if (thirdNumber < 0) {
                thirdNumber = 9;
            }
        }
    }

    @OnClick(R.id.buttonFourthUp) void clickButtonFourthUp(){
        fourthNumber++;
        calculateFourthNumberForUp();

        if (fourthNumber > 9) {
            fourthNumber = fourthNumber % 10;
        }
        calculateFourthNumberForUp();
        textFourth.setText(String.valueOf(fourthNumber));
    }

    private void calculateFourthNumberForUp() {
        while (secondNumber == fourthNumber || thirdNumber == fourthNumber || fourthNumber == firstNumber) {
            fourthNumber++;
            if (fourthNumber > 9) {
                fourthNumber = fourthNumber % 10;
            }
        }
    }

    @OnClick(R.id.buttonFourthDown) void clickButtonFourthDown(){
        fourthNumber--;

        if (fourthNumber < 0) {
            fourthNumber = 9;
        }

        calculateFourtNumberForDown();
        textFourth.setText(String.valueOf(fourthNumber));
    }

    private void calculateFourtNumberForDown() {
        while (secondNumber == fourthNumber || thirdNumber == fourthNumber || fourthNumber == firstNumber) {
            fourthNumber--;
            if (fourthNumber < 0) {
                fourthNumber = 9;
            }
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
                        startTimer(62000);
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
            if(event.has("startGame") && event.getString("startGame").equals("start")){
                manager.setIsConnectedOpponent(true);
                manager.setIsMineMove(true);
                showToastMessage("Rakibiniz baglandi. Simdi baslayabilirsiniz.");
                startTimer(62000);
            } else if(event.has("nextMove") && event.getString("nextMove").equals("move")){
                startTimer(60000);
                showToastMessage("Sira sizde. Lutfen hamlenizi yapin.");
                manager.setIsMineMove(true);
            } else if(event.has("endGame") && event.getString("endGame").equals("end")){
                showToastMessage("Oyunu kaybettiniz.");
                //TODO oyunu kaybettim.
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}