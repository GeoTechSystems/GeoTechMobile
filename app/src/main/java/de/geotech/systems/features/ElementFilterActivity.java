package de.geotech.systems.features;

import de.geotech.systems.R;
import de.geotech.systems.main.LeftMenuIconLayout;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;

public class ElementFilterActivity extends Activity {
	Context context;
	
	@Override
	public void onCreate(Bundle savedInstanceState)	{
		super.onCreate(savedInstanceState);
        setContentView(R.layout.element_filter);
        context = (Context) this;
        // Erzeuge Menü
        LinearLayout left_ll = (LinearLayout) findViewById(R.id.element_selector_menu);
        left_ll.setGravity(Gravity.CENTER_HORIZONTAL);
        // Füge Button "Zurück" ein
        left_ll.addView(new LeftMenuIconLayout(context, getString(R.string.back), Color.LTGRAY, R.drawable.ic_project_manager_load, new OnClickListener(){
    		@Override
			public void onClick(View v) {
				Intent returnToLayerManager = new Intent();
				returnToLayerManager.putExtra("de.geotech.systems.fly", false);
				setResult(RESULT_OK, returnToLayerManager);
				finish();
			}
				
		}));
	}
}
