package easygo.main;

import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import easygo.database.DBHelper;
import easygo.database.EasyGoDb;

/**
 * The first screen of the application. Displays an autocomplete text view to
 * select bus. After which direction can be selected.
 * 
 * Feature 1: For autocomplete, on pressing enter, the first match is put in the
 * text box.
 * 
 * Feature 2: Toast is shown if input is invalid or there is no match.
 * 
 * Feature 3: On long click of the autocomplete box, entry is cleared.
 * 
 * @author Shuaihang Wang
 * 
 */
public class SelectBus extends Activity {

	// the autocomplete text view
	private AutoCompleteTextView busText;
	final private Activity self = this;
	private String bus = null, dir = null;

	// the view holding direction information
	private Spinner dirSpin = null;

	// set to positive if there is an invalid input
	private int ERROR = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_selectbus);

		// set all the listeners
		setButtonListener();
		setDropDownListener();
	}

	/*********************** Set All Views and Listeners ***************************/
	// hides the keyboard when the user has made a selection
	private void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(busText.getWindowToken(), 0);
		busText.clearFocus();
	}

	// populate the destination view upon selection of the bus
	private void populateSpin() {
		ArrayList<String> directions = getAllDirections();
		ArrayAdapter<String> directionAdapter = new ArrayAdapter<String>(self,
				R.layout.layout_dropdown, directions);
		dirSpin.setAdapter(directionAdapter);
		dirSpin.setVisibility(View.VISIBLE);
	}

	// what happens when user makes a selection of bus
	private void setDropDownListener() {

		// the autocomplete view
		busText = (AutoCompleteTextView) findViewById(R.id.bus_name);
		dirSpin = (Spinner) findViewById(R.id.direction);

		// only shown when a selection is made
		dirSpin.setVisibility(View.INVISIBLE);

		// initialize all variables that are needed
		ArrayList<String> buses = getAllBuses();

		// the drop down
		ArrayAdapter<String> busAdapter = new ArrayAdapter<String>(this,
				R.layout.layout_dropdown, buses);
		busText.setAdapter(busAdapter);
		busText.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				hideKeyboard();
				populateSpin();
			}
		});

		// Some users might click enter to automatically select the first item
		// on the dropdown
		busText.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View view, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN)
						&& (keyCode == KeyEvent.KEYCODE_ENTER)) {
					try {
						busText.setText(busText.getAdapter().getItem(0)
								.toString());
						hideKeyboard();
						populateSpin();
						return true;
					} catch (IndexOutOfBoundsException e) {
						showError();
						hideKeyboard();
						return true;
					}
				}
				return false;
			}
		});

		// clear autocomplete view when long click
		busText.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View arg0) {
				busText.setText("");
				return true;
			}
		});
	}

	// what happens when user clicks on the button on the screen
	private void setButtonListener() {
		// next
		Button nextButton = (Button) findViewById(R.id.selectOther);
		nextButton.setText("Select Stop");
		nextButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				bus = parseBus();
				dir = parseDirection(0);
				if (bus == null || dir == null)
					return;
				// start Async activity to display spinning wheel
				NextMan manager = new NextMan();
				try {
					manager.execute();
				} catch (NullPointerException e) {
					// invalid input
					showError();
				}
			}
		});

	}

	/*************************** Helper Functions ****************************/
	// Loads data from database and display spinning wheel at the mean time
	private class NextMan extends AsyncTask<Void, Void, Void> {
		// This is the spinning wheel
		private ProgressDialog dialog;

		@Override
		protected void onPreExecute() {
			dialog = new ProgressDialog(self);
			dialog.setMessage("Collecting data for bus: " + bus);
			dialog.setIndeterminate(true);
			dialog.setCancelable(false);
			dialog.show();
		}

		// Prepare to head off to the next activity that selects stops, time
		// given the current bus and direction
		@Override
		protected Void doInBackground(Void... arg0) {
			Intent intent = new Intent(self, SelectOtherActivity.class);
			ArrayList<String> stopsInfo = null;
			EasyGoDb db = new DBHelper(self.getApplicationContext());
			try {
				db.openDataBase();
				stopsInfo = db.viewAllStops(bus, dir);
				db.close();
			} catch (SQLException e) {
				ERROR = 1;
				return null;
			}
			if (stopsInfo.isEmpty()) {
				ERROR = 1;
				return null;
			}

			// Store the information needed in the next activity
			intent.putExtra("bus", bus);
			intent.putExtra("direction", dir);
			intent.putExtra("headSign", parseDirection(1));
			intent.putExtra("stops", stopsInfo);
			startActivity(intent);
			return null;
		}

		// Check if there has been any error occuring and dismiss the spinner
		@Override
		protected void onPostExecute(Void arg0) {
			dialog.dismiss();
			if (ERROR > 0) {
				ERROR = 0;
				showError();
			}
		}
	}

	// Retrieves the direction information after the user makes a selection
	// about the bus
	private ArrayList<String> getAllDirections() {
		String b = parseBus();
		EasyGoDb db = new DBHelper(self.getApplicationContext());
		ArrayList<String> result = new ArrayList<String>();
		try {
			db.openDataBase();
			result = db.viewAllDirections(b);
			db.close();
		} catch (SQLException e) {
			db.close();
			showError();
		}
		return result;
	}

	// Retrieves the information about all buses in the database
	private ArrayList<String> getAllBuses() {
		ArrayList<String> result = null;
		EasyGoDb db = new DBHelper(this);

		try {
			db.openDataBase();
			result = db.viewAllBuses();
			db.close();
		} catch (SQLException e) {
			db.close();
			showError();
		}
		return result;
	}

	// returns the bus code in the autocomplete view
	private String parseBus() {
		try {
			String result = (String) busText.getEditableText().toString()
					.split(" - ")[0];
			return result;
		} catch (NullPointerException e) {
			showError();
			return null;
		}
	}

	// returns the direction. 0 for code, 1 for name of the headsign
	// assumes the specific format stored in the database
	private String parseDirection(int i) {
		try {
			String result = ((String) dirSpin.getSelectedItem()).split(" - ")[i];
			return result;
		} catch (NullPointerException e) {
			showError();
			return null;
		}
	}

	// Let the user know in the form of Toast that an error has occured.
	private void showError() {
		Toast.makeText(
				self.getApplicationContext(),
				"Something went wrong, did you select a bus from the drop down menu?",
				Toast.LENGTH_SHORT).show();
	}

}
