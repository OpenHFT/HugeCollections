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

package net.openhft.chronicle.sandbox.queue.locators.shared.remote.channel.provider;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/**
 * Created by Rob Austin
 */
public class ClientSocketChannelProvider extends AbstractSocketChannelProvider {

    public static final int RECEIVE_BUFFER_SIZE = 256 * 1024;
    private static Logger LOG = LoggerFactory.getLogger(ClientSocketChannelProvider.class);
    private volatile boolean closed;

    public ClientSocketChannelProvider(final int port, @NotNull final String host) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                SocketChannel result = null;
                try {
                    while (!closed) {
                        try {
                            result = SocketChannel.open(new InetSocketAddress(host, port));
                            break;
                        } catch (Exception e) {
                            Thread.sleep(1000);
                            continue;
                        }
                    }

                    LOG.info("successfully connected to host={} , port={}", host, port);

                    result.socket().setReceiveBufferSize(RECEIVE_BUFFER_SIZE);

                    socketChannel.set(result);
                    latch.countDown();
                } catch (Exception e) {
                    LOG.warn("", e);
                    if (result != null)
                        try {
                            result.close();
                        } catch (IOException e1) {
                            LOG.warn("", e);
                        }
                }
            }
        }).start();
    }

    /**
     * @throws IOException
     * @inhre
     */
    public void close() throws IOException {
        closed = true;
        final SocketChannel result = socketChannel.get();
        if (result != null) {
            result.close();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                LOG.warn("", e);
            }
        }

    }

}
