package com.github.wormhole.serialize;

import java.util.concurrent.*;

public class TaskExecutor {
    private static TaskExecutor taskExecutor =  new TaskExecutor();
    private ExecutorService executorService;

    private BlockingQueue<Task> queue;

    public TaskExecutor() {
        this.executorService = Executors.newSingleThreadExecutor();
        this.executorService.submit(() -> {
            try {
                for (;;) {
                    Task task = this.queue.poll(100, TimeUnit.MICROSECONDS);
                    if (task != null) {
                        try {
                            task.run();
                        } catch (Exception e) {

                        }
                    }
                }
            } catch (Exception e) {

            }
        });
        this.queue = new LinkedBlockingQueue<>();
    }

    public void addTask(Task task) {
        this.queue.add(task);
    }

    public static TaskExecutor get() {
        return taskExecutor;
    }
}
