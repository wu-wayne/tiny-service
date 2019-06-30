package net.tiny.service;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ThreadPoolExeutorWrapper implements ExecutorService {

    protected int size = 5;
    protected int max = 10;
    //要求线程数大于核心数，在退出之前等待空闲最长时间为 3秒
    protected int timeout = 3;

    private ThreadPoolExecutor delgate;

    public ThreadPoolExecutor getDelgate() {
        if(delgate == null) {
            delgate = new ThreadPoolExecutor(size, max, timeout,
                    TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(size),
                    new ThreadPoolExecutor.AbortPolicy());
            delgate.allowCoreThreadTimeOut(true);
        }
        return delgate;
    }

    @Override
    public String toString() {
        return String.format(String.format("%s#%d {size:%d, max:%d, timeout:%ds}",
        		getClass().getSimpleName(), hashCode(), size, max, timeout));
    }
    @Override
    public void execute(Runnable command) {
        getDelgate().execute(command);
    }

    @Override
    public void shutdown() {
        getDelgate().shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return getDelgate().shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return getDelgate().isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return getDelgate().isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return getDelgate().awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return getDelgate().submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return getDelgate().submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return getDelgate().submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return getDelgate().invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return getDelgate().invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return getDelgate().invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return getDelgate().invokeAny(tasks, timeout, unit);
    }
}
