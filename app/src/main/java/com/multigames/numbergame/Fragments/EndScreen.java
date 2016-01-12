package com.multigames.numbergame.Fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.multigames.numbergame.NumberGameActivity;
import com.multigames.numbergame.R;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class EndScreen extends Fragment{

    private final String cause;
    @Bind(R.id.resultText)      TextView resultTextView;
    @Bind(R.id.opponentNumber)  TextView opponentNumberTextView;
    @Bind(R.id.textView)        TextView opponentNumber;
    @Bind(R.id.backButton)      Button backButton;

    private String opponentNumberText;
    private boolean isWin;

    private NumberGameActivity activity;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.end_screen, container, false);
        ButterKnife.bind(this, view);
        activity = (NumberGameActivity)getActivity();
        prepareViews();


        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private void prepareViews() {
        opponentNumberTextView.setText(opponentNumberText);
        backButton.setText(getString(R.string.home_screen));
        resultTextView.setText(isWin ? getString(R.string.win) : getString(R.string.lose));
        opponentNumber.setText(getString(R.string.opponent_number));
        if(cause != null) {
            showToastMessage(cause);
        }
    }

    private void showToastMessage(String cause) {
        Toast.makeText(getActivity(), cause, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        YoYo.with(Techniques.Wobble)
                .duration(700)
                .playOn(opponentNumberTextView);

        YoYo.with(Techniques.FadeIn)
                .duration(700)
                .playOn(resultTextView);

        super.onViewCreated(view, savedInstanceState);
    }

    public EndScreen(String opponentNumberText, boolean isWin, String cause) {
        this.opponentNumberText = opponentNumberText;
        this.isWin = isWin;
        this.cause = cause;
    }

    @OnClick(R.id.backButton)
    void back() {
        activity.getNavigationUtil().switchToHomeScreen(activity.getMatchmakingService());
    }
}
