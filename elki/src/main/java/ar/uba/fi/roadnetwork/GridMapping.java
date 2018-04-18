package ar.uba.fi.roadnetwork;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
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
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;
import org.opengis.geometry.BoundingBox;

import com.vividsolutions.jts.geom.Coordinate;

import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * @author mariano kohan
 *
 */
public class GridMapping {

  public int timeSliceLength = 15 * 60; //15 min - TODO: consider parametrize

  private SimpleFeatureSource grid;
  private ReferencedEnvelope bounds;
  private double sideLen;

  private static final Logging LOG = Logging.getLogger(GridMapping.class);
  private static final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

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

  public SimpleFeature snapPointToCell(Coordinate pointCoordinate) {
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
          return featureCell;
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
    return cell;
  }

  public double calculateDistance(SimpleFeature cellFeature1, SimpleFeature cellFeature2) {
    double distanceX = cellFeature1.getBounds().getMinX() - cellFeature2.getBounds().getMinX();
    if (distanceX < 0) {
      distanceX = -1 * distanceX;
    }
    distanceX = distanceX / sideLen;

    double distanceY = cellFeature1.getBounds().getMinY() - cellFeature2.getBounds().getMinY();
    if (distanceY < 0) {
      distanceY = -1 * distanceY;
    }
    distanceY = distanceY / sideLen;

    double distance = (distanceY > distanceX) ? distanceY : distanceX;
    return distance;
  }

  public SimpleFeatureCollection getGridCellFeatures() {
    try {
      return this.grid.getFeatures();
    }
    catch(IOException e) {
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
    }
    return null;
  }

  public SimpleFeatureCollection getCellFeatures(Set<String> cellsId) {

    Set<FeatureId> fids = new HashSet<>();
    for (String id : cellsId) {
        FeatureId fid = ff.featureId(id);
        fids.add(fid);
    }
    Filter filter = ff.id(fids);
    try {
      return grid.getFeatures(filter);
    }
    catch(IOException e) {
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
    }
    return null;
  }

  public SimpleFeatureCollection getCellFeatureFromAttributesId(Set<Integer> cellsId) {
    List<Filter> cellIdFilters = new LinkedList<Filter>();
    for(Integer cellId : cellsId) {
      cellIdFilters.add(ff.equal(ff.property("id"), ff.literal(cellsId), false));
    }

    Filter filter = ff.or(cellIdFilters);
    try {
      return grid.getFeatures(filter);
    }
    catch(IOException e) {
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
    }
    return null;
  }

  public SimpleFeatureCollection getCellFeatureFromAttributeId(Integer cellId) {
    Filter filter = ff.equal(ff.property("id"), ff.literal(cellId), false);
    try {
      return grid.getFeatures(filter);
    }
    catch(IOException e) {
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
    }
    return null;
  }

  public SimpleFeatureCollection getRangeForCell(SimpleFeature cell, double range) {
    BoundingBox edgeBoudingBox = cell.getBounds();
    ReferencedEnvelope boundingRectangle = new ReferencedEnvelope(edgeBoudingBox);
    double expand = this.sideLen * range * 0.9 ;
    boundingRectangle.expandBy(expand, expand);
    FeatureType schema = grid.getSchema();
    String geometryPropertyName = schema.getGeometryDescriptor().getLocalName();
    Filter neighborhoodFilter = ff.bbox(ff.property(geometryPropertyName), boundingRectangle);
    try {
      return grid.getFeatures(neighborhoodFilter);
    }
    catch(IOException e) {
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
    }
    return null;
  }

  public SimpleFeatureCollection getRangeForCell(Integer cellId, double range) throws IOException {
    SimpleFeature cell = this.getCellFeatureFromAttributeId(cellId).features().next();
    return this.getRangeForCell(cell, range);
  }

  public int mapTimestampToSlice(long timestamp) {
    DateTime timestampDateTime = new DateTime(timestamp, DateTimeZone.UTC);
    return timestampDateTime.getSecondOfDay() / timeSliceLength;
  }

}
