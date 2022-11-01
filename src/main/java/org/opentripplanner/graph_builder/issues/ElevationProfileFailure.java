package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.routing.graph.Edge;

public class ElevationProfileFailure implements DataImportIssue {

  public static final String FMT = "Failed to set elevation profile for %s: %s";

  final Edge edge;
  final String reason;

  public ElevationProfileFailure(Edge edge, String reason) {
    this.edge = edge;
    this.reason = reason;
  }

  @Override
  public String getMessage() {
    return String.format(FMT, edge, reason);
  }
}
