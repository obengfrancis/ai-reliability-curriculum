// sample_id: parallel_copilot_03
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
import java.util.concurrent.ExecutionException;

public class parallel_copilot_03 {
    /**
     * Splits a list of integers across four threads, sums the numbers in parallel,
     * and returns the total as a long.
     *
     * @param numbers the list of integers to sum
     * @return the sum of all numbers
     */
    public static long sumInParallel(List<Integer> numbers) {
        if (numbers == null || numbers.isEmpty()) {
            return 0L;
        }

        // Create a thread pool with 4 threads
        ExecutorService executor = Executors.newFixedThreadPool(4);
        
        try {
            int numThreads = 4;
            int size = numbers.size();
            int chunkSize = (size + numThreads - 1) / numThreads; // Ceiling division
            
            @SuppressWarnings("unchecked")
            Future<Long>[] futures = new Future[numThreads];
            
            // Submit tasks to split the work across 4 threads
            for (int i = 0; i < numThreads; i++) {
                int start = i * chunkSize;
                int end = Math.min(start + chunkSize, size);
                
                if (start < size) {
                    final int startIdx = start;
                    final int endIdx = end;
                    
                    futures[i] = executor.submit(() -> {
                        long sum = 0L;
                        for (int j = startIdx; j < endIdx; j++) {
                            sum += numbers.get(j);
                        }
                        return sum;
                    });
                }
            }
            
            // Collect results from all threads
            long totalSum = 0L;
            for (int i = 0; i < numThreads; i++) {
                if (futures[i] != null) {
                    totalSum += futures[i].get();
                }
            }
            
            return totalSum;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error during parallel sum calculation", e);
        } finally {
            executor.shutdown();
        }
    }
    
    // Example usage
    public static void main(String[] args) {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        long result = sumInParallel(numbers);
        System.out.println("Sum: " + result); // Output: Sum: 55
        
        // Test with empty list
        long emptyResult = sumInParallel(List.of());
        System.out.println("Empty sum: " + emptyResult); // Output: Empty sum: 0
    }
}
