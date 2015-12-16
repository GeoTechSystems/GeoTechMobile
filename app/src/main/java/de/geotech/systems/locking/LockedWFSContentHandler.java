/**
 * Contenthandler fuer Locking
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.locking;

import de.geotech.systems.wfs.WFSContentHandler;

import android.content.Context;

public class LockedWFSContentHandler extends WFSContentHandler {

	public LockedWFSContentHandler(Context c) {
		super(c);
	}
	
}
