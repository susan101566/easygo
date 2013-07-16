package easygo.main;

import java.io.IOException;

import easygo.database.DBHelper;
import easygo.database.EasyGoDb;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * This is the entry point of the application. This shows the splash screen with
 * the logo on it. The flash screen lasts for 5 seconds.
 * 
 * @author Shuaihang Wang (ID: shuaihan)
 * 
 */
public class EasyGoMain extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_splash);

		// Get the database adapter. Create the database on the device on first
		// launch.
		EasyGoDb helper = new DBHelper(this);
		try {
			helper.createDB();
		} catch (IOException e) {
			Log.d("database", "cannot create database");
		}
	}

	// create the splash screen.
	public void onResume() {
		super.onResume();
		final Activity self = this;
		new Thread() {
			public void run() {
				try {
					// Sleep for 5 seconds
					Thread.sleep(5000);
					// Lead to the entry point of the application, i.e. to
					// select bus
					Intent i = new Intent(self, SelectBus.class);
					self.startActivity(i);
					self.finish();
				} catch (InterruptedException e) {
					Log.d("splash", "error");
				}
			}
		}.start();

	}
}
