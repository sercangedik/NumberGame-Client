package com.multigames.numbergame.Model;

import com.google.gson.annotations.SerializedName;

public class GameDataModel {
    @SerializedName("move") String myMoves;
    @SerializedName("number") String myNumber;
    @SerializedName("move_end_time") String moveEndTime;
    @SerializedName("current_move_id") String currentMoveId;
    @SerializedName("socket_name") String socketName;
    @SerializedName("move_results") String playerMoveResults;

    public String getCurrentMoveId() {
        return currentMoveId;
    }

    public String getMoveEndTime() {
        return moveEndTime;
    }

    public String getMyMoves() {
        return myMoves;
    }

    public String getMyNumber() {
        return myNumber;
    }

    public String getSocketName() {
        return socketName;
    }

    public String getPlayerMoveResults() {
        return playerMoveResults;
    }
}
