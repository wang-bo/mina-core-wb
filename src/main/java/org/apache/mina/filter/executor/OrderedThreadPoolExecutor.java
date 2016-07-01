package org.apache.mina.filter.executor;

import java.util.concurrent.ThreadPoolExecutor;

import org.apache.mina.core.session.IoEvent;

/**
 * A {@link ThreadPoolExecutor} that maintains the order of {@link IoEvent}s.
 * <p>
 * If you don't need to maintain the order of events per session, please use
 * {@link UnorderedThreadPoolExecutor}.
 * 
 * 一个维护I/O事件顺序执行的线程池。
 * 如果无需维护I/O事件的顺序，请使用UnorderedThreadPoolExecutor。
 * 
 * @date	2016年6月27日 下午4:19:09
 */
public class OrderedThreadPoolExecutor extends ThreadPoolExecutor {

}
