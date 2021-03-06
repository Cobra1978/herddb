/*
 Licensed to Diennea S.r.l. under one
 or more contributor license agreements. See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership. Diennea S.r.l. licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.

 */

package herddb.index.brin;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import herddb.core.RandomPageReplacementPolicy;
import herddb.index.brin.BlockRangeIndex.Block;
import herddb.utils.SizeAwareObject;
import herddb.utils.Sized;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/**
 * Unit tests for PagedBlockRangeIndex
 *
 * @author enrico.olivelli
 */
public class BlockRangeIndexConcurrentTest {

    @Test
    public void testConcurrentWrites() throws Exception {

        int testSize = 10000;
        int parallelism = 10;
        BlockRangeIndex<Sized<Integer>, Sized<String>> index =
                new BlockRangeIndex<>(1024, new RandomPageReplacementPolicy(3));
        index.boot(BlockRangeIndexMetadata.empty());

        try {

            ExecutorService threadpool = Executors.newFixedThreadPool(parallelism);
            CountDownLatch l = new CountDownLatch(testSize);

            try {
                for (int i = 0; i < testSize; i++) {
                    int _i = i;
                    threadpool.submit(() -> {
                        try {
                            index.put(Sized.valueOf(_i), Sized.valueOf("a" + _i));
                            l.countDown();
                        } catch (RuntimeException e) {
                            e.printStackTrace();
                        }
                    });
                }
            } finally {
                threadpool.shutdown();
            }

            assertTrue(l.await(10, TimeUnit.SECONDS));

            index.checkpoint();

            dumpIndex(index);
            verifyIndex(index);

            List<Sized<String>> result = index.search(Sized.valueOf(0), Sized.valueOf(testSize + 1));
            for (Sized<String> res : result) {
                System.out.println("res " + res.dummy);
            }

            for (int i = 0; i < testSize; i++) {
                assertTrue("cannot find " + i, index.containsKey(Sized.valueOf(i)));
            }

        } catch (Exception | AssertionError e) {

            deepDumpIndex(index);
            throw e;
        }
    }

    @Test
    public void testConcurrentReadsWritesWithSplits() throws Exception {

        int testSize = 10000;
        int parallelism = 10;
        BlockRangeIndex<Sized<Integer>, Sized<String>> index = new BlockRangeIndex<>(1024,
                new RandomPageReplacementPolicy(3));
        index.boot(BlockRangeIndexMetadata.empty());
        ExecutorService threadpool = Executors.newFixedThreadPool(parallelism);

        try {

            ConcurrentLinkedQueue<Sized<String>> results = new ConcurrentLinkedQueue<>();
            List<Future<?>> jobs = new ArrayList<>(testSize);

            try {
                for (int i = 0; i < testSize; i++) {
                    int _i = i;
                    jobs.add(threadpool.submit(() -> {
                        index.put(Sized.valueOf(_i), Sized.valueOf("a" + _i));
                        List<Sized<String>> search = index.search(Sized.valueOf(_i));
                        results.addAll(search);
                        if (search.isEmpty()) {
                            search = index.search(Sized.valueOf(_i));
                            throw new IllegalStateException("Empty Search! i " + _i);
                        }
                    }));
                }
            } finally {
                threadpool.shutdown();
            }

            for (Future<?> job : jobs) {
                /* Job exception rethrow */
                try {
                    job.get(10, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    throw (Exception) e.getCause();
                }
            }

            index.checkpoint();

            dumpIndex(index);
            verifyIndex(index);
            List<Sized<String>> result = index.search(Sized.valueOf(0), Sized.valueOf(testSize + 1));
            for (Sized<String> res : result) {
                System.out.println("res " + res.dummy);
            }

            for (int i = 0; i < testSize; i++) {
                assertTrue("cannot find " + i, index.containsKey(Sized.valueOf(i)));
                assertTrue("cannot find a" + i, results.contains(Sized.valueOf("a" + i)));
            }

        } catch (Exception | AssertionError e) {

            deepDumpIndex(index);
            throw e;
        }
    }

    @Test
    public void testConcurrentReadsWritesDeletesWithSplits() throws Exception {

        int testSize = 10000;
        int parallelism = 10;
        BlockRangeIndex<Sized<Integer>, Sized<String>> index =
                new BlockRangeIndex<>(1024, new RandomPageReplacementPolicy(3));
        index.boot(BlockRangeIndexMetadata.empty());

        try {

            ExecutorService threadpool = Executors.newFixedThreadPool(parallelism);
            CountDownLatch l = new CountDownLatch(testSize);
            ConcurrentLinkedQueue<Sized<String>> afterInsertSearch = new ConcurrentLinkedQueue<>();
            ConcurrentLinkedQueue<Sized<String>> afterDeleteSearch = new ConcurrentLinkedQueue<>();
            List<Future<?>> jobs = new ArrayList<>(testSize);

            try {
                for (int i = 0; i < testSize; i++) {
                    int _i = i;
                    jobs.add(threadpool.submit(() -> {
                        try {
                            index.put(Sized.valueOf(_i), Sized.valueOf("a" + _i));
                            List<Sized<String>> search = index.search(Sized.valueOf(_i));
                            afterInsertSearch.addAll(search);
                            if (search.isEmpty()) {
                                throw new IllegalStateException("Empty Search! i " + _i);
                            }

                            index.delete(Sized.valueOf(_i), Sized.valueOf("a" + _i));
                            List<Sized<String>> search2 = index.search(Sized.valueOf(_i));
                            afterDeleteSearch.addAll(search2);
                            l.countDown();
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }));
                }
            } finally {
                threadpool.shutdown();
            }

            for (Future<?> job : jobs) {
                /* Job exception rethrow */
                try {
                    job.get(10, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    throw (Exception) e.getCause();
                }
            }

            index.checkpoint();
            dumpIndex(index);

            verifyIndex(index);
            List<Sized<String>> lookupAfterDeletion = index.search(Sized.valueOf(0), Sized.valueOf(testSize + 1));
            assertTrue(lookupAfterDeletion.isEmpty());

            for (int i = 0; i < testSize; i++) {
                assertTrue("cannot find a" + i, afterInsertSearch.contains(Sized.valueOf("a" + i)));
            }
            assertTrue(afterDeleteSearch.isEmpty());

        } catch (Exception | AssertionError e) {

            deepDumpIndex(index);
            throw e;
        }


    }


    private <X extends Comparable<X> & SizeAwareObject> void dumpIndex(BlockRangeIndex<X, ?> index) {
        for (Block<X, ?> b : index.getBlocks().values()) {
            System.out.println("BLOCK " + b);
        }
    }

    private <X extends Comparable<X> & SizeAwareObject> void deepDumpIndex(BlockRangeIndex<X, ?> index) {
        for (Block<X, ?> block : index.getBlocks().values()) {
            System.out.println("------------------------------------------");
            System.out.println("BLOCK " + block);
            System.out.println("------------------------------------------");

            /* Forcefully load the block to check internal data */
            block.ensureBlockLoaded();

            block.values.forEach((k, v) -> System.out.println(k + ": " + v));
        }
    }

    private void verifyIndex(BlockRangeIndex<Sized<Integer>, Sized<String>> index) {
        Integer lastmax = null;
        for (Block<Sized<Integer>, Sized<String>> b : index.getBlocks().values()) {
            if (b.key == BlockRangeIndex.BlockStartKey.HEAD_KEY) {
                System.out.println("check block " + lastmax + " -> -inf");
            } else {
                System.out.println("check block " + lastmax + " -> " + b.key.minKey.dummy);
            }

            /* Forcefully load the block to check internal data */
            b.ensureBlockLoaded();

            if (b.values.isEmpty() && index.getBlocks().size() != 1 && b.key != BlockRangeIndex.BlockStartKey.HEAD_KEY) {
                fail("non head of degenerate tree empty block");
            }

            if (b.values.isEmpty()) {
                if (index.getBlocks().size() != 1 && b.key != BlockRangeIndex.BlockStartKey.HEAD_KEY) {
                    fail("non head of degenerate tree empty block");
                }
            } else {
                if (lastmax == null) {
                    lastmax = b.values.lastKey().dummy;
                } else {
                    Integer entryMin = b.values.firstKey().dummy;
                    Integer entryMax = b.values.lastKey().dummy;
                    if (entryMin < lastmax) {
                        fail(entryMin + " < " + lastmax);
                    }
                    lastmax = entryMax;
                }
            }
        }
    }

}
