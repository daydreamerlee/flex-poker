package com.flexpoker.controller;

import java.util.List;
import java.util.Set;

import com.flexpoker.model.Game;
import com.flexpoker.model.PocketCards;
import com.flexpoker.model.UserStatusInGame;

public interface FlexController {

    List<Game> fetchAllGames();

    void createGame(Game game);

    void joinGame(Game game);

    Set<UserStatusInGame> fetchAllUserStatusesForGame(Game game);

    void verifyRegistrationForGame(Game game);

    PocketCards fetchPocketCards(Game game);

}
