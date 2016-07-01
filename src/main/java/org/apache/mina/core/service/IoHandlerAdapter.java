package org.apache.mina.core.service;

import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An adapter class for {@link IoHandler}.  You can extend this
 * class and selectively override required event handler methods only.  All
 * methods do nothing by default.
 * 
 * IoHandler的适配器类。
 * 自定义IoHandler可以继承此类，然后有选择的覆盖感兴趣的方法，所有方法默认什么也不做。
 * 
 * @date	2016年6月27日 下午4:21:50	completed
 */
public class IoHandlerAdapter implements IoHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(IoHandlerAdapter.class);
	
	/**
     * {@inheritDoc}
     */
    public void sessionCreated(IoSession session) throws Exception {
        // Empty handler
    }

    /**
     * {@inheritDoc}
     */
    public void sessionOpened(IoSession session) throws Exception {
        // Empty handler
    }

    /**
     * {@inheritDoc}
     */
    public void sessionClosed(IoSession session) throws Exception {
        // Empty handler
    }

    /**
     * {@inheritDoc}
     */
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        // Empty handler
    }

    /**
     * {@inheritDoc}
     */
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("EXCEPTION, please implement " + getClass().getName()
                    + ".exceptionCaught() for proper handling:", cause);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void messageReceived(IoSession session, Object message) throws Exception {
        // Empty handler
    }

    /**
     * {@inheritDoc}
     */
    public void messageSent(IoSession session, Object message) throws Exception {
        // Empty handler
    }

    /**
     * {@inheritDoc}
     */
    public void inputClosed(IoSession session) throws Exception {
        session.closeNow();
    }
}
