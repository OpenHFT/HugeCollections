/*
 * Copyright 2014 Higher Frequency Trading
 * <p/>
 * http://www.higherfrequencytrading.com
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.sandbox.map.shared;

import net.openhft.chronicle.sandbox.queue.shared.SharedJSR166TestCase;
import net.openhft.collections.TimeProvider;
import net.openhft.collections.VanillaSharedReplicatedHashMap;
import net.openhft.collections.VanillaSharedReplicatedHashMapBuilder;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * @author Rob Austin.
 */
public class TimeBasedReplicationTests extends SharedJSR166TestCase {


    private static File getPersistenceFile() {
        String TMP = System.getProperty("java.io.tmpdir");
        File file = new File(TMP + "/shm-test" + System.nanoTime());
        file.delete();
        file.deleteOnExit();
        return file;
    }


    @Test
    public void testIgnoreALatePut() throws IOException {

        final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
        final VanillaSharedReplicatedHashMap map = new VanillaSharedReplicatedHashMapBuilder()
                .entries(10)
                .timeProvider(timeProvider).create(getPersistenceFile(), CharSequence.class, CharSequence.class);

        current(timeProvider);

        // we do a put at the current time
        map.put("key-1", "value-1");
        assertEquals(map.size(), 1);
        assertEquals(map.get("key-1"), "value-1");

        // now test assume that we receive a late update to the map, the following update should be ignored
        late(timeProvider);


        map.put("key-1", "value-2");

        // we'll now flip the time back to the current in order to do the read the result
        current(timeProvider);
        assertEquals(map.size(), 1);
        assertEquals(map.get("key-1"), "value-1");

    }

    @Test
    public void testIgnoreALatePutIfAbsent() throws IOException {

        final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
        final VanillaSharedReplicatedHashMap map = new VanillaSharedReplicatedHashMapBuilder()
                .entries(10)
                .timeProvider(timeProvider).create(getPersistenceFile(), CharSequence.class, CharSequence.class);

        current(timeProvider);

        // we do a put at the current time
        map.put("key-1", "value-1");
        assertEquals(map.size(), 1);
        assertEquals(map.get("key-1"), "value-1");

        // now test assume that we receive a late update to the map, the following update should be ignored
        late(timeProvider);


        final Object o = map.putIfAbsent("key-1", "value-2");
        assertEquals(o, null);

        // we'll now flip the time back to the current in order to do the read the result
        current(timeProvider);
        assertEquals(map.size(), 1);
        assertEquals(map.get("key-1"), "value-1");

    }

    @Test
    public void testIgnoreALateReplace() throws IOException {

        final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
        final VanillaSharedReplicatedHashMap map = new VanillaSharedReplicatedHashMapBuilder()
                .entries(10)
                .timeProvider(timeProvider).create(getPersistenceFile(), CharSequence.class, CharSequence.class);

        current(timeProvider);

        // we do a put at the current time
        map.put("key-1", "value-1");
        assertEquals(map.size(), 1);
        assertEquals(map.get("key-1"), "value-1");

        // now test assume that we receive a late update to the map, the following update should be ignored
        late(timeProvider);


        final Object o = map.replace("key-1", "value-2");
        assertEquals(o, null);

        // we'll now flip the time back to the current in order to do the read the result
        current(timeProvider);
        assertEquals(map.size(), 1);
        assertEquals(map.get("key-1"), "value-1");

    }

    @Test
    public void testIgnoreALateReplaceWithValue() throws IOException {

        final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
        final VanillaSharedReplicatedHashMap map = new VanillaSharedReplicatedHashMapBuilder()
                .entries(10)
                .timeProvider(timeProvider).create(getPersistenceFile(), CharSequence.class, CharSequence.class);

        current(timeProvider);

        // we do a put at the current time
        map.put("key-1", "value-1");
        assertEquals(map.size(), 1);
        assertEquals(map.get("key-1"), "value-1");

        // now test assume that we receive a late update to the map, the following update should be ignored
        late(timeProvider);


        assertEquals(null, map.replace("key-1", "value-1"));


        // we'll now flip the time back to the current in order to do the read the result
        current(timeProvider);
        assertEquals(map.size(), 1);
        assertEquals(map.get("key-1"), "value-1");

    }

    @Test
    public void testIgnoreALateRemoveWithValue() throws IOException {

        final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
        final VanillaSharedReplicatedHashMap map = new VanillaSharedReplicatedHashMapBuilder()
                .entries(10)
                .timeProvider(timeProvider).create(getPersistenceFile(), CharSequence.class, CharSequence.class);

        current(timeProvider);

        // we do a put at the current time
        map.put("key-1", "value-1");
        assertEquals(map.size(), 1);
        assertEquals(map.get("key-1"), "value-1");

        // now test assume that we receive a late update to the map, the following update should be ignored
        late(timeProvider);


        assertEquals(false, map.remove("key-1", "value-1"));

        // we'll now flip the time back to the current in order to do the read the result
        current(timeProvider);
        assertEquals(map.size(), 1);
        assertEquals(map.get("key-1"), "value-1");

    }

    @Test
    public void testIgnoreALateRemove() throws IOException {

        final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
        final VanillaSharedReplicatedHashMap map = new VanillaSharedReplicatedHashMapBuilder()
                .entries(10)
                .timeProvider(timeProvider).create(getPersistenceFile(), CharSequence.class, CharSequence.class);

        current(timeProvider);

        // we do a put at the current time
        map.put("key-1", "value-1");
        assertEquals(map.size(), 1);
        assertEquals(map.get("key-1"), "value-1");

        // now test assume that we receive a late update to the map, the following update should be ignored
        late(timeProvider);
        map.remove("key-1");

        // we'll now flip the time back to the current in order to do the read the result
        current(timeProvider);
        assertEquals(map.size(), 1);
        assertEquals(map.get("key-1"), "value-1");

    }

    private void current(TimeProvider timeProvider) {
        Mockito.when(timeProvider.currentTimeMillis()).thenReturn(System.currentTimeMillis());
    }

    private void late(TimeProvider timeProvider) {
        Mockito.when(timeProvider.currentTimeMillis()).thenReturn(System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(5));
    }
}
