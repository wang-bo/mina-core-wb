package org.apache.mina.core.filterchain;

import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

/**
 * An adapter class for {@link IoFilter}.  You can extend
 * this class and selectively override required event filter methods only.  All
 * methods forwards events to the next filter by default.
 * 
 * IoFilter的适配器类。
 * 自定义IoFilter可以继承此类，然后有选择的覆盖感兴趣的方法，所有方法默认会将事件传递给过滤器链中的下一个IoFilter。
 * 
 * @date	2016年6月14日 下午2:24:19	completed
 */
public class IoFilterAdapter implements IoFilter {

	/**
     * {@inheritDoc}
     */
	public void init() throws Exception {

	}

	/**
     * {@inheritDoc}
     */
	public void destroy() throws Exception {

	}

	/**
     * {@inheritDoc}
     */
	public void onPreAdd(IoFilterChain parent, String name,
			NextFilter nextFilter) throws Exception {

	}

	/**
     * {@inheritDoc}
     */
	public void onPostAdd(IoFilterChain parent, String name,
			NextFilter nextFilter) throws Exception {

	}

	/**
     * {@inheritDoc}
     */
	public void onPreRemove(IoFilterChain parent, String name,
			NextFilter nextFilter) throws Exception {

	}

	/**
     * {@inheritDoc}
     */
	public void onPostRemove(IoFilterChain parent, String name,
			NextFilter nextFilter) throws Exception {

	}

	/**
     * {@inheritDoc}
     */
	public void sessionCreated(NextFilter nextFilter, IoSession session)
			throws Exception {
		nextFilter.sessionCreated(session);
	}

	/**
     * {@inheritDoc}
     */
	public void sessionOpened(NextFilter nextFilter, IoSession session)
			throws Exception {
		nextFilter.sessionOpened(session);
	}

	/**
     * {@inheritDoc}
     */
	public void sessionClosed(NextFilter nextFilter, IoSession session)
			throws Exception {
		nextFilter.sessionClosed(session);
	}

	/**
     * {@inheritDoc}
     */
	public void sessionIdle(NextFilter nextFilter, IoSession session,
			IdleStatus status) throws Exception {
		nextFilter.sessionIdle(session, status);
	}

	/**
     * {@inheritDoc}
     */
	public void exceptionCaught(NextFilter nextFilter, IoSession session,
			Throwable cause) throws Exception {
		nextFilter.exceptionCaught(session, cause);
	}

	/**
     * {@inheritDoc}
     */
	public void inputClosed(NextFilter nextFilter, IoSession session)
			throws Exception {
		nextFilter.inputClosed(session);
	}

	/**
     * {@inheritDoc}
     */
	public void messageReceived(NextFilter nextFilter, IoSession session,
			Object message) throws Exception {
		nextFilter.messageReceived(session, message);
	}

	/**
     * {@inheritDoc}
     */
	public void messageSent(NextFilter nextFilter, IoSession session,
			WriteRequest writeRequest) throws Exception {
		nextFilter.messageSent(session, writeRequest);
	}

	/**
     * {@inheritDoc}
     */
	public void filterClose(NextFilter nextFilter, IoSession session)
			throws Exception {
		nextFilter.filterClose(session);
	}

	/**
     * {@inheritDoc}
     */
	public void filterWrite(NextFilter nextFilter, IoSession session,
			WriteRequest writeRequest) throws Exception {
		nextFilter.filterWrite(session, writeRequest);
	}

}
