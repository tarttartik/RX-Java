@FunctionalInterface
public interface ObservableOnSubscribe<T> {
    void subscribe(Observer<? super T> observer);
}
