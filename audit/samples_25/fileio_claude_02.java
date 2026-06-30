// sample_id: fileio_claude_02
// prompt: P4
// model: Claude
// model_version: claude-sonnet-4.6
// date: 2026-06-2
// compiles: yes
// edits_to_compile: none

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class fileio_claude_02 {

    public List<String> readSettings(String path) throws IOException {
        return Files.readAllLines(Paths.get(path));
    }

    public static void main(String[] args) {
        fileio_claude_02 reader = new fileio_claude_02();
        try {
            List<String> lines = reader.readSettings("settings.txt");
            for (String line : lines) {
                System.out.println(line);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }
}