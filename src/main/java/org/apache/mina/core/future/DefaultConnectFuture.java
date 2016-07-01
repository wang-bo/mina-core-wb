package org.apache.mina.core.future;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.session.IoSession;

/**
 * A default implementation of {@link ConnectFuture}.
 * 
 * ConnectFuture的默认实现
 * 
 * @date	2016年6月16日 上午9:17:34	completed
 */
public class DefaultConnectFuture extends DefaultIoFuture implements ConnectFuture {

	/** A static object stored into the ConnectFuture when the connection has been cancelled */
    private static final Object CANCELED = new Object();
    
    /**
     * Creates a new instance.
     * 
     * 构造方法
     */
    public DefaultConnectFuture() {
        super(null);
    }
    
    /**
     * Creates a new instance of a Connection Failure, with the associated cause.
     * 
     * 当连接失败时，使用异常创建一个ConnectFuture对象。
     * 
     * @param exception The exception that caused the failure
     * @return a new {@link ConnectFuture} which is already marked as 'failed to connect'.
     */
    public static ConnectFuture newFailedFuture(Throwable exception) {
    	DefaultConnectFuture failedFuture = new DefaultConnectFuture();
    	failedFuture.setException(exception);
    	return failedFuture;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public IoSession getSession() {
    	Object v = getValue();
    	if (v instanceof IoSession) {
			return (IoSession) v;
		} else if (v instanceof RuntimeException) {
            throw (RuntimeException) v;
        } else if (v instanceof Error) {
            throw (Error) v;
        } else if (v instanceof Throwable) {
            throw (RuntimeIoException) new RuntimeIoException("Failed to get the session.").initCause((Throwable) v);
        } else  {
            return null;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public Throwable getException() {
        Object v = getValue();
        if (v instanceof Throwable) {
            return (Throwable) v;
        } else {
            return null;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isConnected() {
        return getValue() instanceof IoSession;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isCanceled() {
        return getValue() == CANCELED;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setSession(IoSession session) {
        if (session == null) {
            throw new IllegalArgumentException("session");
        }
        setValue(session);
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
    public boolean cancel() {
        return setValue(CANCELED);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectFuture await() throws InterruptedException {
        return (ConnectFuture) super.await();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectFuture awaitUninterruptibly() {
        return (ConnectFuture) super.awaitUninterruptibly();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectFuture addListener(IoFutureListener<?> listener) {
        return (ConnectFuture) super.addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectFuture removeListener(IoFutureListener<?> listener) {
        return (ConnectFuture) super.removeListener(listener);
    }
}
