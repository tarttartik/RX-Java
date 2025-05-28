import schedulers.Scheduler;

import java.util.function.Function;
import java.util.function.Predicate;

public class Observable<T> {
    private final ObservableOnSubscribe<T> source;

    private Observable(ObservableOnSubscribe<T> source) {
        this.source = source;
    }

    public static <T> Observable<T> create(ObservableOnSubscribe<T> source) {
        return new Observable<>(source);
    }

    public void subscribe(Observer<? super T> observer) {
        source.subscribe(new ObserverWrapper<>(observer));
    }

    public <R> Observable<R> map(Function<? super T, ? extends R> mapper) {
        return new Observable<>(downstream ->
                subscribe(new Observer<T>() {
                    private boolean disposed = false;
                    @Override
                    public void onNext(T item) {
                        try {
                            downstream.onNext(mapper.apply(item));
                        } catch (Throwable t) {
                            onError(t);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        downstream.onError(t);
                    }

                    @Override
                    public void onComplete() {
                        downstream.onComplete();
                    }

                    @Override
                    public void dispose() {
                        disposed = true;
                    }

                    @Override
                    public boolean isDisposed() {
                        return disposed;
                    }
                }));
    }

    public Observable<T> filter(Predicate<? super T> predicate) {
        return new Observable<>(downstream ->
                subscribe(new Observer<T>() {
                    private boolean disposed = false;

                    @Override
                    public void onNext(T item) {
                        try {
                            if (predicate.test(item)) {
                                downstream.onNext(item);
                            }
                        } catch (Throwable t) {
                            onError(t);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        downstream.onError(t);
                    }

                    @Override
                    public void onComplete() {
                        downstream.onComplete();
                    }

                    @Override
                    public void dispose() {
                        disposed = true;
                    }

                    @Override
                    public boolean isDisposed() {
                        return disposed;
                    }
                }));
    }

    public <R> Observable<R> flatMap(Function<? super T, ? extends Observable<? extends R>> mapper) {
        return new Observable<>(downstream ->
                subscribe(new Observer<T>() {
                    private boolean disposed = false;

                    @Override
                    public void onNext(T item) {
                        try {
                            Observable<? extends R> observable = mapper.apply(item);
                            observable.subscribe(new Observer<R>() {
                                boolean disposed = false;
                                @Override
                                public void onNext(R item) {
                                    downstream.onNext(item);
                                }

                                @Override
                                public void onError(Throwable t) {
                                    downstream.onError(t);
                                }

                                @Override
                                public void onComplete() {
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
                        } catch (Throwable t) {
                            onError(t);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        if (!disposed) {
                            downstream.onError(t);
                        }
                    }

                    @Override
                    public void onComplete() {
                        if (!disposed) {
                            downstream.onComplete();
                        }
                    }

                    @Override
                    public void dispose() {
                        disposed = true;
                    }

                    @Override
                    public boolean isDisposed() {
                        return disposed;
                    }
                }));
    }

    public Observable<T> subscribeOn(Scheduler scheduler) {
        return new Observable<>(downstream ->
                scheduler.execute(() -> source.subscribe(new ObserverWrapper<>(downstream))));
    }

    public Observable<T> observeOn(Scheduler scheduler) {
        return new Observable<>(downstream ->
                subscribe(new Observer<T>() {
                    boolean disposed = false;

                    @Override
                    public void onNext(T item) {
                        scheduler.execute(() -> downstream.onNext(item));
                    }

                    @Override
                    public void onError(Throwable t) {
                        scheduler.execute(() -> downstream.onError(t));
                    }

                    @Override
                    public void onComplete() {
                        scheduler.execute(downstream::onComplete);
                    }

                    @Override
                    public void dispose() {
                        disposed = true;
                    }

                    @Override
                    public boolean isDisposed() {
                        return disposed;
                    }
                }));
    }

    private static class ObserverWrapper<T> implements Observer<T>, Disposable {
        private final Observer<? super T> downstream;
        private volatile boolean disposed = false;

        ObserverWrapper(Observer<? super T> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void onNext(T item) {
            if (!disposed) {
                downstream.onNext(item);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (!disposed) {
                downstream.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (!disposed) {
                downstream.onComplete();
            }
        }

        @Override
        public void dispose() {
            disposed = true;
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }

    }
}

