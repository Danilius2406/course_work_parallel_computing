package org.example;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executor;

public class ThreadPool {
    private final Queue<Runnable> taskQueue;
    private final WorkerThread[] workerThreads;
    private volatile boolean isStopped = false;

    public ThreadPool(int numThreads) {
        taskQueue = new LinkedList<>();
        workerThreads = new WorkerThread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            workerThreads[i] = new WorkerThread(taskQueue);
            workerThreads[i].start();
        }
    }

    public synchronized void submitTask(Runnable task) {
        if (isStopped) {
            throw new IllegalStateException("Thread pool is stopped.");
        }
        synchronized (taskQueue) {
            taskQueue.add(task);
            taskQueue.notify();
        }
    }

    public synchronized void shutdown() {
        isStopped = true;
        for (WorkerThread worker : workerThreads) {
            worker.stopWorker();
        }
    }

    public void execute(Runnable task) {
        submitTask(task);
    }

    public Executor asExecutor() {
        return this::execute;
    }

    private static class WorkerThread extends Thread {
        private final Queue<Runnable> taskQueue;
        private volatile boolean isStopped = false;

        public WorkerThread(Queue<Runnable> taskQueue) {
            this.taskQueue = taskQueue;
        }

        @Override
        public void run() {
            while (!isStopped) {
                Runnable task;
                synchronized (taskQueue) {
                    while (taskQueue.isEmpty() && !isStopped) {
                        try {
                            taskQueue.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    if (isStopped) {
                        break;
                    }
                    task = taskQueue.poll();
                }

                if (task != null) {
                    try {
                        task.run();
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void stopWorker() {
            isStopped = true;
            this.interrupt();
        }
    }
}