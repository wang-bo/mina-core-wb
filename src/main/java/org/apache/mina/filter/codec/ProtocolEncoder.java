package org.apache.mina.filter.codec;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;

/**
 * Encodes higher-level message objects into binary or protocol-specific data.
 * MINA invokes {@link #encode(IoSession, Object, ProtocolEncoderOutput)}
 * method with message which is popped from the session write queue, and then
 * the encoder implementation puts encoded messages (typically {@link IoBuffer}s)
 * into {@link ProtocolEncoderOutput} by calling {@link ProtocolEncoderOutput#write(Object)}.
 * <p>
 * Please refer to
 * <a href="../../../../../xref-examples/org/apache/mina/examples/reverser/TextLineEncoder.html"><code>TextLineEncoder</code></a>
 * example.
 * 
 * 将消息对象转换成二进制信息的编码器。
 * 当消息对象从session的写请求队列中弹出时，调用encode(IoSession, Object, ProtocolEncoderOutput)方法，
 * 然后编码器调用ProtocolEncoderOutput.write(Object)将编码好的信息(通常是一个IoBuffer)写到ProtocolEncoderOutput中。
 * 
 * @date	2016年6月28日 下午5:10:14	completed
 */
public interface ProtocolEncoder {

	/**
     * Encodes higher-level message objects into binary or protocol-specific data.
     * MINA invokes {@link #encode(IoSession, Object, ProtocolEncoderOutput)}
     * method with message which is popped from the session write queue, and then
     * the encoder implementation puts encoded messages (typically {@link IoBuffer}s)
     * into {@link ProtocolEncoderOutput}.
     * 
     * 将消息对象转换成二进制信息。
     * 当消息对象从session的写请求队列中弹出时，调用encode(IoSession, Object, ProtocolEncoderOutput)方法，
     * 然后编码器调用ProtocolEncoderOutput.write(Object)将编码好的信息(通常是一个IoBuffer)写到ProtocolEncoderOutput中。
     *
     * @param session The current Session
     * @param message the message to encode
     * @param out The {@link ProtocolEncoderOutput} that will receive the encoded message
     * @throws Exception if the message violated protocol specification
     */
    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception;

    /**
     * Releases all resources related with this encoder.
     * 
     * 释放encoder的所有相关资源。
     *
     * @param session The current Session
     * @throws Exception if failed to dispose all resources
     */
    public void dispose(IoSession session) throws Exception;
}
