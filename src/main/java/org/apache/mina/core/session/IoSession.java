package org.apache.mina.core.session;

import java.net.SocketAddress;
import java.util.Set;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ReadFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestQueue;

/**
 * <p>
 *   A handle which represents connection between two end-points regardless of
 *   transport types.
 * </p>
 * <p>
 *   {@link IoSession} provides user-defined attributes.  User-defined attributes
 *   are application-specific data which are associated with a session.
 *   It often contains objects that represents the state of a higher-level protocol
 *   and becomes a way to exchange data between filters and handlers.
 * </p>
 * <h3>Adjusting Transport Type Specific Properties</h3>
 * <p>
 *   You can simply downcast the session to an appropriate subclass.
 * </p>
 * <h3>Thread Safety</h3>
 * <p>
 *   {@link IoSession} is thread-safe.  But please note that performing
 *   more than one {@link #write(Object)} calls at the same time will
 *   cause the {@link IoFilter#filterWrite(IoFilter.NextFilter,IoSession,WriteRequest)}
 *   to be executed simultaneously, and therefore you have to make sure the
 *   {@link IoFilter} implementations you're using are thread-safe, too.
 * </p>
 * <h3>Equality of Sessions</h3>
 * TODO : The getId() method is totally wrong. We can't base
 * a method which is designed to create a unique ID on the hashCode method.
 * {@link Object#equals(Object)} and {@link Object#hashCode()} shall not be overriden
 * to the default behavior that is defined in {@link Object}.
 * 
 * 1. IoSession代表了两点间的连接，不关心底层使用的传输方式。
 * 2. IoSession提供了用户定义属性的功能，通常包含一个表示较高级别的协议的状态的对象，作为在filter和handler间交换数据的通道。
 * 3. IoSession是线程安全的，注意：如果同时调用write(Object)方法，会触发IoFilter的filterWrite(IoFilter.NextFilter,IoSession,WriteRequest)
 * 方法同时执行，所以要保证IoFilter的实现也是线程安全的。
 * 4. getId()方法是完全错误的，我们使用hashCode()方法来创建唯一的ID，所以不要覆盖equals()和hashCode()方法。
 * 5. 查看实现的AbstractIoSession.getId()方法中，是保证了ID的唯一性的，所以第4点注释是有问题的。
 * 
 * @date	2016年5月27日 下午6:21:26	completed
 */
public interface IoSession {

	/**
     * @return a unique identifier for this session.  Every session has its own
     * ID which is different from each other.
     * 
     * TODO : The way it's implemented does not guarantee that the contract is
     * respected. It uses the HashCode() method which don't guarantee the key
     * unicity.
     * 
     * 返回当前session的唯一标识，每个session的ID都不相同。
     * 注意：实现方法中不保证ID的唯一(不保证契约被遵守)，它使用hashCode()方法，但hashcode()方法是不保证唯一性的。
     * 但是：查看实现类中的AbstractIoSession.getId()方法，是保证了唯一性的，所以这段注释是有问题的。
     */
    public long getId();
    
    /**
     * @return the {@link IoService} which provides I/O service to this session.
     * 
     * 返回为当前session提供服务的IoService。
     */
    public IoService getService();

    /**
     * @return the {@link IoHandler} which handles this session.
     * 
     * 返回处理当前session的IoHandler。
     */
    public IoHandler getHandler();

    /**
     * @return the configuration of this session.
     * 
     * 返回当前session的配置
     */
    public IoSessionConfig getConfig();

    /**
     * @return the filter chain that only affects this session.
     * 
     * 返回影响当前session的过滤器链
     */
    public IoFilterChain getFilterChain();

    /**
     * Get the queue that contains the message waiting for being written.
     * As the reader might not be ready, it's frequent that the messages
     * aren't written completely, or that some older messages are waiting
     * to be written when a new message arrives. This queue is used to manage
     * the backlog of messages.
     * 
     * 返回当前session上保存待写入的message的队列(WriteRequestQueue)。
     * 
     * @return The queue containing the pending messages.
     */
    public WriteRequestQueue getWriteRequestQueue();

    /**
     * @return the {@link TransportMetadata} that this session runs on.
     * 
     * 返回当前session的依赖的TransportMetadata
     */
    public TransportMetadata getTransportMetadata();
    
    /**
     * TODO This javadoc is wrong. The return tag should be short.
     * 
     * @return a {@link ReadFuture} which is notified when a new message is
     * received, the connection is closed or an exception is caught.  This
     * operation is especially useful when you implement a client application.
     * TODO : Describe here how we enable this feature.
     * However, please note that this operation is disabled by default and
     * throw {@link IllegalStateException} because all received events must be
     * queued somewhere to support this operation, possibly leading to memory
     * leak.  This means you have to keep calling {@link #read()} once you
     * enabled this operation.  To enable this operation, please call
     * {@link IoSessionConfig#setUseReadOperation(boolean)} with <tt>true</tt>.
     * 
     * 返回一个ReadFuture，当新消息到达、连接关闭、异常捕获时会得到通知，这个操作在实现客户端时特别有用。
     * 注意：这个方法默认是关闭的，如果调用会抛出IllegalStateException异常，
     * 因为所有到达的事件都必须保存在某个地方来支持这个操作，很可能会导致内存泄漏。
     * 如果要开启此方法，请先调用IoSessionConfig.setUseReadOperation(true)方法。
     *
     * @throws IllegalStateException if
     * {@link IoSessionConfig#setUseReadOperation(boolean) useReadOperation}
     * option has not been enabled.
     */
    public ReadFuture read();
    
    /**
     * Writes the specified <code>message</code> to remote peer.  This
     * operation is asynchronous; {@link IoHandler#messageSent(IoSession,Object)}
     * will be invoked when the message is actually sent to remote peer.
     * You can also wait for the returned {@link WriteFuture} if you want
     * to wait for the message actually written.
     * 
     * 把指定的message发送给远程节点。
     * 这个方法是异步的，当message成功发送后，IoHandler.messageSent()方法会被触发。
     * 也可以使用返回的WriteFuture，来等待并确认消息被成功发送。
     * 
     * @param message The message to write
     * @return The associated WriteFuture
     */
    public WriteFuture write(Object message);

    /**
     * (Optional) Writes the specified <tt>message</tt> to the specified <tt>destination</tt>.
     * This operation is asynchronous; {@link IoHandler#messageSent(IoSession, Object)}
     * will be invoked when the message is actually sent to remote peer. You can
     * also wait for the returned {@link WriteFuture} if you want to wait for
     * the message actually written.
     * <p>
     * When you implement a client that receives a broadcast message from a server
     * such as DHCP server, the client might need to send a response message for the
     * broadcast message the server sent.  Because the remote address of the session
     * is not the address of the server in case of broadcasting, there should be a
     * way to specify the destination when you write the response message.
     * This interface provides {@link #write(Object, SocketAddress)} method so you
     * can specify the destination.
     * 
     * 把指定的message发送给指定的远程Socket地址。
     * 这个方法是异步的，当message成功发送后，IoHandler.messageSent()方法会被触发。
     * 也可以使用返回的WriteFuture，来等待并确认消息被成功发送。
     * 
     * 注意：当实现一个客户端，并接收服务的广播(如DHCP服务)时，客户端可能需要发送一个响应给服务端，但是服务端的地址不是
     * 接收广播的这个session的记录的远程地址，所以需要指定远程Socket地址来发送响应。这个接口就是做这个功能用的。
     * 
     * @param message The message to write
     * @param destination <tt>null</tt> if you want the message sent to the
     *                    default remote address
     * @return The associated WriteFuture
     */
    public WriteFuture write(Object message, SocketAddress destination);
    
    /**
     * Closes this session immediately or after all queued write requests
     * are flushed.  This operation is asynchronous.  Wait for the returned
     * {@link CloseFuture} if you want to wait for the session actually closed.
     * 
     * 关闭当前session，立即关闭或等待所有写请求发送完后再关闭。
     * 这个方法是异步的，可以使用返回的CloseFuture，来等待并确认session被真正的关闭。
     * 这个方法已过时，请改用closeNow()或closeOnFlush()方法。
     *
     * @param immediately {@code true} to close this session immediately
     *                    . The pending write requests
     *                    will simply be discarded.
     *                    {@code false} to close this session after all queued
     *                    write requests are flushed.
     * @return The associated CloseFuture
     * @deprecated Use either the closeNow() or the flushAndClose() methods
     */
    @Deprecated
    public CloseFuture close(boolean immediately);

    /**
     * Closes this session immediately.  This operation is asynchronous, it 
     * returns a {@link CloseFuture}.
     * 
     * 立即关闭当前session。
     * 这个方法是异步的，可以使用返回的CloseFuture，来等待并确认session被真正的关闭。
     */
    public CloseFuture closeNow();

    /**
     * Closes this session after all queued write requests are flushed.  This operation 
     * is asynchronous.  Wait for the returned {@link CloseFuture} if you want to wait 
     * for the session actually closed.
     * 
     * 等待所有写请求发送完后关闭当前session。
     * 这个方法是异步的，可以使用返回的CloseFuture，来等待并确认session被真正的关闭。
     *
     * @return The associated CloseFuture
     */
    public CloseFuture closeOnFlush();

    /**
     * Closes this session after all queued write requests
     * are flushed. This operation is asynchronous.  Wait for the returned
     * {@link CloseFuture} if you want to wait for the session actually closed.
     * 
     * 等待所有写请求发送完后关闭当前session。
     * 这个方法是异步的，可以使用返回的CloseFuture，来等待并确认session被真正的关闭。
     * 这个方法已过时，请改用closeNow()或closeOnFlush()方法。
     * 
     * @deprecated use {@link #close(boolean)}
     * 
     * @return The associated CloseFuture
     */
    @Deprecated
    public CloseFuture close();
    
    /**
     * Returns an attachment of this session.
     * This method is identical with <tt>getAttribute( "" )</tt>.
     * 
     * 返回当前sessio的附件，这个方法和getAttribute("")方法是一致的。
     * 这个方法已过时，请改用getAttribute(Object)方法。
     *
     * @return The attachment
     * @deprecated Use {@link #getAttribute(Object)} instead.
     */
    @Deprecated
    public Object getAttachment();

    /**
     * Sets an attachment of this session.
     * This method is identical with <tt>setAttribute( "", attachment )</tt>.
     * 
     * 设置当前sessio的附件，这个方法和getAttribute("", attachment)方法是一致的。
     * 这个方法已过时，请改用setAttribute(Object, Object)方法。
     *
     * @param attachment The attachment
     * @return Old attachment. <tt>null</tt> if it is new.
     * @deprecated Use {@link #setAttribute(Object, Object)} instead.
     */
    @Deprecated
    public Object setAttachment(Object attachment);
    
    /**
     * Returns the value of the user-defined attribute of this session.
     * 
     * 返回当前session上用户自定义的属性。
     *
     * @param key the key of the attribute
     * @return <tt>null</tt> if there is no attribute with the specified key
     */
    public Object getAttribute(Object key);

    /**
     * Returns the value of user defined attribute associated with the
     * specified key.  If there's no such attribute, the specified default
     * value is associated with the specified key, and the default value is
     * returned.  This method is same with the following code except that the
     * operation is performed atomically.
     * <pre>
     * if (containsAttribute(key)) {
     *     return getAttribute(key);
     * } else {
     *     setAttribute(key, defaultValue);
     *     return defaultValue;
     * }
     * </pre>
     * 
     * 返回当前session上用户自定义的属性，如果没有，则先设置defaultValue，并返回defaultValue。
     * 
     * @param key the key of the attribute we want to retreive
     * @param defaultValue the default value of the attribute
     * @return The retrieved attribute or <tt>null</tt> if not found
     */
    public Object getAttribute(Object key, Object defaultValue);

    /**
     * Sets a user-defined attribute.
     * 
     * 在当前session上设置一个用户自定义属性。
     *
     * @param key the key of the attribute
     * @param value the value of the attribute
     * @return The old value of the attribute.  <tt>null</tt> if it is new.
     */
    public Object setAttribute(Object key, Object value);

    /**
     * Sets a user defined attribute without a value.  This is useful when
     * you just want to put a 'mark' attribute.  Its value is set to
     * {@link Boolean#TRUE}.
     * 
     * 在当前session上设置一个用户自定义属性，只有key没有value，value会设为默认值：Boolean.TRUE。
     * 这个方法适合用在只是做个标记的场合。
     *
     * @param key the key of the attribute
     * @return The old value of the attribute.  <tt>null</tt> if it is new.
     */
    public Object setAttribute(Object key);

    /**
     * Sets a user defined attribute if the attribute with the specified key
     * is not set yet.  This method is same with the following code except
     * that the operation is performed atomically.
     * <pre>
     * if (containsAttribute(key)) {
     *     return getAttribute(key);
     * } else {
     *     return setAttribute(key, value);
     * }
     * </pre>
     * 
     * 在当前session上设置一个用户自定义属性，只有当key不存在时才真正设置。
     * 
     * @param key The key of the attribute we want to set
     * @param value The value we want to set
     * @return The old value of the attribute.  <tt>null</tt> if not found.
     */
    public Object setAttributeIfAbsent(Object key, Object value);

    /**
     * Sets a user defined attribute without a value if the attribute with
     * the specified key is not set yet.  This is useful when you just want to
     * put a 'mark' attribute.  Its value is set to {@link Boolean#TRUE}.
     * This method is same with the following code except that the operation
     * is performed atomically.
     * <pre>
     * if (containsAttribute(key)) {
     *     return getAttribute(key);  // might not always be Boolean.TRUE.
     * } else {
     *     return setAttribute(key);
     * }
     * </pre>
     * 
     * 在当前session上设置一个用户自定义属性，只有当key不存在时才真正设置。
     * 只有key没有value，value会设为默认值：Boolean.TRUE。
     * 这个方法适合用在只是做个标记的场合。
     * 
     * @param key The key of the attribute we want to set
     * @return The old value of the attribute.  <tt>null</tt> if not found.
     */
    public Object setAttributeIfAbsent(Object key);

    /**
     * Removes a user-defined attribute with the specified key.
     * 
     * 在当前session上移除指定key的用户自定义属性。
     *
     * @param key The key of the attribute we want to remove
     * @return The old value of the attribute.  <tt>null</tt> if not found.
     */
    public Object removeAttribute(Object key);

    /**
     * Removes a user defined attribute with the specified key if the current
     * attribute value is equal to the specified value.  This method is same
     * with the following code except that the operation is performed
     * atomically.
     * <pre>
     * if (containsAttribute(key) &amp;&amp; getAttribute(key).equals(value)) {
     *     removeAttribute(key);
     *     return true;
     * } else {
     *     return false;
     * }
     * </pre>
     * 
     * 在当前session上移除指定key和value的用户自定义属性。
     * 
     * @param key The key we want to remove
     * @param value The value we want to remove
     * @return <tt>true</tt> if the removal was successful
     */
    public boolean removeAttribute(Object key, Object value);

    /**
     * Replaces a user defined attribute with the specified key if the
     * value of the attribute is equals to the specified old value.
     * This method is same with the following code except that the operation
     * is performed atomically.
     * <pre>
     * if (containsAttribute(key) &amp;&amp; getAttribute(key).equals(oldValue)) {
     *     setAttribute(key, newValue);
     *     return true;
     * } else {
     *     return false;
     * }
     * </pre>
     * 
     * 在当前session上替换指定key和oldValue的用户自定义属性的值为newValue。
     * 
     * @param key The key we want to replace
     * @param oldValue The previous value
     * @param newValue The new value
     * @return <tt>true</tt> if the replacement was successful
     */
    public boolean replaceAttribute(Object key, Object oldValue, Object newValue);

    /**
     * 返回当前session上是否存在指定key的用户自定义属性。
     * 
     * @param key The key of the attribute we are looking for in the session 
     * @return <tt>true</tt> if this session contains the attribute with
     * the specified <tt>key</tt>.
     */
    public boolean containsAttribute(Object key);

    /**
     * @return the set of keys of all user-defined attributes.
     * 
     * 返回当前session上所有用户自定义属性的key的集合。
     */
    public Set<Object> getAttributeKeys();
    
    /**
     * @return <tt>true</tt> if this session is connected with remote peer.
     * 
     * 返回当前session是否和一个远程节点相连。
     */
    public boolean isConnected();
    
    /**
     * @return <tt>true</tt> if this session is active.
     * 
     * 返回当前session是否活跃。
     */
    public boolean isActive();

    /**
     * @return <tt>true</tt> if and only if this session is being closed
     * (but not disconnected yet) or is closed.
     * 
     * 返回当前session是否正处于关闭动作进行中状态(开始关闭但是还没关闭)，或者已关闭。
     */
    public boolean isClosing();
    
    /**
     * @return <tt>true</tt> if the session has started and initialized a SslEngine,
     * <tt>false</tt> if the session is not yet secured (the handshake is not completed)
     * or if SSL is not set for this session, or if SSL is not even an option.
     * 
     * 返回true：当前session开启并初始化了SslEngine。
     * 返回false：当前session的SSL握手还没完成或者当前session没有配置SSL选型。
     */
    public boolean isSecured();
    
    /**
     * @return the {@link CloseFuture} of this session.  This method returns
     * the same instance whenever user calls it.
     * 
     * 返回当前session的CloseFuture，无论什么时候调用，这个方法都返回同一个CloseFuture实例。
     */
    public CloseFuture getCloseFuture();
    
    /**
     * @return the socket address of remote peer.
     * 
     * 返回当前session连接的远程Socket地址。
     */
    public SocketAddress getRemoteAddress();

    /**
     * @return the socket address of local machine which is associated with this
     * session.
     * 
     * 返回当前session关联的本地Socket地址。
     */
    public SocketAddress getLocalAddress();

    /**
     * @return the socket address of the {@link IoService} listens to to manage
     * this session.  If this session is managed by {@link IoAcceptor}, it
     * returns the {@link SocketAddress} which is specified as a parameter of
     * {@link IoAcceptor#bind()}.  If this session is managed by
     * {@link IoConnector}, this method returns the same address with
     * that of {@link #getRemoteAddress()}.
     * 
     * 返回管理当前session的IoService监听的Socket地址。
     * 如果是IoAcceptor，返回bind()指定的参数。
     * 如果是IoConnector，返回的地址同getRemoteAddress()方法的返回值一样。
     */
    public SocketAddress getServiceAddress();
    
    /**
     * Associate the current write request with the session
     * 
     * 设置当前的写请求(WriteRequest)。
     *
     * @param currentWriteRequest the current write request to associate
     */
    public void setCurrentWriteRequest(WriteRequest currentWriteRequest);
    
    /**
     * Suspends read operations for this session.
     * 
     * 阻塞当前session上的读操作。
     */
    public void suspendRead();

    /**
     * Suspends write operations for this session.
     * 
     * 阻塞当前session上的写操作。
     */
    public void suspendWrite();

    /**
     * Resumes read operations for this session.
     * 
     * 恢复当前session上的读操作。
     */
    public void resumeRead();

    /**
     * Resumes write operations for this session.
     * 
     * 恢复当前session上的写操作。
     */
    public void resumeWrite();

    /**
     * Is read operation is suspended for this session. 
     * 
     * 返回当前session上的读操作是否被阻塞。
     * 
     * @return <tt>true</tt> if suspended
     */
    public boolean isReadSuspended();

    /**
     * Is write operation is suspended for this session.
     * 
     * 返回当前session上的写操作是否被阻塞。
     * 
     * @return <tt>true</tt> if suspended
     */
    public boolean isWriteSuspended();
    
    /**
     * Update all statistical properties related with throughput assuming
     * the specified time is the current time.  By default this method returns
     * silently without updating the throughput properties if they were
     * calculated already within last
     * {@link IoSessionConfig#getThroughputCalculationInterval() calculation interval}.
     * If, however, <tt>force</tt> is specified as <tt>true</tt>, this method
     * updates the throughput properties immediately.
     * 
     * 更新指定时间的所有的吞吐率统计。
     * 默认情况下，此方法不更新吞吐率而直接返回(因为IoSessionConfig.getThroughputCalculationInterval()配置了吞吐率计算方式)。
     * 如果指定force为true时，此方法会立即更新吞吐率属性。
     * 
     * @param currentTime the current time in milliseconds
     * @param force Force the update if <tt>true</tt>
     */
    public void updateThroughput(long currentTime, boolean force);
    
    /**
     * @return the total number of bytes which were read from this session.
     * 
     * 返回当前session已读的字节数。
     */
    public long getReadBytes();

    /**
     * @return the total number of bytes which were written to this session.
     * 
     * 返回当前session已写的字节数。
     */
    public long getWrittenBytes();

    /**
     * @return the total number of messages which were read and decoded from this session.
     * 
     * 返回当前session已读并解码的message数。
     */
    public long getReadMessages();

    /**
     * @return the total number of messages which were written and encoded by this session.
     * 
     * 返回当前session已写并编码的message数。
     */
    public long getWrittenMessages();

    /**
     * @return the number of read bytes per second.
     * 
     * 返回当前session上的每秒读的字节数(吞吐率)
     */
    public double getReadBytesThroughput();

    /**
     * @return the number of written bytes per second.
     * 
     * 返回当前session上的每秒写的字节数(吞吐率)
     */
    public double getWrittenBytesThroughput();

    /**
     * @return the number of read messages per second.
     * 
     * 返回当前session上的每秒读的message数(吞吐率)
     */
    public double getReadMessagesThroughput();

    /**
     * @return the number of written messages per second.
     * 
     * 返回当前session上的每秒写的message数(吞吐率)
     */
    public double getWrittenMessagesThroughput();

    /**
     * @return the number of messages which are scheduled to be written to this session.
     * 
     * 返回当前session上的等待写的message数。
     */
    public int getScheduledWriteMessages();

    /**
     * @return the number of bytes which are scheduled to be written to this
     * session.
     * 
     * 返回当前session上的等待写的字节数。
     */
    public long getScheduledWriteBytes();

    /**
     * Returns the message which is being written by {@link IoService}.
     * 
     * 返回IoService在写的当前session上的message。
     * 当没有消息在写时返回null。
     * 
     * @return <tt>null</tt> if and if only no message is being written
     */
    public Object getCurrentWriteMessage();

    /**
     * Returns the {@link WriteRequest} which is being processed by
     * {@link IoService}.
     * 
     * 返回IoService在处理的当前session上的WriteRequest。
     * 当没有消息在写时返回null。
     *
     * @return <tt>null</tt> if and if only no message is being written
     */
    public WriteRequest getCurrentWriteRequest();
    
    /**
     * @return the session's creation time in milliseconds
     * 
     * 返回当前session的创建时间(毫秒)。
     */
    public long getCreationTime();

    /**
     * @return the time in millis when I/O occurred lastly.
     * 
     * 返回当前session上最后一次读或写操作发生的时间(毫秒)。
     */
    public long getLastIoTime();

    /**
     * @return the time in millis when read operation occurred lastly.
     * 
     * 返回当前session上最后一次读操作发生的时间(毫秒)。
     */
    public long getLastReadTime();

    /**
     * @return the time in millis when write operation occurred lastly.
     * 
     * 返回当前session上最后一次写操作发生的时间(毫秒)。
     */
    public long getLastWriteTime();
    
    /**
     * @param status The researched idle status
     * @return <tt>true</tt> if this session is idle for the specified
     * {@link IdleStatus}.
     * 
     * 返回当前session在指定的IdleStatus状态是否空闲。
     */
    public boolean isIdle(IdleStatus status);

    /**
     * @return <tt>true</tt> if this session is {@link IdleStatus#READER_IDLE}.
     * @see #isIdle(IdleStatus)
     * 
     * 返回当前session在IdleStatus.READER_IDLE状态是否空闲。
     */
    public boolean isReaderIdle();

    /**
     * @return <tt>true</tt> if this session is {@link IdleStatus#WRITER_IDLE}.
     * @see #isIdle(IdleStatus)
     * 
     * 返回当前session在IdleStatus.WRITER_IDLE状态是否空闲。
     */
    public boolean isWriterIdle();

    /**
     * @return <tt>true</tt> if this session is {@link IdleStatus#BOTH_IDLE}.
     * @see #isIdle(IdleStatus)
     * 
     * 返回当前session在IdleStatus.BOTH_IDLE状态是否空闲。
     */
    public boolean isBothIdle();

    /**
     * @param status The researched idle status
     * @return the number of the fired continuous <tt>sessionIdle</tt> events
     * for the specified {@link IdleStatus}.
     * <p>
     * If <tt>sessionIdle</tt> event is fired first after some time after I/O,
     * <tt>idleCount</tt> becomes <tt>1</tt>.  <tt>idleCount</tt> resets to
     * <tt>0</tt> if any I/O occurs again, otherwise it increases to
     * <tt>2</tt> and so on if <tt>sessionIdle</tt> event is fired again without
     * any I/O between two (or more) <tt>sessionIdle</tt> events.
     * 
     * 返回当前session在指定的IdleStatus状态连续触发空闲事件的次数(要连续触发，中间发生I/O操作会清零并重新计数)。
     */
    public int getIdleCount(IdleStatus status);

    /**
     * @return the number of the fired continuous <tt>sessionIdle</tt> events
     * for {@link IdleStatus#READER_IDLE}.
     * @see #getIdleCount(IdleStatus)
     * 
     * 返回当前session在IdleStatus.READER_IDLE状态连续触发空闲事件的次数(要连续触发，中间发生I/O操作会清零并重新计数)。
     */
    public int getReaderIdleCount();

    /**
     * @return the number of the fired continuous <tt>sessionIdle</tt> events
     * for {@link IdleStatus#WRITER_IDLE}.
     * @see #getIdleCount(IdleStatus)
     * 
     * 返回当前session在IdleStatus.WRITER_IDLE状态连续触发空闲事件的次数(要连续触发，中间发生I/O操作会清零并重新计数)。
     */
    public int getWriterIdleCount();

    /**
     * @return the number of the fired continuous <tt>sessionIdle</tt> events
     * for {@link IdleStatus#BOTH_IDLE}.
     * @see #getIdleCount(IdleStatus)
     * 
     * 返回当前session在IdleStatus.BOTH_IDLE状态连续触发空闲事件的次数(要连续触发，中间发生I/O操作会清零并重新计数)。
     */
    public int getBothIdleCount();

    /**
     * @param status The researched idle status
     * @return the time in milliseconds when the last <tt>sessionIdle</tt> event
     * is fired for the specified {@link IdleStatus}.
     * 
     * 返回当前session在指定的IdleStatus状态上最后一次空闲事件触发的时间(毫秒)。
     */
    public long getLastIdleTime(IdleStatus status);

    /**
     * @return the time in milliseconds when the last <tt>sessionIdle</tt> event
     * is fired for {@link IdleStatus#READER_IDLE}.
     * @see #getLastIdleTime(IdleStatus)
     * 
     * 返回当前session在IdleStatus.READER_IDLE状态上最后一次空闲事件触发的时间(毫秒)。
     */
    public long getLastReaderIdleTime();

    /**
     * @return the time in milliseconds when the last <tt>sessionIdle</tt> event
     * is fired for {@link IdleStatus#WRITER_IDLE}.
     * @see #getLastIdleTime(IdleStatus)
     * 
     * 返回当前session在IdleStatus.WRITER_IDLE状态上最后一次空闲事件触发的时间(毫秒)。
     */
    public long getLastWriterIdleTime();

    /**
     * @return the time in milliseconds when the last <tt>sessionIdle</tt> event
     * is fired for {@link IdleStatus#BOTH_IDLE}.
     * @see #getLastIdleTime(IdleStatus)
     * 
     * 返回当前session在IdleStatus.BOTH_IDLE状态上最后一次空闲事件触发的时间(毫秒)。
     */
    public long getLastBothIdleTime();
}
