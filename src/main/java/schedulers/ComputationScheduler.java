package schedulers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ComputationScheduler implements Scheduler {
    private final ExecutorService executor;

    public ComputationScheduler() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public ComputationScheduler(int threads) {
        executor = Executors.newFixedThreadPool(threads);
    }

    @Override
    public void execute(Runnable task) {
        executor.execute(task);
    }
}
