package org.apache.mina.core.buffer;

import java.nio.ByteBuffer;

/**
 * Allocates {@link IoBuffer}s and manages them.  Please implement this
 * interface if you need more advanced memory management scheme.
 * 
 * 分配IoBuffer并管理它们。
 * 如果需要跟高级的内存管理模式，请实现这个接口，并调用IoBuffer.setAllocator(IoBufferAllocator)方法。
 * 
 * @date	2016年6月16日 下午3:28:33	completed
 */
public interface IoBufferAllocator {

	/**
     * Returns the buffer which is capable of the specified size.
     * 
     * 返回一个指定容量的可用的IoBuffer。
     *
     * @param capacity the capacity of the buffer
     * @param direct <tt>true</tt> to get a direct buffer,
     *               <tt>false</tt> to get a heap buffer.
     * @return The allocated {@link IoBuffer}
     */
    public IoBuffer allocate(int capacity, boolean direct);

    /**
     * Returns the NIO buffer which is capable of the specified size.
     * 
     * 返回一个指定容量的可用的NIO的ByteBuffer。
     *
     * @param capacity the capacity of the buffer
     * @param direct <tt>true</tt> to get a direct buffer,
     *               <tt>false</tt> to get a heap buffer.
     * @return The allocated {@link ByteBuffer}
     */
    public ByteBuffer allocateNioBuffer(int capacity, boolean direct);

    /**
     * Wraps the specified NIO {@link ByteBuffer} into MINA buffer.
     * 
     * 包装指定的NIO的ByteBuffer到Mina的IoBuffer中并返回。
     * 
     * @param nioBuffer The {@link ByteBuffer} to wrap
     * @return The {@link IoBuffer} wrapping the {@link ByteBuffer}
     */
    public IoBuffer wrap(ByteBuffer nioBuffer);

    /**
     * Dispose of this allocator.
     * 
     * 释放本allocator
     */
    public void dispose();
}
