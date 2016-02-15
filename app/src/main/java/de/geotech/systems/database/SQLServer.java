/**
 * @author Torsten Hoch
 */

package de.geotech.systems.database;

import de.geotech.systems.projects.Server;

public class SQLServer {
    // Standardserver
    public static final String[] STANDARDSERVERS = {
//		"scc-bilbo.scc.kit.edu:8080/geoserver/wfs", 
//		"scc-gandalf.scc.kit.edu:8080/geoserver/wfs", 
//		"www.lv-bw.de/dv/geoportal_dtk100_capabilities.xml",
//		"www.lv-bw.de/dv/geoportal_dtk50_capabilities.xml", 
//		"www.lv-bw.de/dv/geoportal_dtk25_capabilities.xml", 
//		"ows.terrestris.de/geoserver/osm/wfs",
//		"ows.terrestris.de/osm/service",
//		"ows.terrestris.de/osm-haltestellen", 
//		"www.umweltkarten-niedersachsen.de/arcgis/services/Hydro_wms/MapServer/WMSServer",
//		"www.umweltkarten-niedersachsen.de/arcgis/services/Basisdaten_wms/MapServer/WMSServer", 
//		"gateway.hamburg.de/OGCFassade/HH_WMS_DOP40.aspx", 
//		"www.wms.nrw.de/wms/Regionalplan", 
//		"www.wms.nrw.de/wms/elwas", 
//		"www.wms.nrw.de/umwelt/forst/hangneigung", 
//		"www.wms.nrw.de/geobasis/oebvi",
            "192.168.0.11:8080/geoserver/wfs"};
    //		"www.webatlasde.de/arcgis/services/Maps4BW/MapServer/WMSServer"};
    // standardserver mit http authentification
    public static final Server[] AUTHENTICATEDSERVERS = {
            //new Server ("gik-ubuntu-09.gik.kit.edu:8081/geoserver/wfs", "admin", "mohPhae4", "test"),
            //new Server ("gik-ubuntu-09.gik.kit.edu:8081/geoserver/wfs", "admin", "mohPhae4", "test")
            new Server("192.168.0.11:8080/geoserver/wfs", "admin", "geoserver", "localhost")
//		new Server ("scc-geodroid.scc.kit.edu:8080/geoserver/wfs", "LGL", "voo3iu4D", "LGL-KIT-Testserver"),
//		new Server ("scc-geodroid.scc.kit.edu:8080/geoserver/wfs", "admin", "havCigs5", "LGL-KIT-Testserver Admin")
    };

    public static final String TABLE_NAME = "server";
    public static final String ID = "_id_wfs";
    private static final String URL = "URL";
    private static final String AUTH = "third";
    private static final String USERNAME = "first";
    private static final String PASSWORD = "second";
    private static final String SERVERNAME = "forth";
    // String with all attributes
    public static final String[] ALLSERVERATTRIBUTES = new String[]{
            ID,
            URL,
            AUTH,
            USERNAME,
            PASSWORD,
            SERVERNAME
    };
    public static final String CREATE_TABLE_Server = "create table "
            + TABLE_NAME + " ("
            + ID + " integer primary key autoincrement, "
            + URL + " text, "
            + AUTH + " boolean, "
            + USERNAME + " text, "
            + PASSWORD + " text, "
            + SERVERNAME + " text);";
}
