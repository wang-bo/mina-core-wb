package org.apache.mina.core.filterchain;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.util.ReferenceCountingFilter;

/**
 * A filter which intercepts {@link IoHandler} events like Servlet
 * filters.  Filters can be used for these purposes:
 * <ul>
 *   <li>Event logging,</li>
 *   <li>Performance measurement,</li>
 *   <li>Authorization,</li>
 *   <li>Overload control,</li>
 *   <li>Message transformation (e.g. encryption and decryption, ...),</li>
 *   <li>and many more.</li>
 * </ul>
 * <p>
 * <strong>Please NEVER implement your filters to wrap
 * {@link IoSession}s.</strong> Users can cache the reference to the
 * session, which might malfunction if any filters are added or removed later.
 *
 * <h3>The Life Cycle</h3>
 * {@link IoFilter}s are activated only when they are inside {@link IoFilterChain}.
 * <p>
 * When you add an {@link IoFilter} to an {@link IoFilterChain}:
 * <ol>
 *   <li>{@link #init()} is invoked by {@link ReferenceCountingFilter} if
 *       the filter is added at the first time.</li>
 *   <li>{@link #onPreAdd(IoFilterChain, String, NextFilter)} is invoked to notify
 *       that the filter will be added to the chain.</li>
 *   <li>The filter is added to the chain, and all events and I/O requests
 *       pass through the filter from now.</li>
 *   <li>{@link #onPostAdd(IoFilterChain, String, NextFilter)} is invoked to notify
 *       that the filter is added to the chain.</li>
 *   <li>The filter is removed from the chain if {@link #onPostAdd(IoFilterChain, String, org.apache.mina.core.filterchain.IoFilter.NextFilter)}
 *       threw an exception.  {@link #destroy()} is also invoked by
 *       {@link ReferenceCountingFilter} if the filter is the last filter which
 *       was added to {@link IoFilterChain}s.</li>
 * </ol>
 * <p>
 * When you remove an {@link IoFilter} from an {@link IoFilterChain}:
 * <ol>
 *   <li>{@link #onPreRemove(IoFilterChain, String, NextFilter)} is invoked to
 *       notify that the filter will be removed from the chain.</li>
 *   <li>The filter is removed from the chain, and any events and I/O requests
 *       don't pass through the filter from now.</li>
 *   <li>{@link #onPostRemove(IoFilterChain, String, NextFilter)} is invoked to
 *       notify that the filter is removed from the chain.</li>
 *   <li>{@link #destroy()} is invoked by {@link ReferenceCountingFilter} if
 *       the removed filter was the last one.</li>
 * </ol>
 * 
 * 1. 用来拦截IoHandler事件的过滤器，就像Servlet过滤器拦截Servlet事件一样。过滤器可以用来实现以下功能：
 * 	a. 事件日志
 * 	b. 性能统计
 * 	c. 身份认证
 * 	d. 重载控制方法
 * 	e. 消息转换(例：加密、解密消息)
 * 	f. 其它
 * 2. 不要包装IoSession的实例来实现本接口，在IoFilter接口实例里保存IoSession实例的引用的话，当filter被add或remove会有问题。
 * 3. 生命周期：IoFilter只有在IoFilterChain里时才是activated的。
 * 4. 将IoFilter放入IoFilterChain时：
 * 	a. 第1次放入过滤器链中时，ReferenceCountingFilter会调用过滤器的init()方法；
 * 	b. onPreAdd(IoFilterChain, String, NextFilter)会触发，通知过滤器将被放入过滤器链中；
 * 	c. 当过滤器放入过滤器链中，所有事件及I/O请求会经过过滤器；
 * 	d. onPostAdd(IoFilterChain, String, NextFilter)会触发，通知过滤器已被放入过滤器链中；
 * 	e. 如果onPostAdd(IoFilterChain, String, NextFilter)方法发生异常，过滤器会被从过滤器链中移除，如果是过滤器的最后
 * 		一个过滤器链，ReferenceCountingFilter会调用过滤器的destory()方法；
 * 5. 将IoFilter从IoFilterChain中移除时：
 * 	a. onPreRemove(IoFilterChain, String, NextFilter)会触发，通知过滤器将被从过滤器链中移除；
 * 	b. 过滤器被从过滤器链中移除，所有事件及I/O请求不会再经过过滤器；
 * 	c. onPostRemove(IoFilterChain, String, NextFilter)会触发，通知过滤器已经从过滤器链中移除；
 * 	d. 如果是过滤器的最后一个过滤器链，ReferenceCountingFilter会调用过滤器的destory()方法；
 * 
 * 
 * @date	2016年6月14日 上午11:55:14	completed
 */
public interface IoFilter {

	/**
     * Invoked by {@link ReferenceCountingFilter} when this filter
     * is added to a {@link IoFilterChain} at the first time, so you can
     * initialize shared resources.  Please note that this method is never
     * called if you don't wrap a filter with {@link ReferenceCountingFilter}.
     * 
     * 当前filter第1次放入过滤器链中时，被ReferenceCountingFilter调用，在这里可以做初始化共享资源的操作。
     * 注意：如果filter没有用ReferenceCountingFilter包装，则该方法永远不会被调用。
     * 
     * @throws Exception If an error occurred while processing the event
     */
    public void init() throws Exception;
    
    /**
     * Invoked by {@link ReferenceCountingFilter} when this filter
     * is not used by any {@link IoFilterChain} anymore, so you can destroy
     * shared resources.  Please note that this method is never called if
     * you don't wrap a filter with {@link ReferenceCountingFilter}.
     * 
     * 当前filter没有被任何过滤器链使用时，被ReferenceCountingFilter调用，在这里可以做释放共享资源的操作。
     * 注意：如果filter没有用ReferenceCountingFilter包装，则该方法永远不会被调用。
     * 
     * @throws Exception If an error occurred while processing the event
     */
    public void destroy() throws Exception;
    
    /**
     * Invoked before this filter is added to the specified <tt>parent</tt>.
     * Please note that this method can be invoked more than once if
     * this filter is added to more than one parents.  This method is not
     * invoked before {@link #init()} is invoked.
     * 
     * 当过滤器将被放入过滤器链中触发。这个方法不会早于init()方法被调用。
     * 注意：如果过滤器放入多个过滤器链中，这个方法会被触发多次。
     *
     * @param parent the parent who called this method
     * @param name the name assigned to this filter
     * @param nextFilter the {@link NextFilter} for this filter.  You can reuse
     *                   this object until this filter is removed from the chain.
     * @throws Exception If an error occurred while processing the event
     */
    public void onPreAdd(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception;

    /**
     * Invoked after this filter is added to the specified <tt>parent</tt>.
     * Please note that this method can be invoked more than once if
     * this filter is added to more than one parents.  This method is not
     * invoked before {@link #init()} is invoked.
     * 
     * 当过滤器被放入过滤器链中后触发。这个方法不会早于init()方法被调用。
     * 注意：如果过滤器放入多个过滤器链中，这个方法会被触发多次。
     *
     * @param parent the parent who called this method
     * @param name the name assigned to this filter
     * @param nextFilter the {@link NextFilter} for this filter.  You can reuse
     *                   this object until this filter is removed from the chain.
     * @throws Exception If an error occurred while processing the event
     */
    public void onPostAdd(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception;

    /**
     * Invoked before this filter is removed from the specified <tt>parent</tt>.
     * Please note that this method can be invoked more than once if
     * this filter is removed from more than one parents.
     * This method is always invoked before {@link #destroy()} is invoked.
     * 
     * 当过滤器将被从过滤器链中移除时触发。这个方法总是早于destroy()方法被调用。
     * 注意：如果过滤器放入多个过滤器链中，这个方法会被触发多次。
     *
     * @param parent the parent who called this method
     * @param name the name assigned to this filter
     * @param nextFilter the {@link NextFilter} for this filter.  You can reuse
     *                   this object until this filter is removed from the chain.
     * @throws Exception If an error occurred while processing the event
     */
    public void onPreRemove(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception;

    /**
     * Invoked after this filter is removed from the specified <tt>parent</tt>.
     * Please note that this method can be invoked more than once if
     * this filter is removed from more than one parents.
     * This method is always invoked before {@link #destroy()} is invoked.
     * 
     * 当过滤器从过滤器链中移除后触发。这个方法总是早于destroy()方法被调用。
     * 注意：如果过滤器放入多个过滤器链中，这个方法会被触发多次。
     *
     * @param parent the parent who called this method
     * @param name the name assigned to this filter
     * @param nextFilter the {@link NextFilter} for this filter.  You can reuse
     *                   this object until this filter is removed from the chain.
     * @throws Exception If an error occurred while processing the event
     */
    public void onPostRemove(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception;
    
    /**
     * Filters {@link IoHandler#sessionCreated(IoSession)} event.
     * 
     * 过滤IoHandler.sessionCreated(IoSession)事件。
     * 
     * @param nextFilter
     *            the {@link NextFilter} for this filter. You can reuse this
     *            object until this filter is removed from the chain.
     * @param session The {@link IoSession} which has received this event
     * @throws Exception If an error occurred while processing the event
     */
    public void sessionCreated(NextFilter nextFilter, IoSession session) throws Exception;
    
    /**
     * Filters {@link IoHandler#sessionOpened(IoSession)} event.
     * 
     * 过滤IoHandler.sessionOpened(IoSession)事件。
     * 
     * @param nextFilter
     *            the {@link NextFilter} for this filter. You can reuse this
     *            object until this filter is removed from the chain.
     * @param session The {@link IoSession} which has received this event
     * @throws Exception If an error occurred while processing the event
     */
    public void sessionOpened(NextFilter nextFilter, IoSession session) throws Exception;

    /**
     * Filters {@link IoHandler#sessionClosed(IoSession)} event.
     * 
     * 过滤IoHandler.sessionClosed(IoSession)事件。
     * 
     * @param nextFilter
     *            the {@link NextFilter} for this filter. You can reuse this
     *            object until this filter is removed from the chain.
     * @param session The {@link IoSession} which has received this event
     * @throws Exception If an error occurred while processing the event
     */
    public void sessionClosed(NextFilter nextFilter, IoSession session) throws Exception;

    /**
     * Filters {@link IoHandler#sessionIdle(IoSession,IdleStatus)} event.
     * 
     * 过滤IoHandler.sessionIdle(IoSession, IdleStatus)事件。
     * 
     * @param nextFilter
     *            the {@link NextFilter} for this filter. You can reuse this
     *            object until this filter is removed from the chain.
     * @param session The {@link IoSession} which has received this event
     * @param status The {@link IdleStatus} type
     * @throws Exception If an error occurred while processing the event
     */
    public void sessionIdle(NextFilter nextFilter, IoSession session, IdleStatus status) throws Exception;

    /**
     * Filters {@link IoHandler#exceptionCaught(IoSession,Throwable)} event.
     * 
     * 过滤IoHandler.exceptionCaught(IoSession, Throwable)事件。
     * 
     * @param nextFilter
     *            the {@link NextFilter} for this filter. You can reuse this
     *            object until this filter is removed from the chain.
     * @param session The {@link IoSession} which has received this event
     * @param cause The exception that cause this event to be received
     * @throws Exception If an error occurred while processing the event
     */
    public void exceptionCaught(NextFilter nextFilter, IoSession session, Throwable cause) throws Exception;

    /**
     * Filters {@link IoHandler#inputClosed(IoSession)} event.
     * 
     * 过滤IoHandler.inputClosed(IoSession)事件。
     * 
     * @param nextFilter
     *            the {@link NextFilter} for this filter. You can reuse this
     *            object until this filter is removed from the chain.
     * @param session The {@link IoSession} which has received this event
     * @throws Exception If an error occurred while processing the event
     */
    public void inputClosed(NextFilter nextFilter, IoSession session) throws Exception;

    /**
     * Filters {@link IoHandler#messageReceived(IoSession,Object)} event.
     * 
     * 过滤IoHandler.messageReceived(IoSession, Object)事件。
     * 
     * @param nextFilter
     *            the {@link NextFilter} for this filter. You can reuse this
     *            object until this filter is removed from the chain.
     * @param session The {@link IoSession} which has received this event
     * @param message The received message
     * @throws Exception If an error occurred while processing the event
     */
    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception;

    /**
     * Filters {@link IoHandler#messageSent(IoSession,Object)} event.
     * 
     * 过滤IoHandler.messageSent(IoSession, Object)事件。
     * 
     * @param nextFilter
     *            the {@link NextFilter} for this filter. You can reuse this
     *            object until this filter is removed from the chain.
     * @param session The {@link IoSession} which has received this event
     * @param writeRequest The {@link WriteRequest} that contains the sent message
     * @throws Exception If an error occurred while processing the event
     */
    public void messageSent(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception;

    /**
     * Filters {@link IoSession#close(boolean)} method invocation.
     * 
     * 过滤IoSession.close(boolean)方法。
     * 
     * @param nextFilter
     *            the {@link NextFilter} for this filter. You can reuse this
     *            object until this filter is removed from the chain.
     * @param session
     *            The {@link IoSession} which has to process this method
     *            invocation
     * @throws Exception If an error occurred while processing the event
     */
    public void filterClose(NextFilter nextFilter, IoSession session) throws Exception;

    /**
     * Filters {@link IoSession#write(Object)} method invocation.
     * 
     * 过滤IoSession.write(Object)方法。
     * 
     * @param nextFilter
     *            the {@link NextFilter} for this filter. You can reuse this
     *            object until this filter is removed from the chain.
     * @param session The {@link IoSession} which has to process this invocation
     * @param writeRequest The {@link WriteRequest} to process
     * @throws Exception If an error occurred while processing the event
     */
    public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception;
    
    /**
     * Represents the next {@link IoFilter} in {@link IoFilterChain}.
     * 
     * 代表过滤器链中的下一个IoFilter
     */
    interface NextFilter {
    	
    	/**
         * Forwards <tt>sessionCreated</tt> event to next filter.
         * 
         * 传递sessionCreated事件给下一个filter。
         * 
         * @param session The {@link IoSession} which has to process this invocation
         */
    	public void sessionCreated(IoSession session);

        /**
         * Forwards <tt>sessionOpened</tt> event to next filter.
         * 
         * 传递sessionOpened事件给下一个filter。
         * 
         * @param session The {@link IoSession} which has to process this invocation
         */
    	public void sessionOpened(IoSession session);

        /**
         * Forwards <tt>sessionClosed</tt> event to next filter.
         * 
         * 传递sessionClosed事件给下一个filter。
         * 
         * @param session The {@link IoSession} which has to process this invocation
         */
    	public void sessionClosed(IoSession session);

        /**
         * Forwards <tt>sessionIdle</tt> event to next filter.
         * 
         * 传递sessionIdle事件给下一个filter。
         * 
         * @param session The {@link IoSession} which has to process this invocation
         * @param status The {@link IdleStatus} type
         */
    	public void sessionIdle(IoSession session, IdleStatus status);

        /**
         * Forwards <tt>exceptionCaught</tt> event to next filter.
         * 
         * 传递exceptionCaught事件给下一个filter。
         * 
         * @param session The {@link IoSession} which has to process this invocation
         * @param cause The exception that cause this event to be received
         */
    	public void exceptionCaught(IoSession session, Throwable cause);

        /**
         * 
         * 传递inputClosed事件给下一个filter。
         * 
         * @param session The {@link IoSession} which has to process this invocation
         * 
         */
    	public void inputClosed(IoSession session);

        /**
         * Forwards <tt>messageReceived</tt> event to next filter.
         * 
         * 传递messageReceived事件给下一个filter。
         * 
         * @param session The {@link IoSession} which has to process this invocation
         * @param message The received message
         */
    	public void messageReceived(IoSession session, Object message);

        /**
         * Forwards <tt>messageSent</tt> event to next filter.
         * 
         * 传递messageSent事件给下一个filter。
         * 
         * @param session The {@link IoSession} which has to process this invocation
         * @param writeRequest The {@link WriteRequest} to process
         */
    	public void messageSent(IoSession session, WriteRequest writeRequest);

        /**
         * Forwards <tt>filterWrite</tt> event to next filter.
         * 
         * 传递filterWrite事件给下一个filter。
         * 
         * @param session The {@link IoSession} which has to process this invocation
         * @param writeRequest The {@link WriteRequest} to process
         */
    	public void filterWrite(IoSession session, WriteRequest writeRequest);

        /**
         * Forwards <tt>filterClose</tt> event to next filter.
         * 
         * 传递filterClose事件给下一个filter。
         * 
         * @param session The {@link IoSession} which has to process this invocation
         */
    	public void filterClose(IoSession session);
    }
}
