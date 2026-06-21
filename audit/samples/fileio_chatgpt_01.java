// sample_id: fileio_chatgpt_01
// prompt: P4
// model: ChatGPT
// model_version: GPT-5.5 Thinking
// date: 2026-05-29
// compiles: yes
// edits_to_compile: none

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class fileio_chatgpt_01 {
    public List<String> readSettings(String path) throws IOException {
        return Files.readAllLines(Path.of(path));
    }
}