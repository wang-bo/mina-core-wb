package org.apache.mina.core.filterchain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.filterchain.IoFilterChain.Entry;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.AbstractIoService;
import org.apache.mina.core.session.AbstractIoSession;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A default implementation of {@link IoFilterChain} that provides
 * all operations for developers who want to implement their own
 * transport layer once used with {@link AbstractIoSession}.
 * 
 * IoFilterChain的默认实现类。
 * 提供了大量操作，支持开发者使用AbstractIoSession实现自己的传输层。
 * 
 * @date	2016年6月16日 上午9:44:25	completed
 */
public class DefaultIoFilterChain implements IoFilterChain {

	/**
     * A session attribute that stores an {@link IoFuture} related with
     * the {@link IoSession}.  {@link DefaultIoFilterChain} clears this
     * attribute and notifies the future when {@link #fireSessionCreated()}
     * or {@link #fireExceptionCaught(Throwable)} is invoked.
     * 
     * 当前session上的一个的自定义属性的key，与一个IoFuture(其实是ConnectionFuture)关联，这个IoFuture表示客户端发起连接请求的结果的future。
     * DefaultIoFilterChain.fireSessionCreated()或fireExceptionCaught(Throwable)方法调用时，清理这个属性并通知future。
     */
    public static final AttributeKey SESSION_CREATED_FUTURE = 
    		new AttributeKey(DefaultIoFilterChain.class, "connectFuture");
    
    /** The associated session.　当前过滤器链关联的session */
    private final AbstractIoSession session;
    
    /** The mapping between the filters and their associated name. 保存过滤器名称和过滤器Entry的Map */
    private final Map<String, Entry> name2entry = new ConcurrentHashMap<String, Entry>();
    
    /** The chain head. 过滤器链的第一个过滤器 */
    private final EntryImpl head;

    /** The chain tail. 过滤器链的第后一个过滤器 */
    private final EntryImpl tail;

    /** The logger for this class */
    private final static Logger LOGGER = LoggerFactory.getLogger(DefaultIoFilterChain.class);
    
    /**
     * Create a new default chain, associated with a session. It will only contain a
     * HeadFilter and a TailFilter.
     *
     * @param session The session associated with the created filter chain
     */
    public DefaultIoFilterChain(AbstractIoSession session) {
    	if (session == null) {
    		throw new IllegalArgumentException("session");
		}
    	this.session = session;
    	head = new EntryImpl(null, null, "head", new HeadFilter());
    	tail = new EntryImpl(head, null, "tail", new TailFilter());
    	head.nextEntry = tail;
    }
    
    /**
     * {@inheritDoc}
     */
    public IoSession getSession() {
        return session;
    }
    
    /**
     * {@inheritDoc}
     */
    public Entry getEntry(String name) {
    	Entry entry = name2entry.get(name);
    	return entry;
    }
    
    /**
     * {@inheritDoc}
     */
    public Entry getEntry(IoFilter filter) {
    	EntryImpl entry = head.nextEntry;
    	while (entry != tail) {
    		if (entry.getFilter() == filter) {
				return entry;
			}
    		entry = entry.nextEntry;
    	}
    	return null;
    }
    
    /**
     * {@inheritDoc}
     */
    public Entry getEntry(Class<? extends IoFilter> filterType) {
        EntryImpl e = head.nextEntry;
        while (e != tail) {
            if (filterType.isAssignableFrom(e.getFilter().getClass())) {
                return e;
            }
            e = e.nextEntry;
        }
        return null;
    }
    
    /**
     * {@inheritDoc}
     */
    public IoFilter get(String name) {
        Entry e = getEntry(name);
        if (e == null) {
            return null;
        }
        return e.getFilter();
    }
    
    /**
     * {@inheritDoc}
     */
    public IoFilter get(Class<? extends IoFilter> filterType) {
        Entry e = getEntry(filterType);
        if (e == null) {
            return null;
        }
        return e.getFilter();
    }
    
    /**
     * {@inheritDoc}
     */
    public NextFilter getNextFilter(String name) {
        Entry e = getEntry(name);
        if (e == null) {
            return null;
        }
        return e.getNextFilter();
    }
    
    /**
     * {@inheritDoc}
     */
    public NextFilter getNextFilter(IoFilter filter) {
        Entry e = getEntry(filter);
        if (e == null) {
            return null;
        }
        return e.getNextFilter();
    }
    
    /**
     * {@inheritDoc}
     */
    public NextFilter getNextFilter(Class<? extends IoFilter> filterType) {
        Entry e = getEntry(filterType);
        if (e == null) {
            return null;
        }
        return e.getNextFilter();
    }
    
    /**
     * {@inheritDoc}
     */
    public synchronized void addFirst(String name, IoFilter filter) {
    	checkAddable(name);
    	register(head, name, filter);
    }
    
    /**
     * {@inheritDoc}
     */
    public synchronized void addLast(String name, IoFilter filter) {
        checkAddable(name);
        register(tail.prevEntry, name, filter);
    }
    
    /**
     * {@inheritDoc}
     */
    public synchronized void addBefore(String baseName, String name, IoFilter filter) {
        EntryImpl baseEntry = checkOldName(baseName);
        checkAddable(name);
        register(baseEntry.prevEntry, name, filter);
    }
    
    /**
     * {@inheritDoc}
     */
    public synchronized void addAfter(String baseName, String name, IoFilter filter) {
        EntryImpl baseEntry = checkOldName(baseName);
        checkAddable(name);
        register(baseEntry, name, filter);
    }
    
    /**
     * {@inheritDoc}
     */
    public synchronized IoFilter remove(String name) {
        EntryImpl entry = checkOldName(name);
        deregister(entry);
        return entry.getFilter();
    }
    
    /**
     * {@inheritDoc}
     */
    public synchronized void remove(IoFilter filter) {
    	EntryImpl entry = head.nextEntry;
    	while (entry != tail) {
    		if (entry.getFilter() == filter) {
				deregister(entry);
				return;
			}
    		entry = entry.nextEntry;
    	}
    	throw new IllegalArgumentException("Filter not found: " + filter.getClass().getName());
    }
    
    /**
     * {@inheritDoc}
     */
    public synchronized IoFilter remove(Class<? extends IoFilter> filterType) {
        EntryImpl e = head.nextEntry;
        while (e != tail) {
            if (filterType.isAssignableFrom(e.getFilter().getClass())) {
                IoFilter oldFilter = e.getFilter();
                deregister(e);
                return oldFilter;
            }
            e = e.nextEntry;
        }
        throw new IllegalArgumentException("Filter not found: " + filterType.getName());
    }
    
    /**
     * {@inheritDoc}
     */
    public synchronized IoFilter replace(String name, IoFilter newFilter) {
        EntryImpl entry = checkOldName(name);
        IoFilter oldFilter = entry.getFilter();
        // Call the preAdd method of the new filter
        try {
            newFilter.onPreAdd(this, name, entry.getNextFilter());
        } catch (Exception e) {
            throw new IoFilterLifeCycleException("onPreAdd(): " + name + ':' + newFilter + " in " + getSession(), e);
        }
        // Now, register the new Filter replacing the old one.
        entry.setFilter(newFilter);
        // Call the postAdd method of the new filter
        try {
            newFilter.onPostAdd(this, name, entry.getNextFilter());
        } catch (Exception e) {
            entry.setFilter(oldFilter);
            throw new IoFilterLifeCycleException("onPostAdd(): " + name + ':' + newFilter + " in " + getSession(), e);
        }
        return oldFilter;
    }
    
    /**
     * {@inheritDoc}
     */
    public synchronized void replace(IoFilter oldFilter, IoFilter newFilter) {
        EntryImpl entry = head.nextEntry;
        // Search for the filter to replace
        while (entry != tail) {
            if (entry.getFilter() == oldFilter) {
                String oldFilterName = null;
                // Get the old filter name. It's not really efficient...
                for (Map.Entry<String, Entry> mapping : name2entry.entrySet()) {
                    if (entry == mapping.getValue() ) {
                        oldFilterName = mapping.getKey();

                        break;
                    }
                }
                // Call the preAdd method of the new filter
                try {
                    newFilter.onPreAdd(this, oldFilterName, entry.getNextFilter());
                } catch (Exception e) {
                    throw new IoFilterLifeCycleException("onPreAdd(): " + oldFilterName + ':' + newFilter + " in "
                            + getSession(), e);
                }
                // Now, register the new Filter replacing the old one.
                entry.setFilter(newFilter);
                // Call the postAdd method of the new filter
                try {
                    newFilter.onPostAdd(this, oldFilterName, entry.getNextFilter());
                } catch (Exception e) {
                    entry.setFilter(oldFilter);
                    throw new IoFilterLifeCycleException("onPostAdd(): " + oldFilterName + ':' + newFilter + " in "
                            + getSession(), e);
                }
                return;
            }
            entry = entry.nextEntry;
        }
        throw new IllegalArgumentException("Filter not found: " + oldFilter.getClass().getName());
    }
    
    /**
     * {@inheritDoc}
     */
    public synchronized IoFilter replace(Class<? extends IoFilter> oldFilterType, IoFilter newFilter) {
        EntryImpl entry = head.nextEntry;
        while (entry != tail) {
            if (oldFilterType.isAssignableFrom(entry.getFilter().getClass())) {
                IoFilter oldFilter = entry.getFilter();
                String oldFilterName = null;
                // Get the old filter name. It's not really efficient...
                for (Map.Entry<String, Entry> mapping : name2entry.entrySet()) {
                    if (entry == mapping.getValue() ) {
                        oldFilterName = mapping.getKey();
                        break;
                    }
                }
                // Call the preAdd method of the new filter
                try {
                    newFilter.onPreAdd(this, oldFilterName, entry.getNextFilter());
                } catch (Exception e) {
                    throw new IoFilterLifeCycleException("onPreAdd(): " + oldFilterName + ':' + newFilter + " in "
                            + getSession(), e);
                }
                entry.setFilter(newFilter);
                // Call the postAdd method of the new filter
                try {
                    newFilter.onPostAdd(this, oldFilterName, entry.getNextFilter());
                } catch (Exception e) {
                    entry.setFilter(oldFilter);
                    throw new IoFilterLifeCycleException("onPostAdd(): " + oldFilterName + ':' + newFilter + " in "
                            + getSession(), e);
                }
                return oldFilter;
            }
            entry = entry.nextEntry;
        }
        throw new IllegalArgumentException("Filter not found: " + oldFilterType.getName());
    }
    
    /**
     * {@inheritDoc}
     */
    public synchronized void clear() throws Exception {
        List<IoFilterChain.Entry> l = new ArrayList<IoFilterChain.Entry>(name2entry.values());

        for (IoFilterChain.Entry entry : l) {
            try {
                deregister((EntryImpl) entry);
            } catch (Exception e) {
                throw new IoFilterLifeCycleException("clear(): " + entry.getName() + " in " + getSession(), e);
            }
        }
    }
    
    /**
     * Register the newly added filter, inserting it between the previous and
     * the next filter in the filter's chain. We also call the preAdd and
     * postAdd methods.
     * 
     * 注册新加的filter，加到指定Entry的后面一个位置。并调用preAdd()、postAdd()方法。
     */
    private void register(EntryImpl prevEntry, String name, IoFilter filter) {
    	EntryImpl newEntry = new EntryImpl(prevEntry, prevEntry.nextEntry, name, filter);
    	try {
    		filter.onPreAdd(this, name, newEntry.getNextFilter());
    	} catch (Exception e) {
    		throw new IoFilterLifeCycleException("onPreAdd(): " + name + ':' + filter + " in " + getSession(), e);
    	}
    	prevEntry.nextEntry.prevEntry = newEntry;
    	prevEntry.nextEntry = newEntry;
    	name2entry.put(name, newEntry);
    	try {
    		filter.onPostAdd(this, name, newEntry.getNextFilter());
    	} catch (Exception e) {
    		deregister0(newEntry);
            throw new IoFilterLifeCycleException("onPostAdd(): " + name + ':' + filter + " in " + getSession(), e);
    	}
    }
    
    /**
     * 删除指定filter。
     * 
     * @param entry
     */
    private void deregister(EntryImpl entry) {
    	IoFilter filter = entry.getFilter();
    	try {
    		filter.onPreRemove(this, entry.getName(), entry.getNextFilter());
    	} catch (Exception e) {
    		throw new IoFilterLifeCycleException("onPreRemove(): " + entry.getName() + ':' + filter + " in "
                    + getSession(), e);
    	}
    	
    	deregister0(entry);
    	
    	try {
    		filter.onPostRemove(this, entry.getName(), entry.getNextFilter());
    	} catch (Exception e) {
            throw new IoFilterLifeCycleException("onPostRemove(): " + entry.getName() + ':' + filter + " in "
                    + getSession(), e);
        }
    }
    
    private void deregister0(EntryImpl entry) {
    	EntryImpl prevEntry = entry.prevEntry;
    	EntryImpl nextEntry = entry.nextEntry;
    	prevEntry.nextEntry = nextEntry;
    	nextEntry.prevEntry = prevEntry;
    	name2entry.remove(entry.name);
    }
    
    /**
     * Throws an exception when the specified filter name is not registered in this chain.
     *
     * 返回指定名称的EntryImpl，如果没有，抛出异常。
     *
     * @return An filter entry with the specified name.
     */
    private EntryImpl checkOldName(String baseName) {
    	EntryImpl entry = (EntryImpl) name2entry.get(baseName);
    	if (entry == null) {
    		throw new IllegalArgumentException("Filter not found:" + baseName);
		}
    	return entry;
    }
    
    /**
     * Checks the specified filter name is already taken and throws an exception if already taken.
     */
    private void checkAddable(String name) {
        if (name2entry.containsKey(name)) {
            throw new IllegalArgumentException("Other filter is using the same name '" + name + "'");
        }
    }
    
    /**
     * 触发过滤器链session创建事件。
     * 
     * {@inheritDoc}
     */
    public void fireSessionCreated() {
    	callNextSessionCreated(head, session);
    }
    
    private void callNextSessionCreated(Entry entry, IoSession session) {
    	try {
    		IoFilter filter = entry.getFilter();
    		NextFilter nextFilter = entry.getNextFilter();
    		filter.sessionCreated(nextFilter, session);
    	} catch (Exception e) {
            fireExceptionCaught(e);
        } catch (Error e) {
            fireExceptionCaught(e);
            throw e;
        }
    }
    
    /**
     * 触发过滤器链session打开事件。
     * 
     * {@inheritDoc}
     */
    public void fireSessionOpened() {
        callNextSessionOpened(head, session);
    }
    
    private void callNextSessionOpened(Entry entry, IoSession session) {
        try {
            IoFilter filter = entry.getFilter();
            NextFilter nextFilter = entry.getNextFilter();
            filter.sessionOpened(nextFilter, session);
        } catch (Exception e) {
            fireExceptionCaught(e);
        } catch (Error e) {
            fireExceptionCaught(e);
            throw e;
        }
    }
    
    /**
     * 触发过滤器链session关闭事件。
     * 
     * {@inheritDoc}
     */
    public void fireSessionClosed() {
        // Update future.
    	try {
    		session.getCloseFuture().setClosed();
    	} catch (Exception e) {
            fireExceptionCaught(e);
        } catch (Error e) {
            fireExceptionCaught(e);
            throw e;
        }

        // And start the chain.
        callNextSessionClosed(head, session);
    }
    
    private void callNextSessionClosed(Entry entry, IoSession session) {
        try {
            IoFilter filter = entry.getFilter();
            NextFilter nextFilter = entry.getNextFilter();
            filter.sessionClosed(nextFilter, session);
        } catch (Exception e) {
            fireExceptionCaught(e);
        } catch (Error e) {
            fireExceptionCaught(e);
        }
    }
    
    /**
     * 触发过滤器链session空闲事件。
     * 
     * {@inheritDoc}
     */
    public void fireSessionIdle(IdleStatus status) {
        session.increaseIdleCount(status, System.currentTimeMillis());
        callNextSessionIdle(head, session, status);
    }
    
    private void callNextSessionIdle(Entry entry, IoSession session, IdleStatus status) {
        try {
            IoFilter filter = entry.getFilter();
            NextFilter nextFilter = entry.getNextFilter();
            filter.sessionIdle(nextFilter, session, status);
        } catch (Exception e) {
            fireExceptionCaught(e);
        } catch (Error e) {
            fireExceptionCaught(e);
            throw e;
        }
    }
    
    /**
     * 触发过滤器链消息接收事件。
     * 
     * {@inheritDoc}
     */
    public void fireMessageReceived(Object message) {
    	if (message instanceof IoBuffer) {
    		session.increaseReadBytes(((IoBuffer) message).remaining(), System.currentTimeMillis());
		}
    	callNextMessageReceived(head, session, message);
    }
    
    private void callNextMessageReceived(Entry entry, IoSession session, Object message) {
        try {
            IoFilter filter = entry.getFilter();
            NextFilter nextFilter = entry.getNextFilter();
            filter.messageReceived(nextFilter, session, message);
        } catch (Exception e) {
            fireExceptionCaught(e);
        } catch (Error e) {
            fireExceptionCaught(e);
            throw e;
        }
    }
    
    /**
     * 触发过滤器链消息发送事件。
     * 
     * {@inheritDoc}
     */
    public void fireMessageSent(WriteRequest request) {
        try {
            request.getFuture().setWritten();
        } catch (Exception e) {
            fireExceptionCaught(e);
        } catch (Error e) {
            fireExceptionCaught(e);
            throw e;
        }

        if (!request.isEncoded()) {
            callNextMessageSent(head, session, request);
        }
    }
    
    private void callNextMessageSent(Entry entry, IoSession session, WriteRequest writeRequest) {
        try {
            IoFilter filter = entry.getFilter();
            NextFilter nextFilter = entry.getNextFilter();
            filter.messageSent(nextFilter, session, writeRequest);
        } catch (Exception e) {
            fireExceptionCaught(e);
        } catch (Error e) {
            fireExceptionCaught(e);
            throw e;
        }
    }
    
    /**
     * 触发过滤器链异常发生事件。
     * 
     * {@inheritDoc}
     */
    public void fireExceptionCaught(Throwable cause) {
        callNextExceptionCaught(head, session, cause);
    }
    
    private void callNextExceptionCaught(Entry entry, IoSession session, Throwable cause) {
    	// Notify the related future.
    	ConnectFuture future = (ConnectFuture) session.removeAttribute(SESSION_CREATED_FUTURE);
    	if (future == null) {
			try {
				IoFilter filter = entry.getFilter();
				NextFilter nextFilter = entry.getNextFilter();
				filter.exceptionCaught(nextFilter, session, cause);
			} catch (Throwable e) {
                LOGGER.warn("Unexpected exception from exceptionCaught handler.", e);
            }
		} else {
			// Please note that this place is not the only place that
            // calls ConnectFuture.setException().
			session.closeNow();
			future.setException(cause);
		}
    }
    
    /**
     * 触发过滤器链输入通道关闭事件。
     * 
     * {@inheritDoc}
     */
    public void fireInputClosed() {
        Entry head = this.head;
        callNextInputClosed(head, session);
    }

    private void callNextInputClosed(Entry entry, IoSession session) {
        try {
            IoFilter filter = entry.getFilter();
            NextFilter nextFilter = entry.getNextFilter();
            filter.inputClosed(nextFilter, session);
        } catch (Throwable e) {
            fireExceptionCaught(e);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void fireFilterWrite(WriteRequest writeRequest) {
        callPreviousFilterWrite(tail, session, writeRequest);
    }

    private void callPreviousFilterWrite(Entry entry, IoSession session, WriteRequest writeRequest) {
        try {
            IoFilter filter = entry.getFilter();
            NextFilter nextFilter = entry.getNextFilter();
            filter.filterWrite(nextFilter, session, writeRequest);
        } catch (Exception e) {
            writeRequest.getFuture().setException(e);
            fireExceptionCaught(e);
        } catch (Error e) {
            writeRequest.getFuture().setException(e);
            fireExceptionCaught(e);
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void fireFilterClose() {
        callPreviousFilterClose(tail, session);
    }

    private void callPreviousFilterClose(Entry entry, IoSession session) {
        try {
            IoFilter filter = entry.getFilter();
            NextFilter nextFilter = entry.getNextFilter();
            filter.filterClose(nextFilter, session);
        } catch (Exception e) {
            fireExceptionCaught(e);
        } catch (Error e) {
            fireExceptionCaught(e);
            throw e;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public List<Entry> getAll() {
    	List<Entry> list = new ArrayList<Entry>();
    	EntryImpl entry = head.nextEntry;
    	while (entry != tail) {
    		list.add(entry);
    		entry = entry.nextEntry;
    	}
    	return list;
    }
    
    /**
     * {@inheritDoc}
     */
    public List<Entry> getAllReversed() {
        List<Entry> list = new ArrayList<Entry>();
        EntryImpl e = tail.prevEntry;
        while (e != head) {
            list.add(e);
            e = e.prevEntry;
        }
        return list;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean contains(String name) {
        return getEntry(name) != null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(IoFilter filter) {
        return getEntry(filter) != null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(Class<? extends IoFilter> filterType) {
        return getEntry(filterType) != null;
    }
    
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("{ ");

        boolean empty = true;

        EntryImpl e = head.nextEntry;

        while (e != tail) {
            if (!empty) {
                buf.append(", ");
            } else {
                empty = false;
            }

            buf.append('(');
            buf.append(e.getName());
            buf.append(':');
            buf.append(e.getFilter());
            buf.append(')');

            e = e.nextEntry;
        }

        if (empty) {
            buf.append("empty");
        }

        buf.append(" }");

        return buf.toString();
    }
    
    /**
     * 过滤器链里的第一个filter：
     * 	1. session写事件发生时把WriteRequest放入队列，session可写时执行flush。
     * 	2. session关闭事件发生时，将session从processor中移除。
     */
    private class HeadFilter extends IoFilterAdapter {
    	@Override
        public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
    		AbstractIoSession s = (AbstractIoSession) session;
    		// Maintain counters.
    		// 1. 更新计数器
    		if (writeRequest.getMessage() instanceof IoBuffer) {
				IoBuffer buffer = (IoBuffer) writeRequest.getMessage();
				// I/O processor implementation will call buffer.reset()
                // it after the write operation is finished, because
                // the buffer will be specified with messageSent event.
				// 写事件结束后，这个buffer还会用在messageSent事件中，所以processor会调用buffer.reset()方法。
				buffer.mark();
				int remaining = buffer.remaining();
				if (remaining > 0) {
					s.increaseScheduledWriteBytes(remaining);
				}
			} else {
				s.increaseScheduledWriteMessages();
			}
    		
    		// 2. 在session不阻塞写的情况下，放入session的写请求队列，并做flush操作。
    		WriteRequestQueue writeRequestQueue = s.getWriteRequestQueue();
    		if (!s.isWriteSuspended()) {
				if (writeRequestQueue.isEmpty(session)) {
					// We can write directly the message
					s.getProcessor().write(s, writeRequest);
				} else {
					s.getWriteRequestQueue().offer(s, writeRequest);
					s.getProcessor().flush(s);
				}
			} else {
				s.getWriteRequestQueue().offer(s, writeRequest);
			}
    	}
    	
    	@Override
        public void filterClose(NextFilter nextFilter, IoSession session) throws Exception {
    		((AbstractIoSession) session).getProcessor().remove((AbstractIoSession) session);
    	}
    }
    
    /**
     * 过滤器链里的最后一个filter：session上事件发生时，负责触发handler上的相应事件。
     */
    private static class TailFilter extends IoFilterAdapter {
    	@Override
        public void sessionCreated(NextFilter nextFilter, IoSession session) throws Exception {
    		try {
				session.getHandler().sessionCreated(session);
			} finally {
				// Notify the related future.
				ConnectFuture future = (ConnectFuture) session.removeAttribute(SESSION_CREATED_FUTURE);
				if (future != null) {
					future.setSession(session);
				}
			}
    	}
    	
    	@Override
        public void sessionOpened(NextFilter nextFilter, IoSession session) throws Exception {
            session.getHandler().sessionOpened(session);
        }
    	
    	@Override
        public void sessionClosed(NextFilter nextFilter, IoSession session) throws Exception {
    		AbstractIoSession s = (AbstractIoSession) session;
            try {
                s.getHandler().sessionClosed(session);
            } finally {
            	try {
            		s.getWriteRequestQueue().dispose(session);
            	} finally {
            		try {
            			s.getAttributeMap().dispose(session);
            		} finally {
            			try {
            				// Remove all filters.
            				session.getFilterChain().clear();
            			} finally {
            				if (s.getConfig().isUseReadOperation()) {
								s.offerClosedReadFuture();
							}
            			}
            		}
            	}
            }
    	}
    	
    	@Override
        public void sessionIdle(NextFilter nextFilter, IoSession session, IdleStatus status) throws Exception {
            session.getHandler().sessionIdle(session, status);
        }

        @Override
        public void exceptionCaught(NextFilter nextFilter, IoSession session, Throwable cause) throws Exception {
            AbstractIoSession s = (AbstractIoSession) session;
            try {
                s.getHandler().exceptionCaught(s, cause);
            } finally {
                if (s.getConfig().isUseReadOperation()) {
                    s.offerFailedReadFuture(cause);
                }
            }
        }

        @Override
        public void inputClosed(NextFilter nextFilter, IoSession session) throws Exception {
            session.getHandler().inputClosed(session);
        }
        
        @Override
        public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
        	AbstractIoSession s = (AbstractIoSession) session;
        	if (!(message instanceof IoBuffer)) {
				s.increaseReadMessages(System.currentTimeMillis());
			}
        	else if (!((IoBuffer) message).hasRemaining()) {
				s.increaseReadMessages(System.currentTimeMillis());
			}
        	
        	// Update the statistics
        	if (session.getService() instanceof AbstractIoService) {
				((AbstractIoService) session.getService()).getStatistics().updateThroughput(System.currentTimeMillis());
			}
        	
        	// Propagate the message
        	// 触发handler消息接收事件
        	try {
        		session.getHandler().messageReceived(s, message);
        	} finally {
        		if (s.getConfig().isUseReadOperation()) {
					s.offerReadFuture(message);
				}
        	}
        }
        
        @Override
        public void messageSent(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        	((AbstractIoSession) session).increaseWrittenMessages(writeRequest, System.currentTimeMillis());
            // Update the statistics
            if (session.getService() instanceof AbstractIoService) {
                ((AbstractIoService) session.getService()).getStatistics().updateThroughput(System.currentTimeMillis());
            }
            // Propagate the message
            // 触发handler消息发送事件
            session.getHandler().messageSent(session, writeRequest.getMessage());
        }
        
        @Override
        public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
            nextFilter.filterWrite(session, writeRequest);
        }

        @Override
        public void filterClose(NextFilter nextFilter, IoSession session) throws Exception {
            nextFilter.filterClose(session);
        }
    }
    
    /**
     * 表示一个名称以及对应的filter的键值对的实现类
     * 
     * @date	2016年6月23日 上午11:04:23
     */
    private final class EntryImpl implements Entry {
    	
    	/** 当前过滤器的前一个过滤器 */
    	private EntryImpl prevEntry;
    	
    	/** 当前过滤器的后一个过滤器 */
    	private EntryImpl nextEntry;
    	
    	/** 当前过滤器的名称 */
    	private final String name;
    	
    	/** 当前过滤器 */
    	private IoFilter filter;
    	
    	/** 当前过滤器的后一个过滤器，实际中是引用了nextEntry实例，即会自动随着nextEntry实例的变化而变化 */
    	private final NextFilter nextFilter;
    	
    	private EntryImpl(EntryImpl prevEntry, EntryImpl nextEntry, String name, IoFilter filter) {
    		if (filter == null) {
                throw new IllegalArgumentException("filter");
            }
            if (name == null) {
                throw new IllegalArgumentException("name");
            }
            
            this.prevEntry = prevEntry;
            this.nextEntry = nextEntry;
            this.name = name;
            this.filter = filter;
            this.nextFilter = new NextFilter() {
				public void sessionCreated(IoSession session) {
					Entry nextEntry = EntryImpl.this.nextEntry;
                    callNextSessionCreated(nextEntry, session);
				}
				
				public void sessionOpened(IoSession session) {
                    Entry nextEntry = EntryImpl.this.nextEntry;
                    callNextSessionOpened(nextEntry, session);
                }

                public void sessionClosed(IoSession session) {
                    Entry nextEntry = EntryImpl.this.nextEntry;
                    callNextSessionClosed(nextEntry, session);
                }

                public void sessionIdle(IoSession session, IdleStatus status) {
                    Entry nextEntry = EntryImpl.this.nextEntry;
                    callNextSessionIdle(nextEntry, session, status);
                }

                public void exceptionCaught(IoSession session, Throwable cause) {
                    Entry nextEntry = EntryImpl.this.nextEntry;
                    callNextExceptionCaught(nextEntry, session, cause);
                }

                public void inputClosed(IoSession session) {
                    Entry nextEntry = EntryImpl.this.nextEntry;
                    callNextInputClosed(nextEntry, session);
                }

                public void messageReceived(IoSession session, Object message) {
                    Entry nextEntry = EntryImpl.this.nextEntry;
                    callNextMessageReceived(nextEntry, session, message);
                }

                public void messageSent(IoSession session, WriteRequest writeRequest) {
                    Entry nextEntry = EntryImpl.this.nextEntry;
                    callNextMessageSent(nextEntry, session, writeRequest);
                }

                public void filterWrite(IoSession session, WriteRequest writeRequest) {
                    Entry nextEntry = EntryImpl.this.prevEntry;
                    callPreviousFilterWrite(nextEntry, session, writeRequest);
                }

                public void filterClose(IoSession session) {
                    Entry nextEntry = EntryImpl.this.prevEntry;
                    callPreviousFilterClose(nextEntry, session);
                }

                public String toString() {
                    return EntryImpl.this.nextEntry.name;
                }
			};
    	}
        
        /**
         * {@inheritDoc}
         */
        public String getName() {
            return name;
        }

        /**
         * {@inheritDoc}
         */
        public IoFilter getFilter() {
            return filter;
        }
        
        private void setFilter(IoFilter filter) {
            if (filter == null) {
                throw new IllegalArgumentException("filter");
            }
            this.filter = filter;
        }
        
        /**
         * {@inheritDoc}
         */
        public NextFilter getNextFilter() {
            return nextFilter;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            // Add the current filter
            sb.append("('").append(getName()).append('\'');
            // Add the previous filter
            sb.append(", prev: '");
            if (prevEntry != null) {
                sb.append(prevEntry.name);
                sb.append(':');
                sb.append(prevEntry.getFilter().getClass().getSimpleName());
            } else {
                sb.append("null");
            }
            // Add the next filter
            sb.append("', next: '");
            if (nextEntry != null) {
                sb.append(nextEntry.name);
                sb.append(':');
                sb.append(nextEntry.getFilter().getClass().getSimpleName());
            } else {
                sb.append("null");
            }
            sb.append("')");
            return sb.toString();
        }
        
        /**
         * {@inheritDoc}
         */
        public void addAfter(String name, IoFilter filter) {
            DefaultIoFilterChain.this.addAfter(getName(), name, filter);
        }

        /**
         * {@inheritDoc}
         */
        public void addBefore(String name, IoFilter filter) {
            DefaultIoFilterChain.this.addBefore(getName(), name, filter);
        }

        /**
         * {@inheritDoc}
         */
        public void remove() {
            DefaultIoFilterChain.this.remove(getName());
        }

        /**
         * {@inheritDoc}
         */
        public void replace(IoFilter newFilter) {
            DefaultIoFilterChain.this.replace(getName(), newFilter);
        }
    }
}
