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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SharedMapErrorListeners {

    /**
     * Factories a more flexible than public static instances.
     * We can add some configuration-on-the-first-call in the future.
     */

    private static final SharedMapErrorListener LOGGING = new SharedMapErrorListener() {
        final Logger LOG = LoggerFactory.getLogger(getClass());
        @Override
        public void onLockTimeout(long threadId) throws IllegalStateException {
            if (threadId > 1L << 32) {
                LOG.warn("Grabbing lock held by processId: {}, threadId: {}",
                    (threadId >>> 33), (threadId & 0xFFFFFFFFL));
            } else {
                LOG.warn("Grabbing lock held by threadId: {}",threadId);
            }
        }

        @Override
        public void errorOnUnlock(IllegalMonitorStateException e) {
            LOG.warn("Failed to unlock as expected", e);
        }
    };

    public static SharedMapErrorListener logging() {
        return LOGGING;
    }

    private static final SharedMapErrorListener ERROR = new SharedMapErrorListener() {
        @Override
        public void onLockTimeout(long threadId) throws IllegalStateException {
            throw new IllegalStateException("Unable to acquire lock held by threadId: " + threadId);
        }

        @Override
        public void errorOnUnlock(IllegalMonitorStateException e) {
            throw e;
        }
    };

    public static SharedMapErrorListener error() {
        return ERROR;
    }
}
