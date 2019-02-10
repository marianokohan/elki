package ar.uba.fi.visualization.geo.map;

import java.util.List;

import ar.uba.fi.visualization.geo.map.unfolding.GeoJSONReaderMultiLineString;
import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.utils.MapUtils;

public class ColdRoutesMapVisualizer extends RoutesMapVisualizer {

  private String coldRoutesEdgesFileName = "cold_routes_edges.json";
  /* defined below
  private int edgeColor = color(0, 118, 214);
  private int edgeWeight = 9;
  */
  private String coldRoutesColdTrafficFileName = "cold_routes_cold_traffic.json";
  private int coldTrafficColor = color(56, 150, 30);
  private int coldTrafficWeight = 10;
  //for end user point of view "cold traffic" edges has no additional meaning
  private int edgeColor = coldTrafficColor;
  private int edgeWeight = coldTrafficWeight;
  private String coldRoutesStartsFileName = "cold_routes_starts.json";
  private String coldRoutesEndsFileName = "cold_routes_ends.json";

  @Override
  public void setup() {
      size(1400, 900);
      providerSetup();
      // Add mouse and keyboard interactions
      MapUtils.createDefaultEventDispatcher(this, map);
      map.zoomLevel(12);

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
