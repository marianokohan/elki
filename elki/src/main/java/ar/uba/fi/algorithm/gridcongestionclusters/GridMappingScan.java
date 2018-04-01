package ar.uba.fi.algorithm.gridcongestionclusters;

import java.io.File;

import ar.uba.fi.result.CongestionClusters;
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
import de.lmu.ifi.dbs.elki.result.Result;

/**
 * implementation discovery of congestion clusters from grid mapping of (Liu et. al., 2017)
 *
 * @author mariano kohan
 *
 */
public class GridMappingScan implements Algorithm {

  protected RoadNetwork roadNetwork;
  protected GridSpeeds gridSpeeds;

  public GridMappingScan() {
    // TODO parametrizar;
    File roadNetworkFile = new File("/media/data/doctorado_fiuba/datasets/reales/openstreetmap/mapzen_metro-extracts/Beijing/20170226/osm2pgsl-shapefiles/beijing_china_osm_line.shp");
    double[] area = { 116.201203, 116.545, 40.0257582, 39.754980};
    double sideLen = 0.001;
    //processed format
    // trajectory id (from sampling rate preprocessor); timestamp (in milliseconds); longitude; latitude; speed (in km/h)
    //File data = new File("/media/data/doctorado_fiuba/datasets/reales/ms_research/t-drive/sample_5/processed_speed/v4/1_processed_sampling-rate-cutted_proc-SRF_proc-S.txt");
    //viene automagico de elki

    roadNetwork = RoadNetwork.getInstance(roadNetworkFile);
    roadNetwork.setGridMapping(area, sideLen);

  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.ANY); //TODO: validar si se ajusta impl. DBSCAN con existente elki
  }

  @Override
  public Result run(Database database) {
    this.gridSpeeds = new GridSpeeds(database, this.roadNetwork.getGridMapping());

    CongestionClusters result = new CongestionClusters(this.roadNetwork);
    result.mappedCellsId = this.gridSpeeds.mappedCellsId;
    return result;
  }


}
