package com.multigames.numbergame.Util;

import android.os.CountDownTimer;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;

import com.multigames.numbergame.NumberGameActivity;

public class LoadingWidgetManager{

    private ProgressBar progressBar;
    private int timeOut = 10000;
    private CountDownTimer countDownTimer;
    private NumberGameActivity activity;

    public LoadingWidgetManager(NumberGameActivity activity) {
        this.activity = activity;
        this.progressBar = activity.getProgressBar();
    }

    public void start() {
        countDownTimer = new CountDownTimer(timeOut, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                stop();
            }
        }.start();
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        progressBar.setVisibility(View.VISIBLE);
    }

    public void stop() {
        if(countDownTimer == null) {
            return;
        }
        progressBar.setVisibility(View.GONE);
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        countDownTimer.cancel();
    }

    public void startWithTimeOut(int timeOut) {
        this.timeOut = timeOut;
        start();
    }
}
