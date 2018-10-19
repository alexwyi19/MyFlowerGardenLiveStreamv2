package ch.makery.address.selenium;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ch.makery.address.view.SupremeBotOverviewController;

public class Request implements Runnable {
	
	//Main bot overview controller
	private final SupremeBotOverviewController controller;
	
	//Task variables
	private int taskNumber;
	private String keyword;
	private String size;
	private String category;
	private String color;
	private String profileLoader;
	
	//Printer Writer for log text file, declared globally so other methods can use them
	private PrintWriter printWriter;
	
	//Create global variables to connect to supremenewyork for all methods
	private HttpURLConnection mainConnection;
	
	//Variables
	private final String mainShop = "http://www.supremenewyork.com";
	private final String mobile_stock = "http://www.supremenewyork.com/mobile_stock.json";

	//Variants from product data
	private String keywordProductID;
	private String keyword_style_colour;
	private String keyword_size;

	/* CONSTRUCTOR: Gets the references from the main controller and stores into variables */
	public Request(SupremeBotOverviewController controller, int taskNumber, String keyword, String size, String category, String color, String profileLoader) {
		this.controller = controller;
		this.taskNumber = taskNumber;
		this.keyword = keyword;
		this.size = size;
		this.category = category;
		this.color = color;
		this.profileLoader = profileLoader;
	}
	
	/* Run: Runs this request thread using Runnable interface */
	@Override
	public void run() {
		try {
			this.main(null);
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	

	public void main(String[] args) throws IOException {
		
		//Ensure status column is update
		controller.statusColumnUpdateRunning();
		
		//Create log file
		this.log_creator();
		
		//Test printer writer logger
		printWriter.println("LOG [TASK: " + "1 - MODE[Requests] - " +  " Time: " + new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()) + "]");
		printWriter.println();
		System.out.println("Trying to run the mobileStockChecker");
		//finds keyword and saves 
		try {
			this.mobile_stock_checker();
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			System.out.println("Failed to run");
			e1.printStackTrace();
		}
		
		//Finds the correct colour and variant for the keyword
		try {
			System.out.println("Finding variants");
			this.variant_finder();
			System.out.println("Finish finding variants");
		} catch (JSONException e) {
			System.out.println("Could not find");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Create POST Request to add the item to the cart
		this.add_to_cart();
		this.checkout();
		
		
		//Create POST Request to check out
	}
	  private static String readAll(Reader rd) throws IOException {
		    StringBuilder sb = new StringBuilder();
		    int cp;
		    while ((cp = rd.read()) != -1) {
		      sb.append((char) cp);
		    }
		    return sb.toString();
		  }
	public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		//////////////////////////////////////////////////////
		System.out.println(url);
		URL u = new URL (url);
		HttpURLConnection huc =  ( HttpURLConnection )  u.openConnection (); 
//		huc.setRequestMethod ("GET");  //OR  huc.setRequestMethod ("HEAD"); 
		huc.setRequestMethod("HEAD");
		huc.connect () ; 
		
		String redirect = huc.getHeaderField("Location");
		InputStreamReader is;
		if(redirect != null) {
			is = new InputStreamReader(new URL(redirect).openStream());
		}else {
			is = new InputStreamReader(new URL(url).openStream());
		}
		int code = huc.getResponseCode() ;
		System.out.println(code);
		System.out.println(huc.getResponseMessage());
		//////////////////////////////////////////////////////
//	    InputStreamReader is = new InputStreamReader(new URL(url).openStream());
//	    		new URL(url).openStream();
	    try {
//	      BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
	      BufferedReader rd = new BufferedReader(is);
	      String jsonText = readAll(rd);
	      System.out.println("this is the text: "+ jsonText);
	      jsonText = " " + jsonText;
	      JSONObject json = new JSONObject(jsonText);
	      return json;
	    } finally {
	      is.close();
	    }
	  }
	public void mobile_stock_checker() throws JSONException, MalformedURLException, IOException {
//		System.out.println(IOUtils.toString(new URL(mobile_stock), Charset.forName("UTF-8")));
//		System.out.println(mobile_stock);
		// Convert to supremnewyork stock url and store as json object
//		JSONObject json = new JSONObject(IOUtils.toString(new URL(mobile_stock), Charset.forName("UTF-8")));
		JSONObject json = readJsonFromUrl(mobile_stock);
		JSONObject main = json.getJSONObject("products_and_categories");
		JSONArray categoryItem = main.getJSONArray(category);
		
		//Iterate throught the array objects until keyword is found
		for (int i = 0; i < categoryItem.length(); i++) {
			  Object productSearch = categoryItem.get(i);
			  System.out.println("searching");
			  System.out.println(((JSONObject) productSearch).optString("name"));
			  System.out.println(keyword);
			  if ( ((JSONObject) productSearch).optString("name").contains(keyword)) {
				  System.out.println("found");
					//If keyword found outputs element object
					keywordProductID = ((JSONObject) productSearch).optString("id").toString();
					controller.statusColumnUpdateItemFound();
					controller.getConsole().appendText("[" + new SimpleDateFormat("HH:mm:ss:SS").format(new Date()) + "] - " + "Task - Item Found \n");
				} 
		}
//		for (Object productSearch : categoryItem) {
//			if ( ((JSONObject) productSearch).optString("name").contains(keyword)) {
//				//If keyword found outputs element object
//				keywordProductID = ((JSONObject) productSearch).optString("id").toString();
//				controller.statusColumnUpdateItemFound();
//				controller.getConsole().appendText("[" + new SimpleDateFormat("HH:mm:ss:SS").format(new Date()) + "] - " + "Task - Item Found \n");
//			} 
//		}
	}
	
	public void variant_finder() throws JSONException, MalformedURLException, IOException {
		System.out.println("I AM FINDING KEYWORD2");
		// Convert to supremnewyork stock url and store as json object
		JSONObject json = readJsonFromUrl(mainShop + "/shop/" + keywordProductID + ".json");
//		JSONObject json = new JSONObject(IOUtils.toString(new URL(mainShop + "/shop/" + keywordProductID + ".json"), Charset.forName("UTF-8")));
		
		JSONArray styleJson = json.getJSONArray("styles");
		
		
		//Iterate throught the array objects until style colour is found
		for (int i = 0; i < styleJson.length(); i++) {
			  Object productSearch = styleJson.get(i);
				if ( ((JSONObject) productSearch).optString("name").contains(color)) {
					//If keyword found outputs element object
					keyword_style_colour = ((JSONObject) productSearch).optString("id").toString();

				}  
		}
//		for (Object productSearch : styleJson) {
//			if ( ((JSONObject) productSearch).optString("name").contains(color)) {
//				//If keyword found outputs element object
//				keyword_style_colour = ((JSONObject) productSearch).optString("id").toString();
//
//			} 
//		}
		for (int i = 0; i < styleJson.length(); i++) {
			  Object colorSearch = styleJson.get(i);
			  Object sizes = ((JSONObject) colorSearch).get("sizes");
			  for(int j=0;j<((JSONArray) sizes).length();j++) {
				  Object djf = ((JSONArray) sizes).get(j);
					if (((JSONObject) djf).optString("name").contains(size)) {
						keyword_size = ((JSONObject) djf).optString("id").toString();
					}	
			  }
//				for (Object djf : ((JSONArray) sizes)) {
//					if (((JSONObject) djf).optString("name").contains(size)) {
//						keyword_size = ((JSONObject) djf).optString("id").toString();
//					}			
//				}
		}
//		for (Object colorSearch: styleJson) {
//			  Object sizes = ((JSONObject) colorSearch).get("sizes");
//			for (Object djf : ((JSONArray) sizes)) {
//				if (((JSONObject) djf).optString("name").contains(size)) {
//					keyword_size = ((JSONObject) djf).optString("id").toString();
//				}			
//			}
//		}
	
		//Console and Status update
		controller.statusColumnUpdateFetchingVariants();
		controller.getConsole().appendText("[" + new SimpleDateFormat("HH:mm:ss:SS").format(new Date()) + "] - " + "Task - Fetching variants \n");
	}

	
	public void add_to_cart() throws IOException {
		System.out.println("In add to cart");
		System.out.println("KeywordProductID: "+ keywordProductID + " color: "+ keyword_style_colour + " Size: "+ keyword_size);
		URL cartPost = new URL("https://www.supremenewyork.com/shop/" + keywordProductID + "/add.json");
		
		// Create POST Request
		mainConnection = (HttpURLConnection) cartPost.openConnection();
//		String redirect = mainConnection.getHeaderField("Location");
//		cartPost = new URL(redirect);
//		mainConnection = (HttpURLConnection) cartPost.openConnection();

		mainConnection.setReadTimeout(5000);
		
		//Attach POST Paramters (sie and style)
		mainConnection.setRequestMethod("POST");
//		mainConnection.setRequestProperty("Host", "www.supremenewyork.com");
//		mainConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:62.0) Gecko/20100101 Firefox/62.0");
//		mainConnection.setRequestProperty("Accept", "*/*;q=0.5, text/javascript, application/javascript, application/ecmascript, application/x-ecmascript");
//		mainConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
//		mainConnection.setRequestProperty("Acept-Encoding", "gzip, deflate, br");
//		mainConnection.setRequestProperty("Referer", "https://www.supremenewyork.com/shop/accessories/h1x6q8bg3/evr2ecnmx");
//		mainConnection.setRequestProperty("X-CSRF-Token", "K5RJeLkkK8Iee0PBaKBfiU73BhTF10VhEuQ8IoDl2YderUEYWQApP4qzwUP+Z/M9y9yxV1ni4UNCcExpbYq0Xg==");
//		mainConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
//		mainConnection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
//		mainConnection.setRequestProperty("Content-Length", "50");
//		mainConnection.setRequestProperty("Cookie", "");
//		mainConnection.setRequestProperty("Connection", "keep-alive");
//		mainConnection.setRequestProperty("TE", "Trailers");
		mainConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36");
		mainConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
		mainConnection.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");

		
		//Post Variables
		mainConnection.setRequestProperty("utf8", "%E2%9C%93");
		mainConnection.setRequestProperty("st", keyword_style_colour);
		//mainConnection.setRequestProperty("s", keyword_size);
		mainConnection.setRequestProperty("commit", "add+to+cart");
//		mainConnection.setRequestProperty("charset", "utf-8");
//		mainConnection.setRequestProperty("style", keyword_style_colour);
//		mainConnection.setRequestProperty("size", keyword_size);
//		mainConnection.setRequestProperty("commit", "add+to+basket");

			
		mainConnection.setRequestProperty("s","59764");/////////////////////////////////////////////
		
		mainConnection.connect();
		/////////////////////////////////////////////////////////////////////////
		String redirect = mainConnection.getHeaderField("Location");
		System.out.println("Redirect: " + redirect);
		if(redirect!=null) {
			cartPost = new URL(redirect);
			mainConnection = (HttpURLConnection) cartPost.openConnection();
		}
		////////////////////////////////////////////////////////////////////
		//Get Page Source and print it
		BufferedReader r = new BufferedReader(new InputStreamReader(mainConnection.getInputStream(), Charset.forName("UTF-8")));

		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = r.readLine()) != null) {
		    sb.append(line);
		}
		
		System.out.println("Printing out string: " + sb.toString());

		//Check if add to cart was successful, Status code 200 = OK
		System.out.println(mainConnection.getResponseCode());
		if(mainConnection.getResponseCode()>=200) {
			System.out.println(mainConnection.getResponseCode());
			
			//Console and Status update
			controller.statusColumnUpdateAddingToCart();
			controller.getConsole().appendText("[" + new SimpleDateFormat("HH:mm:ss:SS").format(new Date()) + "] - " + "Task - Adding to cart \n");
		}
		
//		try {
//			Thread.currentThread().wait();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
	}
	
	/* TO DO:  */
	public void checkout() throws IOException {
		System.out.println("In Checkout");
		URL checkOut = new URL("https://www.supremenewyork.com/checkout.json");
		
		// Create POST Request
		mainConnection = (HttpURLConnection) checkOut.openConnection();
//		String redirect = mainConnection.getHeaderField("Location");
//		cartPost = new URL(redirect);
//		mainConnection = (HttpURLConnection) cartPost.openConnection();

		mainConnection.setReadTimeout(5000);
		
		//Attach POST Paramters (sie and style)
		mainConnection.setRequestMethod("GET");
		mainConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36");
		mainConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
		mainConnection.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
		
		mainConnection.connect();
		/////////////////////////////////////////////////////////////////////////
//		String redirect = mainConnection.getHeaderField("Location");
//		System.out.println("Redirect: " + redirect);
//		if(redirect!=null) {
//			checkOut = new URL(redirect);
//			mainConnection = (HttpURLConnection) checkOut.openConnection();
//		}
		////////////////////////////////////////////////////////////////////
		//Get Page Source and print it
		BufferedReader r = new BufferedReader(new InputStreamReader(mainConnection.getInputStream(), Charset.forName("UTF-8")));

		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = r.readLine()) != null) {
		    sb.append(line);
		}
		
		System.out.println("Printing out string: " + sb.toString());

		//Check if add to cart was successful, Status code 200 = OK
		System.out.println(mainConnection.getResponseCode());
		if(mainConnection.getResponseCode()>=200) {
			System.out.println("Finished Successful Checkout");
			System.out.println(mainConnection.getResponseCode());
			
			//Console and Status update
			controller.statusColumnUpdateCheckedOut();
			controller.getConsole().appendText("[" + new SimpleDateFormat("HH:mm:ss:SS").format(new Date()) + "] - " + "Task - Checking out \n");
		}
	}

	
	public void log_creator() throws IOException {
		//Create Log File
		try (Writer file = new FileWriter(System.getProperty("user.dir")+ "/resources/Logs/" + "/Log_Task_" + "1" + ".txt")) {
			file.flush();
			controller.getConsole().appendText("[" + new SimpleDateFormat("HH.mm.ss.SSS").format(new Date()) +  "]" + " - " + "Successfully created log file \n");
		}
				
		//Start the print writer to Log to the file
		FileWriter rawLogOutput = new FileWriter(System.getProperty("user.dir")+ "/resources/Logs/Log_Task_1.txt");
		printWriter = new PrintWriter(rawLogOutput);
	}
}