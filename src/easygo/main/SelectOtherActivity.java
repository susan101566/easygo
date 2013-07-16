package easygo.main;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Intent;
import android.database.SQLException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import easygo.database.DBHelper;
import easygo.database.EasyGoDb;

/**
 * The second screen that the user will see. This assumes that the user has
 * successfully selected the bus and direction in the previous view. Here,
 * he/she will select the time and stop and whether arrival or departure is
 * expected.
 * 
 * @author Shuaihang Wang
 * 
 */
public class SelectOtherActivity extends Activity {

	// All the shared variables in the class
	final private Activity self = this;
	private String bus, direction, headSign;
	private ArrayList<String> stops;
	private Spinner weekdaySpin, stopSpin;
	private final int TIME_DIALOG_ID = 42;
	private int pHour, pMinute;
	private TextView busView;
	private Button timeButton;
	private int ERROR = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_selectother);

		// get the information passed down from the previous view
		direction = this.getIntent().getStringExtra("direction");
		headSign = this.getIntent().getStringExtra("headSign");
		bus = this.getIntent().getStringExtra("bus");
		stops = this.getIntent().getStringArrayListExtra("stops");

		// sets up the views and the listeners
		setButtonListener();
		populateDropDowns();
	}

	/********************** Set up views **********************/
	private void populateDropDowns() {
		weekdaySpin = (Spinner) findViewById(R.id.weekday);
		stopSpin = (Spinner) findViewById(R.id.stops);

		ArrayAdapter<String> stopsAdapter = new ArrayAdapter<String>(self,
				R.layout.layout_dropdown, stops);

		stopSpin.setAdapter(stopsAdapter);
	}

	/******************** Parse Functions ********************/
	// returns the stop selected
	private String parseStop(int i) {
		String stop = (String) stopSpin.getSelectedItem();
		stop = stop.split(" - ")[i];
		return stop;
	}

	// returns the time selected in right format
	private String parseTime() {
		return timeButton.getText().toString();
	}

	// returns the weekday selected in right format
	private String parseWeekday() {
		return weekdaySpin.getSelectedItem().toString().toLowerCase();
	}

	// returns a map of all the needed values to pass down to the next view
	private HashMap<String, String> parseValues() {
		String weekday = parseWeekday();
		String time = parseTime();
		String stop = parseStop(0);
		HashMap<String, String> table = new HashMap<String, String>(4);
		table.put("weekday", weekday);
		table.put("time", time);
		table.put("stop", stop);
		return table;
	}

	/******************* Buttons *******************/

	// set up button listeners for all those on the screen
	private void setButtonListener() {

		// the back button
		Button backButton = (Button) findViewById(R.id.back);
		backButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});

		// the leaving button
		Button leaveButton = (Button) findViewById(R.id.leaving);
		ButtonListener leavelistener = new ButtonListener(0);
		leaveButton.setOnClickListener(leavelistener);

		// the arriving button
		Button arriveButton = (Button) findViewById(R.id.arriving);
		ButtonListener arrivelistener = new ButtonListener(1);
		arriveButton.setOnClickListener(arrivelistener);

		busView = (TextView) findViewById(R.id.busText);
		busView.setText(bus + "\n" + headSign);

		// set time button
		setCurrentTime();
		timeButton = (Button) findViewById(R.id.timeButton);
		// time is defaulted to the current time of the phone
		updateTimeDisplay();

		// time is selected in the dialog
		timeButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				showDialog(TIME_DIALOG_ID);
			}
		});
	}

	/********************** Time helper functions ************************/
	// sets current time to the time view.
	private void setCurrentTime() {
		final Calendar cal = Calendar.getInstance();
		pHour = cal.get(Calendar.HOUR_OF_DAY);
		pMinute = cal.get(Calendar.MINUTE);
	}

	// converts c, which is either hour or minute to a format that the database
	// can read
	private String pad(int c) {
		if (c >= 10)
			return String.valueOf(c);
		else
			return "0" + String.valueOf(c);
	}

	// sets the display to the selected time
	private void updateTimeDisplay() {
		timeButton.setText(new StringBuilder().append(pad(pHour)).append(":")
				.append(pad(pMinute)).append(":00"));
	}

	// what happens when the user clicks ok on the time selector dialog
	private OnTimeSetListener mTimeSetListener = new OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			pHour = hourOfDay;
			pMinute = minute;
			updateTimeDisplay();
		}
	};

	// what happens when the activity holds a dialog
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		// if it is the time dialog
		case (TIME_DIALOG_ID):
			return new TimePickerDialog(self, mTimeSetListener, pHour, pMinute,
					true);
		}
		return null;
	}

	/*********************** What happens when NEXT is clicked ***********************/
	private class ButtonListener implements OnClickListener {

		private int arriveP;

		public ButtonListener(int c) {
			arriveP = c;
		}

		// when user clicks arriving or departing
		// displays spinning wheel, meanwhile loading data from database
		@Override
		public void onClick(View v) {
			if (weekdaySpin == null || stopSpin == null)
				return;
			weekdaySpin.clearFocus();
			stopSpin.clearFocus();
			HashMap<String, String> info = parseValues();
			DownloadMan dm = new DownloadMan(info, arriveP);
			dm.execute();
		}
	}

	/****************** Spinning wheel ********************/
	private class DownloadMan extends AsyncTask<Void, Void, Void> {
		private ProgressDialog dialog;
		private HashMap<String, String> i;
		private int arriveP;

		// info is all that is needed to pass to the next view
		DownloadMan(HashMap<String, String> info, int c) {
			i = info;
			arriveP = c;
		}

		// sets up dialog for display
		@Override
		protected void onPreExecute() {
			dialog = new ProgressDialog(self);
			dialog.setMessage("Retrieving schedule for the selected time");
			dialog.setIndeterminate(true);
			dialog.setCancelable(false);
			dialog.show();
		}

		// loads data from database
		@Override
		protected Void doInBackground(Void... arg0) {
			Intent intent = new Intent(self, SelectScheduleActivity.class);
			String weekday = i.get("weekday");
			String time = i.get("time");
			String stop = i.get("stop");

			ArrayList<String> times;

			EasyGoDb db = new DBHelper(self.getApplicationContext());
			try {
				db.openDataBase();
				// gets the times of the schedules closest to the user's
				// selection
				times = db.getTimes(bus, direction, weekday, time, stop,
						arriveP);
				db.close();

				// if no bus is available at the selected time.
				if (times.size() < 2) {
					ERROR = 1;
					return null;
				}

			} catch (SQLException e) {
				ERROR = 1;
				return null;
			}
			intent.putExtra("bus", bus);
			intent.putExtra("weekday", weekday);
			intent.putExtra("time", time);
			intent.putExtra("direction", direction);
			intent.putExtra("headSign", headSign);
			intent.putExtra("stop", stop);
			intent.putExtra("stopName", parseStop(1));
			intent.putExtra("times", times);
			intent.putExtra("arriveP", arriveP);

			startActivity(intent);
			return null;
		}

		// if error is set to positive, then there is no data available for the
		// selected times, let the user know about this fact.
		@Override
		protected void onPostExecute(Void arg0) {
			dialog.dismiss();
			if (ERROR > 0) {
				ERROR = 0;
				Toast.makeText(self.getApplicationContext(),
						"No bus available at this time", Toast.LENGTH_SHORT)
						.show();
			}
		}
	}

}
