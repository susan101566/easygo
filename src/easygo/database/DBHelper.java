package easygo.database;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * The adapter that helps the application communicate with the database
 * 
 * @author Shuaihang Wang
 * 
 */
public class DBHelper extends SQLiteOpenHelper implements EasyGoDb{

	// the directory to hold the database
	private static String path = "/data/data/easygo.main/databases/";

	// name of the database
	private static String name = "schedules";

	// reference to the database
	private SQLiteDatabase myDataBase;

	// context of the view displaying the information from the database
	private Context cxt;

	public DBHelper(Context context) {
		super(context, name, null, 1);
		cxt = context;
	}

	/********************* Database Creation ************************/
	// creates the database
	public void createDB() throws IOException {
		boolean exist = existDBP();
		if (!exist) {
			this.getReadableDatabase();
			try {
				copyDatabase();
			} catch (IOException e) {
				throw new Error("Error copying database");
			}
		}
	}

	// copies the database to the device upon first activation of the
	// application
	private void copyDatabase() throws IOException {
		// get the db file
		InputStream myInput = cxt.getAssets().open("schedules.db");
		// get the destination file
		String outFileName = path + name;
		// sets up the output stream
		OutputStream myOutput = new FileOutputStream(outFileName);
		// how many bytes transferred at a time
		byte[] buffer = new byte[1024];
		int length;
		// how much data has been transferred
		while ((length = myInput.read(buffer)) > 0) {
			myOutput.write(buffer, 0, length);
		}

		// Close the streams
		myOutput.flush();
		myOutput.close();
		myInput.close();
	}

	// checks if the device already contains the database
	private boolean existDBP() {
		SQLiteDatabase db = null;
		try {
			String p = path + name;
			db = SQLiteDatabase.openDatabase(p, null,
					SQLiteDatabase.OPEN_READONLY);
		} catch (SQLiteException e) {
		}
		if (db != null) {
			db.close();
		}
		return db != null ? true : false;
	}

	/*********************** Open or close database ***********************/
	public void openDataBase() throws SQLException {
		// Open the database
		String myPath = path + name;
		myDataBase = SQLiteDatabase.openDatabase(myPath, null,
				SQLiteDatabase.OPEN_READONLY);
	}

	@Override
	public synchronized void close() {
		if (myDataBase != null)
			myDataBase.close();
		super.close();

	}

	/************************
	 * Interface for database queries. i.e. functions that are used by the
	 * application explicitly
	 ************************/

	// returns an arraylist of all buses in the database in the
	// "buscode - bus name" format
	public ArrayList<String> viewAllBuses() throws SQLException {
		ArrayList<String> result = new ArrayList<String>();
		String query = "SELECT route_short_name, route_long_name FROM routes;";
		Cursor c = myDataBase.rawQuery(query, null);
		if (c.moveToFirst()) {
			do {
				result.add(c.getString(0) + " - " + c.getString(1));
			} while (c.moveToNext());
		}
		return result;
	}

	// find all stops that a bus can possibly make in the direction given
	public ArrayList<String> viewAllStops(String bus, String dir)
			throws SQLException {
		ArrayList<String> result = new ArrayList<String>();
		String query1 = "SELECT trip_id FROM trips JOIN routes "
				+ "ON routes.route_id=trips.route_id WHERE route_short_name LIKE '"
				+ bus + "' AND direction_id like '" + dir + "' LIMIT 1;";
		Cursor c1 = myDataBase.rawQuery(query1, null);
		String trip1 = null;
		if (c1.moveToFirst()) {
			trip1 = c1.getString(0);
		}
		String query = "SELECT DISTINCT stops.stop_id,stops.stop_name FROM stop_times "
				+ "JOIN stops ON stops.stop_id=stop_times.stop_id WHERE stop_times.trip_id LIKE '"
				+ trip1 + "' ORDER BY stop_times.departure_time;";
		Cursor c = myDataBase.rawQuery(query, null);

		if (c.moveToFirst()) {
			do {
				result.add(c.getString(0) + " - " + c.getString(1));
			} while (c.moveToNext());
		}
		return result;
	}

	// Get the bus stop times at the desired stop closes to time in the input.
	// There are two items in the result. The first one is the best guess, the
	// second is the next best. Otherwise, the bus does not run within a
	// reasonable time of time.
	public ArrayList<String> getTimes(String bus, String direction,
			String weekday, String time, String stop, int arriveP)
			throws SQLException {
		ArrayList<String> result = new ArrayList<String>();

		String query = null;
		String queryExtra = null;
		String placeholder = "select departure_time from stop_times "
				+ "where trip_id in (select trip_id from trips join routes "
				+ "on routes.route_id=trips.route_id join calendar on "
				+ "calendar.service_id=trips.service_id where routes.route_short_name like '"
				+ bus + "' and calendar." + weekday
				+ " like '1' and trips.direction_id like '" + direction
				+ "') and departure_time%s'" + time + "' and stop_id like '"
				+ stop + "' order by departure_time %s limit 1;";

		// want the actual time to be before or after the selected time?
		// make different queries to the database
		String queryBefore = String.format(placeholder, "<=", "desc");
		String queryAfter = String.format(placeholder, ">=", "asc");
		if (arriveP == 0) {
			// Leaving the stop at the time
			query = queryAfter;
			queryExtra = queryBefore;
		} else {
			// Arriving at the stop at the time
			query = queryBefore;
			queryExtra = queryAfter;
		}

		// best guess
		Cursor c = myDataBase.rawQuery(query, null);
		if (c.moveToFirst()) {
			result.add(c.getString(0));
		}

		// second best guess
		Cursor ce = myDataBase.rawQuery(queryExtra, null);
		if (ce.moveToFirst()) {
			result.add(ce.getString(0));
		}
		return result;
	}

	// get all stops with the schedule defined by the bus name, direction,
	// weekday, time and stop
	public ArrayList<String> getAllStops(String bus, String direction,
			String weekday, String time, String stop) throws SQLException {
		ArrayList<String> result = new ArrayList<String>();
		// 0 is for leaving, 1 is for arriving
		String query = "select departure_time,stop_name from stop_times join stops "
				+ "on stops.stop_id=stop_times.stop_id where trip_id in (select trip_id "
				+ "from stop_times join stops on stops.stop_id=stop_times.stop_id "
				+ "where trip_id in(select trip_id from trips join calendar on "
				+ "trips.service_id=calendar.service_id join routes on "
				+ "trips.route_id=routes.route_id where routes.route_short_name like '"
				+ bus
				+ "' and calendar."
				+ weekday
				+ " like '1' and trips.direction_id like '"
				+ direction
				+ "') and stop_times.stop_id like '"
				+ stop
				+ "' and departure_time like '"
				+ time
				+ "' limit 1) order by departure_time;";
		Cursor c = myDataBase.rawQuery(query, null);
		if (c.moveToFirst()) {
			do {
				result.add(c.getString(0) + " - " + c.getString(1));
			} while (c.moveToNext());
		}
		return result;
	}

	// get terminal information of a bus. I.e. the terminal name and code of the
	// bus
	public ArrayList<String> viewAllDirections(String bus) throws SQLException {
		ArrayList<String> result = new ArrayList<String>();
		String query = "SELECT DISTINCT direction_id, trip_headsign FROM trips "
				+ "JOIN routes ON routes.route_id=trips.route_id "
				+ "WHERE routes.route_short_name like " + "'" + bus + "';";
		Cursor c = myDataBase.rawQuery(query, null);
		if (c.moveToFirst()) {
			do {
				result.add(c.getString(0) + " - " + c.getString(1));
			} while (c.moveToNext());
		}
		return result;
	}

	@Override
	public void onCreate(SQLiteDatabase arg0) {
	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
	}

}
