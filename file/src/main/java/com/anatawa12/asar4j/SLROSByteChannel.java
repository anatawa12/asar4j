package com.anatawa12.asar4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;

// Size Limited Read Only Seekable
class SLROSByteChannel implements SeekableByteChannel {
    private SeekableByteChannel channel;
    private final long offset;
    private final long size;
    private long position = 0;

    SLROSByteChannel(SeekableByteChannel channel, long offset, long size) {
        if (offset < 0) throw new IllegalArgumentException("negative offset");
        if (size < 0) throw new IllegalArgumentException("negative size");
        if (offset + size < 0) throw new IllegalArgumentException("size + offset overflow");
        this.channel = channel;
        this.offset = offset;
        this.size = size;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        checkClose();
        if (position >= size) return -1;
        channel.position(offset + position);
        int read;
        if (dst.remaining() <= limit()) {
            read = channel.read(dst);
        } else {
            int dstLimit = dst.limit();
            dst.limit(dst.position() + (int) limit());
            read = channel.read(dst);
            dst.limit(dstLimit);
        }
        assert read <= limit();
        if (read == -1) {
            return -1;
        }
        position += read;
        return read;
    }

    private long limit() {
        return size - position;
    }

    @Override
    public boolean isOpen() {
        return channel != null;
    }

    @Override
    public void close() {
        channel = null;
    }

    private void checkClose() {
        if (!isOpen()) throw new IllegalStateException("closed");
    }

    @Override
    public long position() {
        return position;
    }

    @Override
    public SeekableByteChannel position(long newPosition) {
        if (newPosition < 0) throw new IllegalArgumentException("negative position");
        position = newPosition;
        return this;
    }

    @Override
    public long size() {
        return size;
    }

    // modification operations

    @Override
    public int write(ByteBuffer src) {
        throw new NonWritableChannelException();
    }

    @Override
    public SeekableByteChannel truncate(long size) {
        throw new NonWritableChannelException();
    }
}
