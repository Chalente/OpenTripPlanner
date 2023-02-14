package org.opentripplanner.street.model.edge;

import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

public abstract class SingleStateTraversalEdge extends Edge {

  protected SingleStateTraversalEdge(Vertex v1, Vertex v2) {
    super(v1, v2);
  }

  @Override
  public State[] multiTraverse(State u) {
    return State.of(traverse(u));
  }

  /**
   * Traverse this edge.
   *
   * @param s0 The State coming into the edge.
   * @return The State upon exiting the edge.
   */
  public abstract State traverse(State s0);
}
