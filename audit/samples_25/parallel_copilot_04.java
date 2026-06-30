// sample_id: parallel_copilot_04
// prompt: P5
// model: Copilot
// model_version: Haiku 4.5
// date: 2026-06-3
// compiles: yes
// edits_to_compile: none

import java.util.List;
import java.util.concurrent.*;

public class parallel_copilot_04 {
    /**
     * Sums a list of integers using parallel processing across four threads.
     * The list is split into four chunks, each processed by a separate thread.
     * 
     * @param numbers the list of integers to sum
     * @return the total sum of all numbers
     */
    public static long sumInParallel(List<Integer> numbers) {
        int threadCount = 4;
        int listSize = numbers.size();
        
        // Handle edge case: empty list
        if (listSize == 0) {
            return 0L;
        }
        
        // Create a thread pool with 4 threads
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        try {
            // Calculate the chunk size for each thread
            int chunkSize = (listSize + threadCount - 1) / threadCount; // Ceiling division
            
            // Create a list to hold future results
            java.util.List<Future<Long>> futures = new java.util.ArrayList<>();
            
            // Submit tasks to sum each chunk
            for (int i = 0; i < threadCount; i++) {
                int start = i * chunkSize;
                int end = Math.min(start + chunkSize, listSize);
                
                // Only create a task if there are elements to process
                if (start < listSize) {
                    final List<Integer> sublist = numbers.subList(start, end);
                    
                    Future<Long> future = executor.submit(() -> {
                        long sum = 0;
                        for (Integer num : sublist) {
                            sum += num;
                        }
                        return sum;
                    });
                    
                    futures.add(future);
                }
            }
            
            // Aggregate results from all threads
            long totalSum = 0;
            for (Future<Long> future : futures) {
                totalSum += future.get(); // Blocks until the result is available
            }
            
            return totalSum;
            
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error during parallel sum computation", e);
        } finally {
            // Shutdown the executor to clean up resources
            executor.shutdown();
            try {
                // Wait for all tasks to complete (with timeout)
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    // Example usage and testing
    public static void main(String[] args) {
        // Test case 1: Normal list
        List<Integer> numbers1 = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        System.out.println("Sum of " + numbers1 + " = " + sumInParallel(numbers1)); // Expected: 55
        
        // Test case 2: Small list (fewer elements than threads)
        List<Integer> numbers2 = List.of(10, 20);
        System.out.println("Sum of " + numbers2 + " = " + sumInParallel(numbers2)); // Expected: 30
        
        // Test case 3: Empty list
        List<Integer> numbers3 = List.of();
        System.out.println("Sum of " + numbers3 + " = " + sumInParallel(numbers3)); // Expected: 0
        
        // Test case 4: Large list
        List<Integer> numbers4 = java.util.stream.IntStream.rangeClosed(1, 1000)
            .boxed()
            .toList();
        System.out.println("Sum of 1..1000 = " + sumInParallel(numbers4)); // Expected: 500500
    }
}