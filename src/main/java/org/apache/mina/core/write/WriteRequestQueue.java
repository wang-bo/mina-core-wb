package org.apache.mina.core.write;

import org.apache.mina.core.session.IoSession;

/**
 * Stores {@link WriteRequest}s which are queued to an {@link IoSession}.
 * 
 * 用来保存IoSession里写请求的队列。
 * 
 * @date	2016年6月16日 上午10:26:09	completed
 */
public interface WriteRequestQueue {

	/**
     * Get the first request available in the queue for a session.
     * 
     * 返回IoSession的写请求队列里的第一个有效写请求。
     * 
     * @param session The session
     * @return The first available request, if any.
     */
	public WriteRequest poll(IoSession session);

    /**
     * Add a new WriteRequest to the session write's queue
     * 
     * 添加一个新的写请求到IoSession的写请求队列里。
     * 
     * @param session The session
     * @param writeRequest The writeRequest to add
     */
	public void offer(IoSession session, WriteRequest writeRequest);

    /**
     * Tells if the WriteRequest queue is empty or not for a session
     * 
     * 返回IoSession的写请求队列是否为空。
     * 
     * @param session The session to check
     * @return <tt>true</tt> if the writeRequest is empty
     */
	public boolean isEmpty(IoSession session);

    /**
     * Removes all the requests from this session's queue.
     * 
     * 请求IoSession的写请求队列里所有的写请求。
     * 
     * @param session The associated session
     */
	public void clear(IoSession session);

    /**
     * Disposes any releases associated with the specified session.
     * This method is invoked on disconnection.
     * 
     * 清理指定IoSession的关联的所有资源，这个方法在IoSession关闭连接的时候调用。
     * 
     * @param session The associated session
     */
	public void dispose(IoSession session);

    /**
     * @return the number of objects currently stored in the queue.
     * 
     * 返回当前写请求队列的大小(即所有写请求的数量)。
     */
	public int size();
}
