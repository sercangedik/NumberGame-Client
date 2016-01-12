package com.multigames.numbergame;

import com.multigames.numbergame.Controllers.GameScreenManager;
import com.multigames.numbergame.Network.MatchmakingImpl;
import com.pusher.client.Pusher;

import org.junit.Test;
import org.mockito.Mockito;

import java.lang.String;

import static org.junit.Assert.*;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void botNumberValid(){
        MatchmakingImpl matchmaking = Mockito.mock(MatchmakingImpl.class);
        Pusher pusher = Mockito.mock(Pusher.class);
        GameScreenManager gameScreenManager = new GameScreenManager(matchmaking, "", pusher);
        gameScreenManager.generateBotNumber("");
        String botNumber = gameScreenManager.getBotNumber();
        String[] botNumberArray = botNumber.split("(?!^)");

        for (int i = 0; i < botNumber.length(); i++) {
            for (int j = i+1; j < botNumber.length(); j++) {
                assertNotEquals(botNumberArray[i], botNumberArray[j]);
            }
        }
    }
}