package net.flibusta.concurrent;

public interface LockManager {

    void lock(String id) throws InterruptedException;

    void unlock(String id);

}
