package main.java.com.vsu.smartvuetypef.model;


import java.util.List;
 
 

import android.graphics.Color;

import com.parse.GetCallback;
import com.parse.ParseClassName; 
import com.parse.ParseException; 
import com.parse.ParseObject; 
import com.parse.ParseQuery;
import main.java.com.vsu.smartvuetypef.model.ParseFace;

 
 
@ParseClassName("Session") 
public class Session extends ParseObject { 
	public String getName() {
		return getString("name"); 
	} 
 
 
	public int getColor() { 
		int color = Color.rgb(46, 69, 81); // Default color
		List<Number> displayColor = getList("displayColor");
		if (displayColor != null) {
			color = Color.rgb(displayColor.get(0).intValue(),
					displayColor.get(1).intValue(), displayColor.get(2)
							.intValue());
		} 
		return color;
	} 
	
	public int getStartTime(){
		int start = getInt("startTime");
		return start;
	}
	
	public int getEndTime(){
		int endt = getInt("endTime");
		return endt;
	}
	
	public int getCompletionTime(){
		int total = getInt("startTime") - getInt("endTime");
		return total;
	}
 
	public List<ParseFace> getFaces() {
		return getList("faces"); 
	}
 
	public String getDescription() {
		String description = getString("description");
		if (description == null) {
			description = "";
		} 
		return description;
	} 
	 
	public static void findInBackground(int order,
			final GetCallback<Phase> callback) {
		ParseQuery<Phase> roomQuery = ParseQuery.getQuery(Phase.class);
		roomQuery.whereEqualTo("order", order);
		roomQuery.getFirstInBackground(new GetCallback<Phase>() {
			@Override 
			public void done(Phase room, ParseException e) {
				if (e == null) { 
					callback.done(room, null);
				} else { 
					callback.done(null, e);
				} 
			} 
		}); 
	} 
 
} 
