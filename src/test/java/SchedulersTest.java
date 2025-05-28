import org.junit.jupiter.api.Test;
import schedulers.ComputationScheduler;
import schedulers.IOThreadScheduler;
import schedulers.SingleThreadScheduler;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class SchedulersTest {

    @Test
    void testSubscribeOn() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<String> threadNames = new ArrayList<>();

        Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
                    threadNames.add(Thread.currentThread().getName());
                    emitter.onNext(1);
                    emitter.onComplete();
                })
                .subscribeOn(new IOThreadScheduler())
                .subscribe(new Observer<Integer>() {
                    private boolean disposed = false;

                    @Override
                    public void onNext(Integer item) {
                        threadNames.add(Thread.currentThread().getName());
                    }

                    @Override
                    public void onError(Throwable t) {
                        latch.countDown();
                    }

                    @Override
                    public void onComplete() {
                        latch.countDown();
                    }

                    @Override
                    public void dispose() {
                        disposed = true;
                    }

                    @Override
                    public boolean isDisposed() {
                        return disposed;
                    }
                });

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(2, threadNames.size());
        assertNotEquals(Thread.currentThread().getName(), threadNames.get(0));
        assertEquals(threadNames.get(0), threadNames.get(1));
    }

    @Test
    void testObserveOn() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<String> threadNames = new ArrayList<>();

        Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
                    threadNames.add(Thread.currentThread().getName());
                    emitter.onNext(1);
                    emitter.onComplete();
                })
                .observeOn(new ComputationScheduler())
                .subscribe(new Observer<Integer>() {
                    private boolean disposed = false;

                    @Override
                    public void onNext(Integer item) {
                        threadNames.add(Thread.currentThread().getName());
                    }

                    @Override
                    public void onError(Throwable t) {
                        latch.countDown();
                    }

                    @Override
                    public void onComplete() {
                        latch.countDown();
                    }

                    @Override
                    public void dispose() {
                        disposed = true;
                    }

                    @Override
                    public boolean isDisposed() {
                        return disposed;
                    }
                });

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(2, threadNames.size());
        assertEquals(Thread.currentThread().getName(), threadNames.get(0));
        assertNotEquals(threadNames.get(0), threadNames.get(1));
    }

    @Test
    void testMultipleThreadsWithSchedulers() throws InterruptedException {
        int testItems = 100;
        CountDownLatch allItemsProcessed = new CountDownLatch(testItems);
        CountDownLatch completionSignal = new CountDownLatch(1);

        ConcurrentLinkedQueue<String> producerThreads = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<String> consumerThreads = new ConcurrentLinkedQueue<>();
        AtomicInteger receivedCount = new AtomicInteger(0);

        Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
                    try {
                        for (int i = 0; i < testItems; i++) {
                            producerThreads.add(Thread.currentThread().getName());
                            emitter.onNext(i);
                        }
                        emitter.onComplete();
                    } catch (Exception e) {
                        emitter.onError(e);
                    }
                })
                .subscribeOn(new IOThreadScheduler())
                .observeOn(new ComputationScheduler())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer item) {
                        consumerThreads.add(Thread.currentThread().getName());
                        receivedCount.incrementAndGet();
                        allItemsProcessed.countDown();
                    }

                    @Override
                    public void onError(Throwable t) {
                        completionSignal.countDown();
                    }

                    @Override
                    public void onComplete() {
                        completionSignal.countDown();
                    }

                    @Override public void dispose() {}
                    @Override public boolean isDisposed() { return false; }
                });

        boolean completed = allItemsProcessed.await(5, TimeUnit.SECONDS);
        if (!completed) {
            completed = completionSignal.await(1, TimeUnit.SECONDS);
        }

        assertTrue(completed, "Processing didn't complete in time");
        assertEquals(testItems, receivedCount.get(), "Not all items were received");
        assertEquals(testItems, producerThreads.size(), "Producer threads count mismatch");
        assertEquals(testItems, consumerThreads.size(), "Consumer threads count mismatch");


        assertNotEquals(producerThreads.peek(), consumerThreads.peek(),
                "Producer and consumer should use different threads");

        Set<String> uniqueConsumerThreads = new HashSet<>(consumerThreads);
        assertTrue(uniqueConsumerThreads.size() >= 1 &&
                        uniqueConsumerThreads.size() <= Runtime.getRuntime().availableProcessors(),
                "Consumer threads count should be between 1 and available processors");


        Set<String> uniqueProducerThreads = new HashSet<>(producerThreads);
        assertTrue(uniqueProducerThreads.size() >= 1,
                "Producer should use at least one thread");
    }

    @Test
    void testDifferentSchedulerTypes() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<String> threadNames = new ArrayList<>();

        Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
                    threadNames.add("producer: " + Thread.currentThread().getName());
                    emitter.onNext(1);
                    emitter.onComplete();
                })
                .subscribeOn(new SingleThreadScheduler())
                .observeOn(new ComputationScheduler())
                .map(i -> {
                    threadNames.add("map: " + Thread.currentThread().getName());
                    return i;
                })
                .observeOn(new IOThreadScheduler())
                .subscribe(new Observer<Integer>() {
                    private boolean disposed = false;

                    @Override
                    public void onNext(Integer item) {
                        threadNames.add("consumer: " + Thread.currentThread().getName());
                    }

                    @Override
                    public void onError(Throwable t) {
                        latch.countDown();
                    }

                    @Override
                    public void onComplete() {
                        latch.countDown();
                    }

                    @Override
                    public void dispose() {
                        disposed = true;
                    }

                    @Override
                    public boolean isDisposed() {
                        return disposed;
                    }
                });

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(3, threadNames.size());


        assertTrue(threadNames.get(0).startsWith("producer: pool-"));
        assertTrue(threadNames.get(1).startsWith("map: pool-"));
        assertTrue(threadNames.get(2).startsWith("consumer: pool-"));

        assertNotEquals(threadNames.get(0), threadNames.get(1));
        assertNotEquals(threadNames.get(1), threadNames.get(2));
        assertNotEquals(threadNames.get(0), threadNames.get(2));
    }
}
