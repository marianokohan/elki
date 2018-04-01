package ar.uba.fi.result;

import java.util.Set;

import ar.uba.fi.roadnetwork.RoadNetwork;
import de.lmu.ifi.dbs.elki.result.Result;
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

/**
 * congestion clusters discovered from grid mapping (Liu et. al., 2017)
 *
 * @author mariano kohan
 *
 */
public class CongestionClusters implements Result {

  protected RoadNetwork roadNetwork;

  //public List<SimpleFeature> mappedCells; //TODO: to be improved into clusters
  public Set<String> mappedCellsId;

  public CongestionClusters(RoadNetwork gridMappedRoadNetwork) {
    this.roadNetwork = gridMappedRoadNetwork;
  }

  @Override
  public String getLongName() {
    return "Grid mapping congestion clusters";
  }

  @Override
  public String getShortName() {
    return "Congestion clusters";
  }

  public RoadNetwork getRoadNetwork() {
    return roadNetwork;
  }

}
