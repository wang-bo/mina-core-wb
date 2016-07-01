package org.apache.mina.core.future;

import org.apache.mina.core.session.IoSession;

/**
 * A default implementation of {@link WriteFuture}.
 * 
 * WriteFuture的默认实现
 * 
 * @date	2016年6月16日 上午9:34:15	completed
 */
public class DefaultWriteFuture extends DefaultIoFuture implements WriteFuture {

	/**
     * Returns a new {@link DefaultWriteFuture} which is already marked as 'written'.
     * 
     * 创建一个代表写成功的WriteFuture
     * 
     * @param session The associated session
     * @return A new future for a written message
     */
    public static WriteFuture newWrittenFuture(IoSession session) {
        DefaultWriteFuture writtenFuture = new DefaultWriteFuture(session);
        writtenFuture.setWritten();
        return writtenFuture;
    }

    /**
     * Returns a new {@link DefaultWriteFuture} which is already marked as 'not written'.
     * 
     * 创建一个代表写失败的WriteFuture
     * 
     * @param session The associated session
     * @param cause The reason why the message has not be written
     * @return A new future for not written message
     */
    public static WriteFuture newNotWrittenFuture(IoSession session, Throwable cause) {
        DefaultWriteFuture unwrittenFuture = new DefaultWriteFuture(session);
        unwrittenFuture.setException(cause);
        return unwrittenFuture;
    }

    /**
     * Creates a new instance.
     * 
     * 构造方法
     * 
     * @param session The associated session
     */
    public DefaultWriteFuture(IoSession session) {
        super(session);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isWritten() {
        if (isDone()) {
            Object v = getValue();
            if (v instanceof Boolean) {
                return ((Boolean) v).booleanValue();
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Throwable getException() {
        if (isDone()) {
            Object v = getValue();
            if (v instanceof Throwable) {
                return (Throwable) v;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void setWritten() {
        setValue(Boolean.TRUE);
    }

    /**
     * {@inheritDoc}
     */
    public void setException(Throwable exception) {
        if (exception == null) {
            throw new IllegalArgumentException("exception");
        }
        setValue(exception);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WriteFuture await() throws InterruptedException {
        return (WriteFuture) super.await();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WriteFuture awaitUninterruptibly() {
        return (WriteFuture) super.awaitUninterruptibly();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WriteFuture addListener(IoFutureListener<?> listener) {
        return (WriteFuture) super.addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WriteFuture removeListener(IoFutureListener<?> listener) {
        return (WriteFuture) super.removeListener(listener);
    }
}
