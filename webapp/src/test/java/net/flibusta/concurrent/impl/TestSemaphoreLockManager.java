package net.flibusta.concurrent.impl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.*;

public class TestSemaphoreLockManager {
    ExecutorService executorService;

    @Before
    public void setUp() throws Exception {
        executorService = Executors.newCachedThreadPool();

    }

    @After
    public void tearDown() throws Exception {
        executorService.shutdown();
    }

    @Test
    public void testSimple() throws InterruptedException {
        final SemaphoreLockManager lockManager = new SemaphoreLockManager();
        lockManager.lock("1");
        lockManager.unlock("1");
        lockManager.unlock("1");
        lockManager.unlock("2");
    }


    @Test
    public void testLocked() throws InterruptedException {

        final SemaphoreLockManager lockManager = new SemaphoreLockManager();

        lockManager.lock("1");


        Future<?> future = executorService.submit(new Locker(lockManager));

        try {
            Object o = future.get(100, TimeUnit.MILLISECONDS);
            Assert.fail("Object expected to be locked by another thread");
        } catch (ExecutionException e) {
            Assert.fail();
        } catch (TimeoutException e) {
            //ok
        }

        // try unlock
        future = executorService.submit(new Getter(future));
        lockManager.unlock("1");

        try {
            future.get(1, TimeUnit.SECONDS);
            Assert.assertTrue("Normal execution expected", future.isDone());
            Assert.assertFalse("Normal execution expected", future.isCancelled());
        } catch (ExecutionException e) {
            e.printStackTrace();
            Assert.fail();
        } catch (TimeoutException e) {
            Assert.fail("Object not expected to be locked by another thread");
        }

    }

    private static class Locker implements Runnable {
        private final SemaphoreLockManager lockManager;

        public Locker(SemaphoreLockManager lockManager) {
            this.lockManager = lockManager;
        }

        @Override
        public void run() {
            try {
                lockManager.lock("1");
            } catch (InterruptedException e) {
                throw new RuntimeException();
            }
        }
    }

    private static class Getter implements Callable<Future> {
        Future future;

        private Getter(Future future) {
            this.future = future;
        }

        @Override
        public Future call() throws Exception {
            future.get(2000, TimeUnit.MILLISECONDS);
            return future;
        }
    }

}
