package com.flexpoker.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.flexpoker.model.Game;
import com.flexpoker.repository.api.GameRepository;

@Repository
public class GameInMemoryRepository implements GameRepository {

    private final Map<UUID, Game> gameMap;
    
    public GameInMemoryRepository() {
        gameMap = new HashMap<>();
    }
    
    @Override
    public Game findById(UUID id) {
        return gameMap.get(id);
    }

    @Override
    public List<Game> findAll() {
        return new ArrayList<>(gameMap.values());
    }

    @Override
    public void saveNew(Game game) {
        game.setId(UUID.randomUUID());
        gameMap.put(game.getId(), game);
    }

    @Override
    public void update(Game game) {
        // do nothing for now.  when we move to redis or some other db store,
        // this should be implemented then
    }
}