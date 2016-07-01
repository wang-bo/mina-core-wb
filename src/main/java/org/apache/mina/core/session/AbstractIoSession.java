package org.apache.mina.core.session;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.file.DefaultFileRegion;
import org.apache.mina.core.file.FilenameFileRegion;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.DefaultCloseFuture;
import org.apache.mina.core.future.DefaultReadFuture;
import org.apache.mina.core.future.DefaultWriteFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.ReadFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.AbstractIoService;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteException;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestQueue;
import org.apache.mina.core.write.WriteTimeoutException;
import org.apache.mina.core.write.WriteToClosedSessionException;
import org.apache.mina.util.ExceptionMonitor;

/**
 * Base implementation of {@link IoSession}.
 * 
 * 一个实现IoSession接口的基类
 * 
 * @date	2016年6月16日 上午10:00:59	completed
 */
public abstract class AbstractIoSession implements IoSession {

	/** The associated handler. 当前session关联的处理器 */
    private final IoHandler handler;
    
    /** The session config. 当前session关联的配置类 */
    protected IoSessionConfig config;

    /** The service which will manage this session. 管理当前session的service */
    private final IoService service;
    
    private static final AttributeKey READY_READ_FUTURES_KEY = 
    		new AttributeKey(AbstractIoSession.class, "readyReadFutures");
    
    private static final AttributeKey WAITING_READ_FUTURES_KEY = 
    		new AttributeKey(AbstractIoSession.class, "waitingReadFutures");
    
    /** 当前session关闭成功后触发的监听器：清理各统计数值项的值 */
    private static final IoFutureListener<CloseFuture> SCHEDULED_COUNTER_RESETTER = new IoFutureListener<CloseFuture>() {
        public void operationComplete(CloseFuture future) {
            AbstractIoSession session = (AbstractIoSession) future.getSession();
            session.scheduledWriteBytes.set(0);
            session.scheduledWriteMessages.set(0);
            session.readBytesThroughput = 0;
            session.readMessagesThroughput = 0;
            session.writtenBytesThroughput = 0;
            session.writtenMessagesThroughput = 0;
        }
    };
    
    /**
     * An internal write request object that triggers session close.
     * 
     * 一个内部的写请求对象，用来触发session关闭。
     * 
     * @see #writeRequestQueue
     */
    private static final WriteRequest CLOSE_REQUEST = new DefaultWriteRequest(new Object());
    
    private final Object lock = new Object();

    /** 保存自定义属性的Map */
    private IoSessionAttributeMap attributes;

    /** 保存写请求的队列 */
    private WriteRequestQueue writeRequestQueue;

    /** IoService在处理的当前session上的WriteRequest */
    private WriteRequest currentWriteRequest;
    
    /** The Session creation's time. 当前session创建时间 */
    private final long creationTime;

    /** An id generator guaranteed to generate unique IDs for the session. 为session生成唯一ID的生成器 */
    private static AtomicLong idGenerator = new AtomicLong(0);

    /** The session ID */
    private long sessionId;
    
    /** A future that will be set 'closed' when the connection is closed. 当前session关闭时的CloseFuture */
    private final CloseFuture closeFuture = new DefaultCloseFuture(this);

    /** 标记当前session是否处于关闭中状态 */
    private volatile boolean closing;

    /** traffic control. 传输控制开关 */
    private boolean readSuspended = false;

    private boolean writeSuspended = false;

    /** Status variables. 标记状态的变量 */
    private final AtomicBoolean scheduledForFlush = new AtomicBoolean();

    private final AtomicInteger scheduledWriteBytes = new AtomicInteger();

    private final AtomicInteger scheduledWriteMessages = new AtomicInteger();

    private long readBytes;

    private long writtenBytes;

    private long readMessages;

    private long writtenMessages;

    private long lastReadTime;

    private long lastWriteTime;

    private long lastThroughputCalculationTime;

    private long lastReadBytes;

    private long lastWrittenBytes;

    private long lastReadMessages;

    private long lastWrittenMessages;

    private double readBytesThroughput;

    private double writtenBytesThroughput;

    private double readMessagesThroughput;

    private double writtenMessagesThroughput;

    private AtomicInteger idleCountForBoth = new AtomicInteger();

    private AtomicInteger idleCountForRead = new AtomicInteger();

    private AtomicInteger idleCountForWrite = new AtomicInteger();

    private long lastIdleTimeForBoth;

    private long lastIdleTimeForRead;

    private long lastIdleTimeForWrite;

    private boolean deferDecreaseReadBuffer = true;
    
    /**
     * Create a Session for a service
     * 
     * 构造方法
     * 
     * @param service the Service for this session
     */
    protected AbstractIoSession(IoService service) {
        this.service = service;
        this.handler = service.getHandler();

        // Initialize all the Session counters to the current time
        // 在session创建时，初始化各统计项的统计时间为当前值。
        long currentTime = System.currentTimeMillis();
        creationTime = currentTime;
        lastThroughputCalculationTime = currentTime;
        lastReadTime = currentTime;
        lastWriteTime = currentTime;
        lastIdleTimeForBoth = currentTime;
        lastIdleTimeForRead = currentTime;
        lastIdleTimeForWrite = currentTime;

        // 添加监听当前session关闭成功事件的监听器，用来清理各统计数据项的值。
        closeFuture.addListener(SCHEDULED_COUNTER_RESETTER);

        // Set a new ID for this session
        sessionId = idGenerator.incrementAndGet();
    }
    
    /**
     * {@inheritDoc}
     * 
     * We use an AtomicLong to guarantee that the session ID are unique.
     */
    public final long getId() {
        return sessionId;
    }
    
    /**
     * @return The associated IoProcessor for this session
     * 
     * 返回当前session管理的IoProcessor
     */
    public abstract IoProcessor<AbstractIoSession> getProcessor();
    
    /**
     * {@inheritDoc}
     */
    public final boolean isConnected() {
        return !closeFuture.isClosed();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isActive() {
        // Return true by default
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isClosing() {
        return closing || closeFuture.isClosed();
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isSecured() {
        // Always false...
        return false;
    }
    
    /**
     * {@inheritDoc}
     */
    public final CloseFuture getCloseFuture() {
        return closeFuture;
    }
    
    /**
     * Tells if the session is scheduled for flushed
     * 
     * 返回当前session是否已安排刷新。
     * 
     * @return true if the session is scheduled for flush
     */
    public final boolean isScheduledForFlush() {
        return scheduledForFlush.get();
    }
    
    /**
     * Schedule the session for flushed
     * 
     * 安排当前session刷新，标记scheduledForFlush为true。
     */
    public final void scheduledForFlush() {
        scheduledForFlush.set(true);
    }
    
    /**
     * Change the session's status : it's not anymore scheduled for flush
     * 
     * 取消安排当前session刷新，标记scheduledForFlush为false。
     */
    public final void unscheduledForFlush() {
        scheduledForFlush.set(false);
    }
    
    /**
     * Set the scheduledForFLush flag. As we may have concurrent access to this
     * flag, we compare and set it in one call.
     * 
     * 设置scheduledForFlush标记的值。
     * 因为有可能多个线程在同时访问这个字段，我们在一次调用中使用比较再设置值。
     * 
     * @param schedule
     *            the new value to set if not already set.
     * @return true if the session flag has been set, and if it wasn't set
     *         already.
     */
    public final boolean setScheduledForFlush(boolean schedule) {
    	if (schedule) {
    		// If the current tag is set to false, switch it to true,
            // otherwise, we do nothing but return false : the session
            // is already scheduled for flush
    		// 如果scheduledForFlush的值为false，修改为true，否则不修改并返回false：因为如值为true表示当前session已经处于被安排刷新状态了。
    		return scheduledForFlush.compareAndSet(false, schedule);
		}
    	scheduledForFlush.set(schedule);
    	return true;
    }
    
    /**
     * {@inheritDoc}
     */
    public final CloseFuture close(boolean immediately) {
    	if (immediately) {
    		return closeNow();
        } else {
            return closeOnFlush();
        }
    }
    
    /**
     * {@inheritDoc}
     * 
     * 感觉这里不用在finally中写return，参考上面的close(boolean)方法的实现。
     */
    public final CloseFuture close() {
    	return closeNow();
//        try {
//            closeNow();
//        } finally {
//            return closeFuture;
//        }
    }
    
    /**
     * {@inheritDoc}
     */
    public final CloseFuture closeOnFlush() {
    	if (!isClosing()) {
			getWriteRequestQueue().offer(this, CLOSE_REQUEST);
			getProcessor().flush(this);
			return closeFuture;
		} else {
			return closeFuture;
		}
    }
    
    /**
     * {@inheritDoc}
     */
    public final CloseFuture closeNow() {
    	synchronized (lock) {
            if (isClosing()) {
                return closeFuture;
            }
            closing = true;
        }
    	getFilterChain().fireFilterClose();
    	return closeFuture;
    }
    
    /**
     * {@inheritDoc}
     */
    public IoHandler getHandler() {
        return handler;
    }

    /**
     * {@inheritDoc}
     */
    public IoSessionConfig getConfig() {
        return config;
    }

    /**
     * {@inheritDoc}
     */
    public final ReadFuture read() {
    	// 1. 检测是否允许读
    	if (!getConfig().isUseReadOperation()) {
            throw new IllegalStateException("useReadOperation is not enabled.");
        }
    	
    	// 2. 获取读操作已完成的ReadFuture队列
    	Queue<ReadFuture> readyReadFutures = getReadyReadFutures();
    	ReadFuture future;
        synchronized (readyReadFutures) {
        	future = readyReadFutures.poll();
        	if (future != null) {
				// 3. Let other readers get notified.
        		// 如果future关联的session已关闭，重新放入队列，然后返回future？这里为何如此，还不是很理解...
				if (future.isClosed()) {
					readyReadFutures.offer(future);
				}
			} else {
				future = new DefaultReadFuture(this);
                getWaitingReadFutures().offer(future);
			}
        }
        return future;
    }
    
    /**
     * Associates a message to a ReadFuture
     * 
     * 把读取到的message放入ReadFuture
     * 
     * @param message the message to associate to the ReadFuture
     * 
     */
    public final void offerReadFuture(Object message) {
    	newReadFuture().setRead(message);
    }
    
    /**
     * Associates a failure to a ReadFuture
     * 
     * 把读取过程中发生的异常放入ReadFuture
     * 
     * @param exception the exception to associate to the ReadFuture
     */
    public final void offerFailedReadFuture(Throwable exception) {
        newReadFuture().setException(exception);
    }
    
    /**
     * Inform the ReadFuture that the session has been closed
     * 
     * 通知ReadFuture当前session已经关闭。
     */
    public final void offerClosedReadFuture() {
        Queue<ReadFuture> readyReadFutures = getReadyReadFutures();
        synchronized (readyReadFutures) {
            newReadFuture().setClosed();
        }
    }
    
    /**
     * @return a readFuture get from the waiting ReadFuture
     * 
     * 从等待读的ReadFuture队列中获取一个ReadFuture：如果没有话就新建一个并放入完成读的ReadFuture队列中
     */
    private ReadFuture newReadFuture() {
        Queue<ReadFuture> readyReadFutures = getReadyReadFutures();
        Queue<ReadFuture> waitingReadFutures = getWaitingReadFutures();
        ReadFuture future;
        synchronized (readyReadFutures) {
            future = waitingReadFutures.poll();
            if (future == null) {
                future = new DefaultReadFuture(this);
                readyReadFutures.offer(future);
            }
        }
        return future;
    }
    
    /**
     * @return a queue of ReadFuture
     * 
     * 返回完成读的ReadFuture队列：如果不存在就new一个。
     */
    private Queue<ReadFuture> getReadyReadFutures() {
    	Queue<ReadFuture> readyReadFutures = (Queue<ReadFuture>) getAttribute(READY_READ_FUTURES_KEY);
    	if (readyReadFutures == null) {
			readyReadFutures = new ConcurrentLinkedQueue<ReadFuture>();
			Queue<ReadFuture> oldReadyReadFutures = 
					(Queue<ReadFuture>) setAttributeIfAbsent(READY_READ_FUTURES_KEY, readyReadFutures);
			if (oldReadyReadFutures != null) {
				readyReadFutures = oldReadyReadFutures;
			}
		}
    	return readyReadFutures;
    }
    
    /**
     * @return the queue of waiting ReadFuture
     * 
     * 返回等待读的ReadFuture队列：如果不存在就new一个。
     */
    private Queue<ReadFuture> getWaitingReadFutures() {
        Queue<ReadFuture> waitingReadyReadFutures = (Queue<ReadFuture>) getAttribute(WAITING_READ_FUTURES_KEY);
        if (waitingReadyReadFutures == null) {
            waitingReadyReadFutures = new ConcurrentLinkedQueue<ReadFuture>();
            Queue<ReadFuture> oldWaitingReadyReadFutures = (Queue<ReadFuture>) setAttributeIfAbsent(
                    WAITING_READ_FUTURES_KEY, waitingReadyReadFutures);
            if (oldWaitingReadyReadFutures != null) {
                waitingReadyReadFutures = oldWaitingReadyReadFutures;
            }
        }
        return waitingReadyReadFutures;
    }
    
    /**
     * {@inheritDoc}
     */
    public WriteFuture write(Object message) {
        return write(message, null);
    }
    
    /**
     * {@inheritDoc}
     */
    public WriteFuture write(Object message, SocketAddress remoteAddress) {
        if (message == null) {
            throw new IllegalArgumentException("Trying to write a null message : not allowed");
        }

        // We can't send a message to a connected session if we don't have
        // the remote address
        // 如果没有远程Socket地址，无法发送数据
        if (!getTransportMetadata().isConnectionless() && (remoteAddress != null)) {
			throw new UnsupportedOperationException();
		}
        
        // If the session has been closed or is closing, we can't either
        // send a message to the remote side. We generate a future
        // containing an exception.
        // 如果当前session已关闭或没有连接，则创建一个future并设置一个exception。
        if (isClosing() || !isConnected()) {
			WriteFuture future = new DefaultWriteFuture(this);
			WriteRequest request = new DefaultWriteRequest(message, future, remoteAddress);
			WriteException exception = new WriteToClosedSessionException(request);
			future.setException(exception);
			return future;
		}
        
        // TODO: remove this code as soon as we use InputStream
        // instead of Object for the message.
        FileChannel openedFileChannel = null;
        try {
        	if ((message instanceof IoBuffer) && !((IoBuffer) message).hasRemaining()) {
        		// Nothing to write : probably an error in the user code
        		// IoBuffer中无数据可写。
                throw new IllegalArgumentException("message is empty. Forgot to call flip()?");
			} else if (message instanceof FileChannel) {
				FileChannel fileChannel = (FileChannel) message;
				message = new DefaultFileRegion(fileChannel, 0, fileChannel.size());
			} else if (message instanceof File) {
				File file = (File) message;
				openedFileChannel = new FileInputStream(file).getChannel();
				message = new FilenameFileRegion(file, openedFileChannel, 0, openedFileChannel.size());
			}
        } catch (IOException e) {
        	ExceptionMonitor.getInstance().exceptionCaught(e);
        	return DefaultWriteFuture.newNotWrittenFuture(this, e);
        }
        
        // Now, we can write the message. First, create a future
        // 开始写message，首先创建WriteFuture。
        WriteFuture writeFuture = new DefaultWriteFuture(this);
        WriteRequest writeRequest = new DefaultWriteRequest(message, writeFuture, remoteAddress);
        
    	// Then, get the chain and inject the WriteRequest into it
        // 然后，将WriteRequest通知给滤器链中的所有过滤器
        IoFilterChain filterChain = getFilterChain();
        filterChain.fireFilterWrite(writeRequest);
        
        // TODO : This is not our business ! The caller has created a
        // FileChannel,
        // he has to close it !
        if (openedFileChannel != null) {
        	// If we opened a FileChannel, it needs to be closed when the write
            // has completed
        	final FileChannel fileChannel = openedFileChannel;
        	writeFuture.addListener(new IoFutureListener<WriteFuture>() {
        		public void operationComplete(WriteFuture future) {
        			try {
						fileChannel.close();
					} catch (IOException e) {
						ExceptionMonitor.getInstance().exceptionCaught(e);
					}
        		}
			});
		}
        
        // Return the WriteFuture
        return writeFuture;
    }
    
    /**
     * {@inheritDoc}
     */
    public final Object getAttachment() {
        return getAttribute("");
    }

    /**
     * {@inheritDoc}
     */
    public final Object setAttachment(Object attachment) {
        return setAttribute("", attachment);
    }

    /**
     * {@inheritDoc}
     */
    public final Object getAttribute(Object key) {
        return getAttribute(key, null);
    }

    /**
     * {@inheritDoc}
     */
    public final Object getAttribute(Object key, Object defaultValue) {
        return attributes.getAttribute(this, key, defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    public final Object setAttribute(Object key, Object value) {
        return attributes.setAttribute(this, key, value);
    }

    /**
     * {@inheritDoc}
     */
    public final Object setAttribute(Object key) {
        return setAttribute(key, Boolean.TRUE);
    }

    /**
     * {@inheritDoc}
     */
    public final Object setAttributeIfAbsent(Object key, Object value) {
        return attributes.setAttributeIfAbsent(this, key, value);
    }

    /**
     * {@inheritDoc}
     */
    public final Object setAttributeIfAbsent(Object key) {
        return setAttributeIfAbsent(key, Boolean.TRUE);
    }

    /**
     * {@inheritDoc}
     */
    public final Object removeAttribute(Object key) {
        return attributes.removeAttribute(this, key);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean removeAttribute(Object key, Object value) {
        return attributes.removeAttribute(this, key, value);
    }
    
    /**
     * {@inheritDoc}
     */
    public final boolean replaceAttribute(Object key, Object oldValue, Object newValue) {
        return attributes.replaceAttribute(this, key, oldValue, newValue);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean containsAttribute(Object key) {
        return attributes.containsAttribute(this, key);
    }

    /**
     * {@inheritDoc}
     */
    public final Set<Object> getAttributeKeys() {
        return attributes.getAttributeKeys(this);
    }

    /**
     * @return The map of attributes associated with the session
     * 
     * 返回当前session关联的保存用户自定义属性的Map。
     */
    public final IoSessionAttributeMap getAttributeMap() {
        return attributes;
    }

    /**
     * Set the map of attributes associated with the session
     * 
     * 设置当前session关联的保存用户自定义属性的Map。
     * 
     * @param attributes The Map of attributes
     */
    public final void setAttributeMap(IoSessionAttributeMap attributes) {
        this.attributes = attributes;
    }
    
    /**
     * Create a new close aware write queue, based on the given write queue.
     * 
     * 设置当前session的保存写请求的队列，使用CloseAwareWriteQueue，这个队列在获取到CLOSE_REQUEST时，会关闭session。
     * 
     * @param writeRequestQueue The write request queue
     */
    public final void setWriteRequestQueue(WriteRequestQueue writeRequestQueue) {
        this.writeRequestQueue = new CloseAwareWriteQueue(writeRequestQueue);
    }
    
    /**
     * {@inheritDoc}
     */
    public final void suspendRead() {
    	readSuspended = true;
    	if (isClosing() || !isConnected()) {
			return;
		}
    	getProcessor().updateTrafficControl(this);
    }
    
    /**
     * {@inheritDoc}
     */
    public final void suspendWrite() {
        writeSuspended = true;
        if (isClosing() || !isConnected()) {
            return;
        }
        getProcessor().updateTrafficControl(this);
    }
    
    /**
     * {@inheritDoc}
     */
    public final void resumeRead() {
        readSuspended = false;
        if (isClosing() || !isConnected()) {
            return;
        }
        getProcessor().updateTrafficControl(this);
    }

    /**
     * {@inheritDoc}
     */
    public final void resumeWrite() {
        writeSuspended = false;
        if (isClosing() || !isConnected()) {
            return;
        }
        getProcessor().updateTrafficControl(this);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isReadSuspended() {
        return readSuspended;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isWriteSuspended() {
        return writeSuspended;
    }
    
    /**
     * {@inheritDoc}
     */
    public final long getReadBytes() {
        return readBytes;
    }

    /**
     * {@inheritDoc}
     */
    public final long getWrittenBytes() {
        return writtenBytes;
    }

    /**
     * {@inheritDoc}
     */
    public final long getReadMessages() {
        return readMessages;
    }

    /**
     * {@inheritDoc}
     */
    public final long getWrittenMessages() {
        return writtenMessages;
    }

    /**
     * {@inheritDoc}
     */
    public final double getReadBytesThroughput() {
        return readBytesThroughput;
    }

    /**
     * {@inheritDoc}
     */
    public final double getWrittenBytesThroughput() {
        return writtenBytesThroughput;
    }

    /**
     * {@inheritDoc}
     */
    public final double getReadMessagesThroughput() {
        return readMessagesThroughput;
    }

    /**
     * {@inheritDoc}
     */
    public final double getWrittenMessagesThroughput() {
        return writtenMessagesThroughput;
    }
    
    /**
     * {@inheritDoc}
     */
    public final void updateThroughput(long currentTime, boolean force) {
    	int interval = (int) (currentTime - lastThroughputCalculationTime);
        long minInterval = getConfig().getThroughputCalculationIntervalInMillis();
        
        if (((minInterval == 0) || (interval < minInterval)) && !force) {
            return;
        }

        readBytesThroughput = (readBytes - lastReadBytes) * 1000.0 / interval;
        writtenBytesThroughput = (writtenBytes - lastWrittenBytes) * 1000.0 / interval;
        readMessagesThroughput = (readMessages - lastReadMessages) * 1000.0 / interval;
        writtenMessagesThroughput = (writtenMessages - lastWrittenMessages) * 1000.0 / interval;

        lastReadBytes = readBytes;
        lastWrittenBytes = writtenBytes;
        lastReadMessages = readMessages;
        lastWrittenMessages = writtenMessages;

        lastThroughputCalculationTime = currentTime;
    }
    
    /**
     * {@inheritDoc}
     */
    public final long getScheduledWriteBytes() {
        return scheduledWriteBytes.get();
    }
    
    /**
     * {@inheritDoc}
     */
    public final int getScheduledWriteMessages() {
        return scheduledWriteMessages.get();
    }

    /**
     * Set the number of scheduled write bytes
     * 
     * @param byteCount The number of scheduled bytes for write
     */
    protected void setScheduledWriteBytes(int byteCount) {
        scheduledWriteBytes.set(byteCount);
    }

    /**
     * Set the number of scheduled write messages
     * 
     * @param messages The number of scheduled messages for write
     */
    protected void setScheduledWriteMessages(int messages) {
        scheduledWriteMessages.set(messages);
    }
    
    /**
     * Increase the number of read bytes
     * 
     * @param increment The number of read bytes
     * @param currentTime The current time
     */
    public final void increaseReadBytes(long increment, long currentTime) {
    	readMessages++;
        lastReadTime = currentTime;
        idleCountForBoth.set(0);
        idleCountForRead.set(0);
        
        if (getService() instanceof AbstractIoService) {
        	((AbstractIoService) getService()).getStatistics().increaseReadMessages(currentTime);
		}
    }
    
    /**
     * Increase the number of read messages
     * 
     * @param currentTime The current time
     */
    public final void increaseReadMessages(long currentTime) {
        readMessages++;
        lastReadTime = currentTime;
        idleCountForBoth.set(0);
        idleCountForRead.set(0);

        if (getService() instanceof AbstractIoService) {
            ((AbstractIoService) getService()).getStatistics().increaseReadMessages(currentTime);
        }
    }
    
    /**
     * Increase the number of written bytes
     * 
     * @param increment The number of written bytes
     * @param currentTime The current time
     */
    public final void increaseWrittenBytes(int increment, long currentTime) {
        if (increment <= 0) {
            return;
        }

        writtenBytes += increment;
        lastWriteTime = currentTime;
        idleCountForBoth.set(0);
        idleCountForWrite.set(0);

        if (getService() instanceof AbstractIoService) {
            ((AbstractIoService) getService()).getStatistics().increaseWrittenBytes(increment, currentTime);
        }

        increaseScheduledWriteBytes(-increment);
    }
    
    /**
     * Increase the number of written messages
     * 
     * 增加已写的message数。
     * 
     * @param request The written message
     * @param currentTime The current tile
     */
    public final void increaseWrittenMessages(WriteRequest request, long currentTime) {
        Object message = request.getMessage();

        if (message instanceof IoBuffer) {
            IoBuffer b = (IoBuffer) message;
            if (b.hasRemaining()) {
                return;
            }
        }

        writtenMessages++;
        lastWriteTime = currentTime;

        if (getService() instanceof AbstractIoService) {
            ((AbstractIoService) getService()).getStatistics().increaseWrittenMessages(currentTime);
        }

        decreaseScheduledWriteMessages();
    }
    
    /**
     * Increase the number of scheduled write bytes for the session
     * 
     * 增加准备写入session的字节数。
     * 
     * @param increment The number of newly added bytes to write
     */
    public final void increaseScheduledWriteBytes(int increment) {
        scheduledWriteBytes.addAndGet(increment);
        if (getService() instanceof AbstractIoService) {
            ((AbstractIoService) getService()).getStatistics().increaseScheduledWriteBytes(increment);
        }
    }

    /**
     * Increase the number of scheduled message to write
     * 
     * 增加准备写入session的message数。
     */
    public final void increaseScheduledWriteMessages() {
        scheduledWriteMessages.incrementAndGet();
        if (getService() instanceof AbstractIoService) {
            ((AbstractIoService) getService()).getStatistics().increaseScheduledWriteMessages();
        }
    }
    
    /**
     * Decrease the number of scheduled message written
     * 
     * 减少准备写入session的message数(因为已经写完成了)。
     */
    private void decreaseScheduledWriteMessages() {
        scheduledWriteMessages.decrementAndGet();
        if (getService() instanceof AbstractIoService) {
            ((AbstractIoService) getService()).getStatistics().decreaseScheduledWriteMessages();
        }
    }

    /**
     * Decrease the counters of written messages and written bytes when a message has been written
     * 
     * 减少准备写入session的字节数和message数(因为已经写完成了)。
     * 
     * @param request The written message
     */
    public final void decreaseScheduledBytesAndMessages(WriteRequest request) {
        Object message = request.getMessage();
        if (message instanceof IoBuffer) {
            IoBuffer b = (IoBuffer) message;
            if (b.hasRemaining()) {
                increaseScheduledWriteBytes(-((IoBuffer) message).remaining());
            } else {
                decreaseScheduledWriteMessages();
            }
        } else {
            decreaseScheduledWriteMessages();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public final WriteRequestQueue getWriteRequestQueue() {
        if (writeRequestQueue == null) {
            throw new IllegalStateException();
        }
        return writeRequestQueue;
    }

    /**
     * {@inheritDoc}
     */
    public final WriteRequest getCurrentWriteRequest() {
        return currentWriteRequest;
    }
    
    /**
     * {@inheritDoc}
     */
    public final Object getCurrentWriteMessage() {
        WriteRequest req = getCurrentWriteRequest();
        if (req == null) {
            return null;
        }
        return req.getMessage();
    }
    
    /**
     * {@inheritDoc}
     */
    public final void setCurrentWriteRequest(WriteRequest currentWriteRequest) {
        this.currentWriteRequest = currentWriteRequest;
    }
    
    /**
     * Increase the ReadBuffer size (it will double)
     * 
     * 增加session的读缓冲区的大小(翻倍)
     */
    public final void increaseReadBufferSize() {
        int newReadBufferSize = getConfig().getReadBufferSize() << 1;
        if (newReadBufferSize <= getConfig().getMaxReadBufferSize()) {
            getConfig().setReadBufferSize(newReadBufferSize);
        } else {
            getConfig().setReadBufferSize(getConfig().getMaxReadBufferSize());
        }
        deferDecreaseReadBuffer = true;
    }

    /**
     * Decrease the ReadBuffer size (it will be divided by a factor 2)
     * 
     * 减少session的读缓冲区的大小(减半)
     */
    public final void decreaseReadBufferSize() {
        if (deferDecreaseReadBuffer) {
            deferDecreaseReadBuffer = false;
            return;
        }
        if (getConfig().getReadBufferSize() > getConfig().getMinReadBufferSize()) {
            getConfig().setReadBufferSize(getConfig().getReadBufferSize() >>> 1);
        }
        deferDecreaseReadBuffer = true;
    }
    
    /**
     * {@inheritDoc}
     */
    public final long getCreationTime() {
        return creationTime;
    }

    /**
     * {@inheritDoc}
     */
    public final long getLastIoTime() {
        return Math.max(lastReadTime, lastWriteTime);
    }

    /**
     * {@inheritDoc}
     */
    public final long getLastReadTime() {
        return lastReadTime;
    }

    /**
     * {@inheritDoc}
     */
    public final long getLastWriteTime() {
        return lastWriteTime;
    }
    
    /**
     * {@inheritDoc}
     */
    public final boolean isIdle(IdleStatus status) {
        if (status == IdleStatus.BOTH_IDLE) {
            return idleCountForBoth.get() > 0;
        }
        if (status == IdleStatus.READER_IDLE) {
            return idleCountForRead.get() > 0;
        }
        if (status == IdleStatus.WRITER_IDLE) {
            return idleCountForWrite.get() > 0;
        }
        throw new IllegalArgumentException("Unknown idle status: " + status);
    }
    
    /**
     * {@inheritDoc}
     */
    public final boolean isBothIdle() {
        return isIdle(IdleStatus.BOTH_IDLE);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isReaderIdle() {
        return isIdle(IdleStatus.READER_IDLE);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isWriterIdle() {
        return isIdle(IdleStatus.WRITER_IDLE);
    }
    
    /**
     * {@inheritDoc}
     */
    public final int getIdleCount(IdleStatus status) {
    	// 如果设置的空闲时间判断值是0，表示不会进入空闲状态。
        if (getConfig().getIdleTime(status) == 0) {
            if (status == IdleStatus.BOTH_IDLE) {
                idleCountForBoth.set(0);
            }
            if (status == IdleStatus.READER_IDLE) {
                idleCountForRead.set(0);
            }
            if (status == IdleStatus.WRITER_IDLE) {
                idleCountForWrite.set(0);
            }
        }
        if (status == IdleStatus.BOTH_IDLE) {
            return idleCountForBoth.get();
        }
        if (status == IdleStatus.READER_IDLE) {
            return idleCountForRead.get();
        }
        if (status == IdleStatus.WRITER_IDLE) {
            return idleCountForWrite.get();
        }
        throw new IllegalArgumentException("Unknown idle status: " + status);
    }
    
    /**
     * {@inheritDoc}
     */
    public final long getLastIdleTime(IdleStatus status) {
        if (status == IdleStatus.BOTH_IDLE) {
            return lastIdleTimeForBoth;
        }
        if (status == IdleStatus.READER_IDLE) {
            return lastIdleTimeForRead;
        }
        if (status == IdleStatus.WRITER_IDLE) {
            return lastIdleTimeForWrite;
        }
        throw new IllegalArgumentException("Unknown idle status: " + status);
    }
    
    /**
     * Increase the count of the various Idle counter
     * 
     * 增加指定空闲状态的各计数器的值。
     * 
     * @param status The current status
     * @param currentTime The current time
     */
    public final void increaseIdleCount(IdleStatus status, long currentTime) {
        if (status == IdleStatus.BOTH_IDLE) {
            idleCountForBoth.incrementAndGet();
            lastIdleTimeForBoth = currentTime;
        } else if (status == IdleStatus.READER_IDLE) {
            idleCountForRead.incrementAndGet();
            lastIdleTimeForRead = currentTime;
        } else if (status == IdleStatus.WRITER_IDLE) {
            idleCountForWrite.incrementAndGet();
            lastIdleTimeForWrite = currentTime;
        } else {
            throw new IllegalArgumentException("Unknown idle status: " + status);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public final int getBothIdleCount() {
        return getIdleCount(IdleStatus.BOTH_IDLE);
    }

    /**
     * {@inheritDoc}
     */
    public final long getLastBothIdleTime() {
        return getLastIdleTime(IdleStatus.BOTH_IDLE);
    }

    /**
     * {@inheritDoc}
     */
    public final long getLastReaderIdleTime() {
        return getLastIdleTime(IdleStatus.READER_IDLE);
    }

    /**
     * {@inheritDoc}
     */
    public final long getLastWriterIdleTime() {
        return getLastIdleTime(IdleStatus.WRITER_IDLE);
    }

    /**
     * {@inheritDoc}
     */
    public final int getReaderIdleCount() {
        return getIdleCount(IdleStatus.READER_IDLE);
    }

    /**
     * {@inheritDoc}
     */
    public final int getWriterIdleCount() {
        return getIdleCount(IdleStatus.WRITER_IDLE);
    }
    
    /**
     * {@inheritDoc}
     */
    public SocketAddress getServiceAddress() {
    	IoService service = getService();
    	if (service instanceof IoAcceptor) {
			return ((IoAcceptor) service).getLocalAddress();
		}
    	return getRemoteAddress();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }
    
    /**
     * {@inheritDoc} 
     * 
     * TODO This is a ridiculous implementation. Need to be replaced.
     * 
     * 这个方法需要被重写
     */
    @Override
    public final boolean equals(Object o) {
        return super.equals(o);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (isConnected() || isClosing()) {
            String remote = null;
            String local = null;
            try {
                remote = String.valueOf(getRemoteAddress());
            } catch (Exception e) {
                remote = "Cannot get the remote address informations: " + e.getMessage();
            }
            try {
                local = String.valueOf(getLocalAddress());
            } catch (Exception e) {
            }
            if (getService() instanceof IoAcceptor) {
                return "(" + getIdAsString() + ": " + getServiceName() + ", server, " + remote + " => " + local + ')';
            }
            return "(" + getIdAsString() + ": " + getServiceName() + ", client, " + local + " => " + remote + ')';
        }
        return "(" + getIdAsString() + ") Session disconnected ...";
    }
    
    /**
     * Get the Id as a String
     * 
     * 以十六进制字符串形式返回id。
     */
    private String getIdAsString() {
        String id = Long.toHexString(getId()).toUpperCase();
        if (id.length() <= 8) {
            return "0x00000000".substring(0, 10 - id.length()) + id;
        } else {
            return "0x" + id;
        }
    }

    /**
     * TGet the Service name
     * 
     * 从TransportMetadata中根据providerName和serviceName作为当前service的名称并返回。
     */
    private String getServiceName() {
        TransportMetadata tm = getTransportMetadata();
        if (tm == null) {
            return "null";
        }
        return tm.getProviderName() + ' ' + tm.getName();
    }
    
    /**
     * {@inheritDoc}
     */
    public IoService getService() {
        return service;
    }
    
    /**
     * Fires a {@link IoEventType#SESSION_IDLE} event to any applicable sessions
     * in the specified collection.
     * 
     * 触发IoEventType.SESSION_IDLE事件给指定集合中的所有session。
     * 每个session会判断指定空闲状态是否被触发，如果触发，则通知过滤器链中的所有过滤器。
     * 
     * @param sessions The sessions that are notified
     * @param currentTime the current time (i.e. {@link System#currentTimeMillis()})
     */
    public static void notifyIdleness(Iterator<? extends IoSession> sessions, long currentTime) {
        while (sessions.hasNext()) {
            IoSession session = sessions.next();
            if (!session.getCloseFuture().isClosed()) {
                notifyIdleSession(session, currentTime);
            }
        }
    }
    
    /**
     * Fires a {@link IoEventType#SESSION_IDLE} event if applicable for the
     * specified {@code session}.
     * 
     * 触发IoEventType.SESSION_IDLE事件给指定的session。
     * 这个session会判断指定空闲状态是否被触发，如果触发，则通知过滤器链中的所有过滤器。
     * 
     * @param session The session that is notified
     * @param currentTime the current time (i.e. {@link System#currentTimeMillis()})
     */
    public static void notifyIdleSession(IoSession session, long currentTime) {
    	// 1. 通知session判断各空闲状态是否已经触发。
        notifyIdleSession0(session, currentTime, session.getConfig().getIdleTimeInMillis(IdleStatus.BOTH_IDLE),
                IdleStatus.BOTH_IDLE, Math.max(session.getLastIoTime(), session.getLastIdleTime(IdleStatus.BOTH_IDLE)));

        notifyIdleSession0(session, currentTime, session.getConfig().getIdleTimeInMillis(IdleStatus.READER_IDLE),
                IdleStatus.READER_IDLE,
                Math.max(session.getLastReadTime(), session.getLastIdleTime(IdleStatus.READER_IDLE)));

        notifyIdleSession0(session, currentTime, session.getConfig().getIdleTimeInMillis(IdleStatus.WRITER_IDLE),
                IdleStatus.WRITER_IDLE,
                Math.max(session.getLastWriteTime(), session.getLastIdleTime(IdleStatus.WRITER_IDLE)));

        // 2. 通知session判断各写超时状态是否已经触发。
        notifyWriteTimeout(session, currentTime);
    }
    
    /**
     * 指定session会判断指定空闲状态是否被触发，如果触发，则通知过滤器链中的所有过滤器。
     * @param session
     * @param currentTime
     * @param idleTime
     * @param status
     * @param lastIoTime
     */
    private static void notifyIdleSession0(IoSession session, long currentTime, long idleTime, IdleStatus status,
            long lastIoTime) {
        if ((idleTime > 0) && (lastIoTime != 0) && (currentTime - lastIoTime >= idleTime)) {
            session.getFilterChain().fireSessionIdle(status);
        }
    }
    
    /**
     * 指定session判断写超时状态是否已经触发。
     * 如果触发，则生成一个WriteTimeoutException、通知过滤器链有异常发生、关闭session。
     * @param session
     * @param currentTime
     */
    private static void notifyWriteTimeout(IoSession session, long currentTime) {
        long writeTimeout = session.getConfig().getWriteTimeoutInMillis();
        if ((writeTimeout > 0) && (currentTime - session.getLastWriteTime() >= writeTimeout)
                && !session.getWriteRequestQueue().isEmpty(session)) {
            WriteRequest request = session.getCurrentWriteRequest();
            if (request != null) {
                session.setCurrentWriteRequest(null);
                WriteTimeoutException cause = new WriteTimeoutException(request);
                request.getFuture().setException(cause);
                // 通知过滤器链有异常发生
                session.getFilterChain().fireExceptionCaught(cause);
                // WriteException is an IOException, so we close the session.
                session.closeNow();
            }
        }
    }
    
    /**
     * A queue which handles the CLOSE request.
     * 
     * 一个保存CLOSE请求的队列。
     * 使用了代理模式，代理过程中，只在获取到CLOSE_REQUEST的时候关闭session。
     * 
     * TODO : Check that when closing a session, all the pending requests are
     * correctly sent.
     */
    private class CloseAwareWriteQueue implements WriteRequestQueue {
    	
    	private final WriteRequestQueue queue;
    	
    	/**
         * 构造方法
         */
        public CloseAwareWriteQueue(WriteRequestQueue queue) {
            this.queue = queue;
        }
        
        /**
         * {@inheritDoc}
         */
        public synchronized WriteRequest poll(IoSession session) {
        	WriteRequest answer = queue.poll(session);
        	if (answer == CLOSE_REQUEST) {
				AbstractIoSession.this.close(true);
				dispose(session);
				answer = null;
			}
        	return answer;
        }
        
        /**
         * {@inheritDoc}
         */
        public void offer(IoSession session, WriteRequest e) {
            queue.offer(session, e);
        }

        /**
         * {@inheritDoc}
         */
        public boolean isEmpty(IoSession session) {
            return queue.isEmpty(session);
        }

        /**
         * {@inheritDoc}
         */
        public void clear(IoSession session) {
            queue.clear(session);
        }

        /**
         * {@inheritDoc}
         */
        public void dispose(IoSession session) {
            queue.dispose(session);
        }

        /**
         * {@inheritDoc}
         */
        public int size() {
            return queue.size();
        }
    }
}
