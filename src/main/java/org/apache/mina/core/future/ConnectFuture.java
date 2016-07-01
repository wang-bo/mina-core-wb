package org.apache.mina.core.future;

import org.apache.mina.core.session.IoSession;

/**
 * An {@link IoFuture} for asynchronous connect requests.
 *
 * <h3>Example</h3>
 * <pre>
 * IoConnector connector = ...;
 * ConnectFuture future = connector.connect(...);
 * future.awaitUninterruptibly(); // Wait until the connection attempt is finished.
 * IoSession session = future.getSession();
 * session.write(...);
 * </pre>
 * 
 * IoConnector连接操作的IoFuture
 * 
 * @date	2016年6月15日 上午9:50:39	completed
 */
public interface ConnectFuture extends IoFuture {

	/**
     * Returns {@link IoSession} which is the result of connect operation.
     * 
     * 返回连接成功后生成的IoSession。如果连接不成功则返回null。
     *
     * @return The {link IoSession} instance that has been associated with the connection,
     * if the connection was successful, {@code null} otherwise
     */
    public IoSession getSession();
    
    /**
     * Returns the cause of the connection failure.
     * 
     * 返回连接失败的抛出的异常。如果连接操作没完成或连接成功则返回null。
     *
     * @return <tt>null</tt> if the connect operation is not finished yet,
     *         or if the connection attempt is successful, otherwise returns
     *         teh cause of the exception
     */
    public Throwable getException();
    
    /**
     * @return {@code true} if the connect operation is finished successfully.
     * 
     * 返回连接操作是否成功。
     */
    public boolean isConnected();

    /**
     * @return {@code true} if the connect operation has been canceled by
     * {@link #cancel()} method.
     * 
     * 如果连接操作已被cancel()方法取消，返回true。
     */
    public boolean isCanceled();
    
    /**
     * Sets the newly connected session and notifies all threads waiting for
     * this future.  This method is invoked by MINA internally.  Please do not
     * call this method directly.
     * 
     * 设置连接成功后生成的session，并通知所有等待此future的线程。
     * 这个方法是Mina内部使用的，请不要直接调用此方法。
     * 
     * @param session The created session to store in the ConnectFuture insteance
     */
    public void setSession(IoSession session);

    /**
     * Sets the exception caught due to connection failure and notifies all
     * threads waiting for this future.  This method is invoked by MINA
     * internally.  Please do not call this method directly.
     * 
     * 设置连接导致连接失败的异常，并通知所有等待此future的线程。
     * 这个方法是Mina内部使用的，请不要直接调用此方法。
     * 
     * @param exception The exception to store in the ConnectFuture instance
     */
    public void setException(Throwable exception);
    
    /**
     * Cancels the connection attempt and notifies all threads waiting for
     * this future.
     * 
     * 取消连接请求，并通知所有等待此future的线程。
     * 
     * @return {@code true} if the future has been cancelled by this call, {@code false}
     * if the future was already cancelled.
     */
    public boolean cancel();
    
    /**
     * {@inheritDoc}
     */
    public ConnectFuture await() throws InterruptedException;

    /**
     * {@inheritDoc}
     */
    public ConnectFuture awaitUninterruptibly();

    /**
     * {@inheritDoc}
     */
    public ConnectFuture addListener(IoFutureListener<?> listener);

    /**
     * {@inheritDoc}
     */
    public ConnectFuture removeListener(IoFutureListener<?> listener);
}
