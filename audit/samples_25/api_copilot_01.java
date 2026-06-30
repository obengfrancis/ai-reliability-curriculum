// sample_id: api_copilot_01
// prompt: P2
// model: Copilot
// model_version: Haiku 4.5
// date: 2026-05-29
// compiles: yes
// edits_to_compile: none

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class api_copilot_01 {
    /**
     * Calls a REST API to retrieve user profile data.
     * 
     * @param userId the user ID to retrieve the profile for
     * @return the JSON response body as a String
     * @throws Exception if an I/O error occurs or the URL is invalid
     */
    public static String getUserProfile(String userId) throws Exception {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        
        try {
            // Construct the API URL with the userId parameter
            String apiUrl = "https://api.example.com/users/" + userId;
            URL url = new URL(apiUrl);
            connection = (HttpURLConnection) url.openConnection();
            
            // Set request method and properties
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("Accept", "application/json");
            
            // Get response code
            int responseCode = connection.getResponseCode();
            
            if (responseCode >= 200 && responseCode < 300) {
                // Success response
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } else {
                // Error response
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            }
            
            // Read JSON response body
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
     * Example usage of getUserProfile method
     */
    public static void main(String[] args) {
        try {
            String userProfile = getUserProfile("12345");
            System.out.println("User profile retrieved:");
            System.out.println(userProfile);
        } catch (Exception e) {
            System.err.println("Error retrieving user profile: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
