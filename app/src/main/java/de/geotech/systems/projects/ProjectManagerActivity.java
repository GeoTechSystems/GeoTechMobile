/**
 * Projektverwaltungsactivity
 * 
 * @author Torsten Hoch
 */

package de.geotech.systems.projects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.geotech.systems.R;
import de.geotech.systems.R.drawable;
import de.geotech.systems.main.LeftMenuIconLayout;
import de.geotech.systems.wfs.WFSCheckedListener;
import de.geotech.systems.wms.WMSCheckedListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.LinearLayout;

public class ProjectManagerActivity extends Activity {
	private static final String CLASSTAG = "ProjectManagerActivity";
	protected static int ADD_PROJECT_ACTIVITY = 0;
	private ArrayList<ProjectListItem> projectList;
	private LinearLayout linlay;
	private AlertDialog.Builder builder;
	private Context context;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.projectList = new ArrayList<ProjectListItem>();
		this.linlay = null;
		this.builder = null;
		this.context = this;
		this.buildGUI();
	}

	// Called when activity receives result from another one
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		this.buildGUI();
	}

	@Override
	public void onBackPressed()	{
		Intent returnToMain = new Intent();
		setResult(RESULT_CANCELED, returnToMain);
		finish();
	}

	// Creates the user interface
	private void buildGUI() {
		setContentView(R.layout.project_manager);
		linlay = (LinearLayout) findViewById(R.id.project_manager_projectlist);
		LinearLayout leftMenu = (LinearLayout) findViewById(R.id.project_manager_menu);
		leftMenu.setGravity(Gravity.CENTER);
		leftMenu.setShowDividers(android.widget.LinearLayout.SHOW_DIVIDER_MIDDLE);
		leftMenu.setDividerPadding(10);
//		Resources res = getResources();
//		Drawable divider = res.getDrawable(R.drawable.settings_ics_128);
//		leftMenu.setDividerDrawable(divider);
		clearProjectList();
		createProjectList();
		// Animation    	
		final ScaleAnimation icon_animation = new ScaleAnimation(0.7f, 1f, 0.7f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		icon_animation.setDuration(300);
		// Icon and button for load
		leftMenu.addView(new LeftMenuIconLayout(this, getString(R.string.project_load), Color.GREEN, R.drawable.ic_greendot, 20, 20, new OnClickListener() {
			@Override
			public void onClick(View v) {
				v.startAnimation(icon_animation);
				String projectName = null;
				Iterator<ProjectListItem> projectIterator = projectList.listIterator();
				while (projectIterator.hasNext()) {
					ProjectListItem currentItem = projectIterator.next();
					if (currentItem.getFocus()) {
						projectName = currentItem.getName();
					}
				}
				// wenn kein projekt ausgesucht wurde
				if (projectName == null) {
					// meldung ausgeben
					builder = new AlertDialog.Builder(v.getContext());
					builder.setMessage(getString(R.string.projectManager_no_project_selected));
					builder.setCancelable(false);
					builder.setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
					builder.show();
				} else {
					// sonst wird ausgewähltes Projekt geladen und als aktuelles Projekt gespeichert
					if (ProjectHandler.setCurrentProject(projectName)) {
						Intent returnToMain = new Intent();
						setResult(RESULT_OK, returnToMain);
						// listenverwaltung für gelockte oder angezeigte layer zurücksetzen
						WFSCheckedListener.resetCheckedLists();
						WMSCheckedListener.resetCheckedLists();
						finish();	
					} else {
						Intent returnToMain = new Intent();
						setResult(RESULT_CANCELED, returnToMain);
						// listenverwaltung für gelockte oder angezeigte layer zurücksetzen
						finish();	
					}
				}
			}
		}));
		
		
		
		// Icon and button for new
		leftMenu.addView(new LeftMenuIconLayout(this, getString(R.string.project_name_setting), Color.YELLOW, R.drawable.ic_yellowdot, new OnClickListener(){
			@Override
			public void onClick(View view) {
				view.startAnimation(icon_animation);
				Intent addProject = new Intent(view.getContext(), AddProjectActivity.class);
				startActivityForResult(addProject, ProjectManagerActivity.ADD_PROJECT_ACTIVITY );
			}
		}));
		// Icon and button for delete
		leftMenu.addView(new LeftMenuIconLayout(this, getString(R.string.project_delete), Color.RED, R.drawable.ic_reddot, new OnClickListener(){
			@Override
			public void onClick(final View v) {
				v.startAnimation(icon_animation);
				Iterator<ProjectListItem> project_iter = projectList.listIterator();
				while(project_iter.hasNext()) {
					final ProjectListItem currentItem = project_iter.next();
					if (currentItem.getFocus()) {
						builder = new AlertDialog.Builder(v.getContext());
						builder.setMessage(getString(R.string.project_delete_this_project)+"\n"+currentItem.getName());
						builder.setCancelable(false);
						builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// ausgewähltes Projekt wird gelöscht
								ProjectHandler.deleteProject(currentItem.getName());
								clearProjectList();
								createProjectList();
								dialog.cancel();
							}
						});
						builder.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
						builder.show();
					}
				}
			}
		}));
	}

	// creates the list of available projects reading the activity's file directory
	private void createProjectList() {
		String name;
		String description; 
		ProjectListItem newItem;
		List<Project> projects = ProjectHandler.getAllProjects();
		boolean focussed = false;
		for (Project project : projects) {
			name = project.getProjectName();
			description = project.getDescription();
			newItem = new ProjectListItem(this, R.drawable.ic_check_ok, name, description, projectList);
			if (ProjectHandler.isCurrentProjectSet()){
				if (name == ProjectHandler.getCurrentProject().getProjectName()) {
					newItem.setFocus(true);
					focussed = true;
				}
			}
			linlay.addView(newItem.getItem());
			projectList.add(newItem);
		}
		// focus auf das letzte eingefuegte projekt setzen
		if (!focussed && projectList != null && projectList.size() > 0) {
			projectList.get(projectList.size()-1).setFocus(true);	
		}
	}

	// deletes all projects in the project activity list
	private void clearProjectList() {
		linlay.removeAllViews();
		projectList.clear();
	}

}
