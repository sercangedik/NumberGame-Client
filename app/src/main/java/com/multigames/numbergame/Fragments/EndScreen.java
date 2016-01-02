package com.multigames.numbergame.Fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.multigames.numbergame.NumberGameActivity;
import com.multigames.numbergame.R;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class EndScreen extends Fragment{

    @Bind(R.id.resultText)      TextView resultTextView;
    @Bind(R.id.opponentNumber)  TextView opponentNumberTextView;

    private String opponentNumberText;
    private boolean isWin;

    private NumberGameActivity activity;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.end_screen, container, false);
        ButterKnife.bind(this, view);
        activity = (NumberGameActivity)getActivity();

        opponentNumberTextView.setText(opponentNumberText);
        resultTextView.setText(isWin ? "Kazandınız" : "Kaybettiniz");

        return view;
    }

    public EndScreen(String opponentNumberText, boolean isWin) {
        this.opponentNumberText = opponentNumberText;
        this.isWin = isWin;
    }

    @OnClick(R.id.backButton)
    void back() {
        activity.getNavigationUtil().switchToHomeScreen(activity.getMatchmakingService());
    }
}
