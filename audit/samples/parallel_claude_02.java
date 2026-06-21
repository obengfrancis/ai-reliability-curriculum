// sample_id: parallel_claude_02
// prompt: P5
// model: Claude
// model_version: claude-sonnet-4.6
// date: 2026-06-2
// compiles: yes
// edits_to_compile: none

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class parallel_claude_02 {

    public static long sumInParallel(List<Integer> numbers)
            throws InterruptedException, ExecutionException {

        final int THREADS = 4;
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        List<Future<Long>> futures = new ArrayList<>(THREADS);

        int size = numbers.size();
        int chunkSize = (size + THREADS - 1) / THREADS;   // ceiling division

        for (int i = 0; i < THREADS; i++) {
            int start = i * chunkSize;
            if (start >= size) break;                      // fewer items than threads
            int end = Math.min(start + chunkSize, size);

            List<Integer> chunk = numbers.subList(start, end);  // zero-copy view

            futures.add(executor.submit(() -> {
                long partial = 0;
                for (int n : chunk) partial += n;
                return partial;
            }));
        }

        executor.shutdown();   // no new tasks; lets submitted ones finish

        long total = 0;
        for (Future<Long> f : futures) {
            total += f.get();  // blocks until each chunk is done, rethrows exceptions
        }
        return total;
    }
}