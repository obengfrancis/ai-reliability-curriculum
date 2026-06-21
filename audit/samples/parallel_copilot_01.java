// sample_id: parallel_copilot_01
// prompt: P5
// model: Copilot
// model_version: Haiku 4.5
// date: 2026-05-29
// compiles: yes
// edits_to_compile: none

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;

public class parallel_copilot_01 {
    /**
     * Splits a list of numbers across four threads, sums them in parallel,
     * and returns the total as a long.
     * 
     * @param numbers the list of integers to sum
     * @return the total sum as a long
     * @throws InterruptedException if a thread is interrupted
     * @throws ExecutionException if a task throws an exception
     */
    public static long sumInParallel(List<Integer> numbers) throws InterruptedException, ExecutionException {
        int numThreads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        try {
            int listSize = numbers.size();
            int chunkSize = (listSize + numThreads - 1) / numThreads; // Ceiling division
            
            Future<Long>[] futures = new Future[numThreads];
            
            // Create tasks for each thread
            for (int i = 0; i < numThreads; i++) {
                final int startIndex = i * chunkSize;
                final int endIndex = Math.min(startIndex + chunkSize, listSize);
                
                // Submit task to executor
                futures[i] = executor.submit(() -> {
                    long sum = 0;
                    for (int j = startIndex; j < endIndex; j++) {
                        sum += numbers.get(j);
                    }
                    return sum;
                });
            }
            
            // Collect results from all threads
            long totalSum = 0;
            for (int i = 0; i < numThreads; i++) {
                totalSum += futures[i].get();
            }
            
            return totalSum;
            
        } finally {
            // Shutdown the executor
            executor.shutdown();
        }
    }
    
    /**
     * Example usage of sumInParallel method
     */
    public static void main(String[] args) {
        try {
            // Create a sample list of integers
            List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
            
            long totalSum = sumInParallel(numbers);
            System.out.println("Total sum (parallel): " + totalSum);
            
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error during parallel computation: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
