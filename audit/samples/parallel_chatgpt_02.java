// sample_id: parallel_chatgpt_02
// prompt: P5
// model: GPT-5.5 Thinking
// model_version: GPT-5.5 Thinking
// date: 2026-06-02
// compiles: yes
// edits_to_compile: none

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class parallel_chatgpt_02 {

    public static long sumInParallel(List<Integer> numbers) {
        if (numbers == null || numbers.isEmpty()) {
            return 0L;
        }

        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<Long>> futures = new ArrayList<>();

        int size = numbers.size();
        int chunkSize = (size + threadCount - 1) / threadCount;

        for (int i = 0; i < threadCount; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, size);

            if (start >= size) {
                break;
            }

            Callable<Long> task = () -> {
                long partialSum = 0L;
                for (int j = start; j < end; j++) {
                    partialSum += numbers.get(j);
                }
                return partialSum;
            };

            futures.add(executor.submit(task));
        }

        long total = 0L;

        try {
            for (Future<Long> future : futures) {
                total += future.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Parallel sum was interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Error while computing parallel sum", e);
        } finally {
            executor.shutdown();
        }

        return total;
    }
}