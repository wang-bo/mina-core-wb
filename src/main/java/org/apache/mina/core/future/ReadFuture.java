package org.apache.mina.core.future;

import org.apache.mina.core.session.IoSession;

/**
 * An {@link IoFuture} for {@link IoSession#read() asynchronous read requests}. 
 *
 * <h3>Example</h3>
 * <pre>
 * IoSession session = ...;
 * 
 * // useReadOperation must be enabled to use read operation.
 * session.getConfig().setUseReadOperation(true);
 * 
 * ReadFuture future = session.read();
 * 
 * // Wait until a message is received.
 * future.awaitUninterruptibly();
 * 
 * try {
 *     Object message = future.getMessage();
 * } catch (Exception e) {
 *     ...
 * }
 * </pre>
 * 
 * IoSession读操作的IoFuture
 * 
 * @date	2016年6月15日 上午10:00:43	completed
 */
public interface ReadFuture extends IoFuture {

	/**
     * Get the read message.
     * 
     * 返回IoSession读到的Message。如果读取操作未完成或关联的IoSession已关闭则返回null。
     * 
     * @return the received message.  It returns <tt>null</tt> if this
     * future is not ready or the associated {@link IoSession} has been closed. 
     */
    public Object getMessage();

    /**
     * @return <tt>true</tt> if a message was received successfully.
     * 
     * 如果Message已成功到达，返回true。
     */
    public boolean isRead();

    /**
     * @return <tt>true</tt> if the {@link IoSession} associated with this
     * future has been closed.
     * 
     * 如果关联的IoSession已关闭，返回true。
     */
    public boolean isClosed();

    /**
     * @return the cause of the read failure if and only if the read
     * operation has failed due to an {@link Exception}.  Otherwise,
     * <tt>null</tt> is returned.
     * 
     * 如果读取操作发生异常，返回导致失败的异常对象，否则返回null。
     */
    public Throwable getException();

    /**
     * Sets the message is written, and notifies all threads waiting for
     * this future.  This method is invoked by MINA internally.  Please do
     * not call this method directly.
     * 
     * 设置读取到的Message，并通知所有等待此future的线程。
     * 这个方法是Mina内部使用的，请不要直接调用此方法。
     * 
     * @param message The received message to store in this future
     */
    public void setRead(Object message);

    /**
     * Sets the associated {@link IoSession} is closed.  This method is invoked
     * by MINA internally.  Please do not call this method directly.
     * 
     * 设置关联的IoSession已关闭，并通知所有等待此future的线程。
     * 这个方法是Mina内部使用的，请不要直接调用此方法。
     */
    public void setClosed();

    /**
     * Sets the cause of the read failure, and notifies all threads waiting
     * for this future.  This method is invoked by MINA internally.  Please
     * do not call this method directly.
     * 
     * 设置导致读取失败的异常对象，并通知所有等待此future的线程。
     * 这个方法是Mina内部使用的，请不要直接调用此方法。
     * 
     * @param cause The exception to store in the Future instance
     */
    public void setException(Throwable cause);
    
    /**
     * {@inheritDoc}
     */
    public ReadFuture await() throws InterruptedException;

    /**
     * {@inheritDoc}
     */
    public ReadFuture awaitUninterruptibly();

    /**
     * {@inheritDoc}
     */
    public ReadFuture addListener(IoFutureListener<?> listener);

    /**
     * {@inheritDoc}
     */
    public ReadFuture removeListener(IoFutureListener<?> listener);
}
