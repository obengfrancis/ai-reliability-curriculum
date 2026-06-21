// sample_id: dbquery_claude_03
// prompt: P4
// model: Claude
// model_version: Sonnet 4.6
// date: 2026-06-03
// compiles: yes
// edits_to_compile: none

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class fileio_claude_03 {

    public List<String> readSettings(String path) throws IOException {
        return Files.readAllLines(Paths.get(path));
    }
}