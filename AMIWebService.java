package uk.ac.mmu.advprog.hackathon;
import static spark.Spark.get;
import static spark.Spark.port;

import java.net.http.HttpHeaders;

import spark.Request;
import spark.Response;
import spark.Route;

/**
 * Handles the setting up and starting of the web service
 * You will be adding additional routes to this class, and it might get quite large
 * Feel free to distribute some of the work to additional child classes, like I did with DB
 * @author You, Mainly!
 */
public class AMIWebService {

	/**
	 * Main program entry point, starts the web service
	 * @param args not used
	 */
	public static void main(String[] args) {		
		port(8088);
		
		
		//Simple route so you can check things are working...
		//Accessible via http://localhost:8088/test in your browser
		get("/test", new Route() {
			@Override
			public Object handle(Request request, Response response) throws Exception {
				try (DB db = new DB()) {
					return "Number of Entries: " + db.getNumberOfEntries();
				}
			}			
		});
		
		/*  get("/form", new Route() {
            @Override
            public Object handle(Request arg0, Response arg1) throws Exception{
            	return "<html><head></head><body><form action='http://localhost:8088/lastsignal' method='get' target='http://localhost:8088/lastsignal'>"
            			+ "<input type='text' placeholder='signal id' name='signal_id='/><input type='submit' name='send'/></form></body></html>";
            }
        });*/
		
		/**
		 *'/lastsignal' page is created on the 8088 port
		 * queryParams gathers string info after 'signal_id' in the url 
		 * string info is taken after the last (lastindexof) '='
		 * the string info (id) is passed into the getFinalSignal() method
		 * 
		 */
		
		get("/lastsignal", new Route() {
			@Override
			public Object handle(Request request, Response response) throws Exception {
				
				//String url = request.url().toString();
				String url = request.queryParams("signal_id");	
				String id = url.substring(url.lastIndexOf('=')+1);
				
				try (DB signal = new DB()) {
					return "Final Signal: " + signal.getFinalSignal(id);
				}
			}			
		});
		
		/**
		 *'/frequency' page is created on the 8088 port
		 * queryParams gathers string info after 'motorway' in the url 
		 * string info is taken after the last (lastindexof) '='
		 * the string info (id) is passed into the getFrequency() method
		 * 
		 */
		
		get("/frequency", new Route() {
			@Override
			public Object handle(Request request, Response response) throws Exception {
				
				String url = request.queryParams("motorway");	
				String id = url.substring(url.lastIndexOf('=')+1);
				
				try (DB signal = new DB()) {
					response.type("application/json");
					return signal.getFrequency(id);
					//header("Content-type: application/json; charset=utf-8");
					//.getResponseHeaders().add("Content-Type: application/json", "charset=utf-8");
				}
			}			
		});
		
		/**
		 *'/groups' page is created on the 8088 port
		 * getGroups() method is called for the page to received groups info
		 */
		
		get("/groups", new Route() {
			@Override
			public Object handle(Request request, Response response) throws Exception {
				
				try (DB signal = new DB()) {
					response.type("application/XML");
					return signal.getGroups();
				}
			}			
		});
		
		/**
		 *'/signalsattime' page is created on the 8088 port
		 * queryParams gathers string info after 'group' in the url 
		 * string info is taken after the last (lastindexof) '=' but before the index of '&'
		 * queryParams gathers string info after 'time' in the url 
		 * string info is taken after the last (lastindexof) '=' but before the index of '+'
		 * queryParams gathers string info after 'time' in the url 
		 * string info is taken after the last (lastindexof) '+'
		 * all strings are sent with the getSignalsAtTime() method
		 * 
		 */
		
		get("/signalsattime", new Route() {
			@Override
			public Object handle(Request request, Response response) throws Exception {
				
				String urlg = request.queryParams("group");	
				String group = urlg.substring(urlg.indexOf("=") + 1);
				
				String urld = request.queryParams("time");	
				String date = urld.substring(urld.lastIndexOf('=')+1);
				
				try (DB signal = new DB()) {
					response.type("application/XML");
					return signal.getSignalsAtTime(group, date);
				}
			}			
		});
		
		
		System.out.println("Server up! Don't forget to kill the program when done!");
		
	}

}
