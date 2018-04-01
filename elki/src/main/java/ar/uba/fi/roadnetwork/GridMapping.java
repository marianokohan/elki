package ar.uba.fi.roadnetwork;

import java.io.IOException;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.grid.Grids;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;

import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * @author mariano kohan
 *
 */
public class GridMapping {

  private SimpleFeatureSource grid;
  private ReferencedEnvelope bounds;
  private double sideLen;

  private static final Logging LOG = Logging.getLogger(GridMapping.class);

  public GridMapping(ReferencedEnvelope gridBounds, double sideLen) {
    this.bounds = gridBounds;
    this.sideLen = sideLen;
    this.initGrid();
  }

  private void initGrid() {
    this.grid = Grids.createSquareGrid(bounds, sideLen);
  }

  public SimpleFeatureSource getGrid() {
    return grid;
  }

  public String snapPointToCell(Coordinate pointCoordinate) {
    SimpleFeature cell = null;
    SimpleFeatureCollection features;

    /*
    FilterFactory2 filterFactory = CommonFactoryFinder.getFilterFactory2();
    GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
    GeometryDescriptor geomDescriptor = this.grid.getSchema().getGeometryDescriptor();
    String geometryAttributeName = geomDescriptor.getLocalName();
    Geometry point = geometryFactory.createPoint(pointCoordinate);
    Filter containsPointFilter = filterFactory.contains(filterFactory.property(geometryAttributeName), filterFactory.literal(point));

    try {
      features = this.grid.getFeatures(containsPointFilter);
      if (features.isEmpty()) {
        LOG.error("no features contains point: " + pointCoordinate);
      } else if (features.size() > 1) {
        LOG.error("more than one cell contains point: " + pointCoordinate);
      } else {
        cell = features.features().next();
      }
    }
    catch(IOException e) {
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
    }
     */
    //simple version (seems faster)
    try {
      features = this.grid.getFeatures();
      for(SimpleFeatureIterator iterator = features.features(); iterator.hasNext();) {
        SimpleFeature featureCell = iterator.next();
        if (featureCell.getBounds().contains(pointCoordinate.x, pointCoordinate.y)) {
          return featureCell.getID();
        }

      }
    }
    catch(IOException e) {
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
    }
    if (cell == null) {
      LOG.error("no cell contains point: " + pointCoordinate);
      return null;
    }
    return cell.getID();
  }

}
