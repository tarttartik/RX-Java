import org.junit.jupiter.api.Test;
import schedulers.IOThreadScheduler;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class EdgeCasesTest {

    @Test
    void testEmptyObservable() {
        AtomicInteger receivedCount = new AtomicInteger(0);
        AtomicBoolean completed = new AtomicBoolean(false);

        Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
            emitter.onComplete();
        }).subscribe(new Observer<Integer>() {
            private boolean disposed = false;

            @Override
            public void onNext(Integer item) {
                receivedCount.incrementAndGet();
            }

            @Override
            public void onError(Throwable t) {
                fail("Unexpected error");
            }

            @Override
            public void onComplete() {
                completed.set(true);
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

        assertEquals(0, receivedCount.get());
        assertTrue(completed.get());
    }

    @Test
    void testErrorBeforeItems() {
        AtomicInteger receivedCount = new AtomicInteger(0);
        RuntimeException testError = new RuntimeException("Test error");
        AtomicBoolean errorReceived = new AtomicBoolean(false);

        Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
            emitter.onError(testError);
        }).subscribe(new Observer<Integer>() {
            private boolean disposed = false;

            @Override
            public void onNext(Integer item) {
                receivedCount.incrementAndGet();
            }

            @Override
            public void onError(Throwable t) {
                assertEquals(testError, t);
                errorReceived.set(true);
            }

            @Override
            public void onComplete() {
                fail("Should not complete");
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

        assertEquals(0, receivedCount.get());
        assertTrue(errorReceived.get());
    }

}