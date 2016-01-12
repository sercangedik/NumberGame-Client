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

import java.util.ArrayList;
import java.util.Random;

import retrofit.Call;


public class GameScreenManager {

    private final String socketName;
    private Pusher pusher;
    private PrivateChannel pusherChannel;
    private boolean isConnectedOpponent;
    private boolean isMineMove;
    private MatchmakingImpl matchmakingService;
    private String botNumber = new String();
    private int botMoveCount = 0;
    private ArrayList<String> botMoves = new ArrayList<>();

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

    public void opponentConnected() {
        if (pusher == null || pusherChannel == null)
            return;

        if(pusherChannel.isSubscribed()){
            JSONObject opponentResult = new JSONObject();
            try {
                opponentResult.put("opponentConnected", "opponentConnected");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            pusherChannel.trigger("client-startEvent", opponentResult.toString());
        }
    }

    public boolean isConnectedOpponent() {
        return isConnectedOpponent;
    }

    public boolean isMineMove() {
        return isMineMove;
    }

    public Call<ResponseModel> endMatch(String deviceId) {
        return matchmakingService.endGame(socketName, deviceId);
    }

    public void endGame(String myNumber, boolean isWin) {
        if (pusher == null || pusherChannel == null)
            return;

        if(pusherChannel.isSubscribed()){
            JSONObject opponentResult = new JSONObject();
            try {
                opponentResult.put("opponentNumber", myNumber);
                opponentResult.put("isWin", isWin);
                opponentResult.put("endGame", "end");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            pusherChannel.trigger("client-startEvent", opponentResult.toString());
            isMineMove = false;
        }

        unsubscribeChannel();
    }

    public void sendMyGuessToOpponent(String myGuess) {
        if (pusher == null || pusherChannel == null)
            return;

        if(pusherChannel.isSubscribed()){
            JSONObject myGuessJson = new JSONObject();
            try {
                myGuessJson.put("guessNumber", myGuess);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            pusherChannel.trigger("client-startEvent", myGuessJson.toString());
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

    public Call<ResponseModel> cancelGame(String deviceId) {
        unsubscribeChannel();
        return matchmakingService.cancelMatch(socketName, deviceId);
    }

    public void generateBotNumber(String myNumber){
        Random rand = new Random();
        int i = 0;
        int j = 0;

        while(i != 4){
            String tempValue = String.valueOf(rand.nextInt(10));
            if(!botNumber.contains(tempValue)) {
                botNumber += tempValue;
                i++;
            }
        }

        i = 0;
        botMoveCount = rand.nextInt(8) + 8;
        String botMoveTemp = new String();

        while(i != botMoveCount){
            String tempValue = String.valueOf(rand.nextInt(10));

            if(!botMoveTemp.contains(tempValue)) {
                botMoveTemp += tempValue;
                j++;
            }

            if(j == 4) {
                botMoves.add(botMoveTemp);
                botMoveTemp = new String();
                j = 0;
                i++;
            }
        }

        botMoves.add(myNumber);
    }

    public ArrayList<String> getBotMoves() {
        return botMoves;
    }

    public String getBotNumber() {
        return botNumber;
    }

    public String getAnswerForBot(final String myGuess) {
        String myAnswer = new String();
        int plus = 0;
        int minus = 0;

        String[] myGuessArray = myGuess.split("(?!^)");
        String[] botNumberArray = botNumber.split("(?!^)");

        for (int i = 0; i < myGuessArray.length; i++) {
            for (int j = 0; j < myGuessArray.length; j++) {
                if(myGuessArray[i].equals(botNumberArray[j])){
                    if(i == j) {
                        plus++;
                    } else {
                        minus++;
                    }
                }
            }
        }
        myAnswer += String.valueOf(plus) + "," + String.valueOf(minus);
        return myAnswer;
    }

    public Call addBotGame(String deviceId, String myNumber) {
        return matchmakingService.addBotGame(deviceId, myNumber);
    }
}
