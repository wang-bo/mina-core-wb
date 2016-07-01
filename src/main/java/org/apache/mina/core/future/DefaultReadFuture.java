package org.apache.mina.core.future;

import java.io.IOException;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.session.IoSession;

/**
 * A default implementation of {@link WriteFuture}
 * 
 * ReadFuture的默认实现
 * 
 * @date	2016年6月16日 上午9:29:41	completed
 */
public class DefaultReadFuture extends DefaultIoFuture implements ReadFuture {

	/** A static object used when the session is closed */
    private static final Object CLOSED = new Object();
    
    /**
     * Creates a new instance.
     * 
     * 构造方法
     * 
     * @param session The associated session
     */
    public DefaultReadFuture(IoSession session) {
        super(session);
    }
    
    /**
     * {@inheritDoc}
     */
    public Object getMessage() {
        if (isDone()) {
            Object v = getValue();
            if (v == CLOSED) {
                return null;
            }
            if (v instanceof RuntimeException) {
                throw (RuntimeException) v;
            }
            if (v instanceof Error) {
                throw (Error) v;
            }
            if (v instanceof IOException || v instanceof Exception) {
                throw new RuntimeIoException((Exception) v);
            }
            return v;
        }
        return null;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isRead() {
        if (isDone()) {
            Object v = getValue();
            return (v != CLOSED && !(v instanceof Throwable));
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isClosed() {
        if (isDone()) {
            return getValue() == CLOSED;
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
                return (Throwable)v;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void setClosed() {
        setValue(CLOSED);
    }

    /**
     * {@inheritDoc}
     */
    public void setRead(Object message) {
        if (message == null) {
            throw new IllegalArgumentException("message");
        }
        setValue(message);
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
    public ReadFuture await() throws InterruptedException {
        return (ReadFuture) super.await();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReadFuture awaitUninterruptibly() {
        return (ReadFuture) super.awaitUninterruptibly();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReadFuture addListener(IoFutureListener<?> listener) {
        return (ReadFuture) super.addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReadFuture removeListener(IoFutureListener<?> listener) {
        return (ReadFuture) super.removeListener(listener);
    }
}
