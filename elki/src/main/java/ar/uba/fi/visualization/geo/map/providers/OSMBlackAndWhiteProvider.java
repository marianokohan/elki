package ar.uba.fi.visualization.geo.map.providers;

import de.fhpotsdam.unfolding.core.Coordinate;
import de.fhpotsdam.unfolding.providers.OpenStreetMap.GenericOpenStreetMapProvider;

public class OSMBlackAndWhiteProvider extends GenericOpenStreetMapProvider {

  //based on http://leaflet-extras.github.io/leaflet-providers/preview/index.html
  // theme "OpenStreetMap.BlackAndWhite"

  @Override
  public String[] getTileUrls(Coordinate coordinate) {
    //'http://{s}.tiles.wmflabs.org/bw-mapnik/{z}/{x}/{y}.png'
    String url = "http://a.tiles.wmflabs.org/bw-mapnik/" + getZoomString(coordinate) + ".png";
    return new String[] { url };
  }

}
