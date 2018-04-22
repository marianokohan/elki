package ar.uba.fi.visualization.geo;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
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
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.geotools.swing.JMapFrame;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;


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
 * @author mariano kohan
 *
 */
public class MapVisualizer {

  protected enum PointPositionType {START, END, MIDDLE}

  protected StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
  protected FilterFactory2 filterFactory = CommonFactoryFinder.getFilterFactory2();
  protected JMapFrame mapFrame;

  public MapVisualizer() {
    super();
  }

  protected Layer createRoadNetworkLayer(SimpleFeatureSource featureSource) {
        StyleBuilder styleBuilder = new StyleBuilder();
        LineSymbolizer restrictedSymb = styleBuilder.createLineSymbolizer(Color.LIGHT_GRAY);
        Style mapStyle = styleBuilder.createStyle(restrictedSymb);

        Layer mapLayer = new FeatureLayer(featureSource, mapStyle);
        return mapLayer;
  }

  protected FeatureLayer createPointsLayer(SimpleFeatureCollection points, SimpleFeatureSource featureSource, PointPositionType markType, Color markColor, int markSize) {
    Rule rules[] = {createPointStyleRule(featureSource, markType, markColor, markSize)};
    return createLayer(points, rules);
  }

  protected FeatureLayer createPolygonLayer(SimpleFeatureCollection polygon, SimpleFeatureSource featureSource, Color strokeColor, int strokeWitdh) {
    Rule rules[] = {createPolygonStyleRule(featureSource, strokeColor, strokeWitdh)};
    return createLayer(polygon, rules);
  }

  protected FeatureLayer createLayer(SimpleFeatureCollection features, Rule[] rules) {
    FeatureTypeStyle featureTypeStyle = styleFactory.createFeatureTypeStyle(rules);
    Style style = styleFactory.createStyle();
    style.featureTypeStyles().add(featureTypeStyle);

    FeatureLayer featureLayer = new FeatureLayer(features, style);
    return featureLayer;
  }

  protected FeatureLayer createLayer(SimpleFeatureSource features, Rule[] rules) {
    FeatureTypeStyle featureTypeStyle = styleFactory.createFeatureTypeStyle(rules);
    Style style = styleFactory.createStyle();
    style.featureTypeStyles().add(featureTypeStyle);

    FeatureLayer featureLayer = new FeatureLayer(features, style);
    return featureLayer;
  }

  protected Rule createPolygonStyleRule(SimpleFeatureSource featureSource, Color strokeColor, int strokeWitdh) {
    Stroke markStroke = styleFactory.createStroke(filterFactory.literal(strokeColor),
            filterFactory.literal(strokeWitdh));

    GeometryDescriptor geomDescriptor = featureSource.getSchema().getGeometryDescriptor();
    String geometryAttributeName = geomDescriptor.getLocalName();
    org.geotools.styling.PolygonSymbolizer polygonSymbolizer = styleFactory.createPolygonSymbolizer(markStroke, null, geometryAttributeName);

    Rule lineRule = styleFactory.createRule();
    lineRule.symbolizers().add(polygonSymbolizer);
    return lineRule;
  }

  protected Rule createPointStyleRule(SimpleFeatureSource featureSource, PointPositionType markType, Color markColor, int markSize) {
    Fill markFill = styleFactory.createFill(filterFactory.literal(markColor));
    Mark pointMark;
    switch (markType) {
      case START:
        pointMark = styleFactory.getSquareMark();
        break;
      case END:
        pointMark = styleFactory.getXMark();
        break;
      default:
        pointMark = styleFactory.getCircleMark();
        break;
    }
    pointMark.setFill(markFill);

    Graphic graphic = styleFactory.createDefaultGraphic();
    graphic.graphicalSymbols().clear();
    graphic.graphicalSymbols().add(pointMark);
    graphic.setSize(filterFactory.literal(markSize));

    GeometryDescriptor geomDescriptor = featureSource.getSchema().getGeometryDescriptor();
    String geometryAttributeName = geomDescriptor.getLocalName();
    PointSymbolizer pointSymbolizer = styleFactory.createPointSymbolizer(graphic, geometryAttributeName);

    Rule pointRule = styleFactory.createRule();
    pointRule.symbolizers().add(pointSymbolizer);
    return pointRule;
  }

  protected Point createPoint(Coordinate pointCoordinate) {
    GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
    Point point = geometryFactory.createPoint(pointCoordinate);
    return point;
  }

  public DefaultFeatureCollection createPointFeatureCollection(List<Point> points) {
    DefaultFeatureCollection pointsFeatureCollection = new DefaultFeatureCollection();
    for (Geometry point : points)
    {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("Trajectory");
        builder.setCRS(DefaultGeographicCRS.WGS84); // Coordinate reference system
        builder.add("the_geom", Point.class);
        builder.length(15).add("Name", String.class); // 15 chars width for name field
        SimpleFeatureType pointType = builder.buildFeatureType();

        SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(pointType);
        simpleFeatureBuilder.add(point);
        SimpleFeature pointFeature = simpleFeatureBuilder.buildFeature(null);

        pointsFeatureCollection.add(pointFeature);
    }
    return pointsFeatureCollection;
  }

  public DefaultFeatureCollection createPolygonFeatureCollection(Polygon polygon) {
    SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
    builder.setName("Area");
    builder.setCRS(DefaultGeographicCRS.WGS84); // Coordinate reference system
    builder.add("the_geom", Polygon.class);
    builder.length(15).add("Name", String.class); // 15 chars width for name field
    SimpleFeatureType polygonType = builder.buildFeatureType();

    SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(polygonType);
    simpleFeatureBuilder.add(polygon);
    SimpleFeature polygonFeature = simpleFeatureBuilder.buildFeature(null);

    DefaultFeatureCollection polygonFeatureCollection = new DefaultFeatureCollection();
    polygonFeatureCollection.add(polygonFeature);
    return polygonFeatureCollection;
  }

  protected SimpleFeatureCollection getSelectedFeatureFromMap(SimpleFeatureSource featureSource) {
    ReferencedEnvelope bounds = mapFrame.getMapContent().getViewport().getBounds();
    return filterFeaturesInBox(bounds, featureSource);
  }

  protected SimpleFeatureCollection filterFeaturesInBox(ReferencedEnvelope box, SimpleFeatureSource featureSource) {
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

}