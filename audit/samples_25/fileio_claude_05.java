// sample_id: fileio_claude_05
// prompt: P4
// model: Claude
// model_version: Sonnet 4.6
// date: 2026-06-03
// compiles: yes
// edits_to_compile: none


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class fileio_claude_05 {

    public List<String> readSettings(String path) {
        try {
            return Files.readAllLines(Paths.get(path));
        } catch (IOException e) {
            System.err.println("Failed to read settings from: " + path);
            return Collections.emptyList();
        }
    }
}