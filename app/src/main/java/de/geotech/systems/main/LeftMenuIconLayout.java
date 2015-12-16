/**
 * Klasse beinhaltet Layout für die Buttons in den Toolbars
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.main;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LeftMenuIconLayout extends LinearLayout {

	// constructoren, aufrufe nur aus mainactivity kopiert!
	// TODO optimieren, sieht ja kagge aus....
	public LeftMenuIconLayout(Context context, String text,
			int textColor, int resId, OnClickListener clickListener) {
		super(context);
		createToolBarIcon(context, text, textColor, resId, 10, 10, clickListener);
	}
	
	public LeftMenuIconLayout(Context context, String text,
			int textColor, ImageView icon, OnClickListener clickListener) {
		super(context);
		createToolBarIcon(context, text, textColor, icon, 10, 10,	clickListener);
	}
	
	public LeftMenuIconLayout(Context context, String text,
			int textColor, int resId, int horizontalPadding,
			int verticalPadding, OnClickListener clickListener) {
		super(context);
		createToolBarIcon(context, text, textColor, resId, 10, 10, /*horizontalPadding, verticalPadding,*/ clickListener);
	}
	
	public LeftMenuIconLayout(Context context, String text,
			int textColor, ImageView icon, int horizontalPadding,
			int verticalPadding, OnClickListener clickListener) {
		super(context);
		createToolBarIcon(context, text, textColor, icon, 10, 10,	clickListener);
	}
	
	/**
	 * Function creating a new toolbar icon with text, image, padding and
	 * OnClickListener
	 */
	public void createToolBarIcon(Context context, String text,
			int textColor, int resId, int horizontalPadding,
			int verticalPadding, OnClickListener clickListener) {
		// Vertical LinearLayout for image and text
		this.setOrientation(LinearLayout.VERTICAL);
		this.setGravity(Gravity.CENTER);
		this.setLayoutParams(new LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));
		this.setClickable(true);
		this.setOnClickListener(clickListener);
		// ImageView with given resource id
		ImageView frame_icon = new ImageView(context);
		frame_icon.setImageResource(resId);
		// TextView with given text
		TextView frame_text = new TextView(context);
		frame_text.setText(text);
		frame_text.setTextColor(textColor);
		frame_text.setTextSize(20);
		frame_text.setGravity(Gravity.CENTER);
		// Adding image and text to LinearLayout
		this.addView(frame_icon);
		this.addView(frame_text);
		this.setPadding(horizontalPadding, verticalPadding, horizontalPadding,verticalPadding);
	}

	/**
	 * Function creating a new toolbar icon with text, image, padding and
	 * OnClickListener
	 */
	public void createToolBarIcon(Context context, String text,
			int textColor, ImageView icon, int horizontalPadding,
			int verticalPadding, OnClickListener clickListener) {
		// Vertical LinearLayout for image and text
		this.setOrientation(LinearLayout.VERTICAL);
		this.setGravity(Gravity.CENTER);
		this.setLayoutParams(new LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));
		this.setClickable(true);
		this.setOnClickListener(clickListener);
		// ImageView with given resource id
		ImageView frame_icon = icon;
		// TextView with given text
		TextView frame_text = new TextView(context);
		frame_text.setText(text);
		frame_text.setTextColor(textColor);
		frame_text.setTextSize(20f);
		frame_text.setGravity(Gravity.CENTER);
		// Adding image and text to LinearLayout
		try {
			this.addView(frame_icon);
			this.addView(frame_text);
		} catch (Exception e) {
			Log.d("except", "got child-exception...");
			this.removeView(frame_icon);
			this.removeView(frame_text);
			((ViewGroup) icon.getParent()).removeView(icon);
			frame_icon = icon;
			this.addView(frame_icon);
			this.addView(frame_text);
		}
		this.setPadding(horizontalPadding, verticalPadding, horizontalPadding,
				verticalPadding);
	}

}
