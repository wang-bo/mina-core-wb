package org.apache.mina.filter.codec;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.file.FileRegion;
import org.apache.mina.core.future.WriteFuture;

/**
 * Callback for {@link ProtocolEncoder} to generate encoded messages such as
 * {@link IoBuffer}s.  {@link ProtocolEncoder} must call {@link #write(Object)}
 * for each encoded message.
 * 
 * 当ProtocolEncoder编码好消息后(如编码成IoBuffer)，回调本接口的write(Object)方法。
 * 
 * @date	2016年6月28日 下午5:33:12	completed
 */
public interface ProtocolEncoderOutput {

	/**
     * Callback for {@link ProtocolEncoder} to generate an encoded message such
     * as an {@link IoBuffer}. {@link ProtocolEncoder} must call
     * {@link #write(Object)} for each encoded message.
     * 
     * 当ProtocolEncoder编码好消息后(如编码成IoBuffer)，回调本方法。
     *
     * @param encodedMessage the encoded message, typically an {@link IoBuffer}
     *                       or a {@link FileRegion}.
     */
    public void write(Object encodedMessage);

    /**
     * Merges all buffers you wrote via {@link #write(Object)} into
     * one {@link IoBuffer} and replaces the old fragmented ones with it.
     * This method is useful when you want to control the way MINA generates
     * network packets.  Please note that this method only works when you
     * called {@link #write(Object)} method with only {@link IoBuffer}s.
     * 
     * 合并所有通过write(Object)写入的buffer到一个buffer中。
     * 当希望控制网络数据包时这个方法特别有用。
     * 
     * @throws IllegalStateException if you wrote something else than {@link IoBuffer}
     */
    public void mergeAll();

    /**
     * Flushes all buffers you wrote via {@link #write(Object)} to
     * the session.  This operation is asynchronous; please wait for
     * the returned {@link WriteFuture} if you want to wait for
     * the buffers flushed.
     * 
     * 刷新所有通过write(Object)写入的buffer到session中。
     *
     * @return <tt>null</tt> if there is nothing to flush at all.
     */
    public WriteFuture flush();
}
