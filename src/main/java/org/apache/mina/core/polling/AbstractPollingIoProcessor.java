package org.apache.mina.core.polling;

import java.io.IOException;
import java.net.PortUnreachableException;
import java.nio.channels.ClosedSelectorException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.file.FileRegion;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.filterchain.IoFilterChainBuilder;
import org.apache.mina.core.future.DefaultIoFuture;
import org.apache.mina.core.service.AbstractIoService;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.IoServiceListenerSupport;
import org.apache.mina.core.session.AbstractIoSession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.core.session.SessionState;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestQueue;
import org.apache.mina.core.write.WriteToClosedSessionException;
import org.apache.mina.transport.socket.AbstractDatagramSessionConfig;
import org.apache.mina.util.ExceptionMonitor;
import org.apache.mina.util.NamePreservingRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract implementation of {@link IoProcessor} which helps transport
 * developers to write an {@link IoProcessor} easily. This class is in charge of
 * active polling a set of {@link IoSession} and trigger events when some I/O
 * operation is possible.
 * 
 * 一个实现IoProcessor接口的基类。
 * 这个类负责轮询IoSession集合，当IoSession上有I/O操作准备好时触发事件。
 * 
 * @param <S> the type of the {@link IoSession} this processor can handle
 * 当前processor可以处理的IoSession类型。
 * 
 * @date	2016年6月20日 下午3:57:38	completed
 */
public abstract class AbstractPollingIoProcessor<S extends AbstractIoSession> implements IoProcessor<S> {

	/** A logger for this class */
    private final static Logger LOG = LoggerFactory.getLogger(IoProcessor.class);
    
    /**
     * A timeout used for the select, as we need to get out to deal with idle
     * sessions
     * 选择操作的超时时间，默认1秒，使我们可以离开去处理发生空闲状态的session。
     */
    private static final long SELECT_TIMEOUT = 1000L;
    
    /** A map containing the last Thread ID for each class. 保存每个class的最后一个线程ID的Map。 */
    private static final ConcurrentHashMap<Class<?>, AtomicInteger> threadIds = new ConcurrentHashMap<Class<?>, AtomicInteger>();
    
    /** This IoProcessor instance name. 当前processor的名称 */
    private final String threadName;
    
    /** The executor to use when we need to start the inner Processor. 执行内部Processor的线程执行器 */
    private final Executor executor;
    
    /** A Session queue containing the newly created sessions. 保存新创建session的队列 */
    private final Queue<S> newSessions = new ConcurrentLinkedQueue<S>();

    /** A queue used to store the sessions to be removed.  保存将移除的session的队列 */
    private final Queue<S> removingSessions = new ConcurrentLinkedQueue<S>();

    /** A queue used to store the sessions to be flushed. 保存正在做flush操作的session的队列 */
    private final Queue<S> flushingSessions = new ConcurrentLinkedQueue<S>();
    
    /** A queue used to store the sessions which have a trafficControl to be updated.
     *  保存需要更新交通控制状态(即阻塞或恢复读写)的session的队列 */
    private final Queue<S> trafficControllingSessions = new ConcurrentLinkedQueue<S>();
    
    /** The processor thread : it handles the incoming messages. processor线程，负责处理客户端发送的消息。 */
    private final AtomicReference<Processor> processorRef = new AtomicReference<Processor>();
    
    /** 上次空闲检测的时间 */
    private long lastIdleCheckTime;

    /** 清理用的锁，要清理当前processor关联的资源时必须先获得这个锁 */
    private final Object disposalLock = new Object();

    /** 标记当前processor是否处于disposing状态 */
    private volatile boolean disposing;

    /** 标记当前processor是否处于disposed状态 */
    private volatile boolean disposed;

    /** dispose()方法的执行结果 */
    private final DefaultIoFuture disposalFuture = new DefaultIoFuture(null);

    /** 标记是否执行wakeup操作 */
    protected AtomicBoolean wakeupCalled = new AtomicBoolean(false);
    
    /**
     * Create an {@link AbstractPollingIoProcessor} with the given
     * {@link Executor} for handling I/Os events.
     * 
     * 构造方法：使用给定的Executor来处理I/O事件。
     * 
     * @param executor
     *            the {@link Executor} for handling I/O events
     */
    protected AbstractPollingIoProcessor(Executor executor) {
        if (executor == null) {
            throw new IllegalArgumentException("executor");
        }
        this.threadName = nextThreadName();
        this.executor = executor;
    }
    
    /**
     * Compute the thread ID for this class instance. As we may have different
     * classes, we store the last ID number into a Map associating the class
     * name to the last assigned ID.
     * 
     * 计算当前processor的线程ID。
     * 因为可能有多个不同的class，所以保存这个最后的ID到Map中。
     * 
     * @return a name for the current thread, based on the class name and an
     *         incremental value, starting at 1.
     */
    private String nextThreadName() {
    	Class<?> clazz = getClass();
    	int newThreadId;
    	AtomicInteger threadId = threadIds.putIfAbsent(clazz, new AtomicInteger(1));
    	if (threadId == null) {
			newThreadId = 1;
		} else {
			// Just increment the last ID, and get it.
			newThreadId = threadId.incrementAndGet();
		}
    	// Now we can compute the name for this thread
    	return clazz.getSimpleName() + "-" + newThreadId;
    }
    
    /**
     * {@inheritDoc}
     */
    public final boolean isDisposing() {
        return disposing;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isDisposed() {
        return disposed;
    }
    
    /**
     * {@inheritDoc}
     */
    public final void dispose() {
        if (disposed || disposing) {
            return;
        }
        synchronized (disposalLock) {
            disposing = true;
            startupProcessor();
        }
        disposalFuture.awaitUninterruptibly();
        disposed = true;
    }
    
    /**
     * Dispose the resources used by this {@link IoProcessor} for polling the
     * client connections. The implementing class doDispose method will be
     * called.
     * 
     * 通过轮询所有客户端连接来释放当前processor使用的资源。
     * 
     * @throws Exception
     *             if some low level IO error occurs
     */
    protected abstract void doDispose() throws Exception;
    
    /**
     * poll those sessions for the given timeout
     * 
     * 给定的超时时间里轮询所有session，看有多少session已经准备好做I/O操作了。
     * 
     * @param timeout
     *            milliseconds before the call timeout if no event appear
     * @return The number of session ready for read or for write
     * @throws Exception
     *             if some low level IO error occurs
     */
    protected abstract int select(long timeout) throws Exception;
    
    /**
     * poll those sessions forever
     * 
     * 轮询所有session，看有多少session已经准备好做I/O操作了，这个方法会阻塞直到至少有1个session准备好做I/O操作才返回。
     * 
     * @return The number of session ready for read or for write
     * @throws Exception
     *             if some low level IO error occurs
     */
    protected abstract int select() throws Exception;
    
    /**
     * Say if the list of {@link IoSession} polled by this {@link IoProcessor}
     * is empty
     * 
     * 返回当前processor轮询的session列表是否为空。
     * 
     * @return <tt>true</tt> if at least a session is managed by this {@link IoProcessor}
     */
    protected abstract boolean isSelectorEmpty();
    
    /**
     * Interrupt the {@link #select(long)} call.
     * 
     * 中断select()操作。
     */
    protected abstract void wakeup();
    
    /**
     * Get an {@link Iterator} for the list of {@link IoSession} polled by this
     * {@link IoProcessor}
     * 
     * 返回当前processor轮询的session集合的迭代器。
     * 
     * @return {@link Iterator} of {@link IoSession}
     */
    protected abstract Iterator<S> allSessions();
    
    /**
     * Get an {@link Iterator} for the list of {@link IoSession} found selected
     * by the last call of {@link #select(long)}
     * 
     * 返回上一次select()调用时，所有准备好能做I/O操作的session集合的迭代器。
     * 
     * @return {@link Iterator} of {@link IoSession} read for I/Os operation
     */
    protected abstract Iterator<S> selectedSessions();
    
    /**
     * Get the state of a session (One of OPENING, OPEN, CLOSING)
     * 
     * 返回指定session的状态(OPENING、OPEN、CLOSING)。
     * 
     * @param session the {@link IoSession} to inspect
     * @return the state of the session
     */
    protected abstract SessionState getState(S session);
    
    /**
     * Tells if the session ready for writing
     * 
     * 返回指定session是否已准备好写。
     * 
     * @param session the queried session
     * @return <tt>true</tt> is ready, <tt>false</tt> if not ready
     */
    protected abstract boolean isWritable(S session);
    
    /**
     * Tells if the session ready for reading
     * 
     * 返回指定session是否已准备好读。
     * 
     * @param session the queried session
     * @return <tt>true</tt> is ready, <tt>false</tt> if not ready
     */
    protected abstract boolean isReadable(S session);
    
    /**
     * Set the session to be informed when a write event should be processed
     * 
     * 注册指定session是否对写感兴趣？在写事件处理前？
     * 
     * @param session the session for which we want to be interested in write events
     * @param isInterested <tt>true</tt> for registering, <tt>false</tt> for removing
     * @throws Exception If there was a problem while registering the session 
     */
    protected abstract void setInterestedInWrite(S session, boolean isInterested) throws Exception;

    /**
     * Set the session to be informed when a read event should be processed
     * 
     * 注册指定session是否对读感兴趣？在读事件处理前？
     * 
     * @param session the session for which we want to be interested in read events
     * @param isInterested <tt>true</tt> for registering, <tt>false</tt> for removing
     * @throws Exception If there was a problem while registering the session 
     */
    protected abstract void setInterestedInRead(S session, boolean isInterested) throws Exception;

    /**
     * Tells if this session is registered for reading
     * 
     * 返回当前session是否注册了读。
     * 
     * @param session the queried session
     * @return <tt>true</tt> is registered for reading
     */
    protected abstract boolean isInterestedInRead(S session);

    /**
     * Tells if this session is registered for writing
     * 
     * 返回当前session是否注册了写。
     * 
     * @param session the queried session
     * @return <tt>true</tt> is registered for writing
     */
    protected abstract boolean isInterestedInWrite(S session);
    
    /**
     * Initialize the polling of a session. Add it to the polling process.
     * 
     * 初始化指定session：添加session到轮询列表中。
     * 
     * @param session the {@link IoSession} to add to the polling
     * @throws Exception any exception thrown by the underlying system calls
     */
    protected abstract void init(S session) throws Exception;

    /**
     * Destroy the underlying client socket handle
     * 
     * 销毁指定session的socket连接。
     * 
     * @param session the {@link IoSession}
     * @throws Exception any exception thrown by the underlying system calls
     */
    protected abstract void destroy(S session) throws Exception;
    
    /**
     * Reads a sequence of bytes from a {@link IoSession} into the given
     * {@link IoBuffer}. Is called when the session was found ready for reading.
     * 
     * 从指定session中读取byte数组到指定IoBuffer中。
     * 在session被检测到准备好读时调用。
     * 
     * @param session the session to read
     * @param buf the buffer to fill
     * @return the number of bytes read
     * @throws Exception any exception thrown by the underlying system calls
     */
    protected abstract int read(S session, IoBuffer buf) throws Exception;
    
    /**
     * Write a sequence of bytes to a {@link IoSession}, means to be called when
     * a session was found ready for writing.
     * 
     * 从指定IoBuffer中向指定session写入byte数组（如果length比buf.remaining少，则写入length长度的数据）。
     * 
     * @param session the session to write
     * @param buf the buffer to write
     * @param length the number of bytes to write can be superior to the number of
     *            bytes remaining in the buffer
     * @return the number of byte written
     * @throws Exception any exception thrown by the underlying system calls
     */
    protected abstract int write(S session, IoBuffer buf, int length) throws Exception;
    
    /**
     * Write a part of a file to a {@link IoSession}, if the underlying API
     * isn't supporting system calls like sendfile(), you can throw a
     * {@link UnsupportedOperationException} so the file will be send using
     * usual {@link #write(AbstractIoSession, IoBuffer, int)} call.
     * 
     * 往指定session写入文件的某些部分。
     * 如果底层的API不支持sendfile()系统调用，会抛出UnsupportedOperationException异常，
     * 这样文件发送会使用write(AbstractIoSession, IoBuffer, int)方法。
     * 
     * @param session the session to write
     * @param region the file region to write
     * @param length the length of the portion to send
     * @return the number of written bytes
     * @throws Exception any exception thrown by the underlying system calls
     */
    protected abstract int transferFile(S session, FileRegion region, int length) throws Exception;
    
    /**
     * {@inheritDoc}
     */
    public final void add(S session) {
        if (disposed || disposing) {
            throw new IllegalStateException("Already disposed.");
        }
        // Adds the session to the newSession queue and starts the worker
        newSessions.add(session);
        startupProcessor();
    }
    
    /**
     * {@inheritDoc}
     */
    public final void remove(S session) {
        scheduleRemove(session);
        startupProcessor();
    }

    private void scheduleRemove(S session) {
        if (!removingSessions.contains(session)) {
            removingSessions.add(session);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void write(S session, WriteRequest writeRequest) {
        WriteRequestQueue writeRequestQueue = session.getWriteRequestQueue();
        writeRequestQueue.offer(session, writeRequest);
        if (!session.isWriteSuspended()) {
            this.flush(session);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public final void flush(S session) {
        // add the session to the queue if it's not already
        // in the queue, then wake up the select()
        if (session.setScheduledForFlush(true)) {
            flushingSessions.add(session);
            wakeup();
        }
    }
    
    private void scheduleFlush(S session) {
        // add the session to the queue if it's not already
        // in the queue
        if (session.setScheduledForFlush(true)) {
            flushingSessions.add(session);
        }
    }
    
    /**
     * Updates the traffic mask for a given session
     * 
     * 更新指定session的阻塞读写状态：将session放入trafficControllingSessions队列中。
     * 
     * @param session the session to update
     */
    public final void updateTrafficMask(S session) {
        trafficControllingSessions.add(session);
        wakeup();
    }
    
    /**
     * Starts the inner Processor, asking the executor to pick a thread in its
     * pool. The Runnable will be renamed
     * 
     * 开启内部的Processor，从executor中获取一个线程来执行，这个线程会被改名。
     */
    private void startupProcessor() {
        Processor processor = processorRef.get();
        if (processor == null) {
            processor = new Processor();
            if (processorRef.compareAndSet(null, processor)) {
                executor.execute(new NamePreservingRunnable(processor, threadName));
            }
        }
        // Just stop the select() and start it again, so that the processor
        // can be activated immediately.
        // 关闭select()并重新开启，所以processor可以立即开工。
        wakeup();
    }
    
    /**
     * In the case we are using the java select() method, this method is used to
     * trash the buggy selector and create a new one, registering all the sockets
     * on it.
     * 
     * 注册新的selector。
     * 这里使用java的select()方法，创建一个新的selector来替换旧的，然后注册所有socket。
     * 
     * @throws IOException If we got an exception
     */
    abstract protected void registerNewSelector() throws IOException;
    
    /**
     * Check that the select() has not exited immediately just because of a
     * broken connection. In this case, this is a standard case, and we just
     * have to loop.
     * 
     * 当一个链接被野蛮关闭时返回true。
     * 检测当前select()不存在，因为有broken连接，这是一个标准情况，我们需要循环(这翻译的完全不知所云啊...)
     * 
     * @return <tt>true</tt> if a connection has been brutally closed.
     * @throws IOException If we got an exception
     */
    abstract protected boolean isBrokenConnection() throws IOException;

    /**
     * Loops over the new sessions blocking queue and returns the number of
     * sessions which are effectively created
     * 
     * 处理新的session。
     * 循环保存新session的队列并处理，返回处理的session数。
     * 
     * @return The number of new sessions
     */
    private int handleNewSessions() {
        int addedSessions = 0;
        for (S session = newSessions.poll(); session != null; session = newSessions.poll()) {
            if (addNow(session)) {
                // A new session has been created
                addedSessions++;
            }
        }
        return addedSessions;
    }
    
    /**
     * Process a new session : - initialize it - create its chain - fire the
     * CREATED listeners if any
     * 
     * 处理一个新session：初始化、创建它的过滤器链、触发监听session创建的事件。
     * 
     * @param session The session to create
     * @return <tt>true</tt> if the session has been registered
     */
    private boolean addNow(S session) {
    	boolean registered = false;
    	try {
    		init(session);
    		registered = true;
    		
    		// Build the filter chain of this session.
    		// 创建这个session的过滤器链。
    		IoFilterChainBuilder chainBuilder = session.getService().getFilterChainBuilder();
    		chainBuilder.buildFilterChain(session.getFilterChain());
    		
    		// DefaultIoFilterChain.CONNECT_FUTURE is cleared inside here
            // in AbstractIoFilterChain.fireSessionOpened().
            // Propagate the SESSION_CREATED event up to the chain
    		// 传播SESSION_CREATED事件给过滤器链和service的监听器。
    		IoServiceListenerSupport listeners = ((AbstractIoService) session.getService()).getListeners();
    		listeners.fireSessionCreated(session);
    	} catch (Exception e) {
    		ExceptionMonitor.getInstance().exceptionCaught(e);
    		try {
    			destroy(session);
    		} catch (Exception e1) {
    			ExceptionMonitor.getInstance().exceptionCaught(e1);
    		} finally {
    			registered = false;
    		}
    	}
    	return registered;
    }
    
    /**
     * 移除待移除队列中的session：根据session的三种状态做相应处理。
     * 
     * @return
     */
    private int removeSessions() {
    	int removedSessions = 0;
    	for (S session = removingSessions.poll(); session != null; session = removingSessions.poll()) {
			SessionState state = getState(session);
			// Now deal with the removal accordingly to the session's state
			switch (state) {
				case OPENED:
					// Try to remove this session
					if (removeNow(session)) {
						removedSessions++;
					}
					break;
				case CLOSING:
					// Skip if channel is already closed
                    // In any case, remove the session from the queue
                    removedSessions++;
					break;
				case OPENING:
					// Remove session from the newSessions queue and
                    // remove it
					newSessions.remove(session);
					if (removeNow(session)) {
	                    removedSessions++;
					}
					break;
				default:
					throw new IllegalStateException(String.valueOf(state));
					break;
			}
		}
    	return removedSessions;
    }
    
    /**
     * 执行移除session操作。
     * @param session
     * @return
     */
    private boolean removeNow(S session) {
    	// 1. 清空session上的写请求队列
    	clearWriteRequestQueue(session);
    	try {
    		// 2. 关闭session的连接
			destroy(session);
			return true;
		} catch (Exception e) {
			IoFilterChain filterChain = session.getFilterChain();
			filterChain.fireExceptionCaught(e);
		} finally {
			try {
				// 3. 清空session上的写请求队列
				clearWriteRequestQueue(session);
				// 4. 通知service上的监听器session已被销毁
				((AbstractIoService) session.getService()).getListeners().fireSessionDestroyed(session);
			} catch (Exception e) {
                // The session was either destroyed or not at this point.
                // We do not want any exception thrown from this "cleanup" code to change
                // the return value by bubbling up.
                IoFilterChain filterChain = session.getFilterChain();
                filterChain.fireExceptionCaught(e);
            }
		}
    	return false;
    }
    
    /**
     * 清空session上的写请求队列，销毁session的时候调用
     * @param session
     */
    private void clearWriteRequestQueue(S session) {
    	WriteRequestQueue writeRequestQueue = session.getWriteRequestQueue();
    	WriteRequest request;
    	
    	// 用来保存没写的，在本方法中被移除的WriteRequest的队列。
    	List<WriteRequest> failedRequests = new ArrayList<WriteRequest>();
    	
    	if ((request = writeRequestQueue.poll(session)) != null) {
			Object message = request.getMessage();
			if (message instanceof IoBuffer) {
				IoBuffer buffer = (IoBuffer) message;
				// The first unwritten empty buffer must be
                // forwarded to the filter chain.
				// 第一个没写的空buffer必须转发给过滤器链。
				if (buffer.hasRemaining()) {
					buffer.reset();
					failedRequests.add(request);
				} else {
					IoFilterChain filterChain = session.getFilterChain();
					filterChain.fireMessageSent(request);
				}
			} else {
				failedRequests.add(request);
			}
			// Discard others.
			// 丢弃其它的的请求
			while ((request = writeRequestQueue.poll(session)) != null) {
				failedRequests.add(request);
			}
		}
    	
    	// Create an exception and notify.
    	// 如果有被移除的WriteRequest，创建异常并通知过滤器链。
    	if (!failedRequests.isEmpty()) {
			WriteToClosedSessionException cause = new WriteToClosedSessionException(failedRequests);
			for (WriteRequest r : failedRequests) {
				session.decreaseScheduledBytesAndMessages(r);
				r.getFuture().setException(cause);
			}
			IoFilterChain filterChain = session.getFilterChain();
			filterChain.fireExceptionCaught(cause);
		}
    }
    
    /**
     * 处理上一次select()调用时，所有准备好能做I/O操作的session，给它们做I/O操作。
     * @throws Exception
     */
    private void process() throws Exception {
    	for (Iterator<S> iterator = selectedSessions(); iterator.hasNext();) {
    		S session = iterator.next();
    		process(session);
    		iterator.remove();
    	}
    }
    
    /**
     * Deal with session ready for the read or write operations, or both.
     * 
     * 处理已做好I/O操作准备的session，读、写。
     */
    private void process(S session) {
    	// Process Reads
    	// 1. 处理读
    	if (isReadable(session) && !session.isReadSuspended()) {
			read(session);
		}
    	
    	// Process writes
    	// 2. 处理写
    	if (isWritable(session) && !session.isWriteSuspended()) {
    		// add the session to the queue, if it's not already there
    		// 如果成功设置session的scheduledForFlush，就把它放入flushingSessions队列中。
    		if (session.setScheduledForFlush(true)) {
				flushingSessions.add(session);
			}
		}
    }
    
    /**
     * 对session做读操作
     * @param session
     */
    private void read(S session) {
    	IoSessionConfig config = session.getConfig();
    	int bufferSize = config.getReadBufferSize();
    	IoBuffer buffer = IoBuffer.allocate(bufferSize);
    	
    	// 返回底层传输通道是否支持对传输的数据进行切片和重组，TCP支持UDP不支持。
    	final boolean hasFragmentation = session.getTransportMetadata().hasFragmentation();
    	try {
			int readBytes = 0;
			int ret;
			try {
				if (hasFragmentation) {
					// 支持数据分片时，如果读完了session中的数据或buffer满了时退出循环，这里有可能session中数据没读完。
					while ((ret = read(session, buffer)) > 0) {
						readBytes += ret;
						if (!buffer.hasRemaining()) {
							break;
						}
					}
				} else {
					// 不支持数据分片时，读一次数据，这里可能读完了session中的数据，也可能没读完。
					ret = read(session, buffer);
					if (ret > 0) {
						readBytes = ret;
					}
				}
			} finally {
				// flip操作，可以使填充了数据的buffer准备被读取数据。
				buffer.flip();
			}
			
			if (readBytes > 0) {
				IoFilterChain filterChain = session.getFilterChain();
				filterChain.fireMessageReceived(buffer);
				buffer = null;
				if (hasFragmentation) {
					// 支持数据分片时，如果读到的数据大小小于读缓存的一半大小，收缩缓存大小。
					if (readBytes << 1 < config.getReadBufferSize()) {
						session.decreaseReadBufferSize();
					} 
					// 如果读到的数据大小等于读缓存的大小，扩容缓存大小。
					else if (readBytes == config.getReadBufferSize()) {
						session.increaseReadBufferSize();
					}
				}
			}
			if (ret < 0) {
				// 如果session准备好了读，但是没读到任何数据，说明远端节点发送了fin(即远端节点挥手了)。
				// scheduleRemove(session);
				IoFilterChain filterChain = session.getFilterChain();
				filterChain.fireInputClosed();
			}
		} catch (Exception e) {
			if (e instanceof IOException) {
				if (
						// 当前异常不是端口不可达异常(应该是使用UDP向远端节点发消息时可能触发这个异常)
						!(e instanceof PortUnreachableException) 
						// session的配置类不是AbstractDatagramSessionConfig的子类，即当前不是UPD协议
						|| !AbstractDatagramSessionConfig.class.isAssignableFrom(config.getClass()) 
						// 如果是UPD协议时，且发生了端口不可达异常，看session配置中配置的当端口不可达时是否关闭
                        || ((AbstractDatagramSessionConfig) config).isCloseOnPortUnreachable()) {
					// 在这些情况下，准备移除session。
					scheduleRemove(session);
				}
			}
			// 通知过滤器链发生异常
			IoFilterChain filterChain = session.getFilterChain();
            filterChain.fireExceptionCaught(e);
		}
    }
    
    /**
     * 检测所有的session中是否有空闲状态的，空闲的要触发相应事件。
     * @param currentTime
     * @throws Exception
     */
    private void notifyIdleSessions(long currentTime) throws Exception {
    	// process idle sessions
    	if (currentTime - lastIdleCheckTime >= SELECT_TIMEOUT) {
			lastIdleCheckTime = currentTime;
			AbstractIoSession.notifyIdleness(allSessions(), currentTime);
		}
    }
    
    /**
     * Write all the pending messages
     * 
     * 写所有待写的消息(处理flushingSessions队列中的所有session)。
     */
    private void flush(long currentTime) {
    	if (flushingSessions.isEmpty()) {
			return;
		}
    	do {
    		// the same one with firstSession.
			S session = flushingSessions.poll();
			if (session == null) {
				// Just in case ... It should not happen.
                break;
			}
			// Reset the Schedule for flush flag for this session,
            // as we are flushing it now
			// 因为开始flush这个session了，所以重置它的scheduledForFlush为false，
			// 以便有要写的数据时其它线程可以将它再次放入flushingSessions队列中。
			session.unscheduledForFlush();
			SessionState state = getState(session);
			switch (state) {
				case OPENED:
					try {
						boolean flushedAll = flushNow(session, currentTime);
						// 刷新成功后，如果session中还有写请求，且没有被放在flushingSessions中时，放入flushingSessions队列中。
						if (flushedAll && !session.getWriteRequestQueue().isEmpty(session)
								&& !session.isScheduledForFlush()) {
							scheduleFlush(session);
						}
					} catch (Exception e) {
						scheduleRemove(session);
						session.closeNow();
						IoFilterChain filterChain = session.getFilterChain();
	                    filterChain.fireExceptionCaught(e);
					}
					break;
				case CLOSING:
	                // Skip if the channel is already closed.
	                break;
				case OPENING:
	                // Retry later if session is not yet fully initialized.
	                // (In case that Session.write() is called before addSession()
	                // is processed)
					// 当session还没初始化完成时，将session重新放入flushingSessions队列中以便重试。
					// 发生在Session.write()比addSession()方法先调用的情况下。
	                scheduleFlush(session);
	                // 这里源码中使用了return，感觉是用错了，应该是break。
	                // return; 
	                break;
				default:
					throw new IllegalStateException(String.valueOf(state));
					break;
			}
		} while (!flushingSessions.isEmpty());
    }
    
    /**
     * 写指定session的待写的消息。
     * @param session
     * @param currentTime
     * @return
     */
    private boolean flushNow(S session, long currentTime) {
    	// 1. session没有连接，将session加入removingSessions队列中，等待被移除。
    	if (!session.isConnected()) {
			scheduleRemove(session);
			return false;
		}
    	
    	final boolean hasFragmentation = session.getTransportMetadata().hasFragmentation();
    	final WriteRequestQueue writeRequestQueue = session.getWriteRequestQueue();
    	
    	// Set limitation for the number of written bytes for read-write
        // fairness. I used maxReadBufferSize * 3 / 2, which yields best
        // performance in my experience while not breaking fairness much.
    	// 2. 设置本次写的最大字节数。这里根据作者经验设置为maxReadBufferSize的1.5倍，可以取得最佳性能。
    	final int maxWrittenBytes = session.getConfig().getMaxReadBufferSize()
    			+ (session.getConfig().getMaxReadBufferSize() >>> 1);
    	int writtenBytes = 0;
    	WriteRequest request = null;
    	
    	try {
    		// 3. Clear OP_WRITE
    		setInterestedInWrite(session, false);
    		do {
    			// 4. Check for pending writes.
    			// 检测是否有当前的写请求(这里取到不是null，表示这个写请求上次没写完，这次继续写)。
    			request = session.getCurrentWriteRequest();
    			if (request == null) {
					request = writeRequestQueue.poll(session);
					if (request == null) {
						break;
					}
					session.setCurrentWriteRequest(request);
				}
    			
    			// 5. 写消息
    			int localWrittenBytes = 0;
    			Object message = request.getMessage();
    			if (message instanceof IoBuffer) {
					localWrittenBytes = writeBuffer(session, request, hasFragmentation, 
							maxWrittenBytes - writtenBytes, currentTime);
					// the buffer isn't empty, we re-interest it in writing
					// 6. 写成功了，但是buffer中还有待写的数据时，继续设置session对写感兴趣
					// 这个写请求中的数据一次没有写完，说明无法写更多数据了，先返回，等下次继续写。
					if ((localWrittenBytes > 0) && ((IoBuffer) message).hasRemaining()) {
						writtenBytes += localWrittenBytes;
						setInterestedInWrite(session, true);
						return false;
					}
				} else if (message instanceof FileRegion) {
					localWrittenBytes = writeFile(session, request, hasFragmentation,
							maxWrittenBytes - writtenBytes, currentTime);
					// Fix for Java bug on Linux
                    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5103988
                    // If there's still data to be written in the FileRegion,
                    // return 0 indicating that we need
                    // to pause until writing may resume.
					// 7. 写成功了，但是文件块中还有待写的数据时，继续设置session对写感兴趣
					// 这个写请求中的数据一次没有写完，说明无法写更多数据了，先返回，等下次继续写。
					if ((localWrittenBytes > 0) && (((FileRegion) message).getRemainingBytes() > 0)) {
						writtenBytes += localWrittenBytes;
						setInterestedInWrite(session, true);
						return false;
					}
				} else {
					throw new IllegalStateException("Don't know how to handle message of type '"
                            + message.getClass().getName() + "'.  Are you missing a protocol encoder?");
				}

				// Kernel buffer is full.
				// 6. 如果本次写成功的字节数是0，表示操作系统核心中待写数据满了，所以继续设置session对写感兴趣并返回，等待下次再写。
    			if (localWrittenBytes == 0) {
                    setInterestedInWrite(session, true);
                    return false;
				}

				// Wrote too much
    			// 7. 写了太多数据，超出了本次写入数据的最大值，为了性能平衡，先返回等待下次再写。
    			writtenBytes += localWrittenBytes;
    			if (writtenBytes >= maxWrittenBytes) {
                    scheduleFlush(session);
                    return false;
				}
    			
    			// 8. 释放IoBuffer
    			if (message instanceof IoBuffer) {
                    ((IoBuffer) message).free();
                }
			} while (writtenBytes < maxWrittenBytes);
    	} catch (Exception e) {
    		if (request != null) {
				request.getFuture().setException(e);
			}
    		// 告诉过滤器链有异常发生。
    		IoFilterChain filterChain = session.getFilterChain();
    		filterChain.fireExceptionCaught(e);
    		return false;
    	}
    	return true;
    }
    
    /**
     * 向session中写入IoBuffer
     * @param session
     * @param request
     * @param hasFragmentation
     * @param maxLength
     * @param currentTime
     * @return
     * @throws Exception
     */
    private int writeBuffer(S session, WriteRequest request, boolean hasFragmentation, int maxLength, long currentTime)
            throws Exception {
    	// 1. 获取待写的buffer。
    	IoBuffer buffer = (IoBuffer) request.getMessage();
    	int localWrittenBytes = 0;
    	if (buffer.hasRemaining()) {
    		// 2. 确定本次能写入的字节数。
			int length;
			if (hasFragmentation) {
				length = Math.min(buffer.remaining(), maxLength);
			} else {
				length = buffer.remaining();
			}
			try {
				// 3. 往session写入buffer中的数据
				localWrittenBytes = write(session, buffer, length);
			} catch (IOException e) {
				// We have had an issue while trying to send data to the
                // peer : let's close the session.
				// 重新发送数据还是会有问题，所以这里直接关闭这个session。
				buffer.free();
				session.closeNow();
				destroy(session);
				return 0;
			}
		}
    	
    	// 4. 增加session的已写入字节数。
    	session.increaseWrittenBytes(localWrittenBytes, currentTime);
    	
    	if (!buffer.hasRemaining() || (!hasFragmentation && (localWrittenBytes != 0))) {
        	// Buffer has been sent, clear the current request.
        	// 5. buffer被发送后，标记当前的position位置，重置position为上次mark位置，并触发过滤器链的消息已发送事件。
			int pos = buffer.position();
			buffer.reset(); // 这里为什么做reset()?
			fireMessageSent(session, request);
			// And set it back to its position
			// 6. 过滤器链处理完事件后，把positon位置标回。
			buffer.position(pos);
		}
    	return localWrittenBytes;
    }
    
    /**
     * 向session中写入FileRegion
     * @param session
     * @param request
     * @param hasFragmentation
     * @param maxLength
     * @param currentTime
     * @return
     * @throws Exception
     */
    private int writeFile(S session, WriteRequest request, boolean hasFragmentation, int maxLength, long currentTime)
            throws Exception {
    	// 1. 获取待写的region。
    	FileRegion region = (FileRegion) request.getMessage();
    	int localWrittenBytes = 0;
    	if (region.getRemainingBytes() > 0) {
    		// 2. 确定本次能写入的字节数。
            int length;
            if (hasFragmentation) {
                length = (int) Math.min(region.getRemainingBytes(), maxLength);
            } else {
                length = (int) Math.min(Integer.MAX_VALUE, region.getRemainingBytes());
            }
            // 3. 往session写入region中的数据
            localWrittenBytes = transferFile(session, region, length);
            region.update(localWrittenBytes);
        } else {
            localWrittenBytes = 0;
        }

    	// 4. 增加session的已写入字节数。
        session.increaseWrittenBytes(localWrittenBytes, currentTime);

        // 5. 触发过滤器链的消息已发送事件。
        if ((region.getRemainingBytes() <= 0) || (!hasFragmentation && (localWrittenBytes != 0))) {
            fireMessageSent(session, request);
        }

        return localWrittenBytes;
    }
    
    /**
     * 触发过滤器链的message已发送事件。
     * @param session
     * @param req
     */
    private void fireMessageSent(S session, WriteRequest req) {
        session.setCurrentWriteRequest(null);
        IoFilterChain filterChain = session.getFilterChain();
        filterChain.fireMessageSent(req);
    }
    
    /**
     * Update the trafficControl for all the session.
     * 
     * 更新所有session的trafficControl(阻塞或恢复读写)
     */
    private void updateTrafficMask() {
    	int queueSize = trafficControllingSessions.size();
    	while (queueSize > 0) {
    		S session = trafficControllingSessions.poll();
			// We are done with this queue.
    		// 队列中的session已处理完毕，直接返回。
    		if (session == null) {
                return;
			}
    		SessionState state = getState(session);
    		switch (state) {
				case OPENED:
					updateTrafficControl(session);
					break;
				case CLOSING:
					break;
				case OPENING:
					// Retry later if session is not yet fully initialized.
	                // (In case that Session.suspend??() or session.resume??() is
	                // called before addSession() is processed)
	                // We just put back the session at the end of the queue.
					// 当session还没初始化完成时，将session重新放入trafficControllingSessions队列中以便重试。
					// 发生在Session.suspend()或resume()比addSession()方法先调用的情况下。
	                trafficControllingSessions.add(session);
					break;
				default:
					throw new IllegalStateException(String.valueOf(state));
					break;
			}
    		// As we have handled one session, decrement the number of
            // remaining sessions. The OPENING session will be processed
            // with the next select(), as the queue size has been decreased,
            // even
            // if the session has been pushed at the end of the queue
    		// 每处理完一个session，就将计数器减1。
    		// 这样就算同时有新的session加入到队列中，也要等下次select()操作时处理。
            queueSize--;
    	}
    }
    
    /**
     * {@inheritDoc}
     */
    public void updateTrafficControl(S session) {
    	try {
    		setInterestedInRead(session, !session.isReadSuspended());
    	} catch (Exception e) {
    		IoFilterChain filterChain = session.getFilterChain();
            filterChain.fireExceptionCaught(e);
    	}
    	
    	try {
			setInterestedInWrite(session, 
					!session.getWriteRequestQueue().isEmpty(session) && !session.isWriteSuspended());
		} catch (Exception e) {
			IoFilterChain filterChain = session.getFilterChain();
            filterChain.fireExceptionCaught(e);
		}
    }
    
    /**
     * The main loop. This is the place in charge to poll the Selector, and to
     * process the active sessions. It's done in - handle the newly created
     * sessions -
     * 
     * 程序的主循环：负责轮询selector，并处理准备好I/O的session上的读写事件。
     * 它做的是：处理已经被创建的session。
     */
    private class Processor implements Runnable {
        public void run() {
        	// 1. 判断processorRef保存的实例是否就是本实例。
        	assert (processorRef.get() == this);
        	
        	// 2. 初始化lastIdleCheckTime的值为系统当前时间。
        	int nSessions = 0;
            lastIdleCheckTime = System.currentTimeMillis();
            
            for (;;) {
            	try {
            		// This select has a timeout so that we can manage
                    // idle session when we get out of the select every
                    // second. (note : this is a hack to avoid creating
                    // a dedicated thread).
            		// 2. 本选择器有一个超时时间，所以每次select的时候我们可以管理空闲状态的session。
            		// 备注：这是避免使用一专有线程的方法。
            		long t0 = System.currentTimeMillis();
            		int selected = select(SELECT_TIMEOUT);
            		long t1 = System.currentTimeMillis();
            		long delta = t1 - t0;
            		
            		if (!wakeupCalled.getAndSet(false) && selected == 0 && delta < 100) {
            			// Last chance : the select() may have been
                        // interrupted because we have had an closed channel.
            			// 3. 最后的机会：select()可能被中断，因为我们可能有一个被关闭的通道。
            			if (isBrokenConnection()) {
            				LOG.warn("Broken connection");
                        } else {
                            LOG.warn("Create a new selector. Selected is 0, delta = " + (t1 - t0));
                            // Ok, we are hit by the nasty epoll
                            // spinning.
                            // Basically, there is a race condition
                            // which causes a closing file descriptor not to be
                            // considered as available as a selected channel,
                            // but
                            // it stopped the select. The next time we will
                            // call select(), it will exit immediately for the
                            // same
                            // reason, and do so forever, consuming 100%
                            // CPU.
                            // We have to destroy the selector, and
                            // register all the socket on a new one.
                            // 4. 我们被令人不快的epoll打击了：那儿有一个竞争条件，会导致关闭文件描述符并关闭select操作
                            // 但是不被认为是可作为选择的通道。如果下次我们继续调用select()，就会因为相同的原因而立即返回，
                            // 如果一直这么操作，会导致CPU占用100%，所以要生成一个新的选择器，并注册所有socket。
                            registerNewSelector();
                        }
					}
            		
            		// Manage newly created session first
            		// 5. 处理新创建的session：初始化、创建过滤器链、触发监听session创建的事件。
                    nSessions += handleNewSessions();
                    
                    // 6. 更新所有session的trafficControl(阻塞或恢复读写)。
                    updateTrafficMask();
                    
                    // Now, if we have had some incoming or outgoing events,
                    // deal with them
                    // 7. 处理已准备好I/O操作的session。
                    if (selected > 0) {
                    	// LOG.debug("Processing ..."); // This log hurts one of
                        // the MDCFilter test...
                        process();
                    }
                    
                    // Write the pending requests
                    // 8. 写所有待写的消息(处理flushingSessions队列中的所有session)。
                    long currentTime = System.currentTimeMillis();
                    flush(currentTime);
                    
                    // And manage removed sessions
                    // 9. 移除待移除队列中的session。
                    nSessions -= removeSessions();
                    
                    // Last, not least, send Idle events to the idle sessions
                    // 10. 已处于空闲状态的session，触发空闲事件。
                    notifyIdleSessions(currentTime);
                    
                    // Get a chance to exit the infinite loop if there are no
                    // more sessions on this Processor
                    // 11. 如果当前processor上已没有session了，退出此无限循环。
                    if (nSessions == 0) {
                    	processorRef.set(null);
                    	if (newSessions.isEmpty() && isSelectorEmpty()) {
                        	// newSessions.add() precedes startupProcessor
                    		assert (processorRef.get() != this);
                            break;
						}
                    	assert (processorRef.get() != this);
                    	if (!processorRef.compareAndSet(null, this)) {
                            // startupProcessor won race, so must exit processor
                            assert (processorRef.get() != this);
                            break;
                        }
                        assert (processorRef.get() == this);
                    }
                    
                    // Disconnect all sessions immediately if disposal has been
                    // requested so that we exit this loop eventually.
                    // 12. 如果当前processor已在清理，立即结束所有session的连接，并退出此无限循环。
                    if (isDisposing()) {
                    	boolean hasKeys = false;
                    	for (Iterator<S> iterator = allSessions(); iterator.hasNext();) {
                    		S session = iterator.next();
                    		if (session.isActive()) {
								scheduleRemove(session);
							}
                    	}
                    	if (hasKeys) {
							wakeup();
						}
                    }
            	} catch (ClosedSelectorException e) {
                    // If the selector has been closed, we can exit the loop
                    // But first, dump a stack trace
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                    break;
                } catch (Exception e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        ExceptionMonitor.getInstance().exceptionCaught(e1);
                    }
                }
            }
            // 13. 通过轮询所有客户端连接来释放当前processor使用的资源。
            try {
                synchronized (disposalLock) {
                    if (disposing) {
                        doDispose();
                    }
                }
            } catch (Exception e) {
                ExceptionMonitor.getInstance().exceptionCaught(e);
            } finally {
                disposalFuture.setValue(true);
            }
        }
    }
}
