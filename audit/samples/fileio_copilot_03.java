// sample_id: fileio_copilot_03
// prompt: P4
// model: Copilot
// model_version: Haiku 4.5
// date: 2026-06-3
// compiles: yes
// edits_to_compile: none

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class fileio_copilot_03 {
    /**
     * Reads a text file at the given path and returns its contents as a List of Strings.
     * Each element in the list represents one line from the file.
     *
     * @param path the file path to read
     * @return a List of Strings, one entry per line
     * @throws IOException if an I/O error occurs while reading the file
     */
    public static List<String> readSettings(String path) throws IOException {
        return Files.readAllLines(Paths.get(path));
    }
}