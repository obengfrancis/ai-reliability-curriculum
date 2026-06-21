// sample_id: parallel_claude_03
// prompt: P5
// model: Claude
// model_version: Sonnet 4.6
// date: 2026-06-03
// compiles: yes
// edits_to_compile: none

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class parallel_claude_03 {

    public static long sumInParallel(List<Integer> numbers) {
        if (numbers == null) {
            throw new IllegalArgumentException("numbers cannot be null");
        }

        final int numThreads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<Long>> futures = new ArrayList<>();

        int size = numbers.size();
        int chunkSize = (size + numThreads - 1) / numThreads;

        try {
            for (int i = 0; i < numThreads; i++) {
                int start = i * chunkSize;
                int end = Math.min(start + chunkSize, size);

                Callable<Long> task = () -> {
                    long partialSum = 0;
                    for (int j = start; j < end; j++) {
                        partialSum += numbers.get(j);
                    }
                    return partialSum;
                };

                futures.add(executor.submit(task));
            }

            long total = 0;
            for (Future<Long> future : futures) {
                total += future.get();
            }

            return total;
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Parallel sum was interrupted", e);
        }
        catch (ExecutionException e) {
            throw new RuntimeException("Parallel sum failed", e);
        }
        finally {
            executor.shutdown();
        }
    }
}