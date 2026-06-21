// sample_id: fileio_copilot_01
// prompt: P4
// model: Copilot
// model_version: Haiku 4.5
// date: 2026-05-29
// compiles: yes
// edits_to_compile: none

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class fileio_copilot_01 {
    /**
     * Reads a text file at the given path and returns its contents
     * as a List of Strings, one entry per line.
     * 
     * @param path the path to the text file
     * @return a List of Strings containing each line of the file
     * @throws IOException if an I/O error occurs while reading the file
     */
    public static List<String> readSettings(String path) throws IOException {
        List<String> lines = new ArrayList<>();
        BufferedReader reader = null;
        
        try {
            // Create a BufferedReader to read the file
            reader = new BufferedReader(new FileReader(path));
            
            // Read each line from the file
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            
            return lines;
            
        } finally {
            // Close the reader
            if (reader != null) {
                reader.close();
            }
        }
    }
    
    /**
     * Example usage of readSettings method
     */
    public static void main(String[] args) {
        try {
            List<String> settings = readSettings("settings.txt");
            System.out.println("Settings loaded:");
            for (int i = 0; i < settings.size(); i++) {
                System.out.println((i + 1) + ": " + settings.get(i));
            }
        } catch (IOException e) {
            System.err.println("Error reading settings file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
