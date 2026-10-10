package a.slelin.work.voting.client.ui;

import javax.swing.SwingWorker;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Выполняет запрос к серверу в фоне и возвращает результат в поток Swing (EDT).
 */
public final class Async {

    private Async() {
    }

    public static <T> void run(Callable<T> task, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (ExecutionException e) {
                    onError.accept(e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }
}
