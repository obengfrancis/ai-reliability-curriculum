// sample_id: fileio_chatgpt_02
// prompt: P4
// model: ChatGPT
// model_version: GPT-5.5 Thinking
// date: 2026-05-29
// compiles: yes
// edits_to_compile: none

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class fileio_chatgpt_02 {

    public static List<String> readSettings(String path) throws IOException {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Path must not be null or empty.");
        }

        Path settingsPath = Path.of(path);

        if (!Files.exists(settingsPath)) {
            throw new IOException("Settings file does not exist: " + path);
        }

        if (!Files.isRegularFile(settingsPath)) {
            throw new IOException("Path is not a regular file: " + path);
        }

        return Files.readAllLines(settingsPath, StandardCharsets.UTF_8);
    }
}