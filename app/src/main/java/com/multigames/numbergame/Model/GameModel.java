package com.multigames.numbergame.Model;

import com.google.gson.annotations.SerializedName;


public class GameModel {

    @SerializedName("socket_name") String socketName;
    @SerializedName("game_state") private String gameState;
    @SerializedName("game") GameDataModel[] gameDataModel;

    public String getGameState() {
        return gameState;
    }

    public String getSocketName() {
        return socketName;
    }

    public GameDataModel getGameDataModel() {
        return gameDataModel[0];
    }
}
