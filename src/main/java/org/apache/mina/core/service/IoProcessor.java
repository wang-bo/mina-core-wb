package org.apache.mina.core.service;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

/**
 * An internal interface to represent an 'I/O processor' that performs
 * actual I/O operations for {@link IoSession}s.  It abstracts existing
 * reactor frameworks such as Java NIO once again to simplify transport
 * implementations.
 * 
 * 代表I/O处理器的内部接口，处理IoSession上真正的I/O操作。
 * 它再次抽象了Java NIO之类的通讯框架，简化实现。
 * 
 * @param <S> the type of the {@link IoSession} this processor can handle
 * 
 * @date	2016年6月14日 上午9:28:02	completed
 */
public interface IoProcessor<S extends IoSession> {

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
     * Releases any resources allocated by this processor.  Please note that 
     * the resources might not be released as long as there are any sessions
     * managed by this processor.  Most implementations will close all sessions
     * immediately and release the related resources.
     * 
     * 释放当前processor分配的任何资源。注意：当前processor上有session时，这方法会阻塞。
     * 通常的实现方式会立即关闭所有session，并释放相关资源。
     */
    public void dispose();
    
    /**
     * Adds the specified {@code session} to the I/O processor so that
     * the I/O processor starts to perform any I/O operations related
     * with the {@code session}.
     * 
     * 将IoSession加入当前processor，使当前processor开始处理IoSession的所有I/O操作。
     * 
     * @param session The added session
     */
    public void add(S session);
    
    /**
     * Flushes the internal write request queue of the specified
     * {@code session}.
     * 
     * flush指定IoSession里的内部写请求队列。
     * 
     * @param session The session we want the message to be written
     */
    public void flush(S session);
    
    /**
     * Writes the WriteRequest for the specified {@code session}.
     * 
     * 往指定IoSession里写一个WriteRequest
     * 
     * @param session The session we want the message to be written
     * @param writeRequest the WriteRequest to write
     */
    public void write(S session, WriteRequest writeRequest);
    
    /**
     * Controls the traffic of the specified {@code session} depending of the
     * {@link IoSession#isReadSuspended()} and {@link IoSession#isWriteSuspended()}
     * flags
     * 
     * 使用IoSession.isReadSuspended()和IoSession.isWriteSuspended()方法来控制指定IoSession上的通讯。
     * 
     * @param session The session to be updated
     */
    public void updateTrafficControl(S session);
    
    /**
     * Removes and closes the specified {@code session} from the I/O
     * processor so that the I/O processor closes the connection
     * associated with the {@code session} and releases any other related
     * resources.
     * 
     * 关闭并移除指定IoSession，当前processor会关闭连接并释放相关资源。
     * 
     * @param session The session to be removed
     */
    public void remove(S session);
}
