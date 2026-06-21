// sample_id: parallel_chatgpt_04
// prompt: P5
// model: GPT-5.5 Thinking
// model_version: GPT-5.5 Thinking
// date: 2026-06-03
// compiles: yes
// edits_to_compile: none

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class parallel_chatgpt_04 {

    private static final int THREAD_COUNT = 4;

    public static long sumInParallel(List<Integer> numbers) {
        if (numbers == null) {
            throw new IllegalArgumentException("numbers cannot be null");
        }

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Callable<Long>> tasks = new ArrayList<>();

        int size = numbers.size();
        int chunkSize = (size + THREAD_COUNT - 1) / THREAD_COUNT;

        for (int i = 0; i < THREAD_COUNT; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, size);

            tasks.add(() -> {
                long partialSum = 0;
                for (int j = start; j < end; j++) {
                    partialSum += numbers.get(j);
                }
                return partialSum;
            });
        }

        try {
            long total = 0;
            for (var future : executor.invokeAll(tasks)) {
                total += future.get();
            }
            return total;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Parallel sum was interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Error while computing parallel sum", e);
        } finally {
            executor.shutdown();
        }
    }
}