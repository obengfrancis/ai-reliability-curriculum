// sample_id: fileio_claude_01
// prompt: P4
// model: Claude
// model_version: claude-sonnet-4-6
// date: 2026-05-29
// compiles: yes
// edits_to_compile: none

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * P4 — File I/O
 *
 * Reads a text file at the given path and returns its contents as a
 * List of Strings, one entry per line.
 */
public class fileio_claude_01 {

    /**
     * Reads the file at the given path and returns its lines.
     *
     * @param path the path to the text file to read
     * @return a List of Strings, one entry per line
     * @throws IOException              if an I/O error occurs reading the file
     * @throws IllegalArgumentException if path is null or blank
     */
    public static List<String> readSettings(String path) throws IOException {

        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path must not be null or blank");
        }

        return Files.readAllLines(Paths.get(path));
    }

    // ── Quick smoke test ──────────────────────────────────────────────────────
    public static void main(String[] args) throws Exception {
        String path = "settings.txt";
        System.out.println("Reading settings from: " + path);
        List<String> lines = readSettings(path);
        lines.forEach(System.out::println);
    }
}