package org.apache.mina.core.service;

import java.util.Map;
import java.util.Set;

import org.apache.mina.core.IoUtil;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.filterchain.IoFilterChainBuilder;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.core.session.IoSessionDataStructureFactory;

/**
 * Base interface for all {@link IoAcceptor}s and {@link IoConnector}s
 * that provide I/O service and manage {@link IoSession}s.
 * 
 * IoAcceptor和 IoConnector的基类接口，提供I/O服务、管理IoSession。
 * 
 * @date	2016年5月27日 下午5:25:38	completed
 */
public interface IoService {

	/**
	 * @return the {@link TransportMetadata} that this service runs on.
	 * 
	 * 返回描述service的元数据
	 */
	public TransportMetadata getTransportMetadata();
	
	/**
     * Adds an {@link IoServiceListener} that listens any events related with
     * this service.
     * 
     * 新增一个监听器，监听当前server上发生的事件
     * 
     * @param listener The listener to add
     */
	public void addListener(IoServiceListener listener);
	
	/**
     * Removed an existing {@link IoServiceListener} that listens any events
     * related with this service.
     * 
     * 移除一个已存在的监听器
     * 
     * @param listener The listener to use
     */
	public void removeListener(IoServiceListener listener);
	
	/**
     * @return <tt>true</tt> if and if only {@link #dispose()} method has
     * been called.  Please note that this method will return <tt>true</tt>
     * even after all the related resources are released.
     * 
     * 当dispose()方法被调用中返回true，注意：就算相关资源释放后这方法也会返回true。
     */
    public boolean isDisposing();
    
    /**
     * @return <tt>true</tt> if and if only all resources of this processor
     * have been disposed.
     * 
     * 当前processor上所有资源被释放后返回true。
     */
    public boolean isDisposed();

    /**
     * Releases any resources allocated by this service.  Please note that
     * this method might block as long as there are any sessions managed by
     * this service.
     * 
     * 释放当前service分配的任何资源。注意：当前service上有session时，这方法会阻塞。
     */
    public void dispose();
    
    /**
     * Releases any resources allocated by this service.  Please note that
     * this method might block as long as there are any sessions managed by this service.
     *
     * Warning : calling this method from a IoFutureListener with <code>awaitTermination</code> = true
     * will probably lead to a deadlock.
     * 
     * 释放当前service分配的任何资源。注意：当前service上有session时，这方法会阻塞。
     * 警告：从IoFutureListener中调用本方法时，可能会产生死锁。
     * 如果参数为true，则此方法将阻塞直到IoService里的I/O事件线程执行器Executor终止。
     *
     * @param awaitTermination When true this method will block until the underlying ExecutorService is terminated
     */
    public void dispose(boolean awaitTermination);
    
    /**
     * @return the handler which will handle all connections managed by this service.
     * 
     * 返回处理当前service上所有连接的处理器
     */
    public IoHandler getHandler();

    /**
     * Sets the handler which will handle all connections managed by this service.
     * 
     * 设置处理器，用来处理当前service上所有连接
     * 
     * @param handler The IoHandler to use
     */
    public void setHandler(IoHandler handler);
    
    /**
     * @return the map of all sessions which are currently managed by this
     * service.  The key of map is the {@link IoSession#getId() ID} of the
     * session. An empty collection if there's no session.
     * 
     * 返回当前service管理的所有session，key=IoSession.getId()。如果一个session都没有的话返回空的Map
     */
    public Map<Long, IoSession> getManagedSessions();
    
    /**
     * @return the number of all sessions which are currently managed by this
     * service.
     * 
     * 返回当前service管理的session数量
     */
    public int getManagedSessionCount();
    
    /**
     * @return the default configuration of the new {@link IoSession}s
     * created by this service.
     * 
     * 返回当前service上创建session时的默认session config
     */
    public IoSessionConfig getSessionConfig();
    
    /**
     * @return the {@link IoFilterChainBuilder} which will build the
     * {@link IoFilterChain} of all {@link IoSession}s which is created
     * by this service.
     * The default value is an empty {@link DefaultIoFilterChainBuilder}.
     * 
     * 返回生成IoFilterChain的IoFilterChainBuilder
     */
    public IoFilterChainBuilder getFilterChainBuilder();
    
    /**
     * Sets the {@link IoFilterChainBuilder} which will build the
     * {@link IoFilterChain} of all {@link IoSession}s which is created
     * by this service.
     * If you specify <tt>null</tt> this property will be set to
     * an empty {@link DefaultIoFilterChainBuilder}.
     * 
     * 设置生成IoFilterChain的IoFilterChainBuilder，
     * 如果你在这里设置null，实际会设置1个空的DefaultIoFilterChainBuilder实例
     * 
     * @param builder The filter chain builder to use
     */
    public void setFilterChainBuilder(IoFilterChainBuilder builder);
    
    /**
     * A shortcut for <tt>( ( DefaultIoFilterChainBuilder ) </tt>{@link #getFilterChainBuilder()}<tt> )</tt>.
     * Please note that the returned object is not a <b>real</b> {@link IoFilterChain}
     * but a {@link DefaultIoFilterChainBuilder}.  Modifying the returned builder
     * won't affect the existing {@link IoSession}s at all, because
     * {@link IoFilterChainBuilder}s affect only newly created {@link IoSession}s.
     * 
     * getFilterChainBuilder()方法的快捷方式，这个方法的返回值不是1个IoFilterChain而是1个DefaultIoFilterChainBuilder。
     * 修改这个方法返回的DefaultIoFilterChainBuilder不会影响已经存在的IoSession，因为IoFilterChainBuilder只在创建IoSession使用。
     *
     * @return The filter chain in use
     * @throws IllegalStateException if the current {@link IoFilterChainBuilder} is
     *                               not a {@link DefaultIoFilterChainBuilder}
     */
    public DefaultIoFilterChainBuilder getFilterChain();
    
    /**
     * @return a value of whether or not this service is active
     * 
     * 返回当前service是否活动
     */
    public boolean isActive();
    
    /**
     * @return the time when this service was activated.  It returns the last
     * time when this service was activated if the service is not active now.
     * 
     * 返回当前service活动的时间，如果当前server不活动，就返回上一次活动时间
     */
    public long getActivationTime();
    
    /**
     * Writes the specified {@code message} to all the {@link IoSession}s
     * managed by this service.  This method is a convenience shortcut for
     * {@link IoUtil#broadcast(Object, Collection)}.
     * 
     * 给当前service上的所有session发指定消息。
     * 这个方法是IoUtil.broadcast()方法的快捷方式
     * 
     * @param message the message to broadcast
     * @return The set of WriteFuture associated to the message being broadcasted
     */
    public Set<WriteFuture> broadcast(Object message);
    
    /**
     * @return the {@link IoSessionDataStructureFactory} that provides
     * related data structures for a new session created by this service.
     * 
     * 返回当前service创建session时，用来提供关联数据结构的IoSessionDataStructureFactory
     */
    public IoSessionDataStructureFactory getSessionDataStructureFactory();
    
    /**
     * Sets the {@link IoSessionDataStructureFactory} that provides
     * related data structures for a new session created by this service.
     * 
     * 设置当前service创建session时，用来提供关联数据结构的IoSessionDataStructureFactory
     * 
     * @param sessionDataStructureFactory The factory to use
     */
    public void setSessionDataStructureFactory(IoSessionDataStructureFactory sessionDataStructureFactory);
    
    /**
     * @return The number of bytes scheduled to be written
     * 
     * 返回待写的字节数
     */
    public int getScheduledWriteBytes();

    /**
     * @return The number of messages scheduled to be written
     * 返回待写的消息数
     */
    public int getScheduledWriteMessages();

    /**
     * @return The statistics object for this service.
     * 
     * 返回用来给service做统计的对象
     */
    public IoServiceStatistics getStatistics();
}
