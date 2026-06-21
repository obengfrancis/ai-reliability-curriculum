// sample_id: parallel_claude_04
// prompt: P5
// model: Claude
// model_version: Sonnet 4.6
// date: 2026-06-03
// compiles: yes
// edits_to_compile: none

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class parallel_claude_04 {

    private static final int THREADS = 4;
    private final ExecutorService executor = Executors.newFixedThreadPool(THREADS);

    public long sumInParallel(List<Integer> numbers) throws InterruptedException, ExecutionException {
        List<Future<Long>> futures = new ArrayList<>(THREADS);

        int size = numbers.size();
        int chunkSize = (size + THREADS - 1) / THREADS;

        for (int i = 0; i < THREADS; i++) {
            int from = i * chunkSize;
            int to   = Math.min(from + chunkSize, size);
            if (from >= size) break;

            List<Integer> chunk = numbers.subList(from, to);
            futures.add(executor.submit(() -> {
                long partial = 0;
                for (int n : chunk) partial += n;
                return partial;
            }));
        }

        long total = 0;
        for (Future<Long> f : futures) {
            total += f.get();
        }
        return total;
    }

    public void shutdown() {
        executor.shutdown();
    }
}