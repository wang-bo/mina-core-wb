package org.apache.mina.core.session;

import java.util.concurrent.BlockingQueue;

/**
 * The configuration of {@link IoSession}.
 * 
 * IoSession的配置类
 * 
 * @date	2016年5月27日 下午6:09:00	completed
 */
public interface IoSessionConfig {

	/**
     * @return the size of the read buffer that I/O processor allocates
     * per each read.  It's unusual to adjust this property because
     * it's often adjusted automatically by the I/O processor.
     * 
     * 返回I/O processor为每次read分配的read缓冲区大小，缓冲区大小由I/O processor自动调整
     */
	public int getReadBufferSize();
	
	/**
     * Sets the size of the read buffer that I/O processor allocates
     * per each read.  It's unusual to adjust this property because
     * it's often adjusted automatically by the I/O processor.
     * 
     * 设置I/O processor为每次read分配的read缓冲区大小，其I/O processor会自动调整这个缓冲区的大小
     * 
     * @param readBufferSize The size of the read buffer
     */
	public void setReadBufferSize(int readBufferSize);
	
	/**
     * @return the minimum size of the read buffer that I/O processor
     * allocates per each read.  I/O processor will not decrease the
     * read buffer size to the smaller value than this property value.
     * 
     * 返回I/O processor为每次read分配的read缓冲区大小的最小值，当缓冲区大小小于这个值时，I/O processor不会再减少缓冲区大小
     */
	public int getMinReadBufferSize();
	
	/**
     * Sets the minimum size of the read buffer that I/O processor
     * allocates per each read.  I/O processor will not decrease the
     * read buffer size to the smaller value than this property value.
     * 
     * 设置I/O processor为每次read分配的read缓冲区大小的最小值，当缓冲区大小小于这个值时，I/O processor不会再减少缓冲区大小
     * 
     * @param minReadBufferSize The minimum size of the read buffer
     */
	public void setMinReadBufferSize(int minReadBufferSize);
    
    /**
     * @return the maximum size of the read buffer that I/O processor
     * allocates per each read.  I/O processor will not increase the
     * read buffer size to the greater value than this property value.
     * 
     * 返回I/O processor为每次read分配的read缓冲区大小的最大值，当缓冲区大小大于这个值时，I/O processor不会再增加缓冲区大小
     */
	public int getMaxReadBufferSize();

    /**
     * Sets the maximum size of the read buffer that I/O processor
     * allocates per each read.  I/O processor will not increase the
     * read buffer size to the greater value than this property value.
     * 
     * 设置I/O processor为每次read分配的read缓冲区大小的最大值，当缓冲区大小大于这个值时，I/O processor不会再增加缓冲区大小
     * 
     * @param maxReadBufferSize The maximum size of the read buffer
     */
	public void setMaxReadBufferSize(int maxReadBufferSize);
    
    /**
     * @return the interval (seconds) between each throughput calculation.
     * The default value is <tt>3</tt> seconds.
     * 
     * 返回每次throughput calculation（吞吐量计算?）的间隔时间（秒），默认是3秒
     */
	public int getThroughputCalculationInterval();
	
	/**
     * @return the interval (milliseconds) between each throughput calculation.
     * The default value is <tt>3</tt> seconds.
     * 
     * 返回每次throughput calculation（吞吐量计算?）的间隔时间（毫秒），默认是3秒
     */
	public long getThroughputCalculationIntervalInMillis();
	
	/**
     * Sets the interval (seconds) between each throughput calculation.  The
     * default value is <tt>3</tt> seconds.
     * 
     * 设置每次throughput calculation（吞吐量计算?）的间隔时间（秒），默认是3秒
     * 
     * @param throughputCalculationInterval The interval
     */
	public void setThroughputCalculationInterval(int throughputCalculationInterval);
	
	/**
     * @return idle time for the specified type of idleness in seconds.
     * 
     * 返回当前已设定的触发IoSession的指定空闲状态的空闲时间（秒）
     * 
     * @param status The status for which we want the idle time (One of READER_IDLE,
     * WRITER_IDLE or BOTH_IDLE)
     */
	public int getIdleTime(IdleStatus status);
	
	/**
     * @return idle time for the specified type of idleness in milliseconds.
     * 
     * 返回当前已设定的触发IoSession的指定空闲状态的空闲时间（毫秒）
     * 
     * @param status The status for which we want the idle time (One of READER_IDLE,
     * WRITER_IDLE or BOTH_IDLE)
     */
	public long getIdleTimeInMillis(IdleStatus status);
	
	/**
     * Sets idle time for the specified type of idleness in seconds.
     * 
     * 设置触发IoSession的指定空闲状态的空闲时间（秒）
     * 
     * @param status The status for which we want to set the idle time (One of READER_IDLE,
     * WRITER_IDLE or BOTH_IDLE)
     * @param idleTime The time in second to set
     */
	public void setIdleTime(IdleStatus status, int idleTime);
	
	/**
     * @return idle time for {@link IdleStatus#READER_IDLE} in seconds.
     * 
     * 返回触发IoSession的IdleStatus.READER_IDLE空闲状态的空闲时间（秒）
     */
	public int getReaderIdleTime();

    /**
     * @return idle time for {@link IdleStatus#READER_IDLE} in milliseconds.
     * 
     * 返回触发IoSession的IdleStatus.READER_IDLE空闲状态的空闲时间（毫秒）
     */
    public long getReaderIdleTimeInMillis();

    /**
     * Sets idle time for {@link IdleStatus#READER_IDLE} in seconds.
     * 
     * 设置触发IoSession的IdleStatus.READER_IDLE空闲状态的空闲时间（秒）
     * 
     * @param idleTime The time to set
     */
    public void setReaderIdleTime(int idleTime);

    /**
     * @return idle time for {@link IdleStatus#WRITER_IDLE} in seconds.
     * 
     * 返回触发IoSession的IdleStatus.WRITER_IDLE空闲状态的空闲时间（秒）
     */
    public int getWriterIdleTime();

    /**
     * @return idle time for {@link IdleStatus#WRITER_IDLE} in milliseconds.
     * 
     * 返回触发IoSession的IdleStatus.WRITER_IDLE空闲状态的空闲时间（毫秒）
     */
    public long getWriterIdleTimeInMillis();

    /**
     * Sets idle time for {@link IdleStatus#WRITER_IDLE} in seconds.
     * 
     * 设置触发IoSession的IdleStatus.WRITER_IDLE空闲状态的空闲时间（秒）
     * 
     * @param idleTime The time to set
     */
    public void setWriterIdleTime(int idleTime);

    /**
     * @return idle time for {@link IdleStatus#BOTH_IDLE} in seconds.
     * 
     * 返回触发IoSession的IdleStatus.BOTH_IDLE空闲状态的空闲时间（秒）
     */
    public int getBothIdleTime();

    /**
     * @return idle time for {@link IdleStatus#BOTH_IDLE} in milliseconds.
     * 
     * 返回触发IoSession的IdleStatus.BOTH_IDLE空闲状态的空闲时间（毫秒）
     */
    public long getBothIdleTimeInMillis();

    /**
     * Sets idle time for {@link IdleStatus#WRITER_IDLE} in seconds.
     * 
     * 设置触发IoSession的IdleStatus.BOTH_IDLE空闲状态的空闲时间（秒）
     * 
     * @param idleTime The time to set
     */
    public void setBothIdleTime(int idleTime);
    
    /**
     * @return write timeout in seconds.
     * 
     * 返回write timeout时间（秒）
     */
    public int getWriteTimeout();
    
    /**
     * @return write timeout in milliseconds.
     * 
     * 返回write timeout时间（毫秒）
     */
    public long getWriteTimeoutInMillis();
    
    /**
     * Sets write timeout in seconds.
     * 
     * 设置write timeout时间（秒）
     * 
     * @param writeTimeout The timeout to set
     */
    public void setWriteTimeout(int writeTimeout);
    
    /**
     * @return <tt>true</tt> if and only if {@link IoSession#read()} operation
     * is enabled.  If enabled, all received messages are stored in an internal
     * {@link BlockingQueue} so you can read received messages in more
     * convenient way for client applications.  Enabling this option is not
     * useful to server applications and can cause unintended memory leak, and
     * therefore it's disabled by default.
     * 
     * 返回IoSession.read()操作是否开启。
     * 如果开启，所有收到的message会存储在一个内部的BlockingQueue
     * 所以可以方便的获取这些信息。开启这个选项对应用不一定有帮助，而且可能引起内存泄漏，所以默认是关闭的。
     */
    public boolean isUseReadOperation();
    
    /**
     * Enables or disabled {@link IoSession#read()} operation.  If enabled, all
     * received messages are stored in an internal {@link BlockingQueue} so you
     * can read received messages in more convenient way for client
     * applications.  Enabling this option is not useful to server applications
     * and can cause unintended memory leak, and therefore it's disabled by
     * default.
     * 
     * 设置IoSession.read()操作是否开启。
     * 如果开启，所有收到的message会存储在一个内部的BlockingQueue
     * 所以可以方便的获取这些信息。开启这个选项对应用不一定有帮助，而且可能引起内存泄漏，所以默认是关闭的。
     * 
     * @param useReadOperation <tt>true</tt> if the read operation is enabled, <tt>false</tt> otherwise
     */
    public void setUseReadOperation(boolean useReadOperation);
    
    /**
     * Sets all configuration properties retrieved from the specified <tt>config</tt>.
     * 
     * 将参数congfig中的所有配置项设置到本config中
     * 
     * @param config
     */
    public void setAll(IoSessionConfig config);
}
