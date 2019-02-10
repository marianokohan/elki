package ar.uba.fi.visualization.geo.map;

import java.util.List;

import ar.uba.fi.visualization.geo.map.unfolding.GeoJSONReaderMultiLineString;
import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.utils.MapUtils;

public class JamRoutesMapVisualizer extends RoutesMapVisualizer {

  protected String jamRoutesEdgesFileName = "jam_routes_edges.json";
  protected int edgeColor = color(255, 142, 30);
  protected int edgeWeight = 9;
  protected String jamRoutesJamFileName = "jam_routes_jams.json";
  protected int jamColor = color(232, 4, 0);
  protected int jamWeight = 10;
  protected String jamRoutesStartsFileName = "jam_routes_starts.json";
  protected String jamRoutesEndsFileName = "jam_routes_ends.json";

  @Override
  public void setup() {
      size(1400, 900);
      providerSetup();
      // Add mouse and keyboard interactions
      MapUtils.createDefaultEventDispatcher(this, map);
      map.zoomLevel(12);

      List<Feature> startPoints = GeoJSONReaderMultiLineString.loadData(this, jamRoutesStartsFileName);
      Location panLocation = defaultBeijingLocation;
      if (!startPoints.isEmpty()) {
        panLocation = ((PointFeature)startPoints.get(0)).getLocation();
      }
      map.panTo(panLocation);

      addEdgeMarkers(jamRoutesEdgesFileName, edgeColor, edgeWeight);
      addEdgeMarkers(jamRoutesJamFileName, jamColor, jamWeight);

      List<Marker> startPointsMarkers = createStartPointsMarkers(startPoints);
      map.addMarkers(startPointsMarkers);
      List<Feature> endPoints = GeoJSONReaderMultiLineString.loadData(this, jamRoutesEndsFileName);
      List<Marker> endPointsMarkers = createEndPointsMarkers(endPoints);
      map.addMarkers(endPointsMarkers);
  }

}
