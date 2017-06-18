package ar.uba.fi.visualization.geo.map;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import processing.core.PApplet;
import ar.uba.fi.visualization.geo.map.markers.EndPointMarker;
import ar.uba.fi.visualization.geo.map.markers.StartPointMarker;
import ar.uba.fi.visualization.geo.map.unfolding.GeoJSONReaderMultiLineString;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.MultiFeature;
import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.data.ShapeFeature;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.SimpleLinesMarker;
import de.fhpotsdam.unfolding.providers.EsriProvider;

public class RoutesMapVisualizer extends PApplet {

  protected Location defaultBeijingLocation = new Location(39.92f, 116.39f);

  protected UnfoldingMap map;

  protected void providerSetup() {
    /*=> posibles (TODO: sacar al definir)
    //map = new UnfoldingMap(this, new OpenStreetMap.OpenStreetMapProvider()); //aparecen en chino => no
    //map = new UnfoldingMap(this, new Google.GoogleMapProvider());
      //doc señala uso interno (http://unfoldingmaps.org/javadoc/de/fhpotsdam/unfolding/providers/Google.html) => posible problema de licencia
    //map = new UnfoldingMap(this, new Microsoft.RoadProvider()); //nombres distintos al de google - faltan detalles a mapa
    //map = new UnfoldingMap(this, new GeoMapApp.TopologicalGeoMapProvider()); //se parece al de Microsoft - por lo metodos de la api parece ser
    //map = new UnfoldingMap(this, new ThunderforestProvider.Transport()); //aparece chino e ingles - watermark de "api key required"
      //haciendo mas zoom hay varias en chino solamente
    */
    map = new UnfoldingMap(this, new EsriProvider.WorldStreetMap()); //hay que darle zoom para ver cosas - tiene una marca de agua (mitad chino y mitad ingles - parece ser de copyright)
      //habria que considerar el copyright: http://unfoldingmaps.org/javadoc/de/fhpotsdam/unfolding/providers/EsriProvider.html
  }

  protected List<Marker> createEdgeMarkers(List<Feature> edges, int color, int weight) {
    List<Marker> jamRoutesMarkers = new ArrayList<Marker>();
    for (Feature feature : edges) {
      List<Feature> edgeFeatures = ((MultiFeature)feature).getFeatures();
      for (Feature partFeature : edgeFeatures) {
        ShapeFeature edgePartFeature = (ShapeFeature) partFeature;
        SimpleLinesMarker edgeMarker = new SimpleLinesMarker(edgePartFeature.getLocations());
        edgeMarker.setColor(color);
        edgeMarker.setStrokeWeight(weight);
        jamRoutesMarkers.add(edgeMarker);
      }
    }
    return jamRoutesMarkers;
  }

  protected List<Marker> createStartPointsMarkers(List<Feature> points) {
    List<Marker> pointMarkers = new ArrayList<Marker>();
    for (Feature feature : points) {
      PointFeature pointFeature = (PointFeature) feature;
      StartPointMarker pointMarker = new StartPointMarker(pointFeature.getLocation());
      pointMarkers.add(pointMarker);
    }
    return pointMarkers;
  }

  protected List<Marker> createEndPointsMarkers(List<Feature> points) {
    List<Marker> pointMarkers = new ArrayList<Marker>();
    for (Feature feature : points) {
      PointFeature pointFeature = (PointFeature) feature;
      EndPointMarker pointMarker = new EndPointMarker(pointFeature.getLocation());
      pointMarkers.add(pointMarker);
    }
    return pointMarkers;
  }

  public void draw() {
      map.draw();
  }

  protected void addEdgeMarkers(String geojsonFileName, int color, int weight) {
    File jamRoutesEdgesFile = new File(geojsonFileName);
    if (jamRoutesEdgesFile.exists()) {
      List<Feature> edgeRoutes = GeoJSONReaderMultiLineString.loadData(this, jamRoutesEdgesFile);
      List<Marker> edgeRoutesMarkers = createEdgeMarkers(edgeRoutes, color, weight);
      map.addMarkers(edgeRoutesMarkers);
    }
  }

}
