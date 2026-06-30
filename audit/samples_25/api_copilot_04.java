// sample_id: api_copilot_04
// prompt: P2
// model: Copilot
// model_version: Haiku 4.5
// date: 2026-06-3
// compiles: yes
// edits_to_compile: none

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class api_copilot_04 {
    
    /**
     * Fetches the user profile from the REST API.
     * 
     * @param userId the user ID to fetch the profile for
     * @return the JSON response body as a String
     * @throws Exception if the API call fails or an I/O error occurs
     */
    public static String getUserProfile(String userId) throws Exception {
        String urlString = "https://api.example.com/users/" + userId;
        URL url = new URL(urlString);
        
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("API call failed with response code: " + responseCode);
        }
        
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(connection.getInputStream())
        );
        
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        
        reader.close();
        connection.disconnect();
        
        return response.toString();
    }
    
    // Example usage
    public static void main(String[] args) {
        try {
            String userProfile = getUserProfile("12345");
            System.out.println("User Profile: " + userProfile);
        } catch (Exception e) {
            System.err.println("Error fetching user profile: " + e.getMessage());
            e.printStackTrace();
        }
    }
}