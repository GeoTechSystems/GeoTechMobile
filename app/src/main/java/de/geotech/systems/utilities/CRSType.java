package de.geotech.systems.utilities;


public class CRSType  {
	
	
	public static enum Unit{
		METRIC, DEGREE, UNKNOWN
	}
	
	public static CRSType.Unit getType (CRS crs){
		switch (crs.getCode()){
		case 4326:
			return CRSType.Unit.DEGREE;
		case 31466:
			return CRSType.Unit.METRIC;
		case 31467:
			return CRSType.Unit.METRIC;
		case 31469:
			return CRSType.Unit.METRIC;
		case 32631:
			return CRSType.Unit.METRIC;
		case 32632:
			return CRSType.Unit.METRIC;
		case 32633:
			return CRSType.Unit.METRIC;
		case 2192:
			return CRSType.Unit.METRIC;
		case 3785:
			return CRSType.Unit.METRIC;
			default: 
				return CRSType.Unit.UNKNOWN;
			
		}
	}

}
