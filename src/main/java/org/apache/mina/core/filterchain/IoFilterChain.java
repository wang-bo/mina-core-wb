package org.apache.mina.core.filterchain;

import java.util.List;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

/**
 * A container of {@link IoFilter}s that forwards {@link IoHandler} events
 * to the consisting filters and terminal {@link IoHandler} sequentially.
 * Every {@link IoSession} has its own {@link IoFilterChain} (1-to-1 relationship).
 * 
 * 过滤器链：一个IoFilter的容器，传递IoHandler事件给所有filter。
 * 每个IoSession有一个独立的IoFilterChain，即两者间是1对1的关系。
 * 
 * @date	2016年6月14日 下午2:32:34	completed
 */
public interface IoFilterChain {

	/**
     * @return the parent {@link IoSession} of this chain.
     * 
     * 返回拥有当前IoFilterChain的IoSession。
     */
	public IoSession getSession();
	
	/**
     * Returns the {@link Entry} with the specified <tt>name</tt> in this chain.
     * 
     * 返回当前IoFilterChain中指定名称对应的Entry。
     * 
     * @param name The filter's name we are looking for
     * @return <tt>null</tt> if there's no such name in this chain
     */
	public Entry getEntry(String name);
	
	/**
     * Returns the {@link Entry} with the specified <tt>filter</tt> in this chain.
     * 
     * 返回当前IoFilterChain中指定filter对应的Entry。
     * 
     * @param filter  The Filter we are looking for
     * @return <tt>null</tt> if there's no such filter in this chain
     */
	public Entry getEntry(IoFilter filter);
	
	/**
     * Returns the {@link Entry} with the specified <tt>filterType</tt>
     * in this chain. If there's more than one filter with the specified
     * type, the first match will be chosen.
     * 
     * 返回当前IoFilterChain中指定Class类型对应的Entry。
     * 如果有多个Class类型的filter，会返回匹配的第1个filter。
     * 
     * @param filterType The filter class we are looking for
     * @return <tt>null</tt> if there's no such name in this chain
     */
	public Entry getEntry(Class<? extends IoFilter> filterType);
	
	/**
     * Returns the {@link IoFilter} with the specified <tt>name</tt> in this chain.
     * 
     * 返回当前IoFilterChain中指定名称对应的IoFilter。
     * 
     * @param name the filter's name
     * @return <tt>null</tt> if there's no such name in this chain
     */
	public IoFilter get(String name);
	
	/**
     * Returns the {@link IoFilter} with the specified <tt>filterType</tt>
     * in this chain. If there's more than one filter with the specified
     * type, the first match will be chosen.
     * 
     * 返回当前IoFilterChain中指定Class类型对应的IoFilter。
     * 如果有多个Class类型的filter，会返回匹配的第1个filter。
     * 
     * @param filterType The filter class
     * @return <tt>null</tt> if there's no such name in this chain
     */
    public IoFilter get(Class<? extends IoFilter> filterType);
	
    /**
     * Returns the {@link NextFilter} of the {@link IoFilter} with the
     * specified <tt>name</tt> in this chain.
     * 
     * 返回当前IoFilterChain中指定名称对应的IoFilter的下一个过滤器。
     * 
     * @param name The filter's name we want the next filter
     * @return <tt>null</tt> if there's no such name in this chain
     */
    public NextFilter getNextFilter(String name);
    
    /**
     * Returns the {@link NextFilter} of the specified {@link IoFilter}
     * in this chain.
     * 
     * 返回当前IoFilterChain中指定IoFilter的下一个过滤器。
     * 
     * @param filter The filter for which we want the next filter
     * @return <tt>null</tt> if there's no such name in this chain
     */
    public NextFilter getNextFilter(IoFilter filter);

    /**
     * Returns the {@link NextFilter} of the specified <tt>filterType</tt>
     * in this chain.  If there's more than one filter with the specified
     * type, the first match will be chosen.
     * 
     * 返回当前IoFilterChain中指定Class类型对应的IoFilter的下一个过滤器。
     * 如果有多个Class类型的filter，会返回匹配的第1个filter的下一个过滤器。
     * 
     * @param filterType The Filter class for which we want the next filter
     * @return <tt>null</tt> if there's no such name in this chain
     */
    public NextFilter getNextFilter(Class<? extends IoFilter> filterType);
	
    /**
     * @return The list of all {@link Entry}s this chain contains.
     * 
     * 返回当前IoFilterChain中的所有Entry。
     */
    public List<Entry> getAll();
	
    /**
     * @return The reversed list of all {@link Entry}s this chain contains.
     * 
     * 以相反的顺序返回当前IoFilterChain中的所有Entry。
     */
    public List<Entry> getAllReversed();
	
    /**
     * @param name The filter's name we are looking for
     * 
     * @return <tt>true</tt> if this chain contains an {@link IoFilter} with the
     * specified <tt>name</tt>.
     * 
     * 返回当前IoFilterChain中是否包含指定名称的IoFilter。
     */
    public boolean contains(String name);

    /**
     * @param filter The filter we are looking for
     * 
     * @return <tt>true</tt> if this chain contains the specified <tt>filter</tt>.
     * 
     * 返回当前IoFilterChain中是否包含指定的IoFilter。
     */
    public boolean contains(IoFilter filter);

    /**
     * @param  filterType The filter's class we are looking for
     * 
     * @return <tt>true</tt> if this chain contains an {@link IoFilter} of the
     * specified <tt>filterType</tt>.
     * 
     * 返回当前IoFilterChain中是否包含指定Class类型的IoFilter。
     */
    public boolean contains(Class<? extends IoFilter> filterType);
	
    /**
     * Adds the specified filter with the specified name at the beginning of this chain.
     * 
     * 在当前IoFilterChain的头部以指定的名称插入指定的IoFilter。
     * 
     * @param name The filter's name
     * @param filter The filter to add
     */
    public void addFirst(String name, IoFilter filter);
    
    /**
     * Adds the specified filter with the specified name at the end of this chain.
     * 
     * 在当前IoFilterChain的尾部以指定的名称插入指定的IoFilter。
     * 
     * @param name The filter's name
     * @param filter The filter to add
     */
    public void addLast(String name, IoFilter filter);

    /**
     * Adds the specified filter with the specified name just before the filter whose name is
     * <code>baseName</code> in this chain.
     * 
     * 在当前IoFilterChain中，在名称为baseName的过滤器前以指定的名称插入指定的IoFilter。
     * 
     * @param baseName The targeted Filter's name
     * @param name The filter's name
     * @param filter The filter to add
     */
    public void addBefore(String baseName, String name, IoFilter filter);
    
    /**
     * Adds the specified filter with the specified name just after the filter whose name is
     * <code>baseName</code> in this chain.
     * 
     * 在当前IoFilterChain中，在名称为baseName的过滤器后以指定的名称插入指定的IoFilter。
     * 
     * @param baseName The targeted Filter's name
     * @param name The filter's name
     * @param filter The filter to add
     */
    public void addAfter(String baseName, String name, IoFilter filter);
    
    /**
     * Replace the filter with the specified name with the specified new
     * filter.
     * 
     * 将当前IoFilterChain中的指定名称的旧过滤器替换为新的IoFilter。
     *
     * @param name The name of the filter we want to replace
     * @param newFilter The new filter
     * @return the old filter
     */
    public IoFilter replace(String name, IoFilter newFilter);

    /**
     * Replace the filter with the specified name with the specified new
     * filter.
     * 
     * 将当前IoFilterChain中的指定的旧过滤器替换为新的IoFilter。
     *
     * @param oldFilter The filter we want to replace
     * @param newFilter The new filter
     */
    public void replace(IoFilter oldFilter, IoFilter newFilter);

    /**
     * Replace the filter of the specified type with the specified new
     * filter.  If there's more than one filter with the specified type,
     * the first match will be replaced.
     * 
     * 将当前IoFilterChain中的指定的Class类型对应的旧过滤器替换为新的IoFilter。
     * 如果有多个Class类型的filter，匹配的第1个filter会被替换。
     *
     * @param oldFilterType The filter class we want to replace
     * @param newFilter The new filter
     * @return The replaced IoFilter
     */
    public IoFilter replace(Class<? extends IoFilter> oldFilterType, IoFilter newFilter);
    
    /**
     * Removes the filter with the specified name from this chain.
     * 
     * 在当前IoFilterChain中移除指定名称的过滤器。
     * 
     * @param name The name of the filter to remove
     * @return The removed filter
     */
    public IoFilter remove(String name);

    /**
     * Replace the filter with the specified name with the specified new filter.
     * 
     * 在当前IoFilterChain中移除指定的过滤器。
     * 
     * @param filter
     *            The filter to remove
     */
    public void remove(IoFilter filter);

    /**
     * Replace the filter of the specified type with the specified new filter.
     * If there's more than one filter with the specified type, the first match
     * will be replaced.
     * 
     * 在当前IoFilterChain中移除指定Class类型的过滤器。
     * 如果有多个Class类型的filter，匹配的第1个filter会被移除。
     * 
     * @param filterType
     *            The filter class to remove
     * @return The removed filter
     */
    public IoFilter remove(Class<? extends IoFilter> filterType);

    /**
     * Removes all filters added to this chain.
     * 
     * 清空当前过滤器链中的所有过滤器。
     * 
     * @throws Exception If we weren't able to clear the filters
     */
    public void clear() throws Exception;
    
    /**
     * Fires a {@link IoHandler#sessionCreated(IoSession)} event. Most users don't need to
     * call this method at all. Please use this method only when you implement a new transport
     * or fire a virtual event.
     * 
     * 触发IoHandler.sessionCreated(IoSession)事件。
     * 一般来说不用调用此方法，只有当使用新的传输方式或触发一个虚拟事件时才需要调用此方法。
     */
    public void fireSessionCreated();

    /**
     * Fires a {@link IoHandler#sessionOpened(IoSession)} event. Most users don't need to call
     * this method at all. Please use this method only when you implement a new transport or
     * fire a virtual event.
     * 
     * 触发IoHandler.sessionOpened(IoSession)事件。
     * 一般来说不用调用此方法，只有当使用新的传输方式或触发一个虚拟事件时才需要调用此方法。
     */
    public void fireSessionOpened();

    /**
     * Fires a {@link IoHandler#sessionClosed(IoSession)} event. Most users don't need to call
     * this method at all. Please use this method only when you implement a new transport or
     * fire a virtual event.
     * 
     * 触发IoHandler.sessionClosed(IoSession)事件。
     * 一般来说不用调用此方法，只有当使用新的传输方式或触发一个虚拟事件时才需要调用此方法。
     */
    public void fireSessionClosed();

    /**
     * Fires a {@link IoHandler#sessionIdle(IoSession, IdleStatus)} event. Most users don't
     * need to call this method at all. Please use this method only when you implement a new
     * transport or fire a virtual event.
     * 
     * 触发IoHandler.sessionIdle(IoSession, IdleStatus)事件。
     * 一般来说不用调用此方法，只有当使用新的传输方式或触发一个虚拟事件时才需要调用此方法。
     * 
     * @param status The current status to propagate
     */
    public void fireSessionIdle(IdleStatus status);

    /**
     * Fires a {@link IoHandler#messageReceived(IoSession, Object)} event. Most
     * users don't need to call this method at all. Please use this method only
     * when you implement a new transport or fire a virtual event.
     * 
     * 触发IoHandler.messageReceived(IoSession, Object)事件。
     * 一般来说不用调用此方法，只有当使用新的传输方式或触发一个虚拟事件时才需要调用此方法。
     * 
     * @param message
     *            The received message
     */
    public void fireMessageReceived(Object message);

    /**
     * Fires a {@link IoHandler#messageSent(IoSession, Object)} event. Most
     * users don't need to call this method at all. Please use this method only
     * when you implement a new transport or fire a virtual event.
     * 
     * 触发IoHandler.messageSent(IoSession, Object)事件。
     * 一般来说不用调用此方法，只有当使用新的传输方式或触发一个虚拟事件时才需要调用此方法。
     * 
     * @param request
     *            The sent request
     */
    public void fireMessageSent(WriteRequest request);

    /**
     * Fires a {@link IoHandler#exceptionCaught(IoSession, Throwable)} event. Most users don't
     * need to call this method at all. Please use this method only when you implement a new
     * transport or fire a virtual event.
     * 
     * 触发IoHandler.exceptionCaught(IoSession, Throwable)事件。
     * 一般来说不用调用此方法，只有当使用新的传输方式或触发一个虚拟事件时才需要调用此方法。
     * 
     * @param cause The exception cause
     */
    public void fireExceptionCaught(Throwable cause);

    /**
     * Fires a {@link IoHandler#inputClosed(IoSession)} event. Most users don't
     * need to call this method at all. Please use this method only when you
     * implement a new transport or fire a virtual event.
     * 
     * 触发IoHandler.inputClosed(IoSession)事件。
     * 一般来说不用调用此方法，只有当使用新的传输方式或触发一个虚拟事件时才需要调用此方法。
     */
    public void fireInputClosed();

    /**
     * Fires a {@link IoSession#write(Object)} event. Most users don't need to
     * call this method at all. Please use this method only when you implement a
     * new transport or fire a virtual event.
     * 
     * 触发IoHandler.write(Object)事件。
     * 一般来说不用调用此方法，只有当使用新的传输方式或触发一个虚拟事件时才需要调用此方法。
     * 
     * @param writeRequest
     *            The message to write
     */
    public void fireFilterWrite(WriteRequest writeRequest);

    /**
     * Fires a {@link IoSession#close(boolean)} event. Most users don't need to call this method at
     * all. Please use this method only when you implement a new transport or fire a virtual
     * event.
     * 
     * 触发IoHandler.close(boolean)事件。
     * 一般来说不用调用此方法，只有当使用新的传输方式或触发一个虚拟事件时才需要调用此方法。
     */
    public void fireFilterClose();    
	
	/**
	 * Represents a name-filter pair that an {@link IoFilterChain} contains.
	 * 
	 * 表示IoFilterChain里的名称和filter对的条目。即一个Entry表示一个名称以及对应的filter的键值对。
	 * 
	 * @date	2016年6月14日 下午2:37:27
	 */
	interface Entry {
		/**
         * @return the name of the filter.
         * 
         * 返回filter的名称。
         */
        public String getName();

        /**
         * @return the filter.
         * 
         * 返回filter对象。
         */
        public IoFilter getFilter();

        /**
         * @return The {@link NextFilter} of the filter.
         * 
         * 返回当前filter的下一个filter。
         */
        public NextFilter getNextFilter();

        /**
         * Adds the specified filter with the specified name just before this entry.
         * 
         * 在当前filter之前加入以指定名称加入指定filter。
         * 
         * @param name The Filter's name
         * @param filter The added Filter 
         */
        public void addBefore(String name, IoFilter filter);

        /**
         * Adds the specified filter with the specified name just after this entry.
         * 
         * 在当前filter之后加入以指定名称加入指定filter。
         * 
         * @param name The Filter's name
         * @param filter The added Filter 
         */
        public void addAfter(String name, IoFilter filter);

        /**
         * Replace the filter of this entry with the specified new filter.
         * 
         * 以指定filter代替本filter。
         * 
         * @param newFilter The new filter that will be put in the chain 
         */
        public void replace(IoFilter newFilter);

        /**
         * Removes this entry from the chain it belongs to.
         * 
         * 从过滤器中移除本entry。
         */
        public void remove();
	}
}
