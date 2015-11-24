/*
 * Copyright (c) 2001, 2002, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package sun.nio.ch;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.channels.spi.*;


/**
 * This class is defined here rather than in java.nio.channels.Channels
 * so that code can be shared with SocketAdaptor.
 *
 * @author Mike McCloskey
 * @author Mark Reinhold
 * @since 1.4
 */

public class ChannelInputStream
    extends InputStream
{

    // Android-changed : This code didn't make sense. In particular, the block channel is
    // useless because we throw if the channel is non-blocking!. It would only make sense
    // if it's called on a blocking channel (but we're asked to make it non-blocking before
    // the read) we never do that, though.
    //
    // read(ReadableByteChannel,ByteBuffer, boolean block)
    public static int read(ReadableByteChannel ch, ByteBuffer bb)
        throws IOException
    {
        if (ch instanceof SelectableChannel) {
            SelectableChannel sc = (SelectableChannel)ch;
            synchronized (sc.blockingLock()) {
                boolean bm = sc.isBlocking();
                if (!bm)
                    throw new IllegalBlockingModeException();
                // Android removed.
                // if (bm != block)
                //    sc.configureBlocking(block);
                int n = ch.read(bb);
                // Android removed.
                // if (bm != block)
                //     sc.configureBlocking(bm);
                return n;
            }
        } else {
            return ch.read(bb);
        }
    }

    protected final ReadableByteChannel ch;
    private ByteBuffer bb = null;
    private byte[] bs = null;           // Invoker's previous array
    private byte[] b1 = null;

    public ChannelInputStream(ReadableByteChannel ch) {
        this.ch = ch;
    }

    public synchronized int read() throws IOException {
        if (b1 == null)
            b1 = new byte[1];
        int n = this.read(b1);
        if (n == 1)
            return b1[0] & 0xff;
        return -1;
    }

    public synchronized int read(byte[] bs, int off, int len)
        throws IOException
    {
        if ((off < 0) || (off > bs.length) || (len < 0) ||
            ((off + len) > bs.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0)
            return 0;

        ByteBuffer bb = ((this.bs == bs)
                         ? this.bb
                         : ByteBuffer.wrap(bs));
        bb.limit(Math.min(off + len, bb.capacity()));
        bb.position(off);
        this.bb = bb;
        this.bs = bs;
        return read(bb);
    }

    protected int read(ByteBuffer bb)
        throws IOException
    {
        return ChannelInputStream.read(ch, bb);
    }

    public int available() throws IOException {
        // special case where the channel is to a file
        if (ch instanceof SeekableByteChannel) {
            SeekableByteChannel sbc = (SeekableByteChannel)ch;
            long rem = Math.max(0, sbc.size() - sbc.position());
            return (rem > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int)rem;
        }
        return 0;
    }

    public void close() throws IOException {
        ch.close();
    }

}