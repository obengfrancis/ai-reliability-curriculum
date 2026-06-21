// sample_id: parallel_copilot_05
// prompt: P5
// model: Copilot
// model_version: Haiku 4.5
// date: 2026-06-3
// compiles: yes
// edits_to_compile: none

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;

public class parallel_copilot_05 {
    
    /**
     * Sums the numbers in the provided list using four parallel threads.
     * 
     * @param numbers the list of integers to sum
     * @return the total sum as a long
     */
    public static long sumInParallel(List<Integer> numbers) {
        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        try {
            int totalSize = numbers.size();
            int chunkSize = (totalSize + threadCount - 1) / threadCount; // Ceiling division
            
            List<Future<Long>> futures = new ArrayList<>();
            
            // Split work across 4 threads
            for (int i = 0; i < threadCount; i++) {
                int start = i * chunkSize;
                int end = Math.min(start + chunkSize, totalSize);
                
                // Skip if this thread has no work
                if (start >= totalSize) {
                    continue;
                }
                
                // Submit task to sum a chunk
                Future<Long> future = executor.submit(() -> {
                    long sum = 0;
                    for (int j = start; j < end; j++) {
                        sum += numbers.get(j);
                    }
                    return sum;
                });
                
                futures.add(future);
            }
            
            // Collect results from all threads
            long totalSum = 0;
            for (Future<Long> future : futures) {
                totalSum += future.get(); // Blocks until the result is available
            }
            
            return totalSum;
            
        } catch (Exception e) {
            throw new RuntimeException("Error during parallel sum", e);
        } finally {
            executor.shutdown();
        }
    }
    
    // Example usage
    public static void main(String[] args) {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        long result = sumInParallel(numbers);
        System.out.println("Sum: " + result); // Output: Sum: 55
    }
}