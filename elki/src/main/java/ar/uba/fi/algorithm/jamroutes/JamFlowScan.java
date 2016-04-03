package ar.uba.fi.algorithm.jamroutes;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.geotools.graph.structure.DirectedEdge;
import org.opengis.feature.simple.SimpleFeature;

import ar.uba.fi.algorithm.hotroutes.FlowScan;
//TODO: confirm license description
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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
import ar.uba.fi.algorithm.hotroutes.TrafficSets;
import ar.uba.fi.result.JamRoute;
import ar.uba.fi.result.JamRoutes;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Implementation of JamFlowScan algorithm - discovery of (traffic density-based) jam routes
 *
 * TODO: complete description
 *
 * @author Mariano Kohan
 *
 */
//TODO: tag '@' - min description
public class JamFlowScan extends FlowScan {

  private static final Logging LOG = Logging.getLogger(JamFlowScan.class);

  private Speeds speeds;

  private double jamSpeed;

  public JamFlowScan(File roadNetworkFile, int epsilon, int minTraffic, double jamSpeed, boolean onlyTrajectories) {
    super(roadNetworkFile, epsilon, minTraffic, onlyTrajectories);
    this.jamSpeed = jamSpeed;
  }


  //TODO: considerar declaracion de signature devolviendo Result del tipo HotRoutes (en vez de base Result)
  @Override
  public Result run(Database database) {
    //TODO: iterate through database once in order to instantiate these objects
    trafficSets = new TrafficSets(database);
    speeds = new Speeds(database);

    JamRoutes jamRoutes = new JamRoutes(roadNetwork);
    if (!this.onlyTrajectories) {
      List<DirectedEdge> hotRoutesStarts = discoverHotRouteStarts();
      for(DirectedEdge hotRouteStart : hotRoutesStarts) {
        String edgeId = ((SimpleFeature)hotRouteStart.getObject()).getID();
        JamRoute jamRoute = new JamRoute(hotRouteStart, verifiesJamSpeed(edgeId));
        extendJamRoute(jamRoute, jamRoutes);
      }
      LOG.info("JamFlowScan discovers " + jamRoutes.getJamRoutes().size() + " jam routes");
    } else {
      LOG.info("Only display trajectories (jam routes not discovered)");
    }

    return jamRoutes;
  }

  private boolean verifiesJamSpeed(String edgeId) {
    return speeds.speed(edgeId) <= this.jamSpeed;
  }

  private void extendJamRoute(JamRoute jamRoute, JamRoutes jamRoutes) {
    Set<DirectedEdge> directlyTrafficDensityReachableEdges = getDirectlyTrafficDensityReachableEdges(jamRoute);
    if (directlyTrafficDensityReachableEdges.size() != 0) {
      boolean jamRouteExtended = false;
      for(DirectedEdge directlyTrafficDensityReachableEdge : directlyTrafficDensityReachableEdges) {
        if (isRouteTrafficDensityReachable(directlyTrafficDensityReachableEdge, jamRoute)) {
          boolean isJam = this.isDirectlyTrafficJamReachable(directlyTrafficDensityReachableEdge, jamRoute);
          JamRoute extendedJamRoute = jamRoute.copyWithEdge(directlyTrafficDensityReachableEdge, isJam);
          jamRouteExtended = true;
          extendJamRoute(extendedJamRoute, jamRoutes);
        }
      }
      if (!jamRouteExtended) {
        jamRoutes.addJamRoute(jamRoute);
      }
    } else {
      jamRoutes.addJamRoute(jamRoute);
    }
  }

  /**
   * verifies definition 2 (directly traffic jam-reachable) for the given edge
   * with respect to last edge of the jam route
   * @param directlyTrafficDensityReachableEdge
   * @param jamRoute
   * @return
   */
  private boolean isDirectlyTrafficJamReachable(DirectedEdge directlyTrafficDensityReachableEdge, JamRoute jamRoute) {
    //only condition #3 - the other were verified previously (when obtained on method "getDirectlyTrafficDensityReachableEdges(...)")
    String edgeId = ((SimpleFeature)directlyTrafficDensityReachableEdge.getObject()).getID();
    DirectedEdge lastEdge = jamRoute.getLastEdge();
    String lastEdgeId = ((SimpleFeature)lastEdge.getObject()).getID();
    return verifiesJamSpeed(edgeId) || verifiesJamSpeed(lastEdgeId);
  }

  /**
   * Parameterization class.
   *
   * @author Mariano Kohan
   *
   * @apiviz.exclude
   */
  public static class Parameterizer<O> extends FlowScan.Parameterizer<O> {

    /**
     * Parameter to specify the threshold for maximum speed of a traffic jam
     * Must be an integer greater than 0.
     */
    public static final OptionID JAM_SPEED_ID = new OptionID("jamflowscan.jamspeed", "Threshold for maximum speed to identify a jam inside a jam route");

    private double jamSpeed = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter jamSpeedParameter = new DoubleParameter(JAM_SPEED_ID);
      if(config.grab(jamSpeedParameter)) {
        jamSpeed = jamSpeedParameter.getValue();
      }
    }

    @Override
    protected JamFlowScan makeInstance() {
      return new JamFlowScan(roadNetworkFile, epsilon, minTraffic, jamSpeed, onlyTrajectories);
    }
  }

}
