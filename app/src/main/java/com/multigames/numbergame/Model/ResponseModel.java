package com.multigames.numbergame.Model;

import com.google.gson.annotations.SerializedName;

public class ResponseModel {
    @SerializedName("stat") String stat;
    @SerializedName("code") int code;
    @SerializedName("message") String message;
    @SerializedName("data") String data;

    public String getData() {
        return data;
    }
}
