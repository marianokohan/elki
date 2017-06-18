package ar.uba.fi.visualization.geo.map;

import java.util.List;

import ar.uba.fi.visualization.geo.map.unfolding.GeoJSONReaderMultiLineString;
import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.Marker;

public class JamAndColdRoutesMapVisualizer extends JamRoutesMapVisualizer {

  private String coldRoutesEdgesFileName = "cold_routes_edges.json";
  private int edgeColor = color(0, 118, 214);
  private int edgeWeight = 3;
  private String coldRoutesColdTrafficFileName = "cold_routes_cold_traffic.json";
  private int coldTrafficColor = color(56, 150, 30);
  private int coldTrafficWeight = 4;
  private String coldRoutesStartsFileName = "cold_routes_starts.json";
  private String coldRoutesEndsFileName = "cold_routes_ends.json";

  public void setup() {
      super.setup();

      List<Feature> startPoints = GeoJSONReaderMultiLineString.loadData(this, coldRoutesStartsFileName);
      Location panLocation = defaultBeijingLocation;
      if (!startPoints.isEmpty()) {
        panLocation = ((PointFeature)startPoints.get(0)).getLocation();
      }
      map.panTo(panLocation);

      addEdgeMarkers(coldRoutesEdgesFileName, edgeColor, edgeWeight);
      addEdgeMarkers(coldRoutesColdTrafficFileName, coldTrafficColor, coldTrafficWeight);

      List<Marker> startPointsMarkers = createStartPointsMarkers(startPoints);
      map.addMarkers(startPointsMarkers);
      List<Feature> endPoints = GeoJSONReaderMultiLineString.loadData(this, coldRoutesEndsFileName);
      List<Marker> endPointsMarkers = createEndPointsMarkers(endPoints);
      map.addMarkers(endPointsMarkers);
  }

}
