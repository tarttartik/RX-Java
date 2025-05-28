import org.junit.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class BasicComponentsTest {

    @Test
    public void testObservableCreateAndSubscribe() {
        List<Integer> receivedItems = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);

        Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onNext(3);
            emitter.onComplete();
        }).subscribe(new Observer<Integer>() {
            private boolean disposed = false;

            @Override
            public void onNext(Integer item) {
                receivedItems.add(item);
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

        assertEquals(List.of(1, 2, 3), receivedItems);
        assertTrue(completed.get());
    }

    @Test
    public void testErrorHandling() {
        AtomicBoolean errorReceived = new AtomicBoolean(false);
        RuntimeException testError = new RuntimeException("Test error");

        Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
            emitter.onNext(1);
            emitter.onError(testError);
        }).subscribe(new Observer<Integer>() {
            private boolean disposed = false;
            @Override
            public void onNext(Integer item) {
                // Do nothing
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

        assertTrue(errorReceived.get());
    }

    @Test
    public void testDisposable() {
        AtomicInteger receivedCount = new AtomicInteger(0);
        Observer<Integer> observer = new Observer<Integer>() {
            private boolean disposed = false;

            @Override
            public void onNext(Integer item) {
                receivedCount.incrementAndGet();
                if (item == 5) {
                    dispose();
                }
            }

            @Override
            public void onError(Throwable t) {
                fail("Unexpected error");
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
        };

        Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
            for (int i = 0; i < 100; i++) {
                if (observer.isDisposed()) {
                    return;
                }
                emitter.onNext(i);
            }
        }).subscribe(observer);

        assertTrue(observer.isDisposed());
        assertEquals(6, receivedCount.get()); // 0..5 inclusive
    }
}
