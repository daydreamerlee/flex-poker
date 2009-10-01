package com.flexpoker.model;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class RealTimeHandTest {

    @Test
    public void testRealTimeHand() {
        List<Seat> seats = new ArrayList<Seat>();
        Seat seat1 = new Seat();
        Seat seat2 = new Seat();

        seats.add(seat1);
        seats.add(seat2);

        RealTimeHand realTimeHand = new RealTimeHand(seats);

        assertFalse(realTimeHand.isUserAllowedToPerformAction(GameEventType.CHECK, seat1));
        assertFalse(realTimeHand.isUserAllowedToPerformAction(GameEventType.CHECK, seat2));
        assertFalse(realTimeHand.isUserAllowedToPerformAction(GameEventType.BET, seat1));
        assertFalse(realTimeHand.isUserAllowedToPerformAction(GameEventType.BET, seat2));
        assertNull(realTimeHand.getAmountNeededToCall(seat1));
        assertNull(realTimeHand.getAmountNeededToCall(seat2));
        assertNull(realTimeHand.getAmountNeededToRaise(seat1));
        assertNull(realTimeHand.getAmountNeededToRaise(seat2));
    }

}
