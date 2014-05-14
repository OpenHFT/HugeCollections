/*
 * Copyright 2013 Peter Lawrey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.collections;

import net.openhft.lang.io.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SharedMapEventListeners {

    /**
     * Factories a more flexible than public static instances.
     * We can add some configuration-on-the-first-call in the future.
     */

    static final SharedMapEventListener NOP = new SharedMapEventListener() {};

    public static <K, V, M extends SharedHashMap<K, V>>  SharedMapEventListener<K, V, M> nop() {
        return NOP;
    }

    private static final SharedMapEventListener BYTES_LOGGING = new SharedMapEventListener() {
        public final Logger LOGGER = LoggerFactory.getLogger(getClass());

        @Override
        public Object onGetMissing(SharedHashMap map, Bytes keyBytes,
                                   Object key, Object usingValue) {
            StringBuilder sb = new StringBuilder();
            sb.append(map.file()).append(" missed ");

            keyBytes.toString(sb, 0, 0, keyBytes.limit());

            LOGGER.info(sb.toString());
            return null;
        }

        @Override
        public void onGetFound(SharedHashMap map, Bytes entry, int metaDataBytes,
                               Object key, Object value) {
            logOperation(map, entry, metaDataBytes, " get ");
        }

        private void logOperation(SharedHashMap map, Bytes entry, int metaDataBytes, String oper) {
            StringBuilder sb = new StringBuilder();
            sb.append(map.file()).append(oper);
            if (metaDataBytes > 0) {
                sb.append("Meta: ");
                entry.toString(sb, 0L, 0L, metaDataBytes);
                sb.append(" | ");
            }
            Bytes slice = entry.slice(metaDataBytes, entry.limit() - metaDataBytes);
            long keyLength = slice.readStopBit();
            slice.toString(sb, slice.position(), 0L, slice.position() + keyLength);
            slice.position(slice.position() + keyLength);
            long valueLength = slice.readStopBit();
            slice.alignPositionAddr(4);
            sb.append(" = ");
            slice.toString(sb, slice.position(), 0L, slice.position() + valueLength);
            LOGGER.info(sb.toString());
        }

        @Override
        public void onPut(SharedHashMap map, Bytes entry, int metaDataBytes, boolean added,
                          Object key, Object value, long pos, SharedSegment segment) {
            logOperation(map, entry, metaDataBytes, added ? " +put " : " put ");
        }

        @Override
        public void onRemove(SharedHashMap map, Bytes entry, int metaDataBytes,
                             Object key, Object value, int pos, SharedSegment segment) {
            logOperation(map, entry, metaDataBytes, " remove ");
        }
    };

    public static <K, V, M extends SharedHashMap<K, V>> SharedMapEventListener<K, V, M> bytesLogging() {
        return BYTES_LOGGING;
    }

    private static final SharedMapEventListener KEY_VALUE_LOGGING = new SharedMapEventListener() {
        public final Logger LOGGER = LoggerFactory.getLogger(getClass());

        @Override
        public Object onGetMissing(SharedHashMap map, Bytes keyBytes,
                                   Object key, Object usingValue) {
            LOGGER.info("{} missed {}",map.file(), key);
            return null;
        }

        @Override
        public void onGetFound(SharedHashMap map, Bytes entry, int metaDataBytes,
                               Object key, Object value) {
            LOGGER.info("{} get {} => {}", map.file(), key,  value);
        }

        @Override
        public void onPut(SharedHashMap map, Bytes entry, int metaDataBytes, boolean added,
                          Object key, Object value, long pos, SharedSegment segment) {
            LOGGER.info("{} put {} => {}", map.file(), key,  value);
        }

        @Override
        public void onRemove(SharedHashMap map, Bytes entry, int metaDataBytes,
                             Object key, Object value, int pos, SharedSegment segment) {
            LOGGER.info("{} remove {} was {}", map.file(), key,  value);
        }
    };

    public static <K, V, M extends SharedHashMap<K, V>> SharedMapEventListener<K, V, M> keyValueLogging() {
        return KEY_VALUE_LOGGING;
    }

    private SharedMapEventListeners() {}
}
