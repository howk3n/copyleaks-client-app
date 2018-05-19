import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Main {	
	
	static enum APIService {
		LOGIN, SCAN_FILES, PROCESS, RESULT
	}
	
//	Metoda koja vraca novu konekciju na osnovu API url-a i trazenog servisa
	static HttpURLConnection setupJSONConnection(String extension, APIService service) throws MalformedURLException, IOException {
		String fullUrl = API_URL.concat(extension);
		HttpURLConnection conn = (HttpURLConnection) ((new URL(fullUrl).openConnection()));
		
		conn.setDoOutput(true);
		conn.setRequestProperty("Accept", "application/json");
		switch(service) {
			case LOGIN:
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Content-Type", "application/json");
				break;
			case SCAN_FILES:
				conn.setRequestMethod("POST");
	    		conn.setRequestProperty("Connection", "Keep-Alive");
	    		conn.setRequestProperty("User-Agent", "Android Multipart HTTP Client 1.0");
	    		conn.setRequestProperty("Authorization", "Bearer ".concat(accessToken));
		        conn.setDoInput(true);
	    		conn.setUseCaches(false);
				break;
			case PROCESS:
			case RESULT:
				conn.setRequestMethod("GET");
				conn.setRequestProperty("Authorization", "Bearer ".concat(accessToken));
				break;
			default:
				break;
		}
		
		if(sandbox == true) {
			conn.setRequestProperty("copyleaks-sandbox-mode", "");
		}
		
        return conn;
	}
	
//	Metoda koja vraca AccessToken na osnovu JSON response-a
	static String getAccessToken(InputStream in) throws IOException, JSONException {
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
        JSONObject response = new JSONObject(br.readLine());
        String accessToken = response.getString("access_token");
        return accessToken;
	}
	
//	Metoda koja vraca listu ProcessId-eva na osnovu JSON response-a
	static ArrayList<String> getProcessIdList(InputStream in) throws IOException, JSONException {
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
        JSONObject response = new JSONObject(br.readLine());
        JSONArray success = response.getJSONArray("Success");
        ArrayList<String> processId = new ArrayList<String>();

        for(int i = 0; i < success.length(); i++) {
        	JSONObject current = success.getJSONObject(i);
        	processId.add(current.getString("ProcessId"));
        }
        
        return processId;
	}
	
//	Metoda koja vraca drugi deo URL-a za dat servis na osnovu processId-a
	static String getProcessStatusExtension(String processId) {
		String extension = "/v1/education/" + processId + "/status";
		return extension;
	}
	
//	Metoda koja vraca status obrade fajla na osnovu InputStream-a
	static String getStatus(InputStream in) throws JSONException, IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
        JSONObject response = new JSONObject(br.readLine());
        String status = response.getString("Status");
        int percentage = response.getInt("ProgressPercents");
        System.out.println(status + "\n" + percentage + "%\n");
        return status;
	}
	
//	Metoda koja vraca drugi deo URL-a za dat servis na osnovu processId-a
	static String getResultExtension(String processId) {
		String extension = "/v1/education/" + processId + "/result";
		return extension;
	}
	
//	Metoda koja vraca rezultate obrade datoteke
	static void getResults(InputStream in) throws JSONException, IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		JSONArray arrayOfResults = new JSONArray(br.readLine());
		
		if(arrayOfResults.length() == 0) {
			System.out.println("Nema rezultata za ovu datoteku!");
			return;
		}
		
		for(int i = 0; i < arrayOfResults.length(); i++) {
			System.out.println("Result #" + (i+1));
			JSONObject currentObject = arrayOfResults.getJSONObject(i);
			
	        String url = currentObject.getString("URL");
	        int percents = currentObject.getInt("Percents");
	        int numberOfCopiedWords = currentObject.getInt("NumberOfCopiedWords");
	        String comparisonReport = currentObject.getString("ComparisonReport");
	        String cachedVersion = currentObject.getString("CachedVersion");
	        String title = currentObject.getString("Title");
	        String introduction = currentObject.getString("Introduction");
	        String embededComparison = currentObject.getString("EmbededComparison");
	        
	        System.out.println("URL: " + url);
	        System.out.println("Percents: " + percents);
	        System.out.println("Number of copied words: " + numberOfCopiedWords);
	        System.out.println("Comparison report: " + comparisonReport);
	        System.out.println("Cached version: " + cachedVersion);
	        System.out.println("Title: " + title);
	        System.out.println("Introduction: " + introduction);
	        System.out.println("Embeded Comparison: " + embededComparison);
	        System.out.println("\n");
		}
	}
	
	static String accessToken;
	final static String API_URL = "https://api.copyleaks.com";
	final static String LOGIN_EXTENSION = "/v1/account/login-api";
	final static String SCAN_FILES_EXTENSION = "/v2/education/create-by-file";
	static boolean sandbox = true;
	
	public static void main(String args[]) {
		
		HttpURLConnection conn;
		OutputStream os;
		InputStream in;
		Scanner sc;
		
		byte[] outputBytes;
		
		boolean goAhead;
		boolean hasLogin;
		
		try {
			goAhead = false;
			hasLogin = false;
			sc = new Scanner(System.in);
			
			System.out.println("Dobrodosli na moj projektni zadatak!\n");
			
			while(goAhead == false) {
				System.out.println("Da li imate svoj CopyLeaks nalog za autentikaciju?");
				System.out.println("Ako imate, unesite Y, u suprotnom unesite N");
				String answer = sc.nextLine();
				
				if(answer.toLowerCase().trim().equals("y")) {
					goAhead = true;
					hasLogin = true;
				} else if (answer.toLowerCase().trim().equals("n")) {
					goAhead = true;
				}
				
			}
			
			System.out.println("");
			goAhead = false;
			
			if(hasLogin == true) {
				sc = new Scanner(System.in);
				
				while(goAhead == false) {
					System.out.println("Da li zelite da koristite sandbox mode? Y/N");
					String answer = sc.nextLine();
					
					if(answer.toLowerCase().trim().equals("y")) {
						sandbox = true;
						goAhead = true;
					}
					else if(answer.toLowerCase().trim().equals("n")) {
						sandbox = false;
						goAhead = true;
					}
				}
				goAhead = false;
			}
//			
			
//			LOGIN
        	conn = setupJSONConnection(LOGIN_EXTENSION, APIService.LOGIN);
	        
			if(hasLogin == false) {
		        outputBytes = "{'Email':'dusancvijic1906@gmail.com','ApiKey':'2580AFFF-B941-4E34-9451-74CD2B02E3F6'}".getBytes("UTF-8");
			}
			else {
				sc = new Scanner(System.in);
					
				System.out.println("Molim Vas da pazljivo unesete sledece podatke\n");
				System.out.println("Unesite Vasu email adresu");
				String email = sc.nextLine().trim();
				System.out.println("Unesite Vas ApiKey");
				String apiKey = sc.nextLine().trim();
				System.out.println("");
				
				outputBytes = ("{'Email':'"+ email +"','ApiKey':'" + apiKey + "'}").getBytes("UTF-8");
			}
			
			os = conn.getOutputStream();
	        os.write(outputBytes);
	        os.close();
	        
	        System.out.println(conn.getResponseMessage());
	        System.out.println(conn.getResponseCode() + "\n");
	        
	        in = conn.getInputStream();
	        accessToken = getAccessToken(in);
//	        
	        
//	        SCAN FILE(S)
	        ArrayList<String> processId;
	        
	        {
	        	ArrayList<File> files = new ArrayList<File>();
	        	sc = new Scanner(System.in);
	        	
	        	System.out.println("Unesite apsolutne putanje ka .txt ili .html fajlovima. Slusamo Vas dok ne unesete N");
	        	
	        	while(goAhead == false) {
	        		System.out.println("Unesite putanju, ili ako ne zelite vise, unesti slovo N");
	        		String path;
	        		path = sc.nextLine().trim();
	        		
	        		if(path.endsWith(".txt") || path.endsWith(".html")){
	        			files.add(new File(path));
	        		}
	        		else if(path.toLowerCase().trim().equals("n") && !files.isEmpty()) {
	        			goAhead = true;
	        			sc.close();
	        		}
	        		else if(files.isEmpty()){
	        			System.out.println("Molim Vas bar jedan fajl da unesete");
	        		}
	        		else {
	        			System.out.println("Molim Vas da se pobrinete da fajl bude ili .txt ili .html :)");
	        		}
	        	}
	        	
	        	String twoHyphens = "--";
		    	String boundary =  Long.toString(System.currentTimeMillis());
		    	String lineEnd = "\r\n";
		    	
		    	int bytesRead, bytesAvailable, bufferSize;
		    	byte[] buffer;
		    	int maxBufferSize = 1*1024*1024;
	        	
	        	conn = setupJSONConnection(SCAN_FILES_EXTENSION, APIService.SCAN_FILES);
	    		conn.setRequestProperty("Content-Type", "multipart/mixed; boundary="+boundary);
	    		
		        DataOutputStream outputStream = new DataOutputStream(conn.getOutputStream());
		        FileInputStream fileInputStream;;
		        
		        for(int i = 0; i < files.size(); i++) {
		        	
		        	if(!files.get(i).exists()) {
		        		continue;
		        	}
		        	
		        	File currentFile = files.get(i);
		        	fileInputStream = new FileInputStream(currentFile);
		        	outputStream.writeBytes(twoHyphens + boundary + lineEnd);
		    		outputStream.writeBytes("Content-Disposition: form-data; name=\"file"+i+"\"; filename=\""+currentFile.getName()+"\"" + lineEnd);
		    		
		    		if(currentFile.getName().endsWith(".txt")) {
		    			outputStream.writeBytes("Content-Type: text/plain" + lineEnd);
		    		}
		    		else {
		    			outputStream.writeBytes("Content-Type: text/html" + lineEnd);
		    		}
		    		
		    		outputStream.writeBytes(lineEnd);
		    		
		    		bytesAvailable = fileInputStream.available();
		    		bufferSize = Math.min(bytesAvailable, maxBufferSize);
		    		buffer = new byte[bufferSize];
		    		
		    		bytesRead = fileInputStream.read(buffer, 0, bufferSize);
		    		
		    		while(bytesRead > 0) {
		    			outputStream.write(buffer, 0, bufferSize);
		    			bytesAvailable = fileInputStream.available();
		    			bufferSize = Math.min(bytesAvailable, maxBufferSize);
		    			bytesRead = fileInputStream.read(buffer, 0, bufferSize);
		    		}
		    		
		    		bytesRead = fileInputStream.read(buffer, 0, bufferSize);
		    		outputStream.writeBytes(lineEnd);
		        }
		        
		        outputStream.writeBytes(lineEnd);
		        outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
		        outputStream.flush();
	    		outputStream.close();
	    		
	    		System.out.println(conn.getResponseMessage());
		        System.out.println(conn.getResponseCode() + "\n");
		        
		        in = conn.getInputStream();
		        processId = getProcessIdList(in);
		        System.out.println("Uneli ste " + files.size() + " datoteka, a od njih je " + processId.size() + " uspesno uneseno.\n");
	        }
//	        
	        
//	        PROCESSING
	        if(!processId.isEmpty()){
	        	String processStatusExtension;
	        	String status;
		        long starting;
	        	System.out.println("Sada pocinjemo sa procesuiranjem fajlova...");
	        	
	        	for(int i = 0; i < processId.size(); i++) {
	        		processStatusExtension = getProcessStatusExtension(processId.get(i));
	        		status = "Processing";
	        		starting = System.currentTimeMillis();
	        		
	        		do {
	    	        	if(System.currentTimeMillis() - starting >= 3000) {
	    	        		
	    		        	conn = setupJSONConnection(processStatusExtension, APIService.PROCESS);
	    	
	    			        conn.connect();
	    			        
	    			        in = conn.getInputStream();
	    			        System.out.println("Datoteka " + (i+1) + "/" + (processId.size()));
	    	        		status = getStatus(in);
	    	        		
	    			        starting = System.currentTimeMillis();
	    	        	}
	    	        } while (status.equals("Processing"));
	        		
	        		System.out.println(conn.getResponseMessage());
			        System.out.println(conn.getResponseCode() + "\n");
	        	}
	        	System.out.println("");
	        }
	        
//	        
	        
//	        RESULTS
        	for(int i = 0; i < processId.size(); i++) {
        		String resultExtension = getResultExtension(processId.get(i));
		        conn = setupJSONConnection(resultExtension, APIService.RESULT);
				
				in = conn.getInputStream();
				System.out.println("Rezultati za datoteku #" + (i+1));
				getResults(in);
        	}
//	        	
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}
}
