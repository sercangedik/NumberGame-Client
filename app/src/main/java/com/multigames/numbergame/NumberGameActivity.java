package com.multigames.numbergame;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.multigames.numbergame.Fragments.GameScreen;
import com.multigames.numbergame.Network.MatchmakingImpl;
import com.multigames.numbergame.Util.LoadingWidgetManager;
import com.multigames.numbergame.Util.NavigationUtil;
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;
import com.pusher.client.util.HttpAuthorizer;
import com.squareup.leakcanary.LeakCanary;

import butterknife.Bind;
import butterknife.ButterKnife;
import io.fabric.sdk.android.Fabric;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class NumberGameActivity extends Activity implements ConnectionEventListener {

    @Bind(R.id.loading_widget) ProgressBar loadingView;

    private NavigationUtil navigationUtil;
    private MatchmakingImpl matchmakingService;

    public interface PusherListener {
        void connected();
    }

    private Pusher pusher;
    private String socketName;
    private boolean isConnected = false;
    private static final String API_KEY = "148b47efc8d65594d5c9";
    private PusherListener pusherListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeakCanary.install(getApplication());

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Cronosd.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build());

                Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        pusherConnect();
        matchmakingService = new MatchmakingImpl();
        navigationUtil = new NavigationUtil(this);
        navigationUtil.switchToHomeScreen(matchmakingService);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    public ProgressBar getLoadingView() {
        return loadingView;
    }

    public int getFragmentContainerId() {
        return R.id.container;
    }

    public NavigationUtil getNavigationUtil() {
        return navigationUtil;
    }

    public MatchmakingImpl getMatchmakingService() {
        return matchmakingService;
    }

    public LoadingWidgetManager getLoadingWidget(){
        return navigationUtil.getLoadingWidgetManager();
    }

    private void showToastMessage(final String message) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onConnectionStateChange(ConnectionStateChange connectionStateChange) {
        final ConnectionState newState = connectionStateChange.getCurrentState();

        if (newState == ConnectionState.CONNECTED) {
            isConnected = true;
            if(pusherListener != null) {
                pusherListener.connected();
            }
        }

        if (newState == ConnectionState.DISCONNECTED) {
            if(getNavigationUtil().getCurrentScreenName().equals(NavigationUtil.GAME_SCREEN)){
                showToastMessage("Baglanti sorunu yasadiniz. Lutfen bekleyiniz. Otomatik olarak oyuna yonlendirileceksiniz.");
                navigationUtil.switchToHomeScreen(matchmakingService);
            }

            isConnected = false;
            pusher.connect(this);
        }
    }

    @Override
    public void onError(String s, String s1, Exception e) {

    }

    private void pusherConnect() {
        if (pusher != null) {
            pusher.disconnect();
        }
        HttpAuthorizer authorizer = new HttpAuthorizer("http://www.sercangedik.com/airhockeybackend/pusher-auth.php");
        PusherOptions options = new PusherOptions().setAuthorizer(authorizer);
        options.setEncrypted(true);
        pusher = new Pusher(API_KEY, options);
        pusher.connect(this);
    }

    public String getSocketName() {
        return socketName;
    }

    public void setSocketName(String socketName) {
        this.socketName = socketName;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public Pusher getPusher() {
        return pusher;
    }

    public void setPusherListener(PusherListener pusherListener) {
        this.pusherListener = pusherListener;
    }

    @Override
    public void onBackPressed() {
        if(getNavigationUtil().getCurrentScreenName().equals(NavigationUtil.GAME_SCREEN) && !getLoadingWidget().isActive()){
            ((GameScreen)getFragmentManager().findFragmentByTag(NavigationUtil.GAME_SCREEN)).showQuitDialogBox();
        }

    }
}

