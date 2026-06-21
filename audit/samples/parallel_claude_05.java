// sample_id: parallel_claude_05
// prompt: P4
// model: Claude
// model_version: Sonnet 4.6
// date: 2026-06-03
// compiles: yes
// edits_to_compile: none

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class parallel_claude_05 {

    private static final int THREADS = 4;

    public static long sumInParallel(List<Integer> numbers)
            throws InterruptedException, ExecutionException {

        int size = numbers.size();
        int chunkSize = (size + THREADS - 1) / THREADS;

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        List<Future<Long>> futures = new ArrayList<>();

        for (int i = 0; i < THREADS; i++) {
            int from = i * chunkSize;
            if (from >= size) break;
            int to = Math.min(from + chunkSize, size);

            List<Integer> partition = numbers.subList(from, to);
            futures.add(executor.submit(() ->
                partition.stream().mapToLong(Integer::longValue).sum()
            ));
        }

        executor.shutdown();

        long total = 0L;
        for (Future<Long> f : futures) {
            total += f.get();
        }
        return total;
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        System.out.println("Sum: " + sumInParallel(numbers)); // 55
    }
}