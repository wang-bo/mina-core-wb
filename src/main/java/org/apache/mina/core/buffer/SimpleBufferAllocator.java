package org.apache.mina.core.buffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A simplistic {@link IoBufferAllocator} which simply allocates a new
 * buffer every time.
 * 
 * 一个IoBufferAllocator的简单实现。
 * 
 * @date	2016年6月16日 下午3:36:41	completed
 */
public class SimpleBufferAllocator implements IoBufferAllocator {

	/**
     * {@inheritDoc}
     */
	public IoBuffer allocate(int capacity, boolean direct) {
        return wrap(allocateNioBuffer(capacity, direct));
    }
	
	/**
     * {@inheritDoc}
     */
	public ByteBuffer allocateNioBuffer(int capacity, boolean direct) {
		ByteBuffer nioBuffer;
		if (direct) {
			nioBuffer = ByteBuffer.allocateDirect(capacity);
		} else {
			nioBuffer = ByteBuffer.allocate(capacity);
		}
		return nioBuffer;
	}
	
	/**
     * {@inheritDoc}
     */
	public IoBuffer wrap(ByteBuffer nioBuffer) {
        return new SimpleBuffer(nioBuffer);
    }

	/**
     * {@inheritDoc}
     */
    public void dispose() {
        // Do nothing
    }
    
    private class SimpleBuffer extends AbstractIoBuffer {
    	private ByteBuffer buf;

        protected SimpleBuffer(ByteBuffer buf) {
            super(SimpleBufferAllocator.this, buf.capacity());
            this.buf = buf;
            buf.order(ByteOrder.BIG_ENDIAN);
        }

        protected SimpleBuffer(SimpleBuffer parent, ByteBuffer buf) {
            super(parent);
            this.buf = buf;
        }
        
        @Override
        public ByteBuffer buf() {
            return buf;
        }

        @Override
        protected void buf(ByteBuffer buf) {
            this.buf = buf;
        }

        @Override
        protected IoBuffer duplicate0() {
            return new SimpleBuffer(this, this.buf.duplicate());
        }

        @Override
        protected IoBuffer slice0() {
            return new SimpleBuffer(this, this.buf.slice());
        }

        @Override
        protected IoBuffer asReadOnlyBuffer0() {
            return new SimpleBuffer(this, this.buf.asReadOnlyBuffer());
        }

        @Override
        public byte[] array() {
            return buf.array();
        }

        @Override
        public int arrayOffset() {
            return buf.arrayOffset();
        }

        @Override
        public boolean hasArray() {
            return buf.hasArray();
        }

        @Override
        public void free() {
            // Do nothing
        }
    }
}
