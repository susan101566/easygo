package easygo.main;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

/**
 * The final screen that lets users view all the stops and times given the
 * selection. Organized in chronological order for practicality.
 * 
 * @author Shuaihang Wang
 * 
 */
public class AllStopsActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_stops);
		setButtonListener();

		// Gets the information from the previous view
		ArrayList<String> stops = this.getIntent().getStringArrayListExtra(
				"stops");
		ListView tv = (ListView) findViewById(R.id.allstops);

		// populates the view
		tv.setAdapter(new ArrayAdapter<String>(this, R.layout.layout_dropdown,
				stops));

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

}
