package net.flibusta.concurrent.impl;

import net.flibusta.concurrent.LockManager;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class SemaphoreLockManager implements LockManager {
    Logger logger = org.apache.log4j.Logger.getLogger(SemaphoreLockManager.class);

    private Map<String, CountDownLatch> locks = new HashMap<String, CountDownLatch>();


    @Override
    public void lock(String id) throws InterruptedException {
        CountDownLatch monitor;
        synchronized (this) {
            monitor = locks.get(id);
            if (monitor == null) {
                locks.put(id, new CountDownLatch(1));
                return;
            }
        }
        logger.debug("Waiting lock " + id);
        monitor.await();
        logger.debug("Got lock " + id);
        lock(id);
    }

    @Override
    public void unlock(String id) {
        synchronized (this) {
            CountDownLatch monitor = locks.remove(id);
            if (monitor != null) {
                monitor.countDown();
            }
        }
    }

}
