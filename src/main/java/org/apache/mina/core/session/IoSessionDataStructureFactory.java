package org.apache.mina.core.session;

import java.util.Comparator;

import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestQueue;

/**
 * Provides data structures to a newly created session.
 * 
 * 新建session时提供数据结构的工厂
 * 
 * @date	2016年6月14日 上午11:43:13	completed
 */
public interface IoSessionDataStructureFactory {

	/**
     * @return an {@link IoSessionAttributeMap} which is going to be associated
     * with the specified <tt>session</tt>.  Please note that the returned
     * implementation must be thread-safe.
     * 
     * 返回将要与session关联的IoSessionAttributeMap，实现类必须是线程安全的。
     * 
     * @param session The session for which we want the Attribute Map
     * @throws Exception If an error occured while retrieving the map
     */
    IoSessionAttributeMap getAttributeMap(IoSession session) throws Exception;

    /**
     * @return an {@link WriteRequest} which is going to be associated with
     * the specified <tt>session</tt>.  Please note that the returned
     * implementation must be thread-safe and robust enough to deal
     * with various messages types (even what you didn't expect at all),
     * especially when you are going to implement a priority queue which
     * involves {@link Comparator}.
     * 
     * 返回将要与session关联的WriteRequestQueue，实现类必须是线程安全，且能够高效处理各种message类型的数据，
     * 尤其是使用优先级队列(使用Comparator接口)来实现时。
     * 
     * @param session The session for which we want the WriteRequest queue
     * @throws Exception If an error occured while retrieving the queue
     */
    WriteRequestQueue getWriteRequestQueue(IoSession session) throws Exception;
}
