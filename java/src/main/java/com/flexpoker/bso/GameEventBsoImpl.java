package com.flexpoker.bso;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import com.flexpoker.exception.FlexPokerException;
import com.flexpoker.model.Blinds;
import com.flexpoker.model.CommonCards;
import com.flexpoker.model.FlopCards;
import com.flexpoker.model.Game;
import com.flexpoker.model.GameEventType;
import com.flexpoker.model.GameStage;
import com.flexpoker.model.HandDealerState;
import com.flexpoker.model.HandEvaluation;
import com.flexpoker.model.HandRanking;
import com.flexpoker.model.HandRoundState;
import com.flexpoker.model.PocketCards;
import com.flexpoker.model.Pot;
import com.flexpoker.model.RealTimeGame;
import com.flexpoker.model.RealTimeHand;
import com.flexpoker.model.RiverCard;
import com.flexpoker.model.Seat;
import com.flexpoker.model.Table;
import com.flexpoker.model.TurnCard;
import com.flexpoker.model.User;
import com.flexpoker.model.UserGameStatus;
import com.flexpoker.util.ActionOnSeatPredicate;
import com.flexpoker.util.BigBlindSeatPredicate;
import com.flexpoker.util.ButtonSeatPredicate;
import com.flexpoker.util.SmallBlindSeatPredicate;

@Service("gameEventBso")
public class GameEventBsoImpl implements GameEventBso {

    private GameBso gameBso;

    private DeckBso deckBso;

    private RealTimeGameBso realTimeGameBso;

    private SeatStatusBso seatStatusBso;

    private HandEvaluatorBso handEvaluatorBso;

    private PotBso potBso;

    private ValidationBso validationBso;

    @Override
    public boolean addUserToGame(User user, Game game) {
        synchronized (this) {
            checkIfUserCanJoinGame(game, user);

            UserGameStatus userGameStatus = new UserGameStatus();
            userGameStatus.setUser(user);

            realTimeGameBso.get(game).addUserGameStatus(userGameStatus);

            if (gameBso.fetchUserGameStatuses(game).size() == game.getTotalPlayers()) {
                gameBso.changeGameStage(game, GameStage.STARTING);
                return true;
            }

            return false;
        }
    }

    @Override
    public boolean verifyRegistration(User user, Game game) {
        synchronized (this) {
            RealTimeGame realTimeGame = realTimeGameBso.get(game);
            realTimeGame.verifyEvent(user, "registration");

            if (realTimeGame.isEventVerified("registration")) {
                gameBso.initializePlayersAndTables(game);
                gameBso.changeGameStage(game, GameStage.IN_PROGRESS);
                return true;
            }

            return false;
        }

    }

    @Override
    public boolean verifyGameInProgress(User user, Game game) {
        synchronized (this) {
            RealTimeGame realTimeGame = realTimeGameBso.get(game);
            realTimeGame.verifyEvent(user, "gameInProgress");

            if (realTimeGame.isEventVerified("gameInProgress")) {
                startNewGameForAllTables(game);
                return true;
            }

            return false;
        }
    }

    @Override
    public boolean verifyReadyToStartNewHand(User user, Game game, Table table) {
        synchronized (this) {
            RealTimeGame realTimeGame = realTimeGameBso.get(game);
            table = realTimeGame.getTable(table);

            realTimeGame.verifyEvent(user, table, "readyToStartNewHand");

            if (realTimeGame.isEventVerified(table, "readyToStartNewHand")) {
                realTimeGame.resetEvent(table, "readyToStartNewHand");
                startNewHand(game, table);
                return true;
            }

            return false;
        }
    }

    private void startNewHand(Game game, Table table) {
        seatStatusBso.setStatusForNewHand(table);
        resetTableStatus(game, table);
    }

    private void checkIfUserCanJoinGame(Game game, User user) {
        GameStage gameStage = game.getGameStage();

        if (GameStage.STARTING.equals(gameStage)
            || GameStage.IN_PROGRESS.equals(gameStage)) {
            throw new FlexPokerException("The game has already started");
        }

        if (GameStage.FINISHED.equals(gameStage)) {
            throw new FlexPokerException("The game is already finished.");
        }

        Set<UserGameStatus> userGameStatuses = gameBso.fetchUserGameStatuses(game);

        for (UserGameStatus userGameStatus : userGameStatuses) {
            if (user.equals(userGameStatus.getUser())) {
                throw new FlexPokerException("You are already in this game.");
            }
        }
    }

    private void createNewRealTimeHand(Game game, Table table) {
        RealTimeGame realTimeGame = realTimeGameBso.get(game);
        Blinds currentBlinds = realTimeGame.getCurrentBlinds();
        int smallBlind = currentBlinds.getSmallBlind();
        int bigBlind = currentBlinds.getBigBlind();

        RealTimeHand realTimeHand = new RealTimeHand(table.getSeats());
        Seat smallBlindSeat = (Seat) CollectionUtils.find(table.getSeats(),
                new SmallBlindSeatPredicate());
        Seat bigBlindSeat = (Seat) CollectionUtils.find(table.getSeats(),
                new BigBlindSeatPredicate());


        for (Seat seat : table.getSeats()) {
            UserGameStatus userGameStatus = seat.getUserGameStatus();
            int amountNeededToCall = bigBlind;
            int amountNeededToRaise = bigBlind * 2;

            if (seat.equals(bigBlindSeat)) {
                amountNeededToCall = 0;
                amountNeededToRaise = bigBlind;
                realTimeHand.addPossibleSeatAction(seat, GameEventType.CHECK);
                realTimeHand.addPossibleSeatAction(seat, GameEventType.RAISE);
                seat.setChipsInFront(bigBlind);
                userGameStatus.setChips(userGameStatus.getChips() - bigBlind);
            } else if (seat.equals(smallBlindSeat)) {
                amountNeededToCall = smallBlind;
                realTimeHand.addPossibleSeatAction(seat, GameEventType.CALL);
                realTimeHand.addPossibleSeatAction(seat, GameEventType.FOLD);
                realTimeHand.addPossibleSeatAction(seat, GameEventType.RAISE);
                seat.setChipsInFront(smallBlind);
                userGameStatus.setChips(userGameStatus.getChips() - smallBlind);
            } else {
                realTimeHand.addPossibleSeatAction(seat, GameEventType.CALL);
                realTimeHand.addPossibleSeatAction(seat, GameEventType.RAISE);
                realTimeHand.addPossibleSeatAction(seat, GameEventType.FOLD);
                seat.setChipsInFront(0);
            }

            table.setTotalPotAmount(table.getTotalPotAmount() + seat.getChipsInFront());

            seat.setCallAmount(amountNeededToCall);
            seat.setMinBet(amountNeededToRaise);
        }

        determineNextToAct(table, realTimeHand);
        realTimeHand.setLastToAct(bigBlindSeat);

        realTimeHand.setHandDealerState(HandDealerState.POCKET_CARDS_DEALT);
        realTimeHand.setHandRoundState(HandRoundState.ROUND_IN_PROGRESS);

        List<HandEvaluation> handEvaluations = determineHandEvaluations(game, table);
        realTimeHand.setHandEvaluationList(handEvaluations);

        realTimeGame.addRealTimeHand(table, realTimeHand);
    }

    private List<HandEvaluation> determineHandEvaluations(Game game, Table table) {
        FlopCards flopCards = deckBso.fetchFlopCards(game, table);
        TurnCard turnCard = deckBso.fetchTurnCard(game, table);
        RiverCard riverCard = deckBso.fetchRiverCard(game, table);

        CommonCards commonCards = new CommonCards(flopCards, turnCard, riverCard);

        List<HandRanking> possibleHands = handEvaluatorBso.determinePossibleHands(commonCards);

        List<HandEvaluation> handEvaluations = new ArrayList<HandEvaluation>();

        for (Seat seat : table.getSeats()) {
            if (seat.getUserGameStatus() != null
                    && seat.getUserGameStatus().getUser() != null) {
                User user = seat.getUserGameStatus().getUser();
                PocketCards pocketCards = deckBso.fetchPocketCards(user, game, table);
                HandEvaluation handEvaluation = handEvaluatorBso
                        .determineHandEvaluation(commonCards, user, pocketCards,
                         possibleHands);
                handEvaluations.add(handEvaluation);
            }
        }

        return handEvaluations;
    }

    private void determineLastToAct(Table table, RealTimeHand realTimeHand) {
        List<Seat> seats = table.getSeats();

        int seatIndex;

        if (realTimeHand.getOriginatingBettor() == null) {
            Seat buttonSeat = (Seat) CollectionUtils.find(table.getSeats(),
                    new ButtonSeatPredicate());
            seatIndex = seats.indexOf(buttonSeat);
        } else {
            seatIndex = seats.indexOf(realTimeHand.getOriginatingBettor());
            if (seatIndex == 0) {
                seatIndex = seats.size() - 1;
            } else {
                seatIndex--;
            }
        }

        for (int i = seatIndex; i >= 0; i--) {
            if (seats.get(i).isStillInHand()) {
                realTimeHand.setLastToAct(seats.get(i));
                return;
            }
        }

        for (int i = seats.size() - 1; i > seatIndex; i--) {
            if (seats.get(i).isStillInHand()) {
                realTimeHand.setLastToAct(seats.get(i));
                return;
            }
        }
    }

    private void determineNextToAct(Table table, RealTimeHand realTimeHand) {
        List<Seat> seats = table.getSeats();
        Seat actionOnSeat = (Seat) CollectionUtils.find(seats,
                new ActionOnSeatPredicate());

        int actionOnIndex = seats.indexOf(actionOnSeat);

        for (int i = actionOnIndex + 1; i < seats.size(); i++) {
            if (seats.get(i).isStillInHand()) {
                realTimeHand.setNextToAct(seats.get(i));
                return;
            }
        }

        for (int i = 0; i < actionOnIndex; i++) {
            if (seats.get(i).isStillInHand()) {
                realTimeHand.setNextToAct(seats.get(i));
                return;
            }
        }
    }

    private void startNewGameForAllTables(Game game) {
        for (Table table : gameBso.fetchTables(game)) {
            seatStatusBso.setStatusForNewGame(table);
            resetTableStatus(game, table);
        }
    }

    private void resetTableStatus(Game game, Table table) {
        table.setTotalPotAmount(0);
        deckBso.shuffleDeck(game, table);
        potBso.createNewHandPot(game, table);
        createNewRealTimeHand(game, table);
        determineTablePotAmounts(game, table);
    }

    private void determineTablePotAmounts(Game game, Table table) {
        table.setPotAmounts(new ArrayList<Integer>());
        for (Pot pot : potBso.fetchAllPots(game, table)) {
            table.getPotAmounts().add(pot.getAmount());
        }
    }

    public GameBso getGameBso() {
        return gameBso;
    }

    public void setGameBso(GameBso gameBso) {
        this.gameBso = gameBso;
    }

    public DeckBso getDeckBso() {
        return deckBso;
    }

    public void setDeckBso(DeckBso deckBso) {
        this.deckBso = deckBso;
    }

    public RealTimeGameBso getRealTimeGameBso() {
        return realTimeGameBso;
    }

    public void setRealTimeGameBso(RealTimeGameBso realTimeGameBso) {
        this.realTimeGameBso = realTimeGameBso;
    }

    public SeatStatusBso getSeatStatusBso() {
        return seatStatusBso;
    }

    public void setSeatStatusBso(SeatStatusBso seatStatusBso) {
        this.seatStatusBso = seatStatusBso;
    }

    public HandEvaluatorBso getHandEvaluatorBso() {
        return handEvaluatorBso;
    }

    public void setHandEvaluatorBso(HandEvaluatorBso handEvaluatorBso) {
        this.handEvaluatorBso = handEvaluatorBso;
    }

    public PotBso getPotBso() {
        return potBso;
    }

    public void setPotBso(PotBso potBso) {
        this.potBso = potBso;
    }

    public ValidationBso getValidationBso() {
        return validationBso;
    }

    public void setValidationBso(ValidationBso validationBso) {
        this.validationBso = validationBso;
    }

}
