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

package net.openhft.chronicle.sandbox.queue;

import net.openhft.chronicle.sandbox.queue.locators.DataLocator;
import net.openhft.chronicle.sandbox.queue.locators.RingIndex;
import net.openhft.chronicle.sandbox.queue.locators.local.LocalDataLocator;
import net.openhft.chronicle.sandbox.queue.locators.local.LocalRingIndex;
import net.openhft.chronicle.sandbox.queue.locators.shared.BytesDataLocator;
import net.openhft.chronicle.sandbox.queue.locators.shared.SharedLocalDataLocator;
import net.openhft.chronicle.sandbox.queue.locators.shared.SharedRingIndex;
import net.openhft.chronicle.sandbox.queue.locators.shared.remote.Consumer;
import net.openhft.chronicle.sandbox.queue.locators.shared.remote.Producer;
import net.openhft.chronicle.sandbox.queue.locators.shared.remote.SocketWriter;
import net.openhft.chronicle.sandbox.queue.locators.shared.remote.channel.provider.ClientSocketChannelProvider;
import net.openhft.chronicle.sandbox.queue.locators.shared.remote.channel.provider.ServerSocketChannelProvider;
import net.openhft.chronicle.sandbox.queue.locators.shared.remote.channel.provider.SocketChannelProvider;
import net.openhft.lang.io.ByteBufferBytes;
import net.openhft.lang.io.DirectBytes;
import net.openhft.lang.io.MappedStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Rob Austin
 */
public class ConcurrentBlockingObjectQueueBuilder<E> {

    public static final int SIZE_OF_INT = 4;
    public static final int RECEIVE_BUFFER_SIZE = 256 * 1024;
    public static final int LOCK_TIME_OUT_NS = 100;
    private static Logger LOG = LoggerFactory.getLogger(SocketWriter.class);
    int capacity;

    int maxSize = 128;
    Class<E> clazz;
    Type type = Type.LOCAL;
    private int port = 8096;
    private String host;
    private String fileName = "/share-queue-test" + System.nanoTime();

    /**
     * returns the value to nearest {@parm powerOf2}
     */
    public static int align(int capacity, int powerOf2) {
        return (capacity + powerOf2 - 1) & ~(powerOf2 - 1);
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public void setCapacity(int capacity) {
        // the ring buffer works by having 1 item spare, that's why we add one
        this.capacity = capacity + 1;
    }

    public void setClazz(Class<E> clazz) {
        this.clazz = clazz;
    }


    public BlockingQueue<E> create() throws IOException {

        final DataLocator dataLocator;
        final RingIndex ringIndex;

        if (type == Type.LOCAL) {
            ringIndex = new LocalRingIndex();
            dataLocator = new LocalDataLocator(capacity);

        } else if (type == Type.SHARED) {

            final String tmp = System.getProperty("java.io.tmpdir");
            final File file = new File(tmp + fileName);

            int ringIndexLocationsStart = 0;
            int ringIndexLocationsLen = SIZE_OF_INT * 2;
            int storeLen = capacity * align(maxSize, 4);

            final MappedStore ms = new MappedStore(file, FileChannel.MapMode.READ_WRITE, ringIndexLocationsLen + storeLen);
            final DirectBytes ringIndexSlice = ms.bytes(ringIndexLocationsStart, ringIndexLocationsLen);
            ringIndex = new SharedRingIndex(ringIndexSlice);

            // provides an index to the data in the ring buffer, the size of this index is proportional to the capacity of the ring buffer
            final DirectBytes storeSlice = ms.bytes(ringIndexLocationsLen, storeLen);
            dataLocator = new SharedLocalDataLocator(capacity, maxSize, storeSlice, clazz);

        } else if (type == Type.REMOTE_PRODUCER || type == Type.REMOTE_CONSUMER) {

            final int bufferSize = capacity * align(maxSize, 4);
            final ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder());
            final ByteBufferBytes byteBufferBytes = new ByteBufferBytes(buffer);

            final BytesDataLocator<E, ByteBufferBytes> bytesDataLocator = new BytesDataLocator<E, ByteBufferBytes>(
                    clazz,
                    capacity,
                    maxSize,
                    byteBufferBytes.slice(),
                    byteBufferBytes.slice());

            if (type == Type.REMOTE_PRODUCER) {
                final Producer producer = new Producer<E, ByteBufferBytes>(new LocalRingIndex(), bytesDataLocator, bytesDataLocator, new ServerSocketChannelProvider(port), bytesDataLocator, buffer);
                ringIndex = producer;
                dataLocator = producer;
            } else {
                ringIndex = new Consumer<ByteBufferBytes>(new LocalRingIndex(), bytesDataLocator, bytesDataLocator, new ClientSocketChannelProvider(port, host), buffer);
                dataLocator = bytesDataLocator;
            }

        } else {
            throw new IllegalArgumentException("Unsupported Type=" + type);
        }

        return new ConcurrentBlockingObjectQueue<E>(ringIndex, dataLocator);

    }

    private SocketChannelProvider getProducerSocketChannelProvider() {


        return new SocketChannelProvider() {

            ServerSocketChannel serverSocket = null;

            @Override
            public SocketChannel getSocketChannel() throws IOException {
                serverSocket = ServerSocketChannel.open();
                serverSocket.socket().setReuseAddress(true);
                serverSocket.socket().bind(new InetSocketAddress(port));
                serverSocket.configureBlocking(true);
                LOG.info("Server waiting for client on port {}",port);
                serverSocket.socket().setReceiveBufferSize(RECEIVE_BUFFER_SIZE);
                return serverSocket.accept();
            }

            @Override
            public void close() throws IOException {
                if (serverSocket != null)
                    serverSocket.close();
            }
        };
    }


    public enum Type {

        LOCAL("running within one JVM"),
        SHARED("one producer JVM, one Consumer JVM, with the Jvm's on the same Host"),
        REMOTE_PRODUCER("One Producer that connects via tcp/ip to single REMOTE_CONSUMER"),
        REMOTE_CONSUMER("One Consumer that connects via tcp/ip to single REMOTE_PRODUCER");

        private final String about;

        Type(String about) {
            this.about = about;
        }

        @Override
        public String toString() {
            return about;
        }
    }

}
