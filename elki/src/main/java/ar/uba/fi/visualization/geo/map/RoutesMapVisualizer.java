package ar.uba.fi.visualization.geo.map;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import processing.core.PApplet;
import ar.uba.fi.visualization.geo.map.markers.EndPointMarker;
import ar.uba.fi.visualization.geo.map.markers.StartPointMarker;
import ar.uba.fi.visualization.geo.map.providers.OSMBlackAndWhiteProvider;
import ar.uba.fi.visualization.geo.map.unfolding.GeoJSONReaderMultiLineString;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.MultiFeature;
import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.data.ShapeFeature;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.SimpleLinesMarker;

public class RoutesMapVisualizer extends PApplet {

  protected Location defaultBeijingLocation = new Location(39.92f, 116.39f);

  protected UnfoldingMap map;

  protected void providerSetup() {
    /*=> posibles (TODO: sacar al definir)
    map = new UnfoldingMap(this, new OpenStreetMap.OpenStreetMapProvider()); //aparecen en chino => no
    //map = new UnfoldingMap(this, new Google.GoogleMapProvider());
      //doc señala uso interno (http://unfoldingmaps.org/javadoc/de/fhpotsdam/unfolding/providers/Google.html) => posible problema de licencia
    //map = new UnfoldingMap(this, new Microsoft.RoadProvider()); //nombres distintos al de google - faltan detalles a mapa
    //map = new UnfoldingMap(this, new GeoMapApp.TopologicalGeoMapProvider()); //se parece al de Microsoft - por lo metodos de la api parece ser
    //map = new UnfoldingMap(this, new ThunderforestProvider.Transport()); //aparece chino e ingles - watermark de "api key required"
      //haciendo mas zoom hay varias en chino solamente
    */

    /* nuevas pruebas 2019-01
    //map = new UnfoldingMap(this, new EsriProvider.WorldStreetMap()); //hay que darle zoom para ver cosas - tiene una marca de agua (mitad chino y mitad ingles - parece ser de copyright)
      //habria que considerar el copyright: http://unfoldingmaps.org/javadoc/de/fhpotsdam/unfolding/providers/EsriProvider.html
      //2019-01: parece no estar disponible -- obs.: vuelve a funcionar 26-01-2019
      //=> probamos anteriores
    //map = new UnfoldingMap(this, new OpenStreetMap.OpenStreetMapProvider()); //contenido en chino (si se le hace zoom aparece algunos mas en ingles) - detalles color
    //map = new UnfoldingMap(this, new Microsoft.RoadProvider()); //nombres en ingles (distintos al de google) - faltan detalles a mapa (y parece estar en otra "escala")
    //map = new UnfoldingMap(this, new GeoMapApp.TopologicalGeoMapProvider()); //se parece al de Microsoft - por lo metodos de la api parece ser -- los mismos problemas
    //la mejor alternativa parece ser el de OSM
    //map = new UnfoldingMap(this, new OpenStreetMap.OpenStreetMapProvider()); //contenido en chino (si se le hace zoom aparece algunos mas en ingles) - detalles color
    //pruebas adicionales vistos (para alternativa mejor viz frente a EsriProvider)
    //map = new UnfoldingMap(this, new OpenStreetMap.OSMGrayProvider()); //no disponible
    //map = new UnfoldingMap(this, new StamenMapProvider.TonerBackground()); //se ve bien, pero le faltan ref. de texto
    //map = new UnfoldingMap(this, new StamenMapProvider.TonerLite()); //similar
    //map = new UnfoldingMap(this, new StamenMapProvider.WaterColor()); //como acuarela, mucho colo y sin texto
    //map = new UnfoldingMap(this, new OpenMapSurferProvider.Roads()); //no aparece
    //map = new UnfoldingMap(this, new OpenMapSurferProvider.Grayscale()); //idem
    //propios en base a http://leaflet-extras.github.io/leaflet-providers/preview/index.html
    //map = new UnfoldingMap(this, new OSMBlackAndWhiteProvider()); //demora un rato, pero se ve bastante bueno
    //map = new UnfoldingMap(this, new OMSGrayScaleProvider()); //mas claro, algunas zonas parecen tener menos info (menor referencia)
    */

    // => nuevos a usar (26-01-2019)
    map = new UnfoldingMap(this, new OSMBlackAndWhiteProvider()); //licencia https://www.openstreetmap.org/copyright -- en caso de no tan clara referencia => EsriProvider.WorldStreetMap (siguiente)
    //map = new UnfoldingMap(this, new EsriProvider.WorldStreetMap()); //si no disponible => OSMBlackAndWhiteProvider (anterior) - TODO: considerar validar licencia

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

  @Override
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

  @Override
  protected void exitActual() {
    //"ah ah ah !!! - not exit all, just the current frame
    JFrame window = ((JFrame)this.frame);
    window.setVisible(false); //you can't see me!
    window.dispose(); //Destroy the JFrame object
  }

}
