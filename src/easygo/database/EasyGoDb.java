package easygo.database;

import java.io.IOException;
import java.util.ArrayList;

import android.database.SQLException;

/**
 * Interface for the database communications in the Easy Go application
 * 
 * @author Susan
 * 
 */
public interface EasyGoDb{
	// creates the database
	public void createDB() throws IOException;

	public void openDataBase() throws SQLException;
	public void close() throws SQLException; 

	// returns an arraylist of all buses in the database in the
	// "buscode - bus name" format
	public ArrayList<String> viewAllBuses() throws SQLException;

	// find all stops that a bus can possibly make in the direction given
	public ArrayList<String> viewAllStops(String bus, String dir)
			throws SQLException;

	// Get the bus stop times at the desired stop closes to time in the input.
	// There are two items in the result. The first one is the best guess, the
	// second is the next best. Otherwise, the bus does not run within a
	// reasonable time of time.
	public ArrayList<String> getTimes(String bus, String direction,
			String weekday, String time, String stop, int arriveP)
			throws SQLException;

	// get all stops with the schedule defined by the bus name, direction,
	// weekday, time and stop
	public ArrayList<String> getAllStops(String bus, String direction,
			String weekday, String time, String stop) throws SQLException;

	// get terminal information of a bus. I.e. the terminal name and code of the
	// bus
	public ArrayList<String> viewAllDirections(String bus) throws SQLException;
}
