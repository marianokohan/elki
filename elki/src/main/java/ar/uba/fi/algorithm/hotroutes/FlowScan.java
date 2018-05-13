package ar.uba.fi.algorithm.hotroutes;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.geotools.graph.structure.DirectedEdge;
import org.opengis.feature.simple.SimpleFeature;

import ar.uba.fi.result.HotRoute;
import ar.uba.fi.result.HotRoutes;
import ar.uba.fi.roadnetwork.RoadNetwork;
import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.Logging;
//TODO: confirm license description
/*
 This file is developed to be used as part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Implementation of FlowScan algorithm - discovery of traffic density-based hot routes
 *
 * TODO: complete description
 *
 * @author Mariano Kohan
 *
 */
//TODO: tag '@' - min description
public class FlowScan  implements Algorithm {

  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(FlowScan.class);

  protected RoadNetwork roadNetwork;
  protected TrafficSets trafficSets;

  protected int minTraffic;
  protected int epsilon;

  protected boolean onlyTrajectories = false;

  public FlowScan(File roadNetworkFile, int epsilon, int minTraffic, boolean onlyTrajectories) {
    this.roadNetwork = RoadNetwork.getInstance(roadNetworkFile); //TODO: cons. separar de constructor <-> ¿demora en GUI al setear parametros?
    this.epsilon = epsilon;
    this.minTraffic = minTraffic;
    this.onlyTrajectories = onlyTrajectories;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.ANY);
    //TODO: mejor alternativa de doc: http://elki.dbs.ifi.lmu.de/releases/release0.6.5~20141030/doc/de/lmu/ifi/dbs/elki/data/type/TypeUtil.html
    //posible TypeUtil.FEATURE_VECTORS - pero dimension fija => para formato "normalizado"
    /*
     * formato  init con "<Trid, t, lat, long>" ->  para trucks: "Tr_id(obj_tr),datetime(dd/mm/yyyyThh:mm:ss),lat,lon"
     * -> tipos: string, datetime (o string para formatear - init. se ignoraria), number, number
     */
//    return TypeUtil.array(TypeUtil.EXTERNALID, TypeUtil.STRING, TypeUtil.NUMBER_VECTOR_FIELD_2D);
//    return TypeUtil.array(TypeUtil.STRING, TypeUtil.STRING, TypeUtil.NUMBER_VECTOR_FIELD_2D);
//    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD_2D);
  }

  
  //TODO: considerar declaracion de signature devolviendo Result del tipo HotRoutes (en vez de base Result)
  @Override
  public Result run(Database database) {
    // TODO base impl. from NullAlgorithm
    //TODO: tmp test to define structure
//    displayTransactionsContent(database);
    trafficSets = new TrafficSets(database);

    HotRoutes hotRoutes = new HotRoutes(roadNetwork);
    if (!this.onlyTrajectories) {
      List<DirectedEdge> hotRoutesStarts = discoverHotRouteStarts();
      for(DirectedEdge hotRouteStart : hotRoutesStarts) {
        HotRoute hotRoute = new HotRoute(hotRouteStart);
        extendHotRoute(hotRoute, hotRoutes);
      }
      LOG.info("FlowScan discovers " + hotRoutes.getHotRoutes().size() + " hot routes");
    } else {
      LOG.info("Only display trajectories (hot routes not discovered)");
    }

    return hotRoutes;
  }

  //base def. from paper
  protected List<DirectedEdge> discoverHotRouteStarts() {
    List<DirectedEdge> hotRoutesStarts = new LinkedList<DirectedEdge>();
    Collection edges = this.roadNetwork.getGraph().getEdges();
    for(Iterator edgesIterator = edges.iterator(); edgesIterator.hasNext();) {
      DirectedEdge edge = (DirectedEdge) edgesIterator.next();
      List incidentEdges = edge.getInNode().getInEdges();
      List<String> incidentEdgesWithMinTraffic = new LinkedList<String>();
      SimpleFeature currentEdgeFeature =  ((SimpleFeature)edge.getObject());
      for(Iterator iterator = incidentEdges.iterator(); iterator.hasNext();) {
        DirectedEdge incidentEdge = (DirectedEdge) iterator.next();
        String edgeId = ((SimpleFeature)incidentEdge.getObject()).getID();
        Set<Integer> traffic = trafficSets.traffic(edgeId);
        if (traffic.size() >= minTraffic) {
          incidentEdgesWithMinTraffic.add(edgeId);
        }
      }
      Set<Integer> currentEdgeTraffic = trafficSets.traffic(currentEdgeFeature.getID());
      Set<Integer> incidentEdgesTraffic = trafficSets.trafficUnion(incidentEdgesWithMinTraffic.toArray(new String[]{}));
      if (trafficSets.trafficDifference(currentEdgeTraffic, incidentEdgesTraffic).size() >= minTraffic) {
        hotRoutesStarts.add(edge);
      }
    }
    return hotRoutesStarts;
  }

  //alternative definition according to limitation of adyacent incident edges
  /*
  private List<DirectedEdge> discoverHotRouteStarts() {
    List<DirectedEdge> hotRoutesStarts = new LinkedList<DirectedEdge>();
    Collection edges = this.roadNetwork.getGraph().getEdges();
    for(Iterator edgesIterator = edges.iterator(); edgesIterator.hasNext();) {
      DirectedEdge edge = (DirectedEdge) edgesIterator.next();
      SimpleFeature currentEdgeFeature =  ((SimpleFeature)edge.getObject());
      Set<String> precedingNeighborsWithMinTraffic = getPrecedingNeighborsWithMinTraffic(edge);
      Set<String> currentEdgeTraffic = trafficSets.traffic(currentEdgeFeature.getID());
      Set<String> precedingEdgesTraffic = trafficSets.trafficUnion(precedingNeighborsWithMinTraffic.toArray(new String[]{}));
      if (trafficSets.trafficDifference(currentEdgeTraffic, precedingEdgesTraffic).size() >= minTraffic) {
        hotRoutesStarts.add(edge);
      }
    }
    return hotRoutesStarts;
  }

  private Set<String> getPrecedingNeighborsWithMinTraffic(DirectedEdge edge) {
    Set<String> precedingNeighborsWithMinTraffic = new HashSet<String>();
    Set<DirectedEdge> currentHopEdges = new HashSet<DirectedEdge>();
    currentHopEdges.add(edge);
    Set<DirectedEdge> previousHopEdges = new HashSet<DirectedEdge>();
    int hopNumber = 1;
    while ((hopNumber <= epsilon) && (currentHopEdges.size() > 0)) {
      for(DirectedEdge currentHopEdge : currentHopEdges) {
        List adjacentEdges = currentHopEdge.getInNode().getInEdges();
        for(Iterator adjacentEdgesIterator = adjacentEdges.iterator(); adjacentEdgesIterator.hasNext();) {
          DirectedEdge adjacentEdge = (DirectedEdge) adjacentEdgesIterator.next();
          String adjacentEdgeId = ((SimpleFeature)adjacentEdge.getObject()).getID();
            if (trafficSets.traffic(adjacentEdgeId).size() >= minTraffic) {
              precedingNeighborsWithMinTraffic.add(adjacentEdgeId);
            }
            previousHopEdges.add(adjacentEdge);
        }
      }
      hopNumber++;
      currentHopEdges = previousHopEdges;
      previousHopEdges = new HashSet<DirectedEdge>();
    }
    return precedingNeighborsWithMinTraffic;
  }
  */

  private void extendHotRoute(HotRoute hotRoute, HotRoutes hotRoutes) {
    Set<DirectedEdge> directlyTrafficDensityReachableEdges = getDirectlyTrafficDensityReachableEdges(hotRoute);
    if (directlyTrafficDensityReachableEdges.size() != 0) {
      boolean hotRouteExtended = false;
      for(DirectedEdge directlyTrafficDensityReachableEdge : directlyTrafficDensityReachableEdges) {
        if (isRouteTrafficDensityReachable(directlyTrafficDensityReachableEdge, hotRoute)) {
          DirectedEdge lastEdge = hotRoute.getLastEdge();
          String edgeId = ((SimpleFeature)lastEdge.getObject()).getID();
          HotRoute extendedHotRoute = hotRoute.copyWithEdge(directlyTrafficDensityReachableEdge);
          hotRouteExtended = true;
          extendHotRoute(extendedHotRoute, hotRoutes);
        }
      }
      if (!hotRouteExtended) {
        hotRoutes.addHotRoute(hotRoute);
      }
    } else {
      hotRoutes.addHotRoute(hotRoute);
    }
  }

  protected boolean isRouteTrafficDensityReachable(DirectedEdge directlyTrafficDensityReachableEdge, HotRoute hotRoute) {
    List<String> lastEdgesIds = hotRoute.getLastEdgesIds(epsilon);
    lastEdgesIds.add(((SimpleFeature)directlyTrafficDensityReachableEdge.getObject()).getID());
    return trafficSets.trafficInteresection(lastEdgesIds.toArray(new String[] {})).size() >= minTraffic;
  }

  protected Set<DirectedEdge> getDirectlyTrafficDensityReachableEdges(HotRoute hotRoute) {
    DirectedEdge lastEdge = hotRoute.getLastEdge();
    Set<DirectedEdge> trafficDensityReachableEdges = new HashSet<DirectedEdge>();
    String edgeId = ((SimpleFeature)lastEdge.getObject()).getID();
    Set<DirectedEdge> currentHopEdges = new HashSet<DirectedEdge>();
    currentHopEdges.add(lastEdge);
    Set<DirectedEdge> nextHopEdges = new HashSet<DirectedEdge>();
    int hopNumber = 1;
    while ((hopNumber <= epsilon) && (currentHopEdges.size() > 0)) {
      for(DirectedEdge currentHopEdge : currentHopEdges) {
        List adjacentEdges = currentHopEdge.getOutNode().getOutEdges();
        for(Iterator adjacentEdgesIterator = adjacentEdges.iterator(); adjacentEdgesIterator.hasNext();) {
          DirectedEdge adjacentEdge = (DirectedEdge) adjacentEdgesIterator.next();
          String adjacentEdgeId = ((SimpleFeature)adjacentEdge.getObject()).getID();
          if (!isRoadNetworkCycle(adjacentEdge, currentHopEdge, trafficDensityReachableEdges, hotRoute)) { //to avoid cycles in the road network
            if (trafficSets.trafficInteresection(edgeId, adjacentEdgeId).size() >= minTraffic) {
              trafficDensityReachableEdges.add(adjacentEdge);
            } else {
              nextHopEdges.add(adjacentEdge);
            }
          }
        }
      }
      hopNumber++;
      currentHopEdges = nextHopEdges;
      nextHopEdges = new HashSet<DirectedEdge>();
    }
    return trafficDensityReachableEdges;
  }

  private boolean isRoadNetworkCycle(DirectedEdge adjacentEdge, DirectedEdge currentHopEdge, Set<DirectedEdge> trafficDensityReachableEdges, HotRoute hotRoute) {
    boolean isRoadNetworkCycle = adjacentEdge.equals(currentHopEdge) || hotRoute.contains(adjacentEdge);
    if (!isRoadNetworkCycle) {
      for(DirectedEdge trafficDensityReachableEdge : trafficDensityReachableEdges) {
        if (trafficDensityReachableEdge.getOutNode().getOutEdges().contains(adjacentEdge)) {
          return true;
        }
      }
    }
    return isRoadNetworkCycle;
  }

  /**
   * Parameterization class.
   *
   * @author Mariano Kohan
   *
   * @apiviz.exclude
   */
  public static class Parameterizer<O> extends AbstractParameterizer {

    /**
     * Parameter that specifies the name of the file with the road network data
     * <p>
     * Key: {@code -flowscan.roadnetwork}
     * </p>
     */
    public static final OptionID ROAD_NETWORK_FILE_ID = new OptionID("flowscan.roadnetwork", "The file with the road network (shapefile format with line strings).");

    /**
     * Parameter to specify the maximum number of hops
     * considered in the Eps-neighborhood
     * Must be an integer greater than 0.
     */
    public static final OptionID EPSILON_ID = new OptionID("flowscan.epsilon", "The maximum number of hops in the Eps-neighborhood.");

    /**
     * Parameter to specify the threshold for minimum number of moving objects
     * Must be an integer greater than 0.
     */
    public static final OptionID MIN_TRAFFIC_ID = new OptionID("flowscan.mintraffic", "Threshold for minimum of moving objects to identify a hot route.");

    /**
     * Parameter to specify that only the display of trajectories is expected
     * Must be a boolean value (default false)
     */
    public static final OptionID ONLY_TRAJECTORIES = new OptionID("flowscan.onlyTrajectories", "Only display trajectories (do not discover hot routes).");


    protected File roadNetworkFile;
    protected int epsilon = 0;
    protected int minTraffic = 0;
    protected boolean onlyTrajectories = false;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      FileParameter fileP = new FileParameter(ROAD_NETWORK_FILE_ID, FileParameter.FileType.INPUT_FILE);
      if(config.grab(fileP)) {
        roadNetworkFile = fileP.getValue();
      }
      IntParameter epsilonParameter = new IntParameter(EPSILON_ID);
      epsilonParameter.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(epsilonParameter)) {
        epsilon = epsilonParameter.getValue();
      }
      IntParameter minTrafficParameter = new IntParameter(MIN_TRAFFIC_ID);
      minTrafficParameter.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(minTrafficParameter)) {
        minTraffic = minTrafficParameter.getValue();
      }
      Flag onlyTrajectoriesParameter = new Flag(ONLY_TRAJECTORIES);
      if(config.grab(onlyTrajectoriesParameter)) {
        onlyTrajectories = onlyTrajectoriesParameter.isTrue();
      }
    }

    @Override
    protected FlowScan makeInstance() {
      return new FlowScan(roadNetworkFile, epsilon, minTraffic, onlyTrajectories);
    }
  }

}
