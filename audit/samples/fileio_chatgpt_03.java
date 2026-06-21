// sample_id: fileio_chatgpt_03
// prompt: P4
// model: ChatGPT
// model_version: GPT-5.5 Thinking
// date: 2026-06-03
// compiles: yes
// edits_to_compile: none

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class fileio_chatgpt_03 {

    public static List<String> readSettings(String path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }

        return Files.readAllLines(Path.of(path), StandardCharsets.UTF_8);
    }
}