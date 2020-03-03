package org.opentripplanner.graph_builder.annotation;

public class FloorNumberUnknownGuessedFromAltitude extends GraphBuilderAnnotation {

    public static final String FMT = "Could not determine floor number for layer %s. Guessed %s (0-based) from altitude.";

    final String layer;
    final Integer floorNumber;

    public FloorNumberUnknownGuessedFromAltitude(String layer, Integer floorNumber) {
        this.layer=layer;
        this.floorNumber=floorNumber;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, layer, floorNumber);
    }
}