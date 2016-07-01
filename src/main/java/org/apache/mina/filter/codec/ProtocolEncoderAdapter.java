package org.apache.mina.filter.codec;

import org.apache.mina.core.session.IoSession;

/**
 * An abstract {@link ProtocolEncoder} implementation for those who don't have any
 * resources to dispose.
 * 
 * ProtocolEncoder的适配器类。
 * 自定义ProtocolEncoder可以继承此类，然后有选择的覆盖感兴趣的方法。
 * 
 * @date	2016年6月29日 上午10:45:27	completed
 */
public abstract class ProtocolEncoderAdapter implements ProtocolEncoder {

	/**
	 * {@inheritDoc}
	 * 
     * Override this method dispose all resources related with this encoder.
     * The default implementation does nothing.
     * 
     * 要释放encoder的相关资源时，覆盖此方法。
     */
    public void dispose(IoSession session) throws Exception {
        // Do nothing
    }
}
