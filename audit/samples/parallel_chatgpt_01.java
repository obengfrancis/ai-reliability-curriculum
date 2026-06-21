// sample_id: parallel_chatgpt_01
// prompt: P5
// model: ChatGPT
// model_version: GPT-5.5 Thinking
// date: 2026-05-29
// compiles: yes
// edits_to_compile: none

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class parallel_chatgpt_01 {
    public long sumInParallel(List<Integer> numbers) {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future<Long>> futures = new ArrayList<>();

        int size = numbers.size();
        int chunkSize = (int) Math.ceil(size / 4.0);

        for (int i = 0; i < 4; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, size);

            Callable<Long> task = () -> {
                long sum = 0;
                for (int j = start; j < end; j++) {
                    sum += numbers.get(j);
                }
                return sum;
            };

            futures.add(executor.submit(task));
        }

        long total = 0;

        try {
            for (Future<Long> future : futures) {
                total += future.get();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while summing numbers in parallel", e);
        } finally {
            executor.shutdown();
        }

        return total;
    }
}