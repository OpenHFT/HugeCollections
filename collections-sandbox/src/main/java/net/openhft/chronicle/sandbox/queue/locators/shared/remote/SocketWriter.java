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

package net.openhft.chronicle.sandbox.queue.locators.shared.remote;

import net.openhft.chronicle.sandbox.queue.locators.shared.remote.channel.provider.SocketChannelProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Rob Austin
 * <p/>
 * todo remove the locking in this class by introducing bit sets in this class.
 */
public class SocketWriter<E> {

    private static Logger LOG = LoggerFactory.getLogger(SocketWriter.class);


    @NotNull
    private final String name;
    private final AtomicBoolean isBusy = new AtomicBoolean(true);
    // intentionally not volatile
    private int offset = Integer.MIN_VALUE;
    private long value = Long.MIN_VALUE;
    private int length = Integer.MIN_VALUE;
    private Type type = Type.INT;

    /**
     * @param producerService       this must be a single threaded executor
     * @param socketChannelProvider
     * @param name
     * @param buffer
     */
    public SocketWriter(@NotNull final ExecutorService producerService,
                        @NotNull final SocketChannelProvider socketChannelProvider,
                        @NotNull final String name,
                        @NotNull final ByteBuffer buffer) {

        this.name = name;

        // make a local safe copy
        final ByteBuffer byteBuffer = buffer.duplicate();
        final ByteBuffer intBuffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());

        producerService.submit(new Runnable() {
            @Override
            public void run() {

                try {

                    final SocketChannel socketChannel = socketChannelProvider.getSocketChannel();

                    for (; ; ) {
                        try {

                            synchronized (isBusy) {
                                isBusy.set(false);
                                isBusy.wait();
                            }

                            if (type == Type.INT) {
                                intBuffer.clear();
                                final long value1 = SocketWriter.this.value;
                                intBuffer.putInt((int) value1);
                                intBuffer.flip();
                                socketChannel.write(intBuffer);
                            } else if (type == Type.BYTES) {
                                int offset = SocketWriter.this.offset;
                                byteBuffer.limit(offset + SocketWriter.this.length);
                                byteBuffer.position(offset);
                                socketChannel.write(byteBuffer);
                            } else {
                                throw new IllegalArgumentException("unsupported type=" + type);
                            }


                        } catch (Exception e) {
                            LOG.warn("", e);
                        }

                    }

                } catch (Exception e) {
                    LOG.warn("", e);
                }
            }
        });
    }

    /**
     * used to writeBytes a byte buffer bytes to the socket at {@param offset} and {@param length}
     * It is assumed that the byte buffer will contain the bytes of a serialized instance,
     * The first thing that is written to the socket is the {@param length}, this should be size of your serialized instance
     *
     * @param offset
     * @param length this should be size of your serialized instance
     */

    public void writeBytes(int offset, final int length) {

        while (!isBusy.compareAndSet(false, true)) {
            // spin lock -  we have to touch the spin lock so that messages are not skipped
        }

        synchronized (isBusy) {
            this.type = Type.BYTES;
            this.length = length;
            this.offset = offset;
            isBusy.notifyAll();
        }


    }

    /**
     * the index is encode as a negative number when put on the wire, this is because positive number are used to demote the size of preceding serialized instance
     *
     * @param value used to write an int to the socket
     */
    public void writeInt(final int value) {

        while (!isBusy.compareAndSet(false, true)) {
            // spin lock -  we have to add the spin lock so that messages are not skipped
        }

        synchronized (isBusy) {
            this.type = Type.INT;
            this.value = value;
            isBusy.notifyAll();
        }
    }

    @Override
    public String toString() {
        return "SocketWriter{" +
                ", name='" + name + '\'' +
                '}';
    }


    private enum Type {INT, BYTES}

}