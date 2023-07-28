package uk.ac.mmu.advprog.hackathon;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.json.*;

/**
 * Handles database access from within your web service
 * 
 * @author You, Mainly!
 */
public class DB implements AutoCloseable {

	// allows us to easily change the database used
	private static final String JDBC_CONNECTION_STRING = "jdbc:sqlite:./data/AMI.db";

	// allows us to re-use the connection between queries if desired
	private Connection connection = null;

	/**
	 * Creates an instance of the DB object and connects to the database
	 */
	
	public DB() {
		try {

			connection = DriverManager.getConnection(JDBC_CONNECTION_STRING);
		} catch (SQLException sqle) {
			error(sqle);
		}
	}

	/**
	 * Returns the number of entries in the database, by counting rows
	 * 
	 * @return The number of entries in the database, or -1 if empty
	 */
	
	public int getNumberOfEntries() {

		int result = -1;

		try {

			Statement s = connection.createStatement();
			ResultSet results = s.executeQuery("SELECT COUNT(*) AS count FROM ami_data");
			while (results.next()) { // will only execute once, because SELECT COUNT(*) returns just 1 number
				result = results.getInt(results.findColumn("count"));
			}
		} catch (SQLException sqle) {
			error(sqle);

		}
		return result;
	}
	
	/**
	 * Returns the latest signal_value entry in the database 
	 * latest signal_value result is based on the signal_id which is provided in the url
	 * query ignores results which are 'off' 'nr' or 'blnk' providing the wanted signal
	 * if no data is returned the default result response is 'no results' 
	 * there is a char limit set to make sure the signal id doesn't exceed the signal_id char limit 
	 * the setstring replaces the first instance of a ? inside the prepared statement with the 'url' string which is received when the method is called
	 * the result is printed on the page
	 * 
	 * @param freq
	 * @return
	 * @throws IOException
	 */

	public String getFinalSignal(String url) {

		String result = "no results";
		String query = "SELECT signal_value FROM ami_data WHERE signal_id = ? AND NOT signal_value = 'OFF' AND NOT signal_value = 'NR' AND NOT signal_value = 'BLNK' ORDER BY datetime DESC LIMIT 1;";

		if (url.length() > 0 && url.length() <= 11) {

			try {

				PreparedStatement s = connection.prepareStatement(query);
				s.setString(1, url);
				ResultSet results = s.executeQuery();
				while (results.next()) {
					result = results.getString(results.findColumn("signal_value"));
				}
			} catch (SQLException sqle) {
				error(sqle);

			}
			return result;
		} else
			return "no results";
	}
	
	/**
	 * method returns a value and frequency calculated based on entries in database
	 * query counts each instance of signal value and gives it out as 'frequency'
	 * frequencies are grouped by signal_value and ordered by frequency
	 * if the motorway code provided is not suitable the method will return an empty array
	 * if statement checks the motorway code provided to make sure it does not exceed the correct length
	 * the setstring replaces the first instance of a ? inside the prepared statement with the 'freq' string and a '%' symbol e.g. M42%
	 * when query is executed a new jsonobject is created 
	 * a string of signal value is created
	 * an int of the counted frequency is made
	 * the previously made string and int are inserted into the jsonobject with additional text for formating
	 * array.toString(); prints the array on the page
	 * 
	 * @param freq
	 * @return
	 * @throws IOException
	 */

	public String getFrequency(String freq) throws IOException {

		JSONArray array = new JSONArray();
		String query = "SELECT COUNT(signal_value) AS frequency, signal_value FROM ami_data WHERE signal_id LIKE ? GROUP BY signal_value ORDER BY frequency DESC;";

		if (freq.length() > 0 && freq.length() <= 4) {

			try {

				PreparedStatement s = connection.prepareStatement(query);
				s.setString(1, freq + "%");
				ResultSet results = s.executeQuery();

				while (results.next()) {

					JSONObject jsonobj = new JSONObject();
					String frq = results.getString(results.findColumn("signal_value"));
					int frqs = results.getInt(results.findColumn("frequency"));
					jsonobj.put(" Value: " + frq, " Frequency: " + frqs);
					array.put(jsonobj);

				}

			} catch (SQLException sqle) {
				error(sqle);
			}
			return array.toString();
		} else
			return array.toString();
	}
	
	/**
	 * query gathers signal_groups from the database
	 * xml output code from lectures
	 * result = xml.tostring to display on page
	 * content type is xml so it displays in xml format
	 *  
	 * @return
	 * @throws TransformerFactoryConfigurationError 
	 * @throws ParserConfigurationException 
	 * @throws TransformerException 
	 */

	public String getGroups(){

		String result = "";
		Writer out = new StringWriter();
		String query = "SELECT DISTINCT signal_group FROM ami_data;";

		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			Document doc = dbf.newDocumentBuilder().newDocument();
			
			Element groups = doc.createElement("Groups");
			doc.appendChild(groups);
			
			PreparedStatement s = connection.prepareStatement(query);
			ResultSet results = s.executeQuery();
			
			while (results.next()) { // will only execute once, because SELECT COUNT(*) returns just 1 number
				Element sgroup = doc.createElement("Group");
				sgroup.setTextContent(results.getString(results.findColumn("signal_group")));
				groups.appendChild(sgroup);
				result = out.toString();
			}
			
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.transform(new DOMSource(doc), new StreamResult(out));
			result = out.toString();
			
		} catch (SQLException sqle) {
			error(sqle);

		}
		catch(ParserConfigurationException | TransformerException ioe) {
			return "error";
		}
		return result = out.toString();
	}
	
	/**
	 * same xml stuff as the previous one except with more things inside it
	 * 
	 * 
	 * @param group
	 * @param date
	 * @param time
	 * @return
	 */
	
	public String getSignalsAtTime(String group, String date) {
		
		String result = "";
		Writer out = new StringWriter();
		String query = "SELECT datetime, signal_id, signal_value FROM ami_data WHERE signal_group =? AND datetime <? AND (datetime, signal_id) IN (SELECT MAX(datetime) AS datetime, signal_id FROM ami_data WHERE signal_group =? AND datetime <? GROUP BY signal_id) ORDER BY signal_id";
		
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			Document doc = dbf.newDocumentBuilder().newDocument();
			
			Element signals = doc.createElement("Signals");
			doc.appendChild(signals);
			
			PreparedStatement s = connection.prepareStatement(query);
			s.setString(1, group);
			s.setString(2, date);
			s.setString(3, group);
			s.setString(4, date);
			ResultSet results = s.executeQuery();
			
			while (results.next()) { // will only execute once, because SELECT COUNT(*) returns just 1 number
				Element signal = doc.createElement("Signal");
				signals.appendChild(signal);
				
				Element id = doc.createElement("ID");
				id.setTextContent(results.getString(results.findColumn("signal_id")));
				signal.appendChild(id);
				
				Element dates = doc.createElement("DateSet");
				dates.setTextContent(results.getString(results.findColumn("datetime")));
				signal.appendChild(dates);
				
				Element value = doc.createElement("Value");
				value.setTextContent(results.getString(results.findColumn("signal_value")));
				signal.appendChild(value);
				
				result = out.toString();
				
			}
			
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.transform(new DOMSource(doc), new StreamResult(out));
			result = out.toString();
			
		} catch (SQLException sqle) {
			error(sqle);

		}
		catch(ParserConfigurationException | TransformerException ioe) {
			return "error";
		}
		return result = out.toString();
	}

	/**
	 * Closes the connection to the database, required by AutoCloseable interface.
	 */
	
	@Override
	public void close() {

		try {
			if (!connection.isClosed()) {
				connection.close();
			}
		} catch (SQLException sqle) {
			error(sqle);
		}
	}

	/**
	 * Prints out the details of the SQL error that has occurred, and exits the
	 * programme
	 * 
	 * @param sqle Exception representing the error that occurred
	 */
	
	private void error(SQLException sqle) {

		System.err.println("Problem Opening Database! " + sqle.getClass().getName());
		sqle.printStackTrace();
		System.exit(1);
	}
}
