package ar.uba.fi.algorithm.gridcongestionclusters;

import java.io.File;
import java.util.Date;

import ar.uba.fi.roadnetwork.RoadNetwork;
import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
/*
 This file is developed to run as part of ELKI:
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

/**
 * discovery of congestion clusters from grid mapping of (Liu et. al., 2017): mapping and
 * calculation of performance indexes
 *
 * @author mariano kohan
 *
 */
public class GridMappingCalculator implements Algorithm {

  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(GridMappingCalculator.class);

  protected RoadNetwork roadNetwork;
  protected GridSpeeds gridSpeeds;

  public GridMappingCalculator(File roadNetworkFile, double areaXMin, double areaXMax, double areaYMax, double areaYMin,  double sideLen) {
    double[] area = { areaXMin, areaXMax, areaYMax, areaYMin};
    roadNetwork = RoadNetwork.getInstance(roadNetworkFile);
    roadNetwork.setGridMapping(area, sideLen);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.ANY); //TODO: validar si se ajusta impl. DBSCAN con existente elki
  }

  @Override
  public Result run(Database database) {
    LOG.info(String.format("[%s] Mapping speeds into grid ...", new Date()));
    this.gridSpeeds = new GridSpeeds(database, this.roadNetwork.getGridMapping());
    LOG.info(String.format("[%s] Calculating cells performance index ...", new Date()));
    this.gridSpeeds.calculateCellsPerformanceIndex();
    LOG.info(String.format("[%s] Dumping cells performance index  ...", new Date()));
    //id attribute, (normalized) performanceIndex
    this.gridSpeeds.dumpCells();

    //TODO: improve using directly the dump (or convert into some kind of processor)
    return new Result() {

      @Override
      public String getShortName() {
        return "grid index dump";
      }

      @Override
      public String getLongName() {
        return "grid index dump";
      }
    };
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
     * Key: {@code -gridmappingscan.roadnetwork}
     * </p>
     */
    public static final OptionID ROAD_NETWORK_FILE_ID = new OptionID("gridmappingscan.roadnetwork", "The file with the road network (shapefile format with line strings).");


    /**
     * Parameter to specify the area bound x1
     * Default value: 116.201203
     */
    public static final OptionID AREA_X_MIN_ID = new OptionID("gridmappingscan.areaX1", "Area bound x1");

    /**
     * Parameter to specify the area bound x2
     * Default value: 116.545
     */
    public static final OptionID AREA_X_MAX_ID = new OptionID("gridmappingscan.areaX2", "Area bound x2");

    /**
     * Parameter to specify the area bound y1
     * Default value: 40.0257582
     */
    public static final OptionID AREA_Y_MAX_ID = new OptionID("gridmappingscan.areaY1", "Area bound y1");

    /**
     * Parameter to specify the area bound y1
     * Default value: 39.754980
     */
    public static final OptionID AREA_Y_MIN_ID = new OptionID("gridmappingscan.areaY2", "Area bound y2");

    /**
     * Parameter to specify the side length of the grid mapping cells
     * Default value: 0.001
     */
    public static final OptionID SIDE_LEN = new OptionID("gridmappingscan.sideLen", "Side length of the grid mapping cells");


    protected File roadNetworkFile;
    protected double areaXMin = 116.201203;
    protected double areaXMax = 116.545;
    protected double areaYMax = 40.0257582;
    protected double areaYMin = 39.754980;
    protected double sideLen = 0.001;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      FileParameter fileP = new FileParameter(ROAD_NETWORK_FILE_ID, FileParameter.FileType.INPUT_FILE);
      if(config.grab(fileP)) {
        roadNetworkFile = fileP.getValue();
      }

      DoubleParameter areaXMinParameter = new DoubleParameter(AREA_X_MIN_ID, 116.201203);
      if(config.grab(areaXMinParameter)) {
        areaXMin = areaXMinParameter.getValue();
      }
      DoubleParameter areaXMaxParameter = new DoubleParameter(AREA_X_MAX_ID, 116.545);
      if(config.grab(areaXMaxParameter)) {
        areaXMax = areaXMaxParameter.getValue();
      }
      DoubleParameter areaYMaxParameter = new DoubleParameter(AREA_Y_MAX_ID, 40.0257582);
      if(config.grab(areaYMaxParameter)) {
        areaYMax = areaYMaxParameter.getValue();
      }
      DoubleParameter areaYMinParameter = new DoubleParameter(AREA_Y_MIN_ID, 39.754980);
      if(config.grab(areaYMinParameter)) {
        areaYMin = areaYMinParameter.getValue();
      }
      DoubleParameter sideLenParameter = new DoubleParameter(SIDE_LEN, 0.001);
      if(config.grab(sideLenParameter)) {
        sideLen = sideLenParameter.getValue();
      }

    }

    @Override
    protected GridMappingCalculator makeInstance() {
      return new GridMappingCalculator(roadNetworkFile, areaXMin, areaXMax, areaYMax, areaYMin, sideLen);
    }
  }

}
