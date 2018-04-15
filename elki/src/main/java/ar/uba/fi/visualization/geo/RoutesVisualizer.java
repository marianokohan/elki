package ar.uba.fi.visualization.geo;
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

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.identity.FeatureIdImpl;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.event.MapMouseEvent;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;

import ar.uba.fi.result.Routes;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * @author Mariano Kohan
 *
 */
public class RoutesVisualizer extends MapVisualizer {
/*
 * TODO: consider refactor according to visualize trajectories, only first one (if required) , ....
 */

  private static final Color TRAJECTORY_COLOR = new Color(50, 100, 228);
  static final Logging LOG = Logging.getLogger(RoutesVisualizer.class);
  protected JMapFrame mapFrame;

  public RoutesVisualizer() {
    super();
  }

  private void displayFirstTrajectory(Routes routes, Database database) {
    SimpleFeatureSource featureSource = routes.getRoadNetwork().getRoadsFeatureSource();

    MapContent map = new MapContent();
    map.setTitle(routes.getLongName());

    map.addLayer(createRoadNetworkLayer(featureSource));
    //TODO: these methods should be reviewed with new "only numbers" database format
    map.addLayer(createFirstTrajectoryEdgesLayer(featureSource, database));
    map.addLayer(createFirstTrajectoryLayer(featureSource, database));
    map.addLayer(createFirstTrajectoryPointLayer(featureSource, database));

    JMapFrame.showMap(map);
  }

  protected void displayTrajectories(Routes routes, Database database) {
    SimpleFeatureSource featureSource = routes.getRoadNetwork().getRoadsFeatureSource();
    String edgeFeaturePrefix = routes.getRoadNetwork().getEdgeFeaturePrefix();

    MapContent map = new MapContent();
    map.setTitle(routes.getLongName());

    map.addLayer(createRoadNetworkLayer(featureSource));
    map.addLayer(createTrajectoriesEdgesLayer(featureSource, edgeFeaturePrefix, database));
    map.addLayer(createTrajectoriesLayer(featureSource, database));
    //TODO: commented to avoid memory issue on visualization of big datasets
//    map.addLayer(createTrajectoriesPointsLayer(featureSource, database));

    JMapFrame.showMap(map);
  }

  private FeatureLayer createTrajectoriesPointsLayer(SimpleFeatureSource featureSource, Database database) {
    return createPointGeometriesLayer(featureSource, createTrajectoriesPointGeometries(database));
  }

  private FeatureLayer createFirstTrajectoryPointLayer(SimpleFeatureSource featureSource, Database database) {
    return createPointGeometriesLayer(featureSource, createFirstTrajectoryPointGeometries(database));
  }

  private FeatureLayer createPointGeometriesLayer(SimpleFeatureSource featureSource, List<Geometry> geometries) {
      StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
      FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory2();

      DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();
      for (Geometry geometry : geometries)
      {
          SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
          builder.setName("Trajectory");
          builder.setCRS(DefaultGeographicCRS.WGS84); // Coordinate reference system
          builder.add("the_geom", Point.class);
          builder.length(15).add("Name", String.class); // 15 chars width for name field

          SimpleFeatureType pointType = builder.buildFeatureType();

          SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(pointType);
          simpleFeatureBuilder.add(geometry);

          SimpleFeature pointFeature = simpleFeatureBuilder.buildFeature(null);
          featureCollection.add(pointFeature);
      }

      /*
      Stroke markStroke = styleFactory.createStroke(
              filterFactory.literal(new Color(0xC8, 0x46, 0x63)),
              //circle thickness
              filterFactory.literal(1)
      );
      */

      Fill markFill = styleFactory.createFill(
              filterFactory.literal(new Color(24, 116, 205)));

      //Type of symbol
      Mark mark = styleFactory.getDefaultMark();
      mark.setFill(markFill);
  //    mark.setStroke(markStroke);

      Graphic graphic = styleFactory.createDefaultGraphic();
      graphic.graphicalSymbols().clear();
      graphic.graphicalSymbols().add(mark);

      //circle dimension on the map
      graphic.setSize(filterFactory.literal(5));

      GeometryDescriptor geomDescriptor = featureSource.getSchema().getGeometryDescriptor();
      String geometryAttributeName = geomDescriptor.getLocalName();
      PointSymbolizer pointSymbolizer = styleFactory.createPointSymbolizer(graphic, geometryAttributeName);

      Rule pointRule = styleFactory.createRule();
      pointRule.symbolizers().add(pointSymbolizer);
      Rule rules[] = {pointRule};
      FeatureTypeStyle featureTypeStyle = styleFactory.createFeatureTypeStyle(rules);
      Style pointStyle = styleFactory.createStyle();
      pointStyle.featureTypeStyles().add(featureTypeStyle);

      FeatureLayer featureLayer = new FeatureLayer(featureCollection, pointStyle);
      return featureLayer;
    }

  private List<Geometry> createTrajectoriesPointGeometries(Database database) {
    List<Geometry> geometries = new LinkedList<Geometry>();
    Relation<DoubleVector> trRelation = database.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD, null);
    GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
    for(DBIDIter iditer = trRelation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      DoubleVector trVector = trRelation.get(iditer);
      Coordinate pointCoordinate = new Coordinate(trVector.doubleValue(3), trVector.doubleValue(4));
      geometries.add(geometryFactory.createPoint(pointCoordinate));
      //TODO: handle geometries as MultiPoint for each trajectory?
    }
    return geometries;
  }

  private List<Geometry> createFirstTrajectoryPointGeometries(Database database) {
    List<Geometry> geometries = new LinkedList<Geometry>();
    Relation<DoubleVector> pointRelation = database.getRelation(TypeUtil.NUMBER_VECTOR_FIELD_2D, null);
    Relation<LabelList> trTimestampRelation = database.getRelation(TypeUtil.LABELLIST, null);
    String trajectoryId = null;
    boolean trajectoryCompleted = false;
    for(DBIDIter iditer = pointRelation.iterDBIDs(); iditer.valid() && !trajectoryCompleted; iditer.advance()) {
      LabelList trajectoryElement = trTimestampRelation.get(iditer);
      if (trajectoryId == null)
      {
        trajectoryId = trajectoryElement.get(0);
      }
      else
      {
        if (!trajectoryId.equals(trajectoryElement.get(0)))
        {
          trajectoryCompleted = true;
        }
      }
      DoubleVector pointElement = pointRelation.get(iditer);
      Coordinate pointCoordinate = new Coordinate(pointElement.doubleValue(0), pointElement.doubleValue(1));
      geometries.add(createPoint(pointCoordinate));
    }
    return geometries;
  }

  protected FeatureLayer createTrajectoriesLayer(SimpleFeatureSource featureSource, Database database) {
    return createLineGeometriesLayer(featureSource, createTrajectoriesGeometries(database));
  }

  private FeatureLayer createFirstTrajectoryLayer(SimpleFeatureSource featureSource, Database database) {
    return createLineGeometriesLayer(featureSource, createFirstTrajectoryGeometries(database));
  }

  private FeatureLayer createLineGeometriesLayer(SimpleFeatureSource featureSource, List<Geometry> lineGeometries) {
    StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
    FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory2();

    DefaultFeatureCollection featureCollection = edgeGeometriesToFeatureCollection(lineGeometries);

    Stroke markStroke = styleFactory.createStroke(filterFactory.literal(TRAJECTORY_COLOR),
            filterFactory.literal(3));

    Fill markFill = styleFactory.createFill(
            filterFactory.literal(new Color(0x7f, 0xC8, 0x61)));

    GeometryDescriptor geomDescriptor = featureSource.getSchema().getGeometryDescriptor();
    String geometryAttributeName = geomDescriptor.getLocalName();
    LineSymbolizer lineSymbolizer = styleFactory.createLineSymbolizer(markStroke, geometryAttributeName);

    Rule lineRule = styleFactory.createRule();
    lineRule.symbolizers().add(lineSymbolizer);
    Rule rules[] = {lineRule};
    FeatureTypeStyle featureTypeStyle = styleFactory.createFeatureTypeStyle(rules);
    Style lineStyle = styleFactory.createStyle();
    lineStyle.featureTypeStyles().add(featureTypeStyle);

    FeatureLayer featureLayer = new FeatureLayer(featureCollection, lineStyle);
    return featureLayer;
  }

  private List<Geometry> createFirstTrajectoryGeometries(Database database) {
    List<Geometry> geometries = new LinkedList<Geometry>();
    Relation<DoubleVector> pointRelation = database.getRelation(TypeUtil.NUMBER_VECTOR_FIELD_2D, null);
    Relation<LabelList> trTimestampRelation = database.getRelation(TypeUtil.LABELLIST, null);
    String trajectoryId = null;
    boolean trajectoryCompleted = false;
    List<Coordinate> trayectoryCoordinates = new LinkedList<Coordinate>();
    for(DBIDIter iditer = pointRelation.iterDBIDs(); iditer.valid() && !trajectoryCompleted; iditer.advance()) {
      LabelList trajectoryElement = trTimestampRelation.get(iditer);
      if (trajectoryId == null)
      {
        trajectoryId = trajectoryElement.get(0);
      }
      else
      {
        if (!trajectoryId.equals(trajectoryElement.get(0)))
        {
          trajectoryCompleted = true;
        }
      }
      DoubleVector pointElement = pointRelation.get(iditer);
      trayectoryCoordinates.add(new Coordinate(pointElement.doubleValue(0), pointElement.doubleValue(1)));
    }
    GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
    LineString trayectoryGeometry = geometryFactory.createLineString(trayectoryCoordinates.toArray(new Coordinate[]{}));
    geometries.add(trayectoryGeometry);
    return geometries;
  }

  private List<Geometry> createTrajectoriesGeometries(Database database) {
    List<Geometry> geometries = new LinkedList<Geometry>();
    Relation<DoubleVector> transactionRelation = database.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD, null);
    Integer trajectoryId = null;
    GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
    List<Coordinate> trayectoryCoordinates = new LinkedList<Coordinate>();
    for(DBIDIter iditer = transactionRelation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      DoubleVector trajectoryVector = transactionRelation.get(iditer);
      if ((trajectoryId != null) && (!trajectoryId.equals(trajectoryVector.intValue(0))) ) {
        if (trayectoryCoordinates.size() == 1) {
          LOG.warning("Attemp to create geometry for trajectory of lenght 1 - TrId = " + trajectoryId);
        } else {
          LineString trayectoryGeometry = geometryFactory.createLineString(trayectoryCoordinates.toArray(new Coordinate[]{}));
          geometries.add(trayectoryGeometry);
        }
        trayectoryCoordinates = new LinkedList<Coordinate>();
      }
      DoubleVector pointElement = transactionRelation.get(iditer);
      trayectoryCoordinates.add(new Coordinate(trajectoryVector.doubleValue(3), trajectoryVector.doubleValue(4)));
      trajectoryId = trajectoryVector.intValue(0);
    }
    //for last trajectory - TODO: consider to improve
    if (trayectoryCoordinates.size() == 1) {
      LOG.warning("Attemp to create geometry for trajectory of lenght 1 - TrId = " + trajectoryId);
    } else {
      LineString trayectoryGeometry = geometryFactory.createLineString(trayectoryCoordinates.toArray(new Coordinate[]{}));
      geometries.add(trayectoryGeometry);
    }
    return geometries;
  }

  private FeatureLayer createTrajectoriesEdgesLayer(SimpleFeatureSource featureSource, String edgeFeaturePrefix, Database database) {
    return createEdgeGeometriesLayer(featureSource, createTrajectoriesEdgesGeometries(database, edgeFeaturePrefix, featureSource));
  }

  private FeatureLayer createFirstTrajectoryEdgesLayer(SimpleFeatureSource featureSource, Database database) {
    return createEdgeGeometriesLayer(featureSource, createFirstTrajectoryEdgeGeometries(database, featureSource));
  }

  protected DefaultFeatureCollection edgeGeometriesToFeatureCollection(List<Geometry> edgeGeometries) {
    DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();

    //TODO: review if correct location of this block (out of for cycle
    SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
    builder.setName("Trajectory");
    builder.setCRS(DefaultGeographicCRS.WGS84); // Coordinate reference system
    builder.add("the_geom", LineString.class);
    builder.length(15).add("Name", String.class); // 15 chars width for name field

    SimpleFeatureType lineType = builder.buildFeatureType();
    SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(lineType);
    for (Geometry geometry : edgeGeometries)
    {
        simpleFeatureBuilder.add(geometry);
        SimpleFeature lineFeature = simpleFeatureBuilder.buildFeature(null);
        featureCollection.add(lineFeature);
    }
    return featureCollection;
  }

  private FeatureLayer createEdgeGeometriesLayer(SimpleFeatureSource featureSource, List<Geometry> edgeGeometries) {
    DefaultFeatureCollection featureCollection = edgeGeometriesToFeatureCollection(edgeGeometries);

    //TODO: review refactor with 'createEdgesLayer'
    StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
    FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory2();
    Stroke markStroke = styleFactory.createStroke(filterFactory.literal(new Color(238, 238, 0)),
            filterFactory.literal(8));

    GeometryDescriptor geomDescriptor = featureSource.getSchema().getGeometryDescriptor();
    String geometryAttributeName = geomDescriptor.getLocalName();
    LineSymbolizer lineSymbolizer = styleFactory.createLineSymbolizer(markStroke, geometryAttributeName);

    Rule lineRule = styleFactory.createRule();
    lineRule.symbolizers().add(lineSymbolizer);
    Rule rules[] = {lineRule};
    FeatureTypeStyle featureTypeStyle = styleFactory.createFeatureTypeStyle(rules);
    Style lineStyle = styleFactory.createStyle();
    lineStyle.featureTypeStyles().add(featureTypeStyle);

    FeatureLayer featureLayer = new FeatureLayer(featureCollection, lineStyle);
    return featureLayer;
  }

  private List<Geometry> createFirstTrajectoryEdgeGeometries(Database database, SimpleFeatureSource featureSource) {
  List<Geometry> edgeGeometries = new LinkedList<Geometry>();
  Relation<LabelList> trTimestampRelation = database.getRelation(TypeUtil.LABELLIST, null);
  String trajectoryId = null;
  boolean trajectoryCompleted = false;
  List<FeatureIdImpl> edgeIds = new LinkedList<FeatureIdImpl>();

  for(DBIDIter iditer = trTimestampRelation.iterDBIDs(); iditer.valid() && !trajectoryCompleted; iditer.advance()) {
    LabelList trajectoryElement = trTimestampRelation.get(iditer);
    if (trajectoryId == null)
    {
      trajectoryId = trajectoryElement.get(0);
    }
    else
    {
      if (!trajectoryId.equals(trajectoryElement.get(0)))
      {
        trajectoryCompleted = true;
      }
    }
    edgeIds.add(new FeatureIdImpl(trajectoryElement.get(2)));
  }
  FilterFactory2 ffilterFactory = CommonFactoryFinder.getFilterFactory2();
    SimpleFeatureCollection selectedFeatures = null;
    SimpleFeatureIterator simpleFeatureIterator = null;
    try {
      selectedFeatures = featureSource.getFeatures(ffilterFactory.id(edgeIds.toArray(new FeatureId[] {})));
      simpleFeatureIterator = selectedFeatures.features();
      while (simpleFeatureIterator.hasNext()) {
        edgeGeometries.add((Geometry)simpleFeatureIterator.next().getDefaultGeometry());
      }
    }
    catch(IOException e) {
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
    }
    finally {
      if (simpleFeatureIterator != null)
        simpleFeatureIterator.close();
    }
  return edgeGeometries;
  }

  private List<Geometry> createTrajectoriesEdgesGeometries(Database database, String edgeFeaturePrefix, SimpleFeatureSource featureSource) {
  List<Geometry> edgeGeometries = new LinkedList<Geometry>();
  Relation<DoubleVector> trRelation = database.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD, null);
  boolean trajectoryCompleted = false;
  List<FeatureIdImpl> edgeIds = new LinkedList<FeatureIdImpl>();

  for(DBIDIter iditer = trRelation.iterDBIDs(); iditer.valid() && !trajectoryCompleted; iditer.advance()) {
    DoubleVector transactionVector = trRelation.get(iditer);
    edgeIds.add(new FeatureIdImpl(edgeFeaturePrefix + transactionVector.intValue(2)));
  }
  FilterFactory2 ffilterFactory = CommonFactoryFinder.getFilterFactory2();
    SimpleFeatureCollection selectedFeatures = null;
    SimpleFeatureIterator simpleFeatureIterator = null;
    try {
      selectedFeatures = featureSource.getFeatures(ffilterFactory.id(edgeIds.toArray(new FeatureId[] {})));
      simpleFeatureIterator = selectedFeatures.features();
      while (simpleFeatureIterator.hasNext()) {
        edgeGeometries.add((Geometry)simpleFeatureIterator.next().getDefaultGeometry());
      }
    }
    catch(IOException e) {
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
    }
    finally {
      if (simpleFeatureIterator != null)
        simpleFeatureIterator.close();
    }
  return edgeGeometries;
  }


  /*************************************
   * methods used by child visualizers
   * TODO : consider to improve previous visualization method according to these ones
   *************************************/

  protected FeatureLayer createEdgesLayer(SimpleFeatureCollection edges, SimpleFeatureSource featureSource, Color strokeColor, int strokeWitdh) {
    Rule rules[] = {createLineStyleRule(featureSource, strokeColor, strokeWitdh)};
    return createLayer(edges, rules);
  }

  protected Rule createLineStyleRule(SimpleFeatureSource featureSource, Color strokeColor, int strokeWitdh) {
    Stroke markStroke = styleFactory.createStroke(filterFactory.literal(strokeColor),
            filterFactory.literal(strokeWitdh));

    GeometryDescriptor geomDescriptor = featureSource.getSchema().getGeometryDescriptor();
    String geometryAttributeName = geomDescriptor.getLocalName();
    LineSymbolizer lineSymbolizer = styleFactory.createLineSymbolizer(markStroke, geometryAttributeName);

    Rule lineRule = styleFactory.createRule();
    lineRule.symbolizers().add(lineSymbolizer);
    return lineRule;
  }

  protected SimpleFeatureCollection getSelectedFeaturedFromClick(MapMouseEvent ev, SimpleFeatureSource featureSource) {
    //Construct a 5x5 pixel rectangle centered on the mouse click position
    java.awt.Point screenPos = ev.getPoint();
    Rectangle screenRect = new Rectangle(screenPos.x-2, screenPos.y-2, 5, 5);
    /*
     * Transform the screen rectangle into bounding box in the coordinate
     * reference system of our map context. Note: we are using a naive method
     * here but GeoTools also offers other, more accurate methods.
     */
    AffineTransform screenToWorld = mapFrame.getMapPane().getScreenToWorldTransform();
    Rectangle2D worldRect = screenToWorld.createTransformedShape(screenRect).getBounds2D();
    ReferencedEnvelope bbox = new ReferencedEnvelope(
            worldRect,
            mapFrame.getMapContent().getCoordinateReferenceSystem());
    return filterFeaturesInBox(bbox, featureSource);
  }

  protected SimpleFeatureCollection getSelectedFeatureFromMap(SimpleFeatureSource featureSource) {
    ReferencedEnvelope bounds = mapFrame.getMapContent().getViewport().getBounds();
    return filterFeaturesInBox(bounds, featureSource);
  }

  private SimpleFeatureCollection filterFeaturesInBox(ReferencedEnvelope box, SimpleFeatureSource featureSource) {
    GeometryDescriptor geomDescriptor = featureSource.getSchema().getGeometryDescriptor();
    String geometryAttributeName = geomDescriptor.getLocalName();
    Filter filter = filterFactory.intersects(filterFactory.property(geometryAttributeName), filterFactory.literal(box));

    SimpleFeatureCollection selectedFeatures = null;
    try {
        selectedFeatures = featureSource.getFeatures(filter);
    } catch (Exception ex) {
        ex.printStackTrace();
    }
    return selectedFeatures;
  }

  protected void exportToGeoJson(SimpleFeatureCollection features, String fileName) {
    Path jamRoutesGeoJsonPath = FileSystems.getDefault().getPath(fileName);
    try (OutputStream geoJsonStream = Files.newOutputStream(jamRoutesGeoJsonPath)) {
      FeatureJSON geojson = new FeatureJSON();
      geojson.writeFeatureCollection(features, geoJsonStream);
    } catch (IOException ioException) {
        System.err.format("IOException on export features to geojson file %s: %s%n", jamRoutesGeoJsonPath, ioException);
    }
  }

  protected void deleteExportedFile(String fileName) {
    Path filePath = FileSystems.getDefault().getPath(fileName);
    File fileToDelete = filePath.toFile();
    if (fileToDelete.exists())
       fileToDelete.delete();
  }

  protected FeatureLayer createGridLayer(SimpleFeatureSource grid, Color strokeColor, double strokeWitdh) {
    Rule rules[] = {createGridStyleRule(grid, strokeColor, strokeWitdh)};
    return createLayer(grid, rules);
  }

  protected Rule createGridStyleRule(SimpleFeatureSource featureSource, Color strokeColor, double strokeWitdh) {
    Stroke markStroke = styleFactory.createStroke(filterFactory.literal(strokeColor),
            filterFactory.literal(strokeWitdh));

    GeometryDescriptor geomDescriptor = featureSource.getSchema().getGeometryDescriptor();
    String geometryAttributeName = geomDescriptor.getLocalName();
    LineSymbolizer lineSymbolizer = styleFactory.createLineSymbolizer(markStroke, geometryAttributeName);

    Rule lineRule = styleFactory.createRule();
    lineRule.symbolizers().add(lineSymbolizer);
    return lineRule;
  }

}