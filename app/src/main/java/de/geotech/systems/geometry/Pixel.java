package de.geotech.systems.geometry;

import android.util.FloatMath;

public class Pixel {
	
	float row;
	float col;
	
	// Constructors
	public Pixel(float c, float r) {
		col = c;
		row = r;
	}
	
	// Methods
	public void setCol(float value) {
		col = value;
	}
	
	public void setRow(float value) {
		row = value;
	}
	
	public float getCol() {
		return col;
	}
	
	public float getRow() {
		return row;
	}
	
	public void addCol(float value) {
		col += value;
	}
	
	public void addRow(float value) {
		row += value;
	}
	
	public void copy(Pixel p) {
		col = p.getCol();
		row = p.getRow();
	}
	
	public float distance(Pixel p) {
		float dcol = col-p.getCol();
		float drow = row-p.getRow();
		return FloatMath.sqrt(drow*drow + dcol*dcol);
	}
	
	public float deltaCol(Pixel p) {
		return p.getCol()-col;
	}
	
	public float deltaRow(Pixel p) {
		return p.getRow()-row;
	}

}
