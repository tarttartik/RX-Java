public interface Observer<T> extends Disposable {
    void onNext(T item);
    void onError(Throwable t);
    void onComplete();
}
