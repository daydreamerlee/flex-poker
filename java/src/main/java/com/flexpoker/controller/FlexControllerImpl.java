package com.flexpoker.controller;

import java.util.List;

import org.springframework.flex.remoting.RemotingDestination;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.flexpoker.bso.GameBso;
import com.flexpoker.bso.GameEventBso;
import com.flexpoker.model.Game;
import com.flexpoker.model.User;

@Controller("flexController")
@RemotingDestination
public class FlexControllerImpl implements FlexController {

    private GameBso gameBso;

    private GameEventBso gameEventBso;

    private EventManager eventManager;

    @Override
    public void createGame(Game game) {
        gameBso.createGame(game);
        eventManager.sendGamesUpdatedEvent();
    }

    @Override
    public List<Game> fetchAllGames() {
        return gameBso.fetchAllGames();
    }

    @Override
    public void joinGame(Game game) {
        User user = (User) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        synchronized (this) {
            gameEventBso.addUserToGame(user, game);
            eventManager.sendUserJoinedEvent(user, game);
        }
    }

    public GameBso getGameBso() {
        return gameBso;
    }

    public void setGameBso(GameBso gameBso) {
        this.gameBso = gameBso;
    }

    public GameEventBso getGameEventBso() {
        return gameEventBso;
    }

    public void setGameEventBso(GameEventBso gameEventBso) {
        this.gameEventBso = gameEventBso;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public void setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;
    }

}