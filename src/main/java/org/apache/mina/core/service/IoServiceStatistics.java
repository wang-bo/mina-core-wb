package org.apache.mina.core.service;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Provides usage statistics for an {@link AbstractIoService} instance.
 * 
 * 为AbstractIoService提供统计功能
 * 
 * @date	2016年6月13日 上午9:16:50	completed
 */
public class IoServiceStatistics {

	/** 要统计的IoService */
	private AbstractIoService service;
	
	 /** The number of bytes read per second */
    private double readBytesThroughput;

    /** The number of bytes written per second */
    private double writtenBytesThroughput;

    /** The number of messages read per second */
    private double readMessagesThroughput;

    /** The number of messages written per second */
    private double writtenMessagesThroughput;

    /** The biggest number of bytes read per second */
    private double largestReadBytesThroughput;

    /** The biggest number of bytes written per second */
    private double largestWrittenBytesThroughput;

    /** The biggest number of messages read per second */
    private double largestReadMessagesThroughput;

    /** The biggest number of messages written per second */
    private double largestWrittenMessagesThroughput;
    
    /** The number of read bytes since the service has been started */
    private long readBytes;

    /** The number of written bytes since the service has been started */
    private long writtenBytes;

    /** The number of read messages since the service has been started */
    private long readMessages;

    /** The number of written messages since the service has been started */
    private long writtenMessages;

    /** The time the last read operation occurred */
    private long lastReadTime;

    /** The time the last write operation occurred */
    private long lastWriteTime;
    
    /** 临时变量，计算吞吐率用：从service开启到上次updateThroughput(long)操作为止的已读的字节数 */
    private long lastReadBytes;

    /** 临时变量，计算吞吐率用：从service开启到上次updateThroughput(long)操作为止的已写的字节数 */
    private long lastWrittenBytes;

    /** 临时变量，计算吞吐率用：从service开启到上次updateThroughput(long)操作为止的已读的Message数 */
    private long lastReadMessages;

    /** 临时变量，计算吞吐率用：从service开启到上次updateThroughput(long)操作为止的已写的Message数 */
    private long lastWrittenMessages;

    /** 临时变量，计算吞吐率用：上次吞吐率计算操作执行时间 */
    private long lastThroughputCalculationTime;

    /** 当前service上待写入的字节数 */
    private int scheduledWriteBytes;

    /** 当前service上待写入的Message数 */
    private int scheduledWriteMessages;
    
    /** The time (in second) between the computation of the service's statistics */
    private final AtomicInteger throughputCalculationInterval = new AtomicInteger(3);

    /** 吞吐率计算的锁 */
    private final Lock throughputCalculationLock = new ReentrantLock();

    /**
     * 构造方法
     * @param service
     */
	public IoServiceStatistics(AbstractIoService service) {
		this.service = service;
	}
    
	/**
     * @return The maximum number of sessions which were being managed at the
     *         same time.
     *         
     * 返回同一时间管理的最大session数。
     */
    public final int getLargestManagedSessionCount() {
        return service.getListeners().getLargestManagedSessionCount();
    }
    
    /**
     * @return The cumulative number of sessions which were managed (or are
     *         being managed) by this service, which means 'currently managed
     *         session count + closed session count'.
     *         
     * 返回累计管理的session数：当前管理的session数、已关闭的session数之和。
     */
    public final long getCumulativeManagedSessionCount() {
        return service.getListeners().getCumulativeManagedSessionCount();
    }
    
    /**
     * @return the time in millis when the last I/O operation (read or write)
     *         occurred.
     *         
     * 返回上次I/O操作发生的时间(毫秒)。
     */
    public final long getLastIoTime() {
        throughputCalculationLock.lock();
        try {
            return Math.max(lastReadTime, lastWriteTime);
        } finally {
            throughputCalculationLock.unlock();
        }
    }
    
    /**
     * @return The time in millis when the last read operation occurred.
     * 
     * 返回上次读操作发生的时间(毫秒)。
     */
    public final long getLastReadTime() {
        throughputCalculationLock.lock();
        try {
            return lastReadTime;
        } finally {
            throughputCalculationLock.unlock();
        }
    }
    
    /**
     * @return The time in millis when the last write operation occurred.
     * 
     * 返回上次写操作发生的时间(毫秒)。
     */
    public final long getLastWriteTime() {
        throughputCalculationLock.lock();
        try {
            return lastWriteTime;
        } finally {
            throughputCalculationLock.unlock();
        }
    }
    
    /**
     * @return The number of bytes this service has read so far
     * 
     * 返回service开启到现在已读的总字节数。
     */
    public final long getReadBytes() {
        throughputCalculationLock.lock();
        try {
            return readBytes;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * @return The number of bytes this service has written so far
     * 
     * 返回service开启到现在已写的总字节数。
     */
    public final long getWrittenBytes() {
        throughputCalculationLock.lock();

        try {
            return writtenBytes;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * @return The number of messages this services has read so far
     * 
     * 返回service开启到现在已读的总Message数。
     */
    public final long getReadMessages() {
        throughputCalculationLock.lock();
        try {
            return readMessages;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * @return The number of messages this service has written so far
     * 
     * 返回service开启到现在已写的总Message数。
     */
    public final long getWrittenMessages() {
        throughputCalculationLock.lock();
        try {
            return writtenMessages;
        } finally {
            throughputCalculationLock.unlock();
        }
    }
    
    /**
     * @return The number of read bytes per second.
     * 
     * 返回每秒读取的字节数。
     */
    public final double getReadBytesThroughput() {
        throughputCalculationLock.lock();
        try {
            resetThroughput();
            return readBytesThroughput;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * @return The number of written bytes per second.
     * 返回每秒写入的字节数。
     */
    public final double getWrittenBytesThroughput() {
        throughputCalculationLock.lock();
        try {
            resetThroughput();
            return writtenBytesThroughput;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * @return The number of read messages per second.
     * 
     * 返回每秒读取的Message数。
     */
    public final double getReadMessagesThroughput() {
        throughputCalculationLock.lock();
        try {
            resetThroughput();
            return readMessagesThroughput;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * @return The number of written messages per second.
     * 
     * 返回每秒写入的Message数。
     */
    public final double getWrittenMessagesThroughput() {
        throughputCalculationLock.lock();
        try {
            resetThroughput();
            return writtenMessagesThroughput;
        } finally {
            throughputCalculationLock.unlock();
        }
    }
    
    /**
     * @return The maximum number of bytes read per second since the service has
     *         been started.
     *         
     * 返回service开启到现在的最大每秒读取字节数。
     */
    public final double getLargestReadBytesThroughput() {
        throughputCalculationLock.lock();
        try {
            return largestReadBytesThroughput;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * @return The maximum number of bytes written per second since the service
     *         has been started.
     *         
     * 返回service开启到现在的最大每秒写入字节数。
     */
    public final double getLargestWrittenBytesThroughput() {
        throughputCalculationLock.lock();
        try {
            return largestWrittenBytesThroughput;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * @return The maximum number of messages read per second since the service
     *         has been started.
     *         
     * 返回service开启到现在的最大每秒读取Message数。
     */
    public final double getLargestReadMessagesThroughput() {
        throughputCalculationLock.lock();
        try {
            return largestReadMessagesThroughput;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * @return The maximum number of messages written per second since the
     *         service has been started.
     *         
     * 返回service开启到现在的最大每秒写入Message数。
     */
    public final double getLargestWrittenMessagesThroughput() {
        throughputCalculationLock.lock();
        try {
            return largestWrittenMessagesThroughput;
        } finally {
            throughputCalculationLock.unlock();
        }
    }
    
    /**
     * @return the interval (seconds) between each throughput calculation. The
     *         default value is <tt>3</tt> seconds.
     *         
     * 返回吞吐率计算的时间间隔(秒)，默认3秒。
     */
    public final int getThroughputCalculationInterval() {
        return throughputCalculationInterval.get();
    }

    /**
     * @return the interval (milliseconds) between each throughput calculation.
     * The default value is <tt>3</tt> seconds.
     * 
     * 返回吞吐率计算的时间间隔(毫秒)，默认3秒。
     */
    public final long getThroughputCalculationIntervalInMillis() {
        return throughputCalculationInterval.get() * 1000L;
    }
    
    /**
     * Sets the interval (seconds) between each throughput calculation.  The
     * default value is <tt>3</tt> seconds.
     * 
     * 设置吞吐率计算的时间间隔(秒)，默认3秒。
     * 
     * @param throughputCalculationInterval The interval between two calculation
     */
    public final void setThroughputCalculationInterval(int throughputCalculationInterval) {
        if (throughputCalculationInterval < 0) {
            throw new IllegalArgumentException("throughputCalculationInterval: " + throughputCalculationInterval);
        }
        this.throughputCalculationInterval.set(throughputCalculationInterval);
    }
    
    /**
     * Sets last time at which a read occurred on the service.
     * 
     * 设置上次读操作发生的时间(毫秒)。
     * 
     * @param lastReadTime
     *            The last time a read has occurred
     */
    protected final void setLastReadTime(long lastReadTime) {
        throughputCalculationLock.lock();
        try {
            this.lastReadTime = lastReadTime;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * Sets last time at which a write occurred on the service.
     * 
     * 设置上次写操作发生的时间(毫秒)。
     * 
     * @param lastWriteTime
     *            The last time a write has occurred
     */
    protected final void setLastWriteTime(long lastWriteTime) {
        throughputCalculationLock.lock();
        try {
            this.lastWriteTime = lastWriteTime;
        } finally {
            throughputCalculationLock.unlock();
        }
    }
    
    /**
     * Resets the throughput counters of the service if no session is currently
     * managed.
     * 
     * 如果当前service上没有session，重置吞吐率计算的计数器。
     */
    private void resetThroughput() {
        if (service.getManagedSessionCount() == 0) {
            readBytesThroughput = 0;
            writtenBytesThroughput = 0;
            readMessagesThroughput = 0;
            writtenMessagesThroughput = 0;
        }
    }
    
    /**
     * Updates the throughput counters.
     * 
     * 更新吞吐率计算的计数器(执行吞吐率计算)。
     * 
     * @param currentTime The current time
     */
    public void updateThroughput(long currentTime) {
    	throughputCalculationLock.lock();
    	try {
    		// 1. 判断时间间隔是否已达到设定值，默认最少间隔时间是3秒
			int interval = (int) (currentTime - lastThroughputCalculationTime);
			long minInterval = getThroughputCalculationIntervalInMillis();
			if (minInterval == 0 || interval < minInterval) {
				return ;
			}
			
			// 2. 计算吞吐率
			long readBytes = this.readBytes;
            long writtenBytes = this.writtenBytes;
            long readMessages = this.readMessages;
            long writtenMessages = this.writtenMessages;
            readBytesThroughput = (readBytes - lastReadBytes) * 1000.0 / interval;
            writtenBytesThroughput = (writtenBytes - lastWrittenBytes) * 1000.0 / interval;
            readMessagesThroughput = (readMessages - lastReadMessages) * 1000.0 / interval;
            writtenMessagesThroughput = (writtenMessages - lastWrittenMessages) * 1000.0 / interval;
            
            // 3. 设置最大吞吐率
            if (readBytesThroughput > largestReadBytesThroughput) {
                largestReadBytesThroughput = readBytesThroughput;
            }
            if (writtenBytesThroughput > largestWrittenBytesThroughput) {
                largestWrittenBytesThroughput = writtenBytesThroughput;
            }
            if (readMessagesThroughput > largestReadMessagesThroughput) {
                largestReadMessagesThroughput = readMessagesThroughput;
            }
            if (writtenMessagesThroughput > largestWrittenMessagesThroughput) {
                largestWrittenMessagesThroughput = writtenMessagesThroughput;
            }
            
            // 4. 记录到临时变量，供下次吞吐率计算使用
            lastReadBytes = readBytes;
            lastWrittenBytes = writtenBytes;
            lastReadMessages = readMessages;
            lastWrittenMessages = writtenMessages;
            lastThroughputCalculationTime = currentTime;
		} finally {
            throughputCalculationLock.unlock();
        }
    }
    
    /**
     * Increases the count of read bytes by <code>nbBytesRead</code> and sets
     * the last read time to <code>currentTime</code>.
     * 
     * 增加已读取的字节数，并设置上次读取时间。
     * 
     * @param nbBytesRead
     *            The number of bytes read
     * @param currentTime
     *            The date those bytes were read
     */
    public final void increaseReadBytes(long nbBytesRead, long currentTime) {
        throughputCalculationLock.lock();
        try {
            readBytes += nbBytesRead;
            lastReadTime = currentTime;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * Increases the count of read messages by 1 and sets the last read time to
     * <code>currentTime</code>.
     * 
     * 增加已读取的Message数，并设置上次读取时间。
     * 
     * @param currentTime
     *            The time the message has been read
     */
    public final void increaseReadMessages(long currentTime) {
        throughputCalculationLock.lock();
        try {
            readMessages++;
            lastReadTime = currentTime;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * Increases the count of written bytes by <code>nbBytesWritten</code> and
     * sets the last write time to <code>currentTime</code>.
     * 
     * 增加已写入的字节数，并设置上次写入时间。
     * 
     * @param nbBytesWritten
     *            The number of bytes written
     * @param currentTime
     *            The date those bytes were written
     */
    public final void increaseWrittenBytes(int nbBytesWritten, long currentTime) {
        throughputCalculationLock.lock();
        try {
            writtenBytes += nbBytesWritten;
            lastWriteTime = currentTime;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * Increases the count of written messages by 1 and sets the last write time
     * to <code>currentTime</code>.
     * 
     * 增加已写入的Message数，并设置上次写入时间。
     * 
     * @param currentTime
     *            The date the message were written
     */
    public final void increaseWrittenMessages(long currentTime) {
        throughputCalculationLock.lock();
        try {
            writtenMessages++;
            lastWriteTime = currentTime;
        } finally {
            throughputCalculationLock.unlock();
        }
    }
    
    /**
     * @return The count of bytes scheduled for write.
     * 
     * 返回当前service上待写入的字节数。
     */
    public final int getScheduledWriteBytes() {
        throughputCalculationLock.lock();
        try {
            return scheduledWriteBytes;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * Increments by <code>increment</code> the count of bytes scheduled for write.
     * 
     * 增加当前service上待写入的字节数。
     * 
     * @param increment The number of added bytes for write
     */
    public final void increaseScheduledWriteBytes(int increment) {
        throughputCalculationLock.lock();
        try {
            scheduledWriteBytes += increment;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * @return the count of messages scheduled for write.
     * 
     * 返回当前service上待写入的Message数。
     */
    public final int getScheduledWriteMessages() {
        throughputCalculationLock.lock();
        try {
            return scheduledWriteMessages;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * Increments the count of messages scheduled for write.
     * 
     * 增加当前service上待写入的Message数。
     */
    public final void increaseScheduledWriteMessages() {
        throughputCalculationLock.lock();
        try {
            scheduledWriteMessages++;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * Decrements the count of messages scheduled for write.
     * 
     * 减少当前service上待写入的Message数。
     */
    public final void decreaseScheduledWriteMessages() {
        throughputCalculationLock.lock();
        try {
            scheduledWriteMessages--;
        } finally {
            throughputCalculationLock.unlock();
        }
    }

    /**
     * Sets the time at which throughput counters where updated.
     * 
     * 设置上次吞吐率计算操作执行时间。
     * 
     * @param lastThroughputCalculationTime The time at which throughput counters where updated.
     */
    protected void setLastThroughputCalculationTime(long lastThroughputCalculationTime) {
        throughputCalculationLock.lock();
        try {
            this.lastThroughputCalculationTime = lastThroughputCalculationTime;
        } finally {
            throughputCalculationLock.unlock();
        }
    }
}
