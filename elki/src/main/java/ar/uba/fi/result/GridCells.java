package ar.uba.fi.result;

import java.awt.Color;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;

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
 * grid cells from grid mapping with calculated performance index (Liu et. al., 2017)
 *
 * @author mariano kohan
 *
 */
public class GridCells implements Result {

  protected RoadNetwork roadNetwork;

  public Map<Integer, Double> cellsPerformanceIndex;

  public GridCells(RoadNetwork gridMappedRoadNetwork, Map<Integer, Double> cellsPerformanceIndex) {
    this.roadNetwork = gridMappedRoadNetwork;
    this.cellsPerformanceIndex = cellsPerformanceIndex;
  }

  @Override
  public String getLongName() {
    return "Grid mapping cells with performance index";
  }

  @Override
  public String getShortName() {
    return "grid mapping cells";
  }

  public RoadNetwork getRoadNetwork() {
    return roadNetwork;
  }

  public Map<Integer, Double> getCells() {
    return cellsPerformanceIndex;
  }

  public List<CellCategory>  getCellsCategorized() {
    List<CellCategory> cellCategories = new LinkedList<CellCategory>();
    cellCategories.add(new CellCategory(0, 25, new Color(252, 186, 120)));
    cellCategories.add(new CellCategory(25, 50, new Color(252, 155, 59)));
    cellCategories.add(new CellCategory(50, 75, new Color(252, 99, 99)));
    cellCategories.add(new CellCategory(75, 101, new Color(252, 7, 7)));

    try {
      SimpleFeatureCollection featureCellss = roadNetwork.getGridMapping().getGrid().getFeatures();
      for(SimpleFeatureIterator iterator = featureCellss.features(); iterator.hasNext();) {
        SimpleFeature featureCell = iterator.next();
        Integer cellId = (Integer)featureCell.getAttribute("id");
        Double cellPerformanceIndex = cellsPerformanceIndex.get(cellId);
        if (cellPerformanceIndex != null) {
          for(CellCategory cellCategory : cellCategories) {
            if (cellCategory.contains(cellPerformanceIndex)) {
              cellCategory.add(featureCell);
            }
          }
        }
      }
    }
    catch(IOException e) {
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
    }

    return cellCategories;
  }


  public class CellCategory {
    double min;
    double max;
    Color fill;
    Set<String> cells;
    List<SimpleFeature> features;

    public CellCategory(double min, double max, Color fill) {
      this.min = min;
      this.max = max;
      this.fill = fill;
      this.cells = new HashSet<String>();
      this.features = new LinkedList<SimpleFeature>();
    }

    public Color getFill() {
      return this.fill;
    }

    public SimpleFeatureCollection getCellFeatures() {
      DefaultFeatureCollection cellsFeatureCollection = new DefaultFeatureCollection();
      cellsFeatureCollection.addAll(features);
      return cellsFeatureCollection;
    }

    public boolean contains(double index) {
      return (index >= this.min) && (index < this.max);
    }

    public void add(String cellId) {
      this.cells.add(cellId);
    }

    public void add(SimpleFeature cellFeature) {
      this.features.add(cellFeature);
    }

  }


}
