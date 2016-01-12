package com.multigames.numbergame.Network;

import com.multigames.numbergame.Model.ResponseGameModel;
import com.multigames.numbergame.Model.ResponseModel;

import retrofit.Call;
import retrofit.http.GET;
import retrofit.http.Query;

public interface MatchmakingService {

    @GET("gateway.php")
    Call<ResponseGameModel> getAvailableMatch(@Query("action") String action, @Query("player_id") String playerId);

    @GET("gateway.php")
    Call<ResponseModel> startMatch(@Query("action") String action, @Query("socket_name") String socketName, @Query("player_id") String playerId, @Query("player_number") String playerNumber);

    @GET("gateway.php")
    Call<ResponseModel> endMatch(@Query("action") String action, @Query("socket_name") String socketName, @Query("player_id") String playerId);

    @GET("gateway.php")
    Call<ResponseModel> cancelMatch(@Query("action") String action, @Query("socket_name") String socketName, @Query("player_id") String playerId);

    @GET("gateway.php")
    Call<ResponseModel> addMove(@Query("action") String addMoveToGameData, @Query("player_id") String playerId, @Query("socket_name") String socketName, @Query("move_number")String moveNumber);

    @GET("gateway.php")
    Call<ResponseGameModel> newGame(@Query("action") String newGame, @Query("player_id") String playerId);

    @GET("gateway.php")
    Call<ResponseGameModel> addBotGame(@Query("action")String addBotGame,  @Query("player_id") String deviceId,  @Query("player_number") String myNumber);
}
