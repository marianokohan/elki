package ar.uba.fi.visualization.geo.map.markers;

import processing.core.PGraphics;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.SimplePointMarker;

public class EndPointMarker extends SimplePointMarker {

  public EndPointMarker(Location location) {
    super(location);
  }

  public void draw(PGraphics pg, float x, float y) {
    pg.pushStyle();
    pg.strokeWeight(3);
    pg.stroke(153,76, 0);
    pg.line(x-2, y-2, x+2, y+2);
    pg.line(x+2, y-2, x-2, y+2);
    pg.popStyle();
  }

}
