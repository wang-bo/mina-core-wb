package org.apache.mina.core.service;

import java.net.SocketAddress;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;

/**
 * Connects to endpoint, communicates with the server, and fires events to
 * {@link IoHandler}s.
 * <p>
 * Please refer to
 * <a href="../../../../../xref-examples/org/apache/mina/examples/netcat/Main.html">NetCat</a>
 * example.
 * <p>
 * You should connect to the desired socket address to start communication,
 * and then events for incoming connections will be sent to the specified
 * default {@link IoHandler}.
 * <p>
 * Threads connect to endpoint start automatically when
 * {@link #connect(SocketAddress)} is invoked, and stop when all
 * connection attempts are finished.
 * 
 * 负责连接服务器，与服务器通讯，并调用IoHandler处理事件
 * 应该连接到特定服务器socket地址来开启通讯。
 * 当connect()方法调用后，会自动开始连接请求
 * 
 * @date	2016年6月13日 上午10:17:50	completed
 */
public interface IoConnector extends IoService {

	/**
     * @return the connect timeout in seconds.  The default value is 1 minute.
     * 
     * 返回连接超时时间(秒)，默认1分钟，该方法已废弃
     * 
     * @deprecated
     */
	public int getConnectTimeout();
	
	/**
     * @return the connect timeout in milliseconds.  The default value is 1 minute.
     * 
     * 返回连接超时时间(毫秒)，默认1分钟。
     */
    public long getConnectTimeoutMillis();
    
    /**
     * Sets the connect timeout in seconds.  The default value is 1 minute.
     * 
     * 设置连接超时时间(秒)，默认1分钟，该方法已废弃
     * 
     * @deprecated
     * @param connectTimeout The time out for the connection
     */
    public void setConnectTimeout(int connectTimeout);

    /**
     * Sets the connect timeout in milliseconds.  The default value is 1 minute.
     * 
     * 设置连接超时时间(毫秒)，默认1分钟。
     * 
     * @param connectTimeoutInMillis The time out for the connection
     */
    public void setConnectTimeoutMillis(long connectTimeoutInMillis);
    
    /**
     * @return the default remote address to connect to when no argument
     * is specified in {@link #connect()} method.
     * 
     * 返回默认连接的远程Socket地址，当connect()方法不带参数执行时，会使用这个地址。
     */
    public SocketAddress getDefaultRemoteAddress();
    
    /**
     * Sets the default remote address to connect to when no argument is
     * specified in {@link #connect()} method.
     * 
     * 设置默认连接的远程Socket地址，当connect()方法不带参数执行时，会使用这个地址。
     * 
     * @param defaultRemoteAddress The default remote address
     */
    public void setDefaultRemoteAddress(SocketAddress defaultRemoteAddress);
    
    /**
     * @return the default local address
     * 
     * 返回默认本地Socket地址，当connect()方法不指定本地Socket地址时，会使用这个地址。
     */
    public SocketAddress getDefaultLocalAddress();

    /**
     * Sets the default local address
     * 
     * 设置默认本地Socket地址，当connect()方法不指定本地Socket地址时，会使用这个地址。
     * 
     * @param defaultLocalAddress The default local address
     */
    public void setDefaultLocalAddress(SocketAddress defaultLocalAddress);
    
    /**
     * Connects to the {@link #setDefaultRemoteAddress(SocketAddress) default
     * remote address}.
     * 
     * 连接设置的默认远程Socket地址
     * 
     * @return the {@link ConnectFuture} instance which is completed when the
     *         connection attempt initiated by this call succeeds or fails.
     * @throws IllegalStateException
     *             if no default remoted address is set.
     */
    public ConnectFuture connect();

    /**
     * Connects to the {@link #setDefaultRemoteAddress(SocketAddress) default
     * remote address} and invokes the <code>ioSessionInitializer</code> when
     * the IoSession is created but before {@link IoHandler#sessionCreated(IoSession)}
     * is invoked.  There is <em>no</em> guarantee that the <code>ioSessionInitializer</code>
     * will be invoked before this method returns.
     * 
     * 连接设置的默认远程Socket地址。
     * 当IoSession创建时，在IoHandler.sessionCreated()方法执行前，
     * 先执行IoSessionInitializer.initializeSession()方法，
     * 但不保证IoSessionInitializer.initializeSession()方法在本方法返回前执行。
     * 
     * @param sessionInitializer  the callback to invoke when the {@link IoSession} object is created
     * @return the {@link ConnectFuture} instance which is completed when the
     *         connection attempt initiated by this call succeeds or fails.
     * 
     * @throws IllegalStateException if no default remote address is set.
     */
    public ConnectFuture connect(IoSessionInitializer<? extends ConnectFuture> sessionInitializer);

    /**
     * Connects to the specified remote address.
     * 
     * 连接指定的远程Socket地址
     * 
     * @param remoteAddress The remote address to connect to
     * @return the {@link ConnectFuture} instance which is completed when the
     *         connection attempt initiated by this call succeeds or fails.
     */
    public ConnectFuture connect(SocketAddress remoteAddress);

    /**
     * Connects to the specified remote address and invokes
     * the <code>ioSessionInitializer</code> when the IoSession is created but before
     * {@link IoHandler#sessionCreated(IoSession)} is invoked.  There is <em>no</em>
     * guarantee that the <code>ioSessionInitializer</code> will be invoked before
     * this method returns.
     * 
     * 连接指定的远程Socket地址。
     * 当IoSession创建时，在IoHandler.sessionCreated()方法执行前，
     * 先执行IoSessionInitializer.initializeSession()方法，
     * 但不保证IoSessionInitializer.initializeSession()方法在本方法返回前执行。
     * 
     * @param remoteAddress  the remote address to connect to
     * @param sessionInitializer  the callback to invoke when the {@link IoSession} object is created
     * 
     * @return the {@link ConnectFuture} instance which is completed when the
     *         connection attempt initiated by this call succeeds or fails.
     */
    public ConnectFuture connect(SocketAddress remoteAddress, IoSessionInitializer<? extends ConnectFuture> sessionInitializer);

    /**
     * Connects to the specified remote address binding to the specified local address.
     * 
     * 用指定的本地Socket地址连接指定的远程Socket地址。
     *
     * @param remoteAddress The remote address to connect
     * @param localAddress The local address to bind
     * 
     * @return the {@link ConnectFuture} instance which is completed when the
     *         connection attempt initiated by this call succeeds or fails.
     */
    public ConnectFuture connect(SocketAddress remoteAddress, SocketAddress localAddress);

    /**
     * Connects to the specified remote address binding to the specified local
     * address and and invokes the <code>ioSessionInitializer</code> when the
     * IoSession is created but before {@link IoHandler#sessionCreated(IoSession)}
     * is invoked.  There is <em>no</em> guarantee that the <code>ioSessionInitializer</code>
     * will be invoked before this method returns.
     * 
     * 用指定的本地Socket地址连接指定的远程Socket地址。
     * 当IoSession创建时，在IoHandler.sessionCreated()方法执行前，
     * 先执行IoSessionInitializer.initializeSession()方法，
     * 但不保证IoSessionInitializer.initializeSession()方法在本方法返回前执行。
     * 
     * @param remoteAddress  the remote address to connect to
     * @param localAddress  the local interface to bind to
     * @param sessionInitializer  the callback to invoke when the {@link IoSession} object is created
     *
     * @return the {@link ConnectFuture} instance which is completed when the
     *         connection attempt initiated by this call succeeds or fails.
     */
    public ConnectFuture connect(SocketAddress remoteAddress, SocketAddress localAddress,
            IoSessionInitializer<? extends ConnectFuture> sessionInitializer);
}
