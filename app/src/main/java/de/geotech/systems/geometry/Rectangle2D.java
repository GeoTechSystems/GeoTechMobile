package de.geotech.systems.geometry;

public class Rectangle2D {
	
	public double x;
	public double y;
	public double width;
	public double height;
	
	public Rectangle2D() {
		x = 0.0;
		y = 0.0;
		width = 0.0;
		height = 0.0;
	}
	
	public Rectangle2D(double setx, double sety, double setw, double seth) {
		x = setx;
		y = sety;
		width = setw;
		height = seth;
	}
	
	public double getX() {
		return x;
	}
	
	public double getY() {
		return y;
	}
	
	public double getWidth() {
		return width;
	}
	
	public double getHeight() {
		return height;
	}
	
	public void add(double newx, double newy) {
		
		if(newx < x) {
			x = newx;
		}
		else if( (newx - x) > width ) {
			width = newx - x;
		}
		
		if(newy < y) {
			y = newy;
		}
		else if( (newy - y) > height ) {
			height = newy - y;
		}
		
	}

}
