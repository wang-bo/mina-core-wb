package org.apache.mina.core.future;

/**
 * An {@link IoFuture} for asynchronous close requests.
 *
 * <h3>Example</h3>
 * <pre>
 * IoSession session = ...;
 * CloseFuture future = session.close(true);
 * 
 * // Wait until the connection is closed
 * future.awaitUninterruptibly();
 * 
 * // Now connection should be closed.
 * assert future.isClosed();
 * </pre>
 * 
 * IoSession关闭操作的IoFuture
 * 
 * @date	2016年6月15日 上午9:45:20	completed
 */
public interface CloseFuture extends IoFuture {

	/**
     * @return <tt>true</tt> if the close request is finished and the session is closed.
     * 
     * 如果IoSession的关闭操作已完成，返回true。
     */
    public boolean isClosed();

    /**
     * Marks this future as closed and notifies all threads waiting for this
     * future. This method is invoked by MINA internally.  Please do not call
     * this method directly.
     * 
     * 标记此future已关闭，并通知所有等待此future的线程。
     * 这个方法是Mina内部使用的，请不要直接调用此方法。
     */
    public void setClosed();
    
    /**
     * {@inheritDoc}
     */
    public CloseFuture await() throws InterruptedException;

    /**
     * {@inheritDoc}
     */
    public CloseFuture awaitUninterruptibly();

    /**
     * {@inheritDoc}
     */
    public CloseFuture addListener(IoFutureListener<?> listener);

    /**
     * {@inheritDoc}
     */
    public CloseFuture removeListener(IoFutureListener<?> listener);
}
