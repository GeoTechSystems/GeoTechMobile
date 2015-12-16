/**
 * Creates an entry in the project list table
 *  
 * @author Torsten Hoch
 */

package de.geotech.systems.projects;

import java.util.ArrayList;
import java.util.Iterator;

import android.content.Context;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ProjectListItem {
	
	private boolean focused = false;
	private String itemName = null;
	
	private LinearLayout itemWrapper;
	private LinearLayout textWrapper;
	
	// Constructor
	public ProjectListItem(Context context, int iconId, String name, String desc, final ArrayList<ProjectListItem> list) {
		itemName = name;
		itemWrapper = new LinearLayout(context);
		itemWrapper.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		itemWrapper.setOrientation(LinearLayout.HORIZONTAL);
		itemWrapper.setFocusableInTouchMode(true);
		textWrapper = new LinearLayout(context);
		textWrapper.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		textWrapper.setOrientation(LinearLayout.VERTICAL);
		ImageView icon = new ImageView(context);
		icon.setImageResource(iconId);
		icon.setPadding(10, 10, 10, 10);
		TextView tvName = new TextView(context);
		tvName.setText(name);
		tvName.setTextSize(16f);
		TextView tvDesc = new TextView(context);
		tvDesc.setTextSize(tvName.getTextSize()*0.75f);
		tvDesc.setText(desc);
		textWrapper.addView(tvName);
		textWrapper.addView(tvDesc);
		itemWrapper.addView(icon);
		itemWrapper.addView(textWrapper);
		itemWrapper.setPadding(5, 5, 5, 10);
		itemWrapper.setOnTouchListener(new OnTouchListener(){
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					unfocusAll(list);
					setFocus(true);
					return true;
				}
				return false;
			}
		});
	}
	
	/** Returns LinearLayout with item content */
	public LinearLayout getItem() {
		return itemWrapper;
	}
	
	/** Changes the focus state of the item */
	public void setFocus(boolean focus) {
		AlphaAnimation fade_out = new AlphaAnimation(1f, 0f);
		fade_out.setDuration(300);
		AlphaAnimation fade_in = new AlphaAnimation(0f, 1f);
		fade_in.setDuration(300);
		if (focus && !focused) {
			itemWrapper.startAnimation(fade_out);
			itemWrapper.setBackgroundColor(Color.parseColor("#33B5E5"));
			itemWrapper.startAnimation(fade_in);
		} else if(!focus && focused) {
			itemWrapper.startAnimation(fade_out);
			itemWrapper.setBackgroundColor(Color.TRANSPARENT);
			itemWrapper.startAnimation(fade_in);
		}
		focused = focus;
	}
	
	/** Returns the focus state */
	public boolean getFocus() {
		return focused;
	}
	
	/** Unfocuses all items of given list */
	public void unfocusAll(ArrayList<ProjectListItem> list) {
		Iterator<ProjectListItem> iter = list.listIterator();
		while(iter.hasNext()) {
			iter.next().setFocus(false);
		}
	}
	
	/** Returns item name */
	public String getName() {
		return itemName;
	}
	
}
