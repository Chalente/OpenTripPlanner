package org.opentripplanner.model.plan;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import static java.util.Calendar.FEBRUARY;
import static org.opentripplanner.routing.core.TraverseMode.BICYCLE;
import static org.opentripplanner.routing.core.TraverseMode.BUS;
import static org.opentripplanner.routing.core.TraverseMode.RAIL;
import static org.opentripplanner.routing.core.TraverseMode.WALK;

public class TestItineraryBuilder {
  public static final Place A = place("A", 5.0, 8.0);
  public static final Place B = place("B", 6.0, 8.5);
  public static final Place C = place("C", 7.0, 9.0);
  public static final Place D = place("D", 8.0, 9.5);
  public static final Place E = place("E", 9.0, 10.0);
  public static final Place F = place("F", 9.0, 10.5);
  public static final Place G = place("G", 9.5, 11.0);

  private static final int NOT_SET = -999_999;
  public static final int BOARD_COST = 120;
  public static final float WALK_RELUCTANCE_FACTOR = 2.0f;
  public static final float BICYCLE_RELUCTANCE_FACTOR = 1.0f;
  public static final float WAIT_RELUCTANCE_FACTOR = 0.8f;
  public static final float WALK_SPEED = 1.4f;
  public static final float BICYCLE_SPEED = 5.0f;
  public static final float BUS_SPEED = 12.5f;
  public static final float RAIL_SPEED = 25.0f;

  private Place lastPlace;
  private int lastEndTime;
  private int cost = 0;
  private final List<Leg> legs = new ArrayList<>();

  private TestItineraryBuilder(Place origin, int startTime) {
    this.lastPlace = origin;
    this.lastEndTime = startTime;
  }

  /**
   * Create a new itinerary that start at a stop and continue with a transit leg.
   */
  public static TestItineraryBuilder newItinerary(Place origin) {
    return new TestItineraryBuilder(origin, NOT_SET);
  }

  /**
   * Create a new itinerary that start by waling from a place - the origin.
   * @param startTime  The number on minutes past noon. E.g. 123 is 14:03
   */
  public static TestItineraryBuilder newItinerary(Place origin, int startTime) {
    return new TestItineraryBuilder(origin, startTime);
  }

  /**
   * Add a walking leg to the itinerary
   * @param duration number of minutes to walk
   */
  public TestItineraryBuilder walk(int duration, Place to) {
    if(lastEndTime == NOT_SET) { throw new IllegalStateException("Start time unknown!"); }
    cost += cost(WALK_RELUCTANCE_FACTOR, duration);
    leg(WALK, lastEndTime, lastEndTime + duration, to);
    return this;
  }

  /**
   * Add a bus leg to the itinerary.
   * @param start/end  The number on minutes past noon. E.g. 123 is 14:03
   */
  public TestItineraryBuilder bicycle(int start, int end, Place to) {
    cost += cost(BICYCLE_RELUCTANCE_FACTOR, end - start);
    leg(BICYCLE, start, end, to);
    return this;
  }

  /**
   * Add a bus leg to the itinerary.
   * @param start/end  The number on minutes past noon. E.g. 123 is 14:03
   */
  public TestItineraryBuilder bus(int tripId, int start, int end, Place to) {
    return transit(BUS, "B", tripId, start, end, to);
  }

  /**
   * Add a rail/train leg to the itinerary
   * @param start/end  The number on minutes past noon. E.g. 123 is 14:03
   */
  public TestItineraryBuilder rail(int tripId, int start, int end, Place to) {
    return transit(RAIL, "R", tripId, start, end, to);
  }


  public Itinerary egress(int walkDuration) {
    walk(walkDuration, null);
    return build();
  }

  public Itinerary build() {
    Itinerary itinerary = new Itinerary(legs);
    itinerary.generalizedCost = cost;
    return itinerary;
  }

  public static GregorianCalendar newTime(int minutes) {
    int hours = 12 + minutes / 60;
    minutes = minutes % 60;
    return new GregorianCalendar(2020, FEBRUARY, 2, hours, minutes);
  }


  /* private methods */

  private TestItineraryBuilder transit(TraverseMode mode, String feed, int tripId, int start, int end, Place to) {
    if(lastPlace == null) { throw new IllegalStateException("Trip from place is unknown!"); }
    int waitTime = start - lastEndTime(start);
    cost += cost(WAIT_RELUCTANCE_FACTOR, waitTime);
    cost += cost(1.0f, end - start) + BOARD_COST;
    Leg leg = leg(mode, start, end, to);
    leg.tripId = new FeedScopedId(feed, Integer.toString(tripId));
    return this;
  }

  private Leg leg(TraverseMode mode, int startTime, int endTime, Place to) {

    Leg leg = new Leg();
    leg.mode = mode;
    leg.from = lastPlace;
    leg.startTime = newTime(startTime);
    leg.to = to;
    leg.endTime = newTime(endTime);
    leg.distanceMeters = speed(mode) * 60.0 * (endTime - startTime);
    legs.add(leg);

    // Setup for adding another leg
    lastEndTime = endTime;
    lastPlace = to;

    return leg;
  }

  private double speed(TraverseMode mode) {
    switch (mode) {
      case WALK: return WALK_SPEED;
      case BICYCLE: return BICYCLE_SPEED;
      case BUS: return BUS_SPEED;
      case RAIL: return RAIL_SPEED;
      default: throw new IllegalStateException("Unsupported mode: " + mode);
    }
  }

  private int cost(float reluctance, int durationMinutes) {
    return Math.round(reluctance * (60 * durationMinutes));
  }

  private int lastEndTime(int fallbackTime) {
    return lastEndTime == NOT_SET ? fallbackTime : lastEndTime;
  }

  private static Place place(String name, double lat, double lon) {
    Place p = new Place(lat, lon, name);
    p.stopId = new FeedScopedId("S", name);
    return p;
  }
}
