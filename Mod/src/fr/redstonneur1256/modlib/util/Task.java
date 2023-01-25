package fr.redstonneur1256.modlib.util;

import arc.func.Cons;
import arc.func.Func;
import arc.func.Prov;
import arc.struct.Seq;
import arc.util.Threads;

public class Task<T> {

    protected final Object lock;
    protected boolean have;
    protected Seq<Cons<T>> listeners;
    protected Seq<Cons<Throwable>> failListeners;
    protected T item;
    protected Throwable exception;

    public Task() {
        lock = new Object();
        listeners = new Seq<>();
        failListeners = new Seq<>();
    }

    private <O> Task(Task<O> oldTask, Func<O, T> mapper) {
        this();
        if(oldTask.isComplete()) {
            if(oldTask.exception != null) {
                fail(oldTask.exception);
            } else {
                complete(mapper.get(oldTask.item));
            }
        } else {
            oldTask.onComplete(item -> complete(mapper.get(item)));
            oldTask.onFail(this::fail);
        }
    }

    public static <T> Task<T> completed(T value) {
        Task<T> task = new Task<>();
        task.complete(value);
        return task;
    }

    public static <T> Task<T> failed(Throwable exception) {
        Task<T> task = new Task<>();
        task.fail(exception);
        return task;
    }

    public boolean isCompleted() {
        synchronized(lock) {
            return have;
        }
    }

    public boolean isFailed() {
        synchronized(lock) {
            return have && exception != null;
        }
    }

    public void complete(T item) {
        synchronized(lock) {
            if(have) {
                throw new IllegalStateException("The Task have already been completed");
            }
            this.item = item;
            completed();
        }
    }

    public void fail(Throwable exception) {
        synchronized(lock) {
            if(have) {
                throw new IllegalStateException("The Task have already been completed");
            }
            this.exception = exception;
            this.completed();
        }
    }

    public <U> Task<U> map(Func<T, U> function) {
        return new Task<>(this, function);
    }

    public <U> Task<U> flatMap(Func<T, Task<U>> function) {
        // Horrible but it works, so it's going to stay like that for now
        Task<U> task = new Task<>();
        onComplete(result -> {
            Task<U> other = function.get(result);
            other.onComplete(task::complete);
            other.onFail(task::fail);
        });
        onFail(task::fail);
        return task;
    }

    public boolean isComplete() {
        synchronized(lock) {
            return have;
        }
    }

    public void waitComplete() {
        waitComplete(0);
    }

    public void waitComplete(long timeout) {
        synchronized(lock) {
            if(!have) {
                try {
                    lock.wait(timeout);
                } catch(InterruptedException ignored) {
                }
            }
        }
    }

    public T get() {
        return get(0);
    }

    public T get(long timeout) {
        synchronized(lock) {
            if(!have) {
                try {
                    lock.wait(timeout);
                } catch(InterruptedException ignored) {
                }
            }
            if(!have) {
                return null;
            }

            if(exception != null) {
                throw new RuntimeException(exception);
            }
            return item;
        }
    }

    public T getNow(T orElse) {
        return getNow(() -> orElse);
    }

    public T getNow(Prov<T> orElse) {
        synchronized(lock) {
            if(exception != null) {
                throw new RuntimeException(exception);
            }
            return have ? item : orElse.get();
        }
    }

    public Throwable getException() {
        return getException(0);
    }

    public Throwable getException(long timeout) {
        synchronized(lock) {
            if(!have) {
                try {
                    lock.wait(timeout);
                } catch(InterruptedException ignored) {
                }
            }
            return exception;
        }
    }

    /**
     * Add a listener to the Task
     * Note that if the task has already been completed the listener will be called directly
     */
    public void onComplete(Cons<T> action) {
        synchronized(lock) {
            if(have) {
                if(exception == null) {
                    action.get(item);
                }
            } else {
                listeners.add(action);
            }
        }
    }

    public void onFail(Cons<Throwable> action) {
        synchronized(lock) {
            if(have) {
                if(exception != null) {
                    action.get(exception);
                }
            } else {
                failListeners.add(action);
            }
        }
    }

    public void onCompleteAsync(Cons<T> action) {
        onComplete(item -> Threads.daemon(() -> action.get(item)));
    }

    private void completed() {
        have = true;
        lock.notifyAll();

        if(exception == null) {
            for(Cons<T> listener : listeners) {
                listener.get(item);
            }
        } else {
            for(Cons<Throwable> failListener : failListeners) {
                failListener.get(exception);
            }
        }

        listeners = null;
        failListeners = null;
    }

}