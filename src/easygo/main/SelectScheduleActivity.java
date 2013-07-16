package easygo.main;

import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.SQLException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import easygo.database.DBHelper;
import easygo.database.EasyGoDb;

/**
 * The third screen that the user will see. Assumes that the user has made a
 * selection about bus, direction, time, stop and whether he/she wants to arrive
 * at that stop at the time, or depart the stop at the time.
 * 
 * If there happens to be a bus at the selected time, let the user know about
 * this. Otherwise, let the user know about the bus schedules sandwiching the
 * selected time. The user can then view the schedules for either.
 * 
 * @author Shuaihang Wang
 * 
 */
public class SelectScheduleActivity extends Activity {

	// All variables used in this class
	final private Activity self = this;
	private String bus, weekday, time, direction, headSign, stop, stopName;
	private int arriveP;
	private ArrayList<String> times;
	private int ERROR = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_selectschedule);

		// populates views and sets up listeners
		setButtonListener();
		populateVars();
		populateViews();
	}

	/********************* Populate the views on the screen ************************/
	private void populateViews() {
		TextView prompt1, prompt2, time1, time2, service;

		// Get references for all views that display dynamic data
		prompt1 = (TextView) findViewById(R.id.ss_prompt1);
		prompt2 = (TextView) findViewById(R.id.ss_prompt2);
		time1 = (TextView) findViewById(R.id.ss_time1);
		time2 = (TextView) findViewById(R.id.ss_time2);
		service = (TextView) findViewById(R.id.ss_service);

		// sets the listener for the buttons
		ViewAllListener blistener = new ViewAllListener();
		Button curButton = (Button) findViewById(R.id.viewCur);
		Button extraButton = (Button) findViewById(R.id.viewExtra);
		curButton.setOnClickListener(blistener);
		extraButton.setOnClickListener(blistener);

		// get the Strings from the information got from the previous view
		String promptText1 = getResources().getString(R.string.prompt_1);
		String promptText2 = getResources().getString(R.string.prompt_2);
		String serviceText = (arriveP == 0) ? getResources().getString(
				R.string.prompt_previous) : getResources().getString(
				R.string.prompt_next);

		String prompt1Fill = String.format(promptText1, weekday,
				arriveP == 0 ? "depart from" : "arrive at", stopName);
		String prompt2Fill = String.format(promptText2, headSign);
		String serviceFill = String.format(serviceText, "arrives at");
		String okTime = times.get(0);
		String extraTime = times.get(1);

		// sets these strings to the views parsed earlier
		prompt1.setText(prompt1Fill);
		prompt2.setText(prompt2Fill);
		time1.setText(okTime);

		// handles the case when there is a schedule at the selected time
		TextView nobus = (TextView) findViewById(R.id.nobus);
		if (okTime.equals(extraTime)) {
			nobus.setText("Here's the deal...");
			service.setVisibility(View.GONE);
			time2.setVisibility(View.GONE);
			extraButton.setVisibility(View.GONE);
			TextView p3 = (TextView) findViewById(R.id.prompt_3);
			p3.setVisibility(View.GONE);
			return;
		}

		// otherwise, there is no schedule at the selected time, make best guess
		nobus.setText("Unfortunately, there is no " + bus
				+ (arriveP == 0 ? " departing " : " arriving ") + "at exactly "
				+ time);

		service.setText(serviceFill);
		time2.setText(extraTime);
	}

	// gets the information needed from the data sent to this view upon creation
	private void populateVars() {

		Intent i = this.getIntent();
		bus = i.getStringExtra("bus");
		weekday = i.getStringExtra("weekday");
		time = i.getStringExtra("time");
		direction = i.getStringExtra("direction");
		headSign = i.getStringExtra("headSign");
		stop = i.getStringExtra("stop");
		stopName = i.getStringExtra("stopName");
		times = i.getStringArrayListExtra("times");
		arriveP = i.getIntExtra("arriveP", 0);
	}

	// what happens when the user clicks on view schedule button
	private class ViewAllListener implements OnClickListener {

		// retrieves all stops and times of this schedule while displaying a
		// spinning wheel
		@Override
		public void onClick(View button) {
			int buttonid = button.getId();
			ViewStopsMan manager = new ViewStopsMan(buttonid);
			manager.execute();
		}
	}

	// what happens when the user clicks on the back button
	private void setButtonListener() {
		Button backButton = (Button) findViewById(R.id.back);
		backButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

	/*************************** Database Communications *************************/
	// Displays spinning wheel when loading data
	private class ViewStopsMan extends AsyncTask<Void, Void, Void> {
		private ProgressDialog dialog;
		private int buttonid;

		public ViewStopsMan(int id) {
			buttonid = id;
		}

		@Override
		protected void onPreExecute() {
			dialog = new ProgressDialog(self);
			dialog.setMessage("Retrieving stops for the selected schedule");
			dialog.setIndeterminate(true);
			dialog.setCancelable(false);
			dialog.show();
		}

		// makes different database calls when different buttons are pressed
		@Override
		protected Void doInBackground(Void... arg0) {
			EasyGoDb db = new DBHelper(self.getApplicationContext());
			ArrayList<String> info = new ArrayList<String>();
			try {
				db.openDataBase();
				switch (buttonid) {
				// if the best guess button is pressed
				case (R.id.viewCur):
					info = db.getAllStops(bus, direction, weekday,
							times.get(0), stop);
					break;
				// if the next best guess is pressed
				case (R.id.viewExtra):
					info = db.getAllStops(bus, direction, weekday,
							times.get(1), stop);
					break;
				default:
				}
				db.close();
			} catch (SQLException e) {
				ERROR = 1;
				return null;
			}
			Intent i = new Intent(self, AllStopsActivity.class);
			i.putExtra("stops", info);
			startActivity(i);
			return null;
		}

		// If there was an error in the database calls, let the user know about
		// it
		@Override
		protected void onPostExecute(Void arg0) {
			dialog.dismiss();
			if (ERROR > 0) {
				ERROR = 0;
				Toast.makeText(self.getApplicationContext(),
						"Sorry! Cannot retrieve schedule :(",
						Toast.LENGTH_SHORT).show();
			}
		}
	}
}
