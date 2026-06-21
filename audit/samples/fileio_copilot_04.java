// sample_id: fileio_copilot_04
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

public class fileio_copilot_04 {
    /**
     * Reads a text file at the given path and returns its contents as a List of Strings,
     * one entry per line.
     *
     * @param path the file path to read
     * @return a List of Strings, where each element is a line from the file
     * @throws IOException if an I/O error occurs reading from the file
     */
    public static List<String> readSettings(String path) throws IOException {
        return Files.readAllLines(Paths.get(path));
    }
}