package main.java.com.vsu.smartvuetypef.model;

import java.util.Collections;
import java.util.List;

import android.graphics.Color;

import com.parse.FindCallback; 
 

import com.parse.GetCallback; 
import com.parse.ParseClassName; 
import com.parse.ParseException; 
import com.parse.ParseObject; 
import com.parse.ParseQuery; 
import com.parse.ParseQuery.CachePolicy;
//import main.java.com.vsu.smartvuetypef.util.PhaseComparator;
 
 
@ParseClassName("Phase") 
public class Phase extends ParseObject { 
	 
		/** 
		 * Wraps a FindCallback so that we can use the CACHE_THEN_NETWORK caching 
		 * policy, but only call the callback once, with the first data available. 
		 */ 
		private abstract static class PhaseFindCallback implements FindCallback<Phase> { 
			private boolean isCachedResult = true;
			private boolean calledCallback = false;
	 
	 
			@Override 
			public void done(List<Phase> objects, ParseException e) {
				if (!calledCallback) {
					if (objects != null) {
						// We got a result, use it. 
						calledCallback = true;
						doneOnce(objects, null);
					} else if (!isCachedResult) {
						// We got called back twice, but got a null result both 
						// times. Pass on the latest error. 
						doneOnce(null, e); 
					} 
				} 
				isCachedResult = false;
			} 
	 
	 
			/** 
			 * Override this method with the callback that should only be called 
			 * once. 
			 */ 
			protected abstract void doneOnce(List<Phase> objects, ParseException e);
		} 
	 
	 
		/** 
		 * Creates a query for talks with all the includes 
		 */ 
		private static ParseQuery<Phase> createQuery() { 
			ParseQuery<Phase> query = new ParseQuery<Phase>(Phase.class);
			query.include("speakers");
			query.include("room");
			query.include("slot");
			query.setCachePolicy(CachePolicy.CACHE_THEN_NETWORK);
			return query;
		} 
	 
	 
		/** 
		 * Gets the objectId of the Talk associated with the given URI. 
		 */ 
		/*public static String getTalkId(Uri uri) {
			List<String> path = uri.getPathSegments();
			if (path.size() != 2 || !"talk".equals(path.get(0))) {
				throw new RuntimeException("Invalid URI for talk: " + uri);
			} 
			return path.get(1);
		} */
	 
	 
		/** 
		 * Retrieves the set of all talks, ordered by time. Uses the cache if 
		 * possible. 
		 */ 
		public static void findInBackground(Session room,
				final FindCallback<Phase> callback) {
			ParseQuery<Phase> query = Phase.createQuery();
			if (room != null) {
				query.whereEqualTo("room", room);
			} 
			query.findInBackground(new PhaseFindCallback() {
				@Override 
				protected void doneOnce(List<Phase> objects, ParseException e) {
					if (objects != null) {
						// Sort the talks by start time. 
						//Collections.sort(objects, PhaseComparator.get());
					} 
					callback.done(objects, e);
				} 
			}); 
		} 
	 
	 
		/** 
		 * Gets the data for a single talk. We use this instead of calling fetch on 
		 * a ParseObject so that we can use query cache if possible. 
		 */ 
		public static void getInBackground(final String objectId,
				final GetCallback<Phase> callback) {
			ParseQuery<Phase> query = Phase.createQuery();
			query.whereEqualTo("objectId", objectId);
			query.findInBackground(new PhaseFindCallback() {
				@Override 
				protected void doneOnce(List<Phase> objects, ParseException e) {
					if (objects != null) {
						// Emulate the behavior of getFirstInBackground by using 
						// only the first result. 
						if (objects.size() < 1) {
							callback.done(null, new ParseException(
									ParseException.OBJECT_NOT_FOUND, 
									"No talk with id " + objectId + " was found."));
						} else { 
							callback.done(objects.get(0), e);
						} 
					} else { 
						callback.done(null, e);
					} 
				} 
			}); 
		} 
	 
	 
		public static void findInBackground(String title,
				final GetCallback<Phase> callback) {
			ParseQuery<Phase> talkQuery = ParseQuery.getQuery(Phase.class);
			talkQuery.whereEqualTo("title", title);
			talkQuery.getFirstInBackground(new GetCallback<Phase>() {
	 
	 
				@Override 
				public void done(Phase talk, ParseException e) {
					if (e == null) { 
						callback.done(talk, null);
					} else { 
						callback.done(null, e);
					} 
				} 
			}); 
		} 
	 
	 
		/** 
		 * Returns a URI to use in Intents to represent this talk. The format is 
		 * f8://talk/theObjectId 
		 */ 
		/*public Uri getUri() {
			Uri.Builder builder = new Uri.Builder();
			builder.scheme("f8");
			builder.path("talk/" + getObjectId());
			return builder.build();
		} */
		
		
		public int getCompletionTime(){
			return getInt("completionTime");
		}
	 
		public String getTitle() {
			return getString("title"); 
		} 
	 
	 
		public String getVideoID() {
			String videoID = getString("videoID");
			if (videoID == null) {
				videoID = "";
			} 
			return videoID;
		} 
	 
	 
		public String getAbstract() {
			String talkAbstract = getString("abstract");
			if (talkAbstract == null) {
				talkAbstract = "";
			} 
			return talkAbstract;
		} 
	
	 
	 
		/*public List<Speaker> getSpeakers() {
			return getList("speakers"); 
		} 
	 
	 
		public Slot getSlot() { 
			return (Slot) get("slot"); 
		} 
	 
	 
		public Room getRoom() {
			return (Room) get("room");
		} */
	 
	 
		/** 
		 * Items like breaks and the after party are marked as "alwaysFavorite" so 
		 * they always show up on the Favorites tab of the schedule. We also color 
		 * them slightly differently to make the UI prettier. 
		 */ 
		public boolean isAlwaysFavorite() { 
			return getBoolean("alwaysFavorite"); 
		} 
	 
	 
		public boolean allDay() { 
			return getBoolean("allDay"); 
		} 
	 
	 
		public boolean isBreak() { 
			return getBoolean("isBreak"); 
		} 
	 
} 