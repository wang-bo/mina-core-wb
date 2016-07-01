package org.apache.mina.filter.codec;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.IoSession;

/**
 * Callback for {@link ProtocolDecoder} to generate decoded messages.
 * {@link ProtocolDecoder} must call {@link #write(Object)} for each decoded
 * messages.
 * 
 * 当ProtocolDecoder解码好消息后，回调本接口的write(Object)方法。
 * 
 * @date	2016年6月28日 下午5:38:13	completed
 */
public interface ProtocolDecoderOutput {

	/**
     * Callback for {@link ProtocolDecoder} to generate decoded messages.
     * {@link ProtocolDecoder} must call {@link #write(Object)} for each
     * decoded messages.
     * 
     * 当ProtocolDecoder解码好消息后，回调本接口的write(Object)方法。
     *
     * @param message the decoded message
     */
    public void write(Object message);

    /**
     * Flushes all messages you wrote via {@link #write(Object)} to
     * the next filter.
     * 
     * 刷新所有通过write(Object)写入的消息到下个过滤器中。
     * 
     * @param nextFilter the next Filter
     * @param session The current Session
     */
    public void flush(NextFilter nextFilter, IoSession session);
}
