Отчет о кастомной реализации реактивной библиотеки для Java

1. Архитектура системы

Реализованная система представляет собой упрощенную реактивную библиотеку, аналогичную RxJava, но с минимальным набором функциональности. Архитектура состоит из следующих ключевых компонентов:
1.1 Основные компоненты

Интерфейсы:

    ObservableOnSubscribe<T> - функциональный интерфейс для создания источника данных

    Observer<T> - интерфейс получателя данных с методами:

        onNext(T item) - получение нового элемента

        onError(Throwable t) - обработка ошибки

        onComplete() - сигнал о завершении

        dispose(), isDisposed() - управление подпиской

Класс Observable<T> - ядро системы, предоставляющее:

    Фабричные методы (create())

    Операторы преобразования (map(), filter(), flatMap())

    Методы управления потоками (subscribeOn(), observeOn())

Schedulers - система планирования выполнения задач:

    ComputationScheduler - для CPU-интенсивных задач

    IOThreadScheduler - для I/O операций

    SingleThreadScheduler - для последовательного выполнения

1.2 Принцип работы

Система работает по принципу "push"-модели:

    Создается источник данных (Observable)

    Применяются операторы преобразования

    Указываются планировщики для выполнения операций

    Подписывается Observer, что запускает весь поток данных

2. Принципы работы Schedulers
2.1 Реализации Schedulers

ComputationScheduler:

    Использует фиксированный пул потоков по количеству ядер CPU

    Оптимален для CPU-интенсивных вычислений

    Предотвращает создание избыточного количества потоков

```
public ComputationScheduler() {
    this(Runtime.getRuntime().availableProcessors());
}
```

IOThreadScheduler:

    Использует кешированный пул потоков (newCachedThreadPool)

    Автоматически создает новые потоки по мере необходимости

    Подходит для I/O операций с неопределенным временем выполнения

SingleThreadScheduler:

    Использует один выделенный поток

    Гарантирует последовательное выполнение операций

    Полезен для задач, требующих строгого порядка

2.2 Области применения

ComputationScheduler	

  Лучшие сценарии использования:	Математические вычисления, обработка данных
  
  Не рекомендуется для: Долгих I/O операций
  
IOThreadScheduler	

  Лучшие сценарии использования:	Сетевые запросы, работа с файлами	
  
  Не рекомендуется для: CPU-интенсивных задач
  
SingleThreadScheduler	

 Лучшие сценарии использования:	Последовательная обработка, UI-обновления	
 
 Не рекомендуется для: Параллельных задач
 
3. Процесс тестирования
3.1 Стратегия тестирования

Тестирование проводилось по следующим направлениям:

    Проверка базовой функциональности операторов

    Тестирование многопоточной работы

    Проверка управления подписками

    Тестирование комбинаций операторов

3.2 Основные тестовые сценарии

Тест 1: Проверка работы subscribeOn

```
@Test
void subscribeOn_computationScheduler_shouldRunInDifferentThread() {
    // Проверяем, что производство данных происходит в другом потоке
    assertNotEquals(mainThreadName, observableThreadName.get());
}
```

Тест 2: Проверка порядка элементов при observeOn
```
@Test
void observeOn_singleThreadScheduler_shouldPreserveOrder() {
    // Проверяем, что порядок элементов сохраняется
    assertEquals(counter.incrementAndGet(), item);
}
```

Тест 3: Комплексный тест с несколькими операторами
```
@Test
Observable.create(emitter -> {...})
    .subscribeOn(new IOThreadScheduler())
    .map(i -> transform(i))
    .filter(i -> checkCondition(i))
    .observeOn(new ComputationScheduler())
    .subscribe(...);
```

4. Примеры использования
4.1 Преобразование данных с изменением потока

```
Observable.create((Observer<String> emitter) -> {
        // Производство данных в IO потоке
        emitter.onNext(readFile("data.txt"));
        emitter.onComplete();
    })
    .subscribeOn(new IOThreadScheduler())
    .map(content -> parseJson(content)) // CPU-интенсивная операция
    .observeOn(new ComputationScheduler())
    .subscribe(result -> {
        // Получение результата в Computation потоке
        updateUI(result);
    });
```

4.2 Параллельная обработка элементов

```
Observable.create(emitter -> {
        for (String url : urls) {
            emitter.onNext(url);
        }
        emitter.onComplete();
    })
    .subscribeOn(new IOThreadScheduler())
    .flatMap(url -> 
        Observable.create(emitter -> {
            String data = downloadUrl(url);
            emitter.onNext(data);
            emitter.onComplete();
        })
        .subscribeOn(new IOThreadScheduler())
    )
    .observeOn(new SingleThreadScheduler())
    .subscribe(data -> {
        // Все данные приходят последовательно в одном потоке
        saveToDatabase(data);
    });
```

4.3 Обработка ошибок

```
Observable.create(emitter -> {
        try {
            emitter.onNext(riskyOperation());
            emitter.onComplete();
        } catch (Exception e) {
            emitter.onError(e);
        }
    })
    .subscribeOn(new ComputationScheduler())
    .subscribe(
        result -> handleSuccess(result),
        error -> logError(error) // Обработка ошибок
    );
```

Заключение

Реализованная библиотека предоставляет:

    Основные операторы реактивного программирования

    Гибкую систему управления потоками

    Простую и понятную архитектуру

    Возможности для расширения

Система особенно полезна для:

    Асинхронной обработки данных

    Параллельного выполнения задач

    Сложных цепочек преобразований данных

    Работы с источниками данных различной природы
