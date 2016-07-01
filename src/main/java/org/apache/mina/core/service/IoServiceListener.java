package org.apache.mina.core.service;

import java.util.EventListener;

import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

/**
 * Listens to events related to an {@link IoService}.
 * 
 * 相关service的事件监听器
 * 
 * @date	2016年5月30日 上午9:58:28	completed
 */
public interface IoServiceListener extends EventListener {

	/**
	 * Invoked when a new service is activated by an {@link IoService}.
	 * 
	 * 当service活动的时候触发
	 * 
	 * @param service the {@link IoService}
	 * @throws Exception if an error occurred while the service is being activated
	 */
	public void serviceActivated(IoService service) throws Exception;
	
	/**
     * Invoked when a service is idle.
     * 
     * 当service空闲的时候触发
     * 
     * @param service the {@link IoService}
     * @param idleStatus The idle status
     * @throws Exception if an error occurred while the service is being idled
     */
	public void serviceIdle(IoService service, IdleStatus idleStatus) throws Exception;
	
	/**
     * Invoked when a service is deactivated by an {@link IoService}.
     * 
     * 当service不活动时触发
     *
     * @param service the {@link IoService}
     * @throws Exception if an error occurred while the service is being deactivated
     */
	public void serviceDeactivated(IoService service) throws Exception;
	
	/**
     * Invoked when a new session is created by an {@link IoService}.
     * 
     * 当service有session创建时触发
     *
     * @param session the new session
     * @throws Exception if an error occurred while the session is being created
     */
	public void sessionCreated(IoSession session) throws Exception;
	
	/**
     * Invoked when a new session is closed by an {@link IoService}.
     * 
     * 当service有session关闭时触发
     * 
     * @param session the new session
     * @throws Exception if an error occurred while the session is being closed
     */
    public void sessionClosed(IoSession session) throws Exception;

    /**
     * Invoked when a session is being destroyed by an {@link IoService}.
     * 
     * 当service有session销毁时触发
     * 
     * @param session the session to be destroyed
     * @throws Exception if an error occurred while the session is being destroyed
     */
    public void sessionDestroyed(IoSession session) throws Exception;
}
