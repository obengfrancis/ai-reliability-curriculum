// sample_id: parallel_copilot_02
// prompt: P5
// model: Copilot
// model_version: Haiku 4.5
// date: 2026-06-2
// compiles: yes
// edits_to_compile: none

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class parallel_copilot_02 {
    
    /**
     * Sums the numbers in the given list using parallel processing across four threads.
     * 
     * @param numbers the list of integers to sum
     * @return the total sum as a long
     */
    public static long sumInParallel(List<Integer> numbers) {
        if (numbers == null || numbers.isEmpty()) {
            return 0L;
        }
        
        // Create a thread pool with 4 threads
        ForkJoinPool pool = new ForkJoinPool(4);
        
        try {
            // Invoke the recursive task and get the result
            return pool.invoke(new SumTask(numbers, 0, numbers.size()));
        } finally {
            pool.shutdown();
        }
    }
    
    /**
     * RecursiveTask that splits the list into smaller chunks and sums them.
     */
    static class SumTask extends RecursiveTask<Long> {
        private static final int THRESHOLD = 100; // Threshold for sequential processing
        private final List<Integer> numbers;
        private final int start;
        private final int end;
        
        SumTask(List<Integer> numbers, int start, int end) {
            this.numbers = numbers;
            this.start = start;
            this.end = end;
        }
        
        @Override
        protected Long compute() {
            int length = end - start;
            
            // If the chunk is small enough, compute directly
            if (length <= THRESHOLD) {
                return computeSequentially();
            }
            
            // Otherwise, split the task
            int mid = start + length / 2;
            SumTask leftTask = new SumTask(numbers, start, mid);
            SumTask rightTask = new SumTask(numbers, mid, end);
            
            // Fork both tasks
            leftTask.fork();
            rightTask.fork();
            
            // Join and combine results
            long leftSum = leftTask.join();
            long rightSum = rightTask.join();
            
            return leftSum + rightSum;
        }
        
        private long computeSequentially() {
            long sum = 0;
            for (int i = start; i < end; i++) {
                sum += numbers.get(i);
            }
            return sum;
        }
    }
    
    // Example usage
    public static void main(String[] args) {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        long result = sumInParallel(numbers);
        System.out.println("Sum: " + result); // Output: 55
    }
}