package ar.uba.fi.visualization.geo.map.markers;

import processing.core.PGraphics;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.SimplePointMarker;

public class StartPointMarker extends SimplePointMarker {

  public StartPointMarker(Location location) {
    super(location);
  }

  public void draw(PGraphics pg, float x, float y) {
    pg.pushStyle();
    pg.fill(153,76, 0);
    pg.rect(x-2, y-2, 4, 4);
    pg.popStyle();
  }

}
