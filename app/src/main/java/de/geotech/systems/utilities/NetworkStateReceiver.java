/**
 * OnNetworkStateChangedListener
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.utilities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkStateReceiver extends BroadcastReceiver {
	private OnNetworkStateChangedListener listener = null;

	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent.getExtras()!=null) {
	     	NetworkInfo ni=(NetworkInfo) intent.getExtras().get(ConnectivityManager.EXTRA_NETWORK_INFO);
	        if(ni!=null && ni.getState()==NetworkInfo.State.CONNECTED) {
	        	if(listener != null)
	        		listener.onNetworkStateChanged(true);
	        }
	    }
	    if(intent.getExtras().getBoolean(ConnectivityManager.EXTRA_NO_CONNECTIVITY,Boolean.FALSE)) {
	    	if(listener != null)
        		listener.onNetworkStateChanged(false);
	    }
	}
	
	public void setOnNetworkStateChangedListener(OnNetworkStateChangedListener l) {
		listener = l;
	}
	
	public interface OnNetworkStateChangedListener {
		void onNetworkStateChanged(boolean connected);
	}

}
