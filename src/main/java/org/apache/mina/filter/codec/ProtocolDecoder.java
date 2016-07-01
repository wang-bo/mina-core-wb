package org.apache.mina.filter.codec;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;

/**
 * Decodes binary or protocol-specific data into higher-level message objects.
 * MINA invokes {@link #decode(IoSession, IoBuffer, ProtocolDecoderOutput)}
 * method with read data, and then the decoder implementation puts decoded
 * messages into {@link ProtocolDecoderOutput} by calling
 * {@link ProtocolDecoderOutput#write(Object)}.
 * <p>
 * Please refer to
 * <a href="../../../../../xref-examples/org/apache/mina/examples/reverser/TextLineDecoder.html"><code>TextLineDecoder</code></a>
 * example.
 * 
 * 将二进制信息转换成消息对象的解码器。
 * 当读取消息时，调用decode(IoSession, IoBuffer, ProtocolDecoderOutput)方法，
 * 然后解码器调用ProtocolDecoderOutput.write(Object)将解码好的信息写到ProtocolDecoderOutput中。
 * 
 * @date	2016年6月28日 下午5:18:47	completed
 */
public interface ProtocolDecoder {

	/**
     * Decodes binary or protocol-specific content into higher-level message objects.
     * MINA invokes {@link #decode(IoSession, IoBuffer, ProtocolDecoderOutput)}
     * method with read data, and then the decoder implementation puts decoded
     * messages into {@link ProtocolDecoderOutput}.
     * 
     * 将二进制信息转换成消息对象。
     * 当读取消息时，调用decode(IoSession, IoBuffer, ProtocolDecoderOutput)方法，
     * 然后解码器调用ProtocolDecoderOutput.write(Object)将解码好的信息写到ProtocolDecoderOutput中。
     *
     * @param session The current Session
     * @param in the buffer to decode
     * @param out The {@link ProtocolDecoderOutput} that will receive the decoded message
     * @throws Exception if the read data violated protocol specification
     */
    public void decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception;

    /**
     * Invoked when the specified <tt>session</tt> is closed.  This method is useful
     * when you deal with the protocol which doesn't specify the length of a message
     * such as HTTP response without <tt>content-length</tt> header. Implement this
     * method to process the remaining data that {@link #decode(IoSession, IoBuffer, ProtocolDecoderOutput)}
     * method didn't process completely.
     * 
     * 当指定的session关闭时调用。
     * 当处理没有指定消息长度的协议时(如HTTP的响应头中没有设置content-length)特别有用。
     * 实现这个方法，处理decode()方法没有处理完的消息内容。
     *
     * @param session The current Session
     * @param out The {@link ProtocolDecoderOutput} that contains the decoded message
     * @throws Exception if the read data violated protocol specification
     */
    public void finishDecode(IoSession session, ProtocolDecoderOutput out) throws Exception;

    /**
     * Releases all resources related with this decoder.
     * 
     * 释放decoder的所有相关资源。
     *
     * @param session The current Session
     * @throws Exception if failed to dispose all resources
     */
    public void dispose(IoSession session) throws Exception;
}
