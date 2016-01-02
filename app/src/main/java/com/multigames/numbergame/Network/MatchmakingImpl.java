package com.multigames.numbergame.Network;

import com.multigames.numbergame.Model.ResponseGameModel;
import com.multigames.numbergame.Model.ResponseModel;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.logging.HttpLoggingInterceptor;
import retrofit.Call;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

public class MatchmakingImpl {

    MatchmakingService matchmakingService;

    public MatchmakingImpl() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient httpClient = new OkHttpClient();
        httpClient.interceptors().add(logging);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://www.sercangedik.com/MultiplayerGamesBackend/")
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        matchmakingService = retrofit.create(MatchmakingService.class);
    }

    public Call<ResponseModel> startGame(String socketName, String playerId, String playerNumber) {
        return matchmakingService.startMatch("startMatch", socketName, playerId, playerNumber);
    }

    public Call<ResponseModel> endGame(String socketName, String playerId) {
        return matchmakingService.endMatch("endMatch", socketName, playerId);
    }

    public Call<ResponseModel> cancelMatch(String socketName, String playerId) {
        return matchmakingService.cancelMatch("cancelMatch", socketName, playerId);
    }

    public Call<ResponseGameModel> anyAvailableMatch(String playerId) {
        return matchmakingService.getAvailableMatch("getAnyAvailableMatch", playerId);
    }

    public Call<ResponseModel> addMove(String playerID, String socketName, String moveNumber){
        return matchmakingService.addMove("addMoveToGameData", playerID, socketName, moveNumber);
    }

    public Call<ResponseGameModel> newGame(String deviceId) {
        return matchmakingService.newGame("newMatch", deviceId);
    }
}
