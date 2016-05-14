package ar.uba.fi.algorithm.coldroutes;

import java.io.File;
import java.util.Set;

import ar.uba.fi.result.ColdRoutes;
import ar.uba.fi.result.JamRoutes;
import ar.uba.fi.roadnetwork.RoadNetwork;
import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
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

/**
 * Implementation of ColdScan algorithm - discovery of cold routes (alternative paths to jam routes)
 *
 * TODO: complete description
 *
 * @author Mariano Kohan
 *
 */
//TODO: tag '@' - min description
public class ColdScan implements Algorithm {

  private static final Logging LOG = Logging.getLogger(ColdScan.class);

  protected RoadNetwork roadNetwork;

  private double maxTraffic;
//TODO: define field/parameters for BR
  private File jamRoutesFile;

  //TODO: define parameters for BR
  public ColdScan(File roadNetworkFile, double maxTraffic, File jamRoutesFile) {
    this.roadNetwork = RoadNetwork.getInstance(roadNetworkFile); //TODO: cons. separar de constructor <-> ¿demora en GUI al setear parametros?
    this.maxTraffic = maxTraffic;
    this.jamRoutesFile = jamRoutesFile;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.ANY);
  }

  //TODO: considerar declaracion de signature devolviendo Result del tipo ColdRoutes (en vez de base Result)
  @Override
  public Result run(Database database) {
    LOG.info("ColdScan - jam routes file: " + jamRoutesFile); //TODO: tmp for debug test
    Set<String> jamEdgeIds = JamRoutes.parseJamEdgeIds(jamRoutesFile);
    LOG.info("ColdScan - jamEdgeIds: " + jamEdgeIds); //TODO: tmp for debug test
    ColdRoutes coldRoutes = new ColdRoutes(roadNetwork, jamEdgeIds);
    //TODO: to continue discovery

    return coldRoutes;
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
     * Key: {@code -coldscan.roadnetwork}
     * </p>
     */
    public static final OptionID ROAD_NETWORK_FILE_ID = new OptionID("coldscan.roadnetwork", "The file with the road network (shapefile format with line strings).");

    /**
     * Parameter to specify the threshold for maximum number of moving objects
     * Must be an double greater than 0.
     */
    public static final OptionID MAX_TRAFFIC_ID = new OptionID("coldscan.maxTraffic", "Threshold for maximum of moving objects to identify a cold route.");

    /*
     * TODO: define field/parameters for BR
     */

    /**
     * Parameter that specifies the name of the file with the discovered jam routes
     * <p>
     * Key: {@code -coldscan.jamroutes}
     * </p>
     */
    public static final OptionID ROAD_JAM_ROUTES_FILE_ID = new OptionID("coldscan.jamroutes", "The file with the jam routes discovered (and generated) by JamFlowScan.");

    protected File roadNetworkFile;
    private double maxTraffic;
  //TODO: define field/parameters for BR
    private File jamRoutesFile;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      FileParameter roadNetworkParameter = new FileParameter(ROAD_NETWORK_FILE_ID, FileParameter.FileType.INPUT_FILE);
      if(config.grab(roadNetworkParameter)) {
        roadNetworkFile = roadNetworkParameter.getValue();
      }
      DoubleParameter jamSpeemaxTrafficParameter = new DoubleParameter(MAX_TRAFFIC_ID);
      if(config.grab(jamSpeemaxTrafficParameter)) {
        maxTraffic = jamSpeemaxTrafficParameter.getValue();
      }
      FileParameter jamRoutesFileParameter = new FileParameter(ROAD_JAM_ROUTES_FILE_ID, FileParameter.FileType.INPUT_FILE);
      if(config.grab(jamRoutesFileParameter)) {
        jamRoutesFile = jamRoutesFileParameter.getValue();
      }
    }

    @Override
    protected ColdScan makeInstance() {
      return new ColdScan(roadNetworkFile, maxTraffic, jamRoutesFile);
    }
  }

}
