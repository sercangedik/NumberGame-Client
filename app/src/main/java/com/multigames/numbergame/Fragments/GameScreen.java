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
    @Bind(R.id.yourNumberText)  TextView yourNumberText;

    private ListView moveListView;
    private List<String> moveList;
    private Dialog chooseNumberDialog;
    private String myNumber;
    private String deviceId;
    private final GameScreenManager manager;
    private final GameModel gameModel;
    private boolean isResumingGame;
    private CountDownTimer timer;
    private CountDownTimer timerForBot;
    private NumberGameActivity activity;
    private boolean isConnectionCompleted;
    private DialogInterface.OnClickListener dialogQuitListener;
    private boolean isBotGame;

    private final long NEXT_MOVE_TIME = 65000;
    private final long MY_MOVE_TIME = 60000;
    private final long BOT_MOVE_TIME = 5000;
    private final long BOT_JOIN_TIME = 5000;

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
                    turnDetailText.setText(isMyTurn ? getString(R.string.your_move) : getString(R.string.opponent_move));
                }
            });
        }
    }

    public void startTimerForBot(final long botTime) {
        cancelTimerForBot();
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                timerForBot = new CountDownTimer(botTime, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        if(isBotGame) {
                            long remainingTime = (millisUntilFinished / 1000);
                            timerView.setProgress(54 + remainingTime);
                        }
                    }

                    @Override
                    public void onFinish() {
                        if(!isBotGame) {
                            prepareGameForBot();
                            manager.unsubscribeChannel();
                        } else {
                            if(manager.getBotMoves().size() == moveList.size()) {
                                activity.getNavigationUtil().switchToEndScreen(manager.getBotNumber(), false, null);
                            } else {
                                showToastMessage(getString(R.string.opponent_guess) + manager.getBotMoves().get(moveList.size() - 1));
                                startTimer(MY_MOVE_TIME);
                                showToastMessage(getString(R.string.your_move));
                                setMove(true);
                            }
                        }
                    }
                }.start();
            }
        });
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
                                        activity.getNavigationUtil().switchToEndScreen(response.body().getData(), true, getString(R.string.win_opponent_not_move));
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
                if (timer != null) {
                    timer.cancel();
                    timer = null;
                }
            }
        });
    }

    private void stopTimer() {
        if(timer != null) {
            timer.cancel();
        }
    }

    private void cancelTimerForBot() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (timerForBot != null) {
                    timerForBot.cancel();
                    timerForBot = null;
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
        yourNumberText.setText(getString(R.string.your_number));
        myGuessEditText.setHint(getString(R.string.my_guess));
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
        if (btnStr.equals(getString(R.string.send))) {
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
                                    cancelTimerForBot();
                                    activity.getNavigationUtil().switchToEndScreen(isBotGame ? manager.getBotNumber() : response.body().getData(), false, getString(R.string.lose_quit));
                                }

                                @Override
                                public void onFailure(Throwable t) {

                                }
                            });
                        } else {
                            manager.cancelGame(deviceId).enqueue(new Callback<ResponseModel>() {
                                @Override
                                public void onResponse(Response<ResponseModel> response, Retrofit retrofit) {
                                    showToastMessage(getString(R.string.quit_game));
                                    cancelTimer();
                                    cancelTimerForBot();
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
        builder.setMessage(getString(R.string.quit_game_question)).setPositiveButton(getString(R.string.yes), dialogQuitListener)
                .setNegativeButton(getString(R.string.no), dialogQuitListener).show();

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
                    showToastMessage(getString(R.string.choose_valid_number));
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
        chooseNumberDialog.setTitle(getString(R.string.choose_number));
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
            showToastMessage(getString(R.string.opponent_waiting));
            return;
        } else if(!manager.isMineMove()) {
            showToastMessage(getString(R.string.not_your_turn));
            return;
        } else if(!isConnectionCompleted) {
            showToastMessage(getString(R.string.connection_not_yet));
            return;
        } else if(!manager.checkNumber(myGuessSending)) {
            showToastMessage(getString(R.string.choose_valid_number));
            return;
        } else if(isSendAlready(myGuessSending)) {
            showToastMessage(getString(R.string.already_send));
            return;
        }

        myGuessEditText.setText("");

        if(isBotGame) {
            addMoveForBot(myGuessSending);
        } else {
            addMove(myGuessSending);
        }
    }

    private boolean isSendAlready(String guessNumber){
        for (int i = 0; i < moveList.size(); i++) {
            if(moveList.get(i).contains(guessNumber)){
                return true;
            }
        }

        return false;
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
                    activity.getNavigationUtil().switchToEndScreen(response.body().getData().split(",")[1], true, getString(R.string.correct_number));
                } else {
                    setMove(false);
                    manager.nextMove();
                    String[] myMoveResult = response.body().getData().split(",");
                    String myMove = getString(R.string.guess) + myGuess + "   " + getString(R.string.answer) + "   +" + myMoveResult[0] + " ______ " + " -" + myMoveResult[1];
                    updateList(myMove);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                stopLoadingWidget();
                setMove(true);
                showToastMessage(getString(R.string.move_fail));
            }
        });
        startTimer(NEXT_MOVE_TIME);
    }

    private void addMoveForBot(final String myGuess) {
        setMove(false);
        stopTimer();
        String[] myMoveResult = manager.getAnswerForBot(myGuess).split(",");
        if(myMoveResult[0].equals("4")) {
            activity.getNavigationUtil().switchToEndScreen(manager.getBotNumber(), true, null);
        } else {
            String myMove = getString(R.string.guess) + myGuess + "   " + getString(R.string.answer) + "   +" + myMoveResult[0] + " ______ " + " -" + myMoveResult[1];
            updateList(myMove);

            startTimerForBot(BOT_MOVE_TIME);
        }
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
                        showToastMessage(getString(R.string.number_fail));
                        chooseNumberDialog.show();
                    } else if (response.body().getData().equals("Start")) {
                        cancelTimerForBot();
                        startTimer(NEXT_MOVE_TIME);
                        manager.setIsConnectedOpponent(true);
                        setMove(false);
                        showToastMessage(getString(R.string.game_start_opponent));
                        manager.startGame();
                    } else if (response.body().getData().equals("Ok")) {
                        startTimerForBot(BOT_JOIN_TIME);
                        showToastMessage(getString(R.string.game_waiting_opponent));
                    }
                    stopLoadingWidget();
                }

                @Override
                public void onFailure(Throwable t) {
                    showToastMessage(getString(R.string.number_fail));
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

    private void prepareGameForBot() {
        isBotGame = true;
        manager.setIsConnectedOpponent(true);
        manager.generateBotNumber(myNumber);
        setMove(true);
        showToastMessage(getString(R.string.game_start_mine));
        startTimer(MY_MOVE_TIME);
        manager.addBotGame(deviceId, myNumber).enqueue(new Callback() {
            @Override
            public void onResponse(Response response, Retrofit retrofit) {

            }

            @Override
            public void onFailure(Throwable t) {

            }
        });


        manager.cancelGame(deviceId).enqueue(new Callback<ResponseModel>() {
            @Override
            public void onResponse(Response<ResponseModel> response, Retrofit retrofit) {

            }

            @Override
            public void onFailure(Throwable t) {

            }
        });
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
                cancelTimerForBot();
                manager.setIsConnectedOpponent(true);
                setMove(true);
                showToastMessage(getString(R.string.game_start_mine));
                startTimer(MY_MOVE_TIME);
            } else if(event.has("nextMove") && event.getString("nextMove").equals("move")){
                startTimer(MY_MOVE_TIME);
                showToastMessage(getString(R.string.your_move));
                setMove(true);
            } else if(event.has("endGame") && event.getString("endGame").equals("end")){
                manager.unsubscribeChannel();
                cancelTimer();
                activity.getNavigationUtil().switchToEndScreen(event.getString("opponentNumber"), event.getBoolean("isWin"), null);
            } else if(event.has("opponentConnected")) {
                cancelTimerForBot();
                showToastMessage(getString(R.string.opponent_connected));
            } else if(event.has("guessNumber")) {
                showToastMessage(getString(R.string.opponent_guess) + event.getString("guessNumber"));
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
            String myMove = getString(R.string.guess) + myMoves[i] + "   " + getString(R.string.answer) + "   +" + myMovesResults[(i*2) - 1] + " ______ " + " -" + myMovesResults[i*2];
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