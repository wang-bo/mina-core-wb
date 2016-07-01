package org.apache.mina.core.session;

import org.apache.mina.core.future.IoFuture;

/**
 * Defines a callback for obtaining the {@link IoSession} during
 * session initialization.
 * 
 * 初始化IoSession的类
 * 
 * @param <F> 
 * 
 * @date	2016年6月15日 下午2:51:20	completed
 */
public interface IoSessionInitializer<F extends IoFuture> {

	/**
	 * 初始化IoSession
	 * @param session
	 * @param future
	 */
	public void initializeSession(IoSession session, F future);
}
