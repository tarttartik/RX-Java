import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

class OperatorsTest {

    @Test
    void testMapOperator() {
        List<String> receivedItems = new ArrayList<>();

        Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
                    emitter.onNext(1);
                    emitter.onNext(2);
                    emitter.onNext(3);
                    emitter.onComplete();
                })
                .map(Object::toString)
                .subscribe(new Observer<String>() {
                    private boolean disposed = false;

                    @Override
                    public void onNext(String item) {
                        receivedItems.add(item);
                    }

                    @Override
                    public void onError(Throwable t) {
                        fail("Unexpected error");
                    }

                    @Override
                    public void onComplete() {
                        // Do nothing
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

        assertEquals(List.of("1", "2", "3"), receivedItems);
    }

    @Test
    void testFilterOperator() {
        List<Integer> receivedItems = new ArrayList<>();

        Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
                    for (int i = 0; i < 10; i++) {
                        emitter.onNext(i);
                    }
                    emitter.onComplete();
                })
                .filter(i -> i % 2 == 0)
                .subscribe(new Observer<Integer>() {
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
                        // Do nothing
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

        assertEquals(List.of(0, 2, 4, 6, 8), receivedItems);
    }

    @Test
    void testFlatMapOperator() {
        List<Integer> receivedItems = new ArrayList<>();

        Observable.<Integer>create(emitter -> {
                    emitter.onNext(1);
                    emitter.onNext(2);
                    emitter.onNext(3);
                    emitter.onComplete();
                })
                .flatMap((Function<Integer, Observable<Integer>>) i ->
                        Observable.create(emitter -> {
                            emitter.onNext(i * 10);
                            emitter.onNext(i * 100);
                            emitter.onComplete();
                        })
                )
                .subscribe(new Observer<Integer>() {
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
                        // do nothing
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

        assertEquals(List.of(10, 100, 20, 200, 30, 300), receivedItems);
    }

    @Test
    void testMapWithError() {
        RuntimeException testError = new RuntimeException("Map error");
        AtomicBoolean errorReceived = new AtomicBoolean(false);

        Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
                    emitter.onNext(1);
                    emitter.onNext(2);
                })
                .map(i -> {
                    if (i == 2) throw testError;
                    return i;
                })
                .subscribe(new Observer<Integer>() {
                    private boolean disposed = false;

                    @Override
                    public void onNext(Integer item) {
                        assertEquals(1, item);
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
}