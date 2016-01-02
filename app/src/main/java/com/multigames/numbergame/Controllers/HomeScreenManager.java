package com.multigames.numbergame.Controllers;

import com.multigames.numbergame.Model.ResponseGameModel;
import com.multigames.numbergame.Network.MatchmakingImpl;
import com.pusher.client.channel.PrivateChannelEventListener;

import retrofit.Call;

public class HomeScreenManager   {

    private MatchmakingImpl matchmakingService;

    public HomeScreenManager(MatchmakingImpl matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    public Call<ResponseGameModel> newGame(String deviceId) {
       return matchmakingService.newGame(deviceId);
    }

    public Call<ResponseGameModel> anyAvailableMatch(String deviceId) {
        return matchmakingService.anyAvailableMatch(deviceId);
    }

}
