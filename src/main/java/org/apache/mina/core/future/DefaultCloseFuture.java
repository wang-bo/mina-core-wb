package org.apache.mina.core.future;

import org.apache.mina.core.session.IoSession;

/**
 * A default implementation of {@link CloseFuture}.
 * 
 * CloseFuture的默认实现
 * 
 * @date	2016年6月16日 上午9:12:43	completed
 */
public class DefaultCloseFuture extends DefaultIoFuture implements CloseFuture {
	
	/**
     * Creates a new instance.
     * 
     * 构造方法
     * 
     * @param session The associated session
     */
	public DefaultCloseFuture(IoSession session) {
		super(session);
	}

	/**
     * {@inheritDoc}
     */
	public boolean isClosed() {
		if (isDone()) {
			return ((Boolean) getValue()).booleanValue();
		} else {
			return false;
		}
	}

	/**
     * {@inheritDoc}
     */
    public void setClosed() {
        setValue(Boolean.TRUE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public CloseFuture await() throws InterruptedException {
		return (CloseFuture) super.await();
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public CloseFuture awaitUninterruptibly() {
        return (CloseFuture) super.awaitUninterruptibly();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloseFuture addListener(IoFutureListener<?> listener) {
        return (CloseFuture) super.addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloseFuture removeListener(IoFutureListener<?> listener) {
        return (CloseFuture) super.removeListener(listener);
    }
}
