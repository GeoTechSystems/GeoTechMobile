package de.geotech.systems.utilities;

import java.util.ArrayList;

import android.graphics.Color;

public class ClassesColorModel {
	
	public ClassesColorModel() {
		
	}

	/* Color with android value (int) and color name ------------------------------ */
	
	public class VCColor {
		
		private String 	name;
		private	int		color;
		
		public VCColor(int i, String n) {
			color 		= i;
			name		= n;
		}
		
		public String getText() {
			return name;
		}
		
		public int getColor() {
			return color;
		}
		
		@Override
		public String toString() {
			return getText();
		}
		
	}
	
	/* Color table with usable colors --------------------------------------------- */
	public class ColorTable {
		
		ArrayList<VCColor> table;
		
		public ColorTable(){
			
			table = new ArrayList<VCColor>();
					
			table.add( new VCColor(Color.BLACK, "Black") );
			table.add( new VCColor(Color.BLUE, "Blue") );
			// table.add( new VCColor(Color.CYAN, "Cyan") );
			table.add( new VCColor(Color.DKGRAY, "Dark gray") );
			table.add( new VCColor(Color.GRAY, "Gray") );
			table.add( new VCColor(Color.GREEN, "Green") );
			table.add( new VCColor(Color.LTGRAY, "Light gray") );
			table.add( new VCColor(Color.MAGENTA, "Magenta") );
			table.add( new VCColor(Color.RED, "Red") );
			table.add( new VCColor(Color.WHITE, "White") );
			table.add( new VCColor(Color.YELLOW, "Yellow") );
			
		}
		
		public VCColor[] getColorsAsArray() {
			VCColor[] array = new VCColor[table.size()];
			table.toArray(array);
			return array;
		}
		
	}
	
	public VCColor[] getColorsAsArray() {
		
		ColorTable table = new ColorTable();
		return table.getColorsAsArray();
		
	}
	
}
