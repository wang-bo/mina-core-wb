package org.apache.mina.core.future;

import java.util.EventListener;

import org.apache.mina.core.session.IoSession;

/**
 * Something interested in being notified when the completion
 * of an asynchronous I/O operation : {@link IoFuture}. 
 * 
 * 监听IoFuture里异步I/O操作完成的监听器。
 * 
 * @param <F> 监听的IoFuture类型
 * 
 * @date	2016年6月15日 上午10:25:39	completed
 */
public interface IoFutureListener<F extends IoFuture> extends EventListener {
	
	/**
     * An {@link IoFutureListener} that closes the {@link IoSession} which is
     * associated with the specified {@link IoFuture}.
     * 
     * 当操作成功时关闭IoSession的监听器
     */
	IoFutureListener<IoFuture> CLOSE = new IoFutureListener<IoFuture>() {
		public void operationComplete(IoFuture future) {
			future.getSession().closeNow();
		}
	};

	/**
     * Invoked when the operation associated with the {@link IoFuture}
     * has been completed even if you add the listener after the completion.
     * 
     * 当IoFuture的操作完成时，触发监听器的此方法。如果监听器在操作完成后添加，则直接触发此方法。
     *
     * @param future  The source {@link IoFuture} which called this
     *                callback.
     */
	public void operationComplete(F future);
}
