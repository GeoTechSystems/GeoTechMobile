/**
 * 
 * Diese Klasse kümmert sich um den Text und den Icon des Navigationdrawers in
 * der jeweiligen Zeile.
 *
 * @author svenweisker
 * @author Torsten Hoch
 */

package de.geotech.systems.navigationDrawer;

import de.geotech.systems.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class MenuAdapter extends ArrayAdapter<MenuItemModel> {

	public MenuAdapter(Context context){
		super(context,0);
	}

	// Herzstück des MenuAdapterLeft
	public View getView(int position,View convertView, ViewGroup parent){
		MenuItemModel item = getItem(position);
		ViewHolder holder = null;
		View view = convertView;
		if (view == null) {
			int layout = R.layout.menu_row_counter;
			view = LayoutInflater.from(getContext()).inflate(layout, null);
			TextView text = (TextView) view.findViewById(R.id.menurow_title);
			ImageView image = (ImageView) view.findViewById(R.id.menurow_icon);
			view.setTag(new ViewHolder(text,image));
		}
		if (holder == null && view != null) {
			Object tag = view.getTag();
			if (tag instanceof ViewHolder) {
				holder = (ViewHolder) tag;
			}
		}
		if (item != null && holder != null) {
			if (holder.textHolder != null) {
				holder.textHolder.setText(item.title);
			}
		}
		if (holder.imageHolder != null) {
			if (item.iconRes > 0) {
				holder.imageHolder.setVisibility(View.VISIBLE);
				holder.imageHolder.setImageResource(item.iconRes);
			} else {
				holder.imageHolder.setVisibility(View.GONE);
			}
		}
		return view;
	}

	public void addItem(int title,int icon) {
		add(new MenuItemModel(title, icon));
	}

	public void addItem(MenuItemModel itemModel) {
		add(itemModel);
	}

	// Klasse zur Speicherung der TextView und ImageView
	private static class ViewHolder {
		public final TextView textHolder;
		public final ImageView imageHolder;

		public ViewHolder(TextView text1, ImageView image1) {
			this.textHolder = text1;
			this.imageHolder = image1;
		}
	}

}
