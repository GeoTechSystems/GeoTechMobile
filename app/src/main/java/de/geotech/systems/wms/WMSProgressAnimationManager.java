/**
 * Class to control the ProgressBar in a Activity Title.
 * 
 * @author Mathias Menninghaus
 */

package de.geotech.systems.wms;

import android.app.Activity;
import android.view.Window;

public class WMSProgressAnimationManager {
	private static final String CLASSTAG = "ProgressAnimationManager";

	private Activity activityContext;
	private int loadingCnt;
	private CharSequence originalTitle;

	/**
	 * The Activity must provide the indeterminate
	 * ProgressBar {@link Window.FEATURE_INDETERMINATE_PROGRESS}
	 */
	public WMSProgressAnimationManager(Activity context) {
		this.activityContext = context;
		this.loadingCnt = 0;
		this.originalTitle = this.activityContext.getTitle();
		this.activityContext.setProgressBarIndeterminate(true);
	}

	/**
	 * Start Progress Animation, for every start() somewhen a stop() must be
	 * called.
	 */
	public synchronized void start() {
		this.loadingCnt++;
		if (loadingCnt == 1) {
			this.activityContext.setProgressBarIndeterminateVisibility(true);
		}
	}

	/**
	 * Negates one call of start(). If every start() is negated, the
	 * ProgressAnimation will stop.
	 */
	public synchronized void stop() {
		loadingCnt--;
		if (loadingCnt == 0) {
			this.activityContext.setProgressBarIndeterminateVisibility(false);
		} else if (loadingCnt < 0) {
			loadingCnt = 0;
		}
	}

	/**
	 * Negate all calls of start() and stop the ProgressAnimation.
	 */
	public synchronized void stopImediate() {
		this.loadingCnt = 0;
		this.activityContext.setProgressBarIndeterminateVisibility(false);
		this.activityContext.setTitle(originalTitle);
	}
}
