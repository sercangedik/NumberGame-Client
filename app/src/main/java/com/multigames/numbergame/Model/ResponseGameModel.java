package com.multigames.numbergame.Model;

import com.google.gson.annotations.SerializedName;

public class ResponseGameModel {
    @SerializedName("data") GameModel[] gameModels;
    @SerializedName("stat") String stat;

    public GameModel getGameModels() {
        return gameModels[0];
    }

    public String getStat() {
        return stat;
    }
}
