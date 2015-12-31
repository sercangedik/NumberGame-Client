package com.multigames.numbergame.Controllers;

import android.util.Log;
import com.multigames.numbergame.Model.ResponseModel;
import com.multigames.numbergame.Network.MatchmakingImpl;
import com.pusher.client.Pusher;
import com.pusher.client.channel.PrivateChannel;
import com.pusher.client.channel.PrivateChannelEventListener;
import com.pusher.client.connection.ConnectionState;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit.Call;


public class GameScreenManager {

    private final String socketName;
    private Pusher pusher;
    private PrivateChannel pusherChannel;
    private boolean isConnectedOpponent;
    private boolean isMineMove;
    private MatchmakingImpl matchmakingService;

    public GameScreenManager(MatchmakingImpl matchmakingService, String socketName, Pusher pusher) {
        this.matchmakingService = matchmakingService;
        this.pusher = pusher;
        this.socketName = socketName;
        isConnectedOpponent = false;
        isMineMove = false;
    }

    public void setIsConnectedOpponent(boolean isConnectedOpponent) {
        this.isConnectedOpponent = isConnectedOpponent;
    }

    public void subscribeChannel(final PrivateChannelEventListener listener) {
        pusherChannel = pusher.subscribePrivate(socketName, listener, "client-startEvent");
    }

    public Call<ResponseModel> addMove(final String myGuess, final String deviceId){
        return matchmakingService.addMove(deviceId, socketName, myGuess);
    }

    public void startGame(){
        if (pusher == null || pusherChannel == null)
            return;

        if(pusherChannel.isSubscribed()){
            JSONObject opponentResult = new JSONObject();
            try {
                opponentResult.put("startGame", "start");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            pusherChannel.trigger("client-startEvent", opponentResult.toString());
            isMineMove = false;
            isConnectedOpponent = true;
        }
    }

    public void reconnect() {
        if (pusher == null || pusherChannel == null)
            return;

        if(pusherChannel.isSubscribed()){
            JSONObject opponentResult = new JSONObject();
            try {
                opponentResult.put("reconnectGame", "reconnect");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            pusherChannel.trigger("client-startEvent", opponentResult.toString());
        }
    }

    public void destroy() {
        if (pusher.getConnection().getState() != ConnectionState.DISCONNECTED
                && pusher.getConnection().getState() != ConnectionState.DISCONNECTING) {

            Log.d("A", "Disconnecting in preparation for destroy"); // No one is likely to see this
            pusher.disconnect();
        }
    }

    public boolean isConnectedOpponent() {
        return isConnectedOpponent;
    }

    public boolean isMineMove() {
        return isMineMove;
    }

    public void endGame() {
        if (pusher == null || pusherChannel == null)
            return;

        if(pusherChannel.isSubscribed()){
            JSONObject opponentResult = new JSONObject();
            try {
                opponentResult.put("endGame", "end");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            pusherChannel.trigger("client-startEvent", opponentResult.toString());
            isMineMove = false;
        }
    }

    public void setIsMineMove(boolean isMineMove) {
        this.isMineMove = isMineMove;
    }

    public void nextMove(){
        if (pusher == null || pusherChannel == null)
            return;

        if(pusherChannel.isSubscribed()){
            JSONObject opponentResult = new JSONObject();
            try {
                opponentResult.put("nextMove", "move");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            pusherChannel.trigger("client-startEvent", opponentResult.toString());
            isMineMove = false;
        }
    }

    public void unsubscribeChannel(){
        if (pusher == null || pusherChannel == null)
            return;

        if(pusherChannel.isSubscribed()){
            pusher.unsubscribe(socketName);
        }
    }

    public boolean checkNumber(String playerNumber) {
        if(playerNumber.length() < 4) {
            return false;
        }

        String[] playerNumberArray = playerNumber.split("");

        for (int i = 1; i < playerNumberArray.length; i++) {
            for (int j = i + 1; j < playerNumberArray.length; j++) {
                if(playerNumberArray[i].equals(playerNumberArray[j])){
                    return false;
                }
            }
        }
        return true;
    }

}
