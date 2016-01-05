package com.multigames.numbergame.Fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.multigames.numbergame.NumberGameActivity;
import com.multigames.numbergame.R;
import com.plattysoft.leonids.ParticleSystem;
import com.plattysoft.leonids.modifiers.AlphaModifier;
import com.plattysoft.leonids.modifiers.ScaleModifier;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class EndScreen extends Fragment{

    private final String cause;
    @Bind(R.id.resultText)      TextView resultTextView;
    @Bind(R.id.opponentNumber)  TextView opponentNumberTextView;

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
        if(!isWin) {
            startDustAnimation();
        } else {
            startConfettiAnimation();
        }
        super.onActivityCreated(savedInstanceState);
    }

    private void startDustAnimation() {
        new ParticleSystem(getActivity(), 4, R.drawable.dust, 5000)
                .setSpeedByComponentsRange(-0.025f, 0.025f, -0.06f, -0.08f)
                .setAcceleration(0.00001f, 30)
                .setInitialRotationRange(0, 360)
                .addModifier(new AlphaModifier(255, 0, 1000, 3000))
                .addModifier(new ScaleModifier(0.5f, 2f, 0, 1000))
                .oneShot(getActivity().findViewById(R.id.resultText), 4);
    }

    private void startConfettiAnimation() {
        ParticleSystem ps = new ParticleSystem(getActivity(), 100, R.drawable.star, 5000);
        ps.setScaleRange(0.7f, 1.3f);
        ps.setSpeedModuleAndAngleRange(0.07f, 0.16f, 0, 180);
        ps.setRotationSpeedRange(90, 180);
        ps.setAcceleration(0.00013f, 90);
        ps.setFadeOut(200, new AccelerateInterpolator());
        ps.emit(getActivity().findViewById(R.id.resultText), 100, 2000);
    }


    private void prepareViews() {
        opponentNumberTextView.setText(opponentNumberText);
        resultTextView.setText(isWin ? "Kazandınız" : "Kaybettiniz");

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
