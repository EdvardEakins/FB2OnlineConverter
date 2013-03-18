package net.flibusta.concurrent.impl;

import net.flibusta.concurrent.LockManager;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

public class HashLockManager implements LockManager {
    Logger logger = org.apache.log4j.Logger.getLogger(HashLockManager.class);

    private final Set<String> locks = new HashSet<String>();
    @Override
    public void lock(String id) throws InterruptedException {
        synchronized (locks) {
            while (locks.contains(id)) {
                logger.debug("Waiting lock " + id);
                locks.wait();
                logger.debug("Got lock " + id);
            }
            locks.add(id);
        }
    }

    @Override
    public void unlock(String id) {
        synchronized (locks) {
            locks.remove(id);
            locks.notify();
        }
    }
}
