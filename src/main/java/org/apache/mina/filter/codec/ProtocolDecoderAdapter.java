package org.apache.mina.filter.codec;

import org.apache.mina.core.session.IoSession;

/**
 * An abstract {@link ProtocolDecoder} implementation for those who don't need
 * {@link ProtocolDecoder#finishDecode(IoSession, ProtocolDecoderOutput)} nor
 * {@link ProtocolDecoder#dispose(IoSession)} method.
 * 
 * ProtocolDecoder的适配器类。
 * 自定义ProtocolDecoder可以继承此类，然后有选择的覆盖感兴趣的方法。
 * 
 * @date	2016年6月29日 上午10:40:55	completed
 */
public abstract class ProtocolDecoderAdapter implements ProtocolDecoder {

	/**
	 * {@inheritDoc}
	 * 
     * Override this method to deal with the closed connection.
     * The default implementation does nothing.
     * 
     * 当关闭连接时要做相应处理时，覆盖此方法。
     */
    public void finishDecode(IoSession session, ProtocolDecoderOutput out) throws Exception {
        // Do nothing
    }

    /**
     * {@inheritDoc}
     * 
     * Override this method to dispose all resources related with this decoder.
     * The default implementation does nothing.
     * 
     * 要释放decoder的相关资源时，覆盖此方法。
     */
    public void dispose(IoSession session) throws Exception {
        // Do nothing
    }
}
