package ar.uba.fi.algorithm.denseroutes;

import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.geotools.graph.structure.DirectedEdge;
import org.opengis.feature.simple.SimpleFeature;

import ar.uba.fi.result.DenseRoute;
import ar.uba.fi.result.DenseRoutes;
import ar.uba.fi.roadnetwork.RoadNetwork;
import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.Logging;
/*
 This file is developed to be used as part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

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
 * Implementation of proposed alternative algorithm to JamFlowScan
 * ("checking the density of individual edge and connecting neighboring edges with high density.")
 *  different splits are considered
 *
 * @author Mariano Kohan
 *
 */

public class DensityScan  implements Algorithm {

  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(DensityScan.class);

  protected RoadNetwork roadNetwork;
  protected EdgeDensities edgeDensities;

  protected static enum EXTEND_DIRECTION { FORWARD, BACKWARD};

  protected int minDensity;

  protected boolean onlyTrajectories = false;

  public DensityScan(File roadNetworkFile, int minDensity, boolean onlyTrajectories) {
    this.roadNetwork = RoadNetwork.getInstance(roadNetworkFile); //TODO: cons. separar de constructor <-> ¿demora en GUI al setear parametros?
    this.minDensity = minDensity;
    this.onlyTrajectories = onlyTrajectories;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.ANY);
    //TODO: mejor alternativa de doc: http://elki.dbs.ifi.lmu.de/releases/release0.6.5~20141030/doc/de/lmu/ifi/dbs/elki/data/type/TypeUtil.html
  }


  //TODO: considerar declaracion de signature devolviendo Result del tipo DenseRoutes (en vez de base Result)
  @Override
  public Result run(Database database) {
    this.edgeDensities = new EdgeDensities(database);
    DenseRoutes denseRoutes = new DenseRoutes(roadNetwork);

    if (!this.onlyTrajectories) {
      Set<String> visitedEdges = new HashSet<String>();
      List<DirectedEdge> denseRoutesStarts = denseRouteStarts();
      LOG.info("discovered " + denseRoutesStarts.size()  + " start edges");

      List<DenseRoute> extendedDenseRoutes = new LinkedList<DenseRoute>();
      List<DenseRoute> forwardExtendedDenseRoutes = null;
      for(DirectedEdge denseEdge : denseRoutesStarts) {
        DenseRoute denseRoute = new DenseRoute(denseEdge);
        visitedEdges.add(((SimpleFeature)denseEdge.getObject()).getID());
        forwardExtendedDenseRoutes = this.extendDenseRoute(denseRoute, EXTEND_DIRECTION.FORWARD, visitedEdges);
        for(DenseRoute forwardExtendedColdRoute : forwardExtendedDenseRoutes) {
          extendedDenseRoutes.addAll(this.extendDenseRoute(forwardExtendedColdRoute, EXTEND_DIRECTION.BACKWARD, visitedEdges));
        }
      }
      denseRoutes.addDenseRoutes(extendedDenseRoutes);
      LOG.info("visited " + visitedEdges.size()  + " start edges");
      LOG.info("DensityScan discovers " + denseRoutes.getDenseRoutes().size() + " dense routes");
      denseRoutes.logDenseRoutesSizeByLength(LOG);

    } else {
      LOG.info("Only display trajectories (dense routes not discovered)");
    }

    return denseRoutes;
  }

  //all road network edges above density threshold
  protected List<DirectedEdge> denseRouteStarts() {
    List<DirectedEdge> denseRoutesStarts = new LinkedList<DirectedEdge>();
    Collection edges = this.roadNetwork.getGraph().getEdges();
    for(Iterator edgesIterator = edges.iterator(); edgesIterator.hasNext();) {
      DirectedEdge edge = (DirectedEdge) edgesIterator.next();
        if (this.appliesThresholdCondition((SimpleFeature)edge.getObject())) {
          denseRoutesStarts.add(edge);
        }
      }
    denseRoutesStarts.sort(new Comparator<DirectedEdge>() {
      @Override
      public int compare(DirectedEdge edge1, DirectedEdge edge2) {
        String edge1ID = ((SimpleFeature)edge1.getObject()).getID();
        String edge2ID = ((SimpleFeature)edge2.getObject()).getID();
        Integer edge1Density = edgeDensities.density(edge1ID);
        Integer edge2Density = edgeDensities.density(edge2ID);
        int densityComparison = -1 * edge1Density.compareTo(edge2Density);
        return (densityComparison == 0) ? edge1ID.compareTo(edge2ID) : densityComparison;
      }

    });
    return denseRoutesStarts;
  }


  private List<DenseRoute> extendDenseRoute(DenseRoute denseRoute, EXTEND_DIRECTION direction, Set<String> visitedEdges) {
    List<DenseRoute> extendedDenseRoutes = new LinkedList<DenseRoute>();

    List<DenseRoute> denseRoutesToExtend = new LinkedList<DenseRoute>();
    denseRoutesToExtend.add(denseRoute);
    List<DenseRoute> denseRoutes;

    while (!denseRoutesToExtend.isEmpty()) {
      denseRoutes = denseRoutesToExtend;
      denseRoutesToExtend = new LinkedList<DenseRoute>();
      for(DenseRoute currentDenseRoute : denseRoutes) {
        Set<DirectedEdge> neighboringDenseEdges = this.getNeighboringDenseEdges(currentDenseRoute, direction);
        if (!neighboringDenseEdges.isEmpty()) {
          for(DirectedEdge neighboringDenseEdge : neighboringDenseEdges) {
            if (!visitedEdges.contains(((SimpleFeature)neighboringDenseEdge.getObject()).getID())) {
              boolean addEdgeToEnd = direction.equals(EXTEND_DIRECTION.FORWARD) ? true: false;
              DenseRoute extendedDenseRoute = currentDenseRoute.copyWithEdge(neighboringDenseEdge, addEdgeToEnd);
              denseRoutesToExtend.add(extendedDenseRoute);
              visitedEdges.add(((SimpleFeature)neighboringDenseEdge.getObject()).getID());
            }
          }
        } else {
          extendedDenseRoutes.add(currentDenseRoute);
        }
      }
    }
    return extendedDenseRoutes;
  }

  private Set<DirectedEdge> getNeighboringDenseEdges(DenseRoute denseRoute, EXTEND_DIRECTION direction) {
    Set<DirectedEdge> neighboringDenseEdges = new HashSet<DirectedEdge>();
    DirectedEdge currentEdge;
    List adjacentEdges = null;
    if (direction.equals(EXTEND_DIRECTION.FORWARD)) {
      currentEdge = denseRoute.getLastEdge();
      adjacentEdges = currentEdge.getOutNode().getOutEdges();
    } else { //EXTEND_DIRECTION.BACKWARD
      currentEdge = denseRoute.getStartEdge();
      adjacentEdges = currentEdge.getInNode().getInEdges();
    }
    for(Iterator adjacentEdgesIterator = adjacentEdges.iterator(); adjacentEdgesIterator.hasNext();) {
      DirectedEdge adjacentEdge = (DirectedEdge) adjacentEdgesIterator.next();
      SimpleFeature adjacentEdgeFeature = (SimpleFeature)adjacentEdge.getObject();
      if (this.appliesThresholdCondition(adjacentEdgeFeature)) {
        neighboringDenseEdges.add(adjacentEdge);
      }
    }
    return neighboringDenseEdges;
  }

  private boolean appliesThresholdCondition(SimpleFeature edge) {
    return this.edgeDensities.density(edge.getID()) >= this.minDensity;
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
    public static final OptionID ROAD_NETWORK_FILE_ID = new OptionID("densityscan.roadnetwork", "The file with the road network (shapefile format with line strings).");

    /**
     * Parameter to specify the threshold for minimum number of moving objects
     * Must be an integer greater than 0.
     */
    public static final OptionID MIN_DENSITY_ID = new OptionID("densityscan.mindensity", "Threshold for minimum of moving objects to identify a dense route.");

    /**
     * Parameter to specify that only the display of trajectories is expected
     * Must be a boolean value (default false)
     */
    public static final OptionID ONLY_TRAJECTORIES = new OptionID("densityscan.onlyTrajectories", "Only display trajectories (do not discover hot routes).");

    protected File roadNetworkFile;
    protected int minDensity = 0;
    protected boolean onlyTrajectories = false;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      FileParameter fileP = new FileParameter(ROAD_NETWORK_FILE_ID, FileParameter.FileType.INPUT_FILE);
      if(config.grab(fileP)) {
        roadNetworkFile = fileP.getValue();
      }
      IntParameter minDensityParameter = new IntParameter(MIN_DENSITY_ID);
      minDensityParameter.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(minDensityParameter)) {
        minDensity = minDensityParameter.getValue();
      }
      Flag onlyTrajectoriesParameter = new Flag(ONLY_TRAJECTORIES);
      if(config.grab(onlyTrajectoriesParameter)) {
        onlyTrajectories = onlyTrajectoriesParameter.isTrue();
      }
    }

    @Override
    protected DensityScan makeInstance() {
      return new DensityScan(roadNetworkFile, minDensity, onlyTrajectories);
    }
  }

}
