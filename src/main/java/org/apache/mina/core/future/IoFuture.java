package org.apache.mina.core.future;

import java.util.concurrent.TimeUnit;

import org.apache.mina.core.session.IoSession;

/**
 * Represents the completion of an asynchronous I/O operation on an 
 * {@link IoSession}.
 * Can be listened for completion using a {@link IoFutureListener}.
 * 
 * 表示对IoSession异步I/O操作的结果。
 * 可以使用IoFutureListener监听操作完成事件。
 * 
 * @date	2016年6月15日 上午9:33:12	completed
 */
public interface IoFuture {

	/**
     * @return the {@link IoSession} which is associated with this future.
     * 
     * 返回此future关联的IoSession。
     */
	public IoSession getSession();
	
	/**
     * Wait for the asynchronous operation to complete.
     * The attached listeners will be notified when the operation is 
     * completed.
     * 
     * 等待异步操作完成，如果操作完成，监听器会收到通知。
     * 
     * @return The instance of IoFuture that we are waiting for
     * @exception InterruptedException If the thread is interrupted while waiting
     */
	public IoFuture await() throws InterruptedException;
	
	/**
     * Wait for the asynchronous operation to complete with the specified timeout.
     * 
     * 在指定超时时间内等待异步操作完成。
     *
     * @param timeout The maximum delay to wait before getting out
     * @param unit the type of unit for the delay (seconds, minutes...)
     * @return <tt>true</tt> if the operation is completed. 
     * @exception InterruptedException If the thread is interrupted while waiting
     */
	public boolean await(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Wait for the asynchronous operation to complete with the specified timeout.
     * 
     * 在指定超时时间(毫秒)内等待异步操作完成。
     *
     * @param timeoutMillis The maximum milliseconds to wait before getting out
     * @return <tt>true</tt> if the operation is completed.
     * @exception InterruptedException If the thread is interrupted while waiting
     */
	public boolean await(long timeoutMillis) throws InterruptedException;

    /**
     * Wait for the asynchronous operation to complete uninterruptibly.
     * The attached listeners will be notified when the operation is 
     * completed.
     * 
     * 等待异步操作完成(不被中断)，如果操作完成，监听器会收到通知。
     * 
     * @return the current IoFuture
     */
	public IoFuture awaitUninterruptibly();

    /**
     * Wait for the asynchronous operation to complete with the specified timeout
     * uninterruptibly.
     * 
     * 在指定超时时间内等待异步操作完成(不被中断)。
     *
     * @param timeout The maximum delay to wait before getting out
     * @param unit the type of unit for the delay (seconds, minutes...)
     * @return <tt>true</tt> if the operation is completed.
     */
	public boolean awaitUninterruptibly(long timeout, TimeUnit unit);

    /**
     * Wait for the asynchronous operation to complete with the specified timeout
     * uninterruptibly.
     * 
     * 在指定超时时间(毫秒)内等待异步操作完成(不被中断)。
     *
     * @param timeoutMillis The maximum milliseconds to wait before getting out
     * @return <tt>true</tt> if the operation is finished.
     */
	public boolean awaitUninterruptibly(long timeoutMillis);
	
	/**
     * @deprecated Replaced with {@link #awaitUninterruptibly()}.
     * 
     * 已过时，使用awaitUninterruptibly()方法代替。
     */
	@Deprecated
	public void join();
	
	/**
     * @deprecated Replaced with {@link #awaitUninterruptibly(long)}.
     * 
     * 已过时，使用awaitUninterruptibly(long)方法代替。
     * 
     * @param timeoutMillis The time to wait for the join before bailing out
     * @return <tt>true</tt> if the join was successful
     */
    @Deprecated
    public boolean join(long timeoutMillis);
    
    /**
     * @return <tt>true</tt> if the operation is completed.
     * 
     * 操作是否已完成。
     */
    public boolean isDone();

    /**
     * Adds an event <tt>listener</tt> which is notified when
     * this future is completed. If the listener is added
     * after the completion, the listener is directly notified.
     * 
     * 添加一个监听器，当操作完成时通知监听器。
     * 如果监听器在操作完成后添加，则直接得到通知。
     * 
     * @param listener The listener to add
     * @return the current IoFuture
     */
    public IoFuture addListener(IoFutureListener<?> listener);

    /**
     * Removes an existing event <tt>listener</tt> so it won't be notified when
     * the future is completed.
     * 
     * 移除已存在的监听器。操作完成时将得不到通知。
     * 
     * @param listener The listener to remove
     * @return the current IoFuture
     */
    public IoFuture removeListener(IoFutureListener<?> listener);
}
