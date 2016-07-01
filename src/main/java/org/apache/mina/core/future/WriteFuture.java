package org.apache.mina.core.future;

/**
 * An {@link IoFuture} for asynchronous write requests.
 *
 * <h3>Example</h3>
 * <pre>
 * IoSession session = ...;
 * WriteFuture future = session.write(...);
 * 
 * // Wait until the message is completely written out to the O/S buffer.
 * future.awaitUninterruptibly();
 * 
 * if( future.isWritten() )
 * {
 *     // The message has been written successfully.
 * }
 * else
 * {
 *     // The message couldn't be written out completely for some reason.
 *     // (e.g. Connection is closed)
 * }
 * </pre>
 * 
 * IoSession写操作的IoFuture
 * 
 * @date	2016年6月15日 上午10:16:20	completed
 */
public interface WriteFuture extends IoFuture {

	/**
     * @return <tt>true</tt> if the write operation is finished successfully.
     * 
     * 返回写操作是否已成功。
     */
    public boolean isWritten();

    /**
     * @return the cause of the write failure if and only if the write
     * operation has failed due to an {@link Exception}.  Otherwise,
     * <tt>null</tt> is returned.
     * 
     * 如果写入操作发生异常，返回导致失败的异常对象，否则返回null。
     */
    public Throwable getException();

    /**
     * Sets the message is written, and notifies all threads waiting for
     * this future.  This method is invoked by MINA internally.  Please do
     * not call this method directly.
     * 
     * 设置写操作已成功，并通知所有等待此future的线程。
     * 这个方法是Mina内部使用的，请不要直接调用此方法。
     */
    public void setWritten();

    /**
     * Sets the cause of the write failure, and notifies all threads waiting
     * for this future.  This method is invoked by MINA internally.  Please
     * do not call this method directly.
     * 
     * 设置导致写入失败的异常对象，并通知所有等待此future的线程。
     * 这个方法是Mina内部使用的，请不要直接调用此方法。
     * 
     * @param cause The exception to store in the Future instance
     */
    public void setException(Throwable cause);
    
    /**
     * {@inheritDoc}
     */
    public WriteFuture await() throws InterruptedException;

    /**
     * {@inheritDoc}
     */
    public WriteFuture awaitUninterruptibly();

    /**
     * {@inheritDoc}
     */
    public WriteFuture addListener(IoFutureListener<?> listener);

    /**
     * {@inheritDoc}
     */
    public WriteFuture removeListener(IoFutureListener<?> listener);
}
