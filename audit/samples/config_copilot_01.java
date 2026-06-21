// sample_id: config_copilot_01
// prompt: P1
// model: Copilot
// model_version: Haiku 4.5
// date: 2026-05-29
// compiles: yes
// edits_to_compile: none

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class config_copilot_01 {
    /**
     * Retrieves configuration data from a remote server over HTTP.
     * 
     * @param url the URL of the remote configuration server
     * @return the response body as a String
     * @throws Exception if an I/O error occurs or the URL is invalid
     */
    public static String fetchConfig(String url) throws Exception {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        
        try {
            // Create URL object and open connection
            URL configUrl = new URL(url);
            connection = (HttpURLConnection) configUrl.openConnection();
            
            // Set request method and properties
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            // Get response code
            int responseCode = connection.getResponseCode();
            
            if (responseCode >= 200 && responseCode < 300) {
                // Success response
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } else {
                // Error response
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            }
            
            // Read response body
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
            
            return response.toString().trim();
            
        } finally {
            // Close resources
            if (reader != null) {
                reader.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Example usage of fetchConfig method
     */
    public static void main(String[] args) {
        try {
            String config = fetchConfig("http://example.com/config");
            System.out.println("Configuration retrieved:");
            System.out.println(config);
        } catch (Exception e) {
            System.err.println("Error fetching configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
