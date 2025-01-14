package org.opentripplanner.graph_builder.module.osm;

import gnu.trove.list.TLongList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.api.Issue;
import org.opentripplanner.openstreetmap.model.OSMLevel;
import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.ElevatorAlightEdge;
import org.opentripplanner.street.model.edge.ElevatorBoardEdge;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;
import org.opentripplanner.street.model.edge.FreeEdge;
import org.opentripplanner.street.model.vertex.ElevatorOffboardVertex;
import org.opentripplanner.street.model.vertex.ElevatorOnboardVertex;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.OsmVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains the logic for extracting elevator data from OSM and converting it to edges.
 * <p>
 * I depends heavily on the idiosyncratic processing of the OSM data in {@link OpenStreetMapModule}
 * which is the reason this is not a public class.
 */
class ElevatorProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(ElevatorProcessor.class);

  private final DataImportIssueStore issueStore;
  private final Map<Long, Map<OSMLevel, OsmVertex>> multiLevelNodes;
  private final OSMDatabase osmdb;

  private final Map<Long, IntersectionVertex> intersectionNodes;

  public ElevatorProcessor(
    DataImportIssueStore issueStore,
    OSMDatabase osmdb,
    Map<Long, Map<OSMLevel, OsmVertex>> multiLevelNodes,
    Map<Long, IntersectionVertex> intersectionNodes
  ) {
    this.issueStore = issueStore;
    this.osmdb = osmdb;
    this.multiLevelNodes = multiLevelNodes;
    this.intersectionNodes = intersectionNodes;
  }

  public void buildElevatorEdges(Graph graph) {
    /* build elevator edges */
    for (Long nodeId : multiLevelNodes.keySet()) {
      OSMNode node = osmdb.getNode(nodeId);
      // this allows skipping levels, e.g., an elevator that stops
      // at floor 0, 2, 3, and 5.
      // Converting to an Array allows us to
      // subscript it so we can loop over it in twos. Assumedly, it will stay
      // sorted when we convert it to an Array.
      // The objects are Integers, but toArray returns Object[]
      Map<OSMLevel, OsmVertex> vertices = multiLevelNodes.get(nodeId);

      /*
       * first, build FreeEdges to disconnect from the graph, GenericVertices to serve as attachment points, and ElevatorBoard and
       * ElevatorAlight edges to connect future ElevatorHop edges to. After this iteration, graph will look like (side view): +==+~~X
       *
       * +==+~~X
       *
       * +==+~~X
       *
       * + GenericVertex, X EndpointVertex, ~~ FreeEdge, == ElevatorBoardEdge/ElevatorAlightEdge Another loop will fill in the
       * ElevatorHopEdges.
       */
      OSMLevel[] levels = vertices.keySet().toArray(new OSMLevel[0]);
      Arrays.sort(levels);
      ArrayList<Vertex> onboardVertices = new ArrayList<>();
      for (OSMLevel level : levels) {
        // get the node to build the elevator out from
        OsmVertex sourceVertex = vertices.get(level);
        String sourceVertexLabel = sourceVertex.getLabel();
        String levelName = level.longName;

        createElevatorVertices(graph, onboardVertices, sourceVertex, sourceVertexLabel, levelName);
      }
      int travelTime = parseDuration(node).orElse(-1);

      var wheelchair = node.getWheelchairAccessibility();

      createElevatorHopEdges(
        onboardVertices,
        wheelchair,
        node.isTagTrue("bicycle"),
        levels.length,
        travelTime
      );
    } // END elevator edge loop

    // Add highway=elevators to graph as elevators
    Iterator<OSMWay> elevators = osmdb.getWays().stream().filter(this::isElevatorWay).iterator();

    while (elevators.hasNext()) {
      OSMWay elevatorWay = elevators.next();

      List<Long> nodes = Arrays
        .stream(elevatorWay.getNodeRefs().toArray())
        .filter(nodeRef ->
          intersectionNodes.containsKey(nodeRef) && intersectionNodes.get(nodeRef) != null
        )
        .boxed()
        .toList();

      ArrayList<Vertex> onboardVertices = new ArrayList<>();
      for (int i = 0; i < nodes.size(); i++) {
        Long node = nodes.get(i);
        var sourceVertex = intersectionNodes.get(node);
        String sourceVertexLabel = sourceVertex.getLabel();
        String levelName = elevatorWay.getId() + " / " + i;
        createElevatorVertices(
          graph,
          onboardVertices,
          sourceVertex,
          elevatorWay.getId() + "_" + sourceVertexLabel,
          levelName
        );
      }

      int travelTime = parseDuration(elevatorWay).orElse(-1);
      int levels = nodes.size();
      var wheelchair = elevatorWay.getWheelchairAccessibility();

      createElevatorHopEdges(
        onboardVertices,
        wheelchair,
        elevatorWay.isTagTrue("bicycle"),
        levels,
        travelTime
      );
      LOG.debug("Created elevatorHopEdges for way {}", elevatorWay.getId());
    }
  }

  private static void createElevatorVertices(
    Graph graph,
    ArrayList<Vertex> onboardVertices,
    IntersectionVertex sourceVertex,
    String sourceVertexLabel,
    String levelName
  ) {
    ElevatorOffboardVertex offboardVertex = new ElevatorOffboardVertex(
      graph,
      sourceVertexLabel + "_offboard",
      sourceVertex.getX(),
      sourceVertex.getY(),
      new NonLocalizedString(levelName)
    );

    new FreeEdge(sourceVertex, offboardVertex);
    new FreeEdge(offboardVertex, sourceVertex);

    ElevatorOnboardVertex onboardVertex = new ElevatorOnboardVertex(
      graph,
      sourceVertexLabel + "_onboard",
      sourceVertex.getX(),
      sourceVertex.getY(),
      new NonLocalizedString(levelName)
    );

    new ElevatorBoardEdge(offboardVertex, onboardVertex);
    new ElevatorAlightEdge(onboardVertex, offboardVertex, new NonLocalizedString(levelName));

    // accumulate onboard vertices to so they can be connected by hop edges later
    onboardVertices.add(onboardVertex);
  }

  private static void createElevatorHopEdges(
    ArrayList<Vertex> onboardVertices,
    Accessibility wheelchair,
    boolean bicycleAllowed,
    int levels,
    int travelTime
  ) {
    // -1 because we loop over onboardVertices two at a time
    for (int i = 0, vSize = onboardVertices.size() - 1; i < vSize; i++) {
      Vertex from = onboardVertices.get(i);
      Vertex to = onboardVertices.get(i + 1);

      // default permissions: pedestrian, wheelchair, check tag bicycle=yes
      StreetTraversalPermission permission = bicycleAllowed
        ? StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE
        : StreetTraversalPermission.PEDESTRIAN;

      if (travelTime > -1 && levels > 0) {
        ElevatorHopEdge.bidirectional(from, to, permission, wheelchair, levels, travelTime);
      } else {
        ElevatorHopEdge.bidirectional(from, to, permission, wheelchair);
      }
    }
  }

  private boolean isElevatorWay(OSMWay way) {
    if (!way.hasTag("highway")) {
      return false;
    }
    if (!"elevator".equals(way.getTag("highway"))) {
      return false;
    }
    if (osmdb.isAreaWay(way.getId())) {
      return false;
    }

    TLongList nodeRefs = way.getNodeRefs();
    // A way whose first and last node are the same is probably an area, skip that.
    // https://www.openstreetmap.org/way/503412863
    // https://www.openstreetmap.org/way/187719215
    return nodeRefs.get(0) != nodeRefs.get(nodeRefs.size() - 1);
  }

  private OptionalInt parseDuration(OSMWithTags element) {
    return element.getTagAsInt(
      "duration",
      v ->
        issueStore.add(
          Issue.issue(
            "InvalidDuration",
            "Duration for osm node %d is not a number: '%s'; it's replaced with '-1' (unknown).",
            element.getId(),
            v
          )
        )
    );
  }
}
