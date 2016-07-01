package org.apache.mina.filter.codec;

import org.apache.mina.core.session.IoSession;

/**
 * Provides {@link ProtocolEncoder} and {@link ProtocolDecoder} which translates
 * binary or protocol specific data into message object and vice versa.
 * <p>
 * Please refer to
 * <a href="../../../../../xref-examples/org/apache/mina/examples/reverser/ReverseProtocolProvider.html"><code>ReverserProtocolProvider</code></a>
 * example.
 * 
 * 提供进行二进制信息与消息对象进行转换的编解码器。
 * 
 * 
 * @date	2016年6月28日 下午5:07:44	completed
 */
public interface ProtocolCodecFactory {

	/**
     * Returns a new (or reusable) instance of {@link ProtocolEncoder} which
     * encodes message objects into binary or protocol-specific data.
     * 
     * 返回将消息对象转换成二进制信息的编码器。
     * 
     * @param session The current session
     * @return The encoder instance
     * @throws Exception If an error occurred while retrieving the encoder
     */
    public ProtocolEncoder getEncoder(IoSession session) throws Exception;

    /**
     * Returns a new (or reusable) instance of {@link ProtocolDecoder} which
     * decodes binary or protocol-specific data into message objects.
     * 
     * 返回将二进制信息转换成消息对象的解码器。
     * 
     * @param session The current session
     * @return The decoder instance
     * @throws Exception If an error occurred while retrieving the decoder
     */
    public ProtocolDecoder getDecoder(IoSession session) throws Exception;
}
