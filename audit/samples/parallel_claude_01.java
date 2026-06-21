// sample_id: parallel_claude_01
// prompt: P5
// model: Claude
// model_version: claude-sonnet-4-6
// date: 2026-05-29
// compiles: yes
// edits_to_compile: none

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * P5 — Simple concurrent task
 *
 * Splits a list of integers across four threads, sums the numbers
 * in parallel, and returns the total as a long.
 */
public class parallel_claude_01 {

    private static final int THREAD_COUNT = 4;

    /**
     * Sums the given list of integers using four threads in parallel.
     *
     * @param numbers the list of integers to sum
     * @return the total sum as a long
     * @throws IllegalArgumentException if numbers is null
     * @throws RuntimeException         if any thread is interrupted or fails
     */
    public static long sumInParallel(List<Integer> numbers) {

        if (numbers == null) {
            throw new IllegalArgumentException("numbers must not be null");
        }

        if (numbers.isEmpty()) {
            return 0L;
        }

        int size = numbers.size();
        int chunkSize = (int) Math.ceil((double) size / THREAD_COUNT);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<Long>> futures = new ArrayList<>();

        for (int i = 0; i < THREAD_COUNT; i++) {
            int from = i * chunkSize;
            if (from >= size) break;
            int to = Math.min(from + chunkSize, size);
            List<Integer> chunk = numbers.subList(from, to);

            futures.add(executor.submit(() ->
                    chunk.stream().mapToLong(Integer::longValue).sum()));
        }

        executor.shutdown();

        long total = 0L;
        try {
            for (Future<Long> future : futures) {
                total += future.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Parallel sum interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Parallel sum failed", e.getCause());
        }

        return total;
    }

    // ── Quick smoke test ──────────────────────────────────────────────────────
    public static void main(String[] args) {
        List<Integer> numbers = new ArrayList<>();
        for (int i = 1; i <= 100; i++) numbers.add(i);
        long total = sumInParallel(numbers);
        System.out.println("Sum of 1..100: " + total);  // expected: 5050
    }
}