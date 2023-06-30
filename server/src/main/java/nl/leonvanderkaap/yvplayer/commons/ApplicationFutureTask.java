package nl.leonvanderkaap.yvplayer.commons;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class ApplicationFutureTask<T> extends FutureTask<T> {
    public ApplicationFutureTask(Callable<T> callable) {
        super(callable);
    }

    @Override
    protected void done() {
        try {
            this.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
