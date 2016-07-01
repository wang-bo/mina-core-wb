package org.apache.mina.core.service;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.List;
import java.util.Set;

import org.apache.mina.core.session.IoSession;

/**
 * Accepts incoming connection, communicates with clients, and fires events to
 * {@link IoHandler}s.
 * <p>
 * Please refer to
 * <a href="../../../../../xref-examples/org/apache/mina/examples/echoserver/Main.html">EchoServer</a>
 * example.
 * <p>
 * You should bind to the desired socket address to accept incoming
 * connections, and then events for incoming connections will be sent to
 * the specified default {@link IoHandler}.
 * <p>
 * Threads accept incoming connections start automatically when
 * {@link #bind()} is invoked, and stop when {@link #unbind()} is invoked.
 * 
 * 负责接收连接请求，与客户端通讯，并调用IoHandler处理事件。
 * 应该bind到特定socket地址来接收连接请求。
 * 当bind()方法调用后，会自动开始接收连接请求，unbind()方法调用后会自动结束。
 * 
 * @date	2016年6月13日 上午9:24:52	completed
 */
public interface IoAcceptor extends IoService {

	/**
     * Returns the local address which is bound currently.  If more than one
     * address are bound, only one of them will be returned, but it's not
     * necessarily the firstly bound address.
     * 
     * 返回当前绑定的本地Socket地址，如果超过1个地址被绑定，只会返回一个，而且不一定是第一个被绑定的地址。
     * 
     * @return The bound LocalAddress
     */
	public SocketAddress getLocalAddress();
	
	/**
     * Returns a {@link Set} of the local addresses which are bound currently.
     * 放回当前绑定的本地Socket地址集合。
     * 
     * @return The Set of bound LocalAddresses
     */
	public Set<SocketAddress> getLocalAddresses();
	
	/**
     * Returns the default local address to bind when no argument is specified
     * in {@link #bind()} method.  Please note that the default will not be
     * used if any local address is specified.  If more than one address are
     * set, only one of them will be returned, but it's not necessarily the
     * firstly specified address in {@link #setDefaultLocalAddresses(List)}.
     * 
     * 返回默认绑定的本地Socket地址，当bind()方法不带参数执行时，会使用这个地址。
     * 注意：如果bind()方法指定了本地Socket地址，设置的默认本地Socket地址不会被使用。
     * 如果有多个地址，只会返回1个，但不一定是setDefaultLocalAddresses(List)方法调用时设置的第一个地址。
     * 
     * @return The default bound LocalAddress
     */
	public SocketAddress getDefaultLocalAddress();
	
	/**
     * Returns a {@link List} of the default local addresses to bind when no
     * argument is specified in {@link #bind()} method.  Please note that the
     * default will not be used if any local address is specified.
     * 
     * 返回默认绑定的本地Socket地址，当bind()方法不带参数执行时，会使用这个地址。
     * 注意：如果bind()方法指定了本地Socket地址，设置的默认本地Socket地址不会被使用。
     * 
     * @return The list of default bound LocalAddresses
     */
	public List<SocketAddress> getDefaultLocalAddresses();
	
	/**
     * Sets the default local address to bind when no argument is specified in
     * {@link #bind()} method.  Please note that the default will not be used
     * if any local address is specified.
     * 
     * 设置默认绑定的本地Socket地址，当bind()方法不带参数执行时，会使用这个地址。
     * 注意：如果bind()方法指定了本地Socket地址，设置的默认本地Socket地址不会被使用。
     * 
     * @param localAddress The local addresses to bind the acceptor on
     */
	public void setDefaultLocalAddress(SocketAddress localAddress);
	
	/**
     * Sets the default local addresses to bind when no argument is specified
     * in {@link #bind()} method.  Please note that the default will not be
     * used if any local address is specified.
     * 
     * 设置默认绑定的本地Socket地址(列表)，当bind()方法不带参数执行时，会使用这个地址。
     * 注意：如果bind()方法指定了本地Socket地址，设置的默认本地Socket地址不会被使用。
     * 
     * @param firstLocalAddress The first local address to bind the acceptor on
     * @param otherLocalAddresses The other local addresses to bind the acceptor on
     */
	public void setDefaultLocalAddresses(SocketAddress firstLocalAddress, SocketAddress... otherLocalAddress);
	
	/**
     * Sets the default local addresses to bind when no argument is specified
     * in {@link #bind()} method.  Please note that the default will not be
     * used if any local address is specified.
     * 
     * 设置默认绑定的本地Socket地址(列表)，当bind()方法不带参数执行时，会使用这个地址。
     * 注意：如果bind()方法指定了本地Socket地址，设置的默认本地Socket地址不会被使用。
     * 
     * @param localAddresses The local addresses to bind the acceptor on
     */
	public void setDefaultLocalAddresses(Iterable<? extends SocketAddress> localAddresses);
	
	/**
     * Sets the default local addresses to bind when no argument is specified
     * in {@link #bind()} method.  Please note that the default will not be
     * used if any local address is specified.
     * 
     * 设置默认绑定的本地Socket地址(列表)，当bind()方法不带参数执行时，会使用这个地址。
     * 注意：如果bind()方法指定了本地Socket地址，设置的默认本地Socket地址不会被使用。
     * 
     * @param localAddresses The local addresses to bind the acceptor on
     */
	public void setDefaultLocalAddresses(List<? extends SocketAddress> localAddresses);
	
	/**
     * Returns <tt>true</tt> if and only if all clients are closed when this
     * acceptor unbinds from all the related local address (i.e. when the
     * service is deactivated).
     * 
     * 返回当acceptor从所有关联的本地Socket地址解绑时，是否关闭所有已连接的客户端(true为要关闭，false为不关闭)
     * (即：当service不活动时)。
     * 
     * @return <tt>true</tt> if the service sets the closeOnDeactivation flag
     */
	public boolean isCloseOnDeactivation();
	
	/**
     * Sets whether all client sessions are closed when this acceptor unbinds
     * from all the related local addresses (i.e. when the service is
     * deactivated).  The default value is <tt>true</tt>.
     * 
     * 设置当acceptor从所有关联的本地Socket地址解绑时，是否关闭所有已连接的客户端(true为要关闭，false为不关闭，默认为true)
     * (即：当service不活动时)。
     * 
     * @param closeOnDeactivation <tt>true</tt> if we should close on deactivation
     */
    public void setCloseOnDeactivation(boolean closeOnDeactivation);
    
    /**
     * Binds to the default local address(es) and start to accept incoming
     * connections.
     * 
     * 绑定默认本地Socket地址(列表)，并开始接收连接请求。
     *
     * @throws IOException if failed to bind
     */
    public void bind() throws IOException;
    
    /**
     * Binds to the specified local address and start to accept incoming
     * connections.
     * 
     * 绑定指定的本地Socket地址，并开始接收连接请求。
     *
     * @param localAddress The SocketAddress to bind to
     * 
     * @throws IOException if failed to bind
     */
    public void bind(SocketAddress localAddress) throws IOException;
    
    /**
     * Binds to the specified local addresses and start to accept incoming
     * connections. If no address is given, bind on the default local address.
     * 
     * 绑定指定的本地Socket地址列表，并开始接收连接请求。
     * 
     * @param firstLocalAddress The first address to bind to
     * @param addresses The SocketAddresses to bind to
     * 
     * @throws IOException if failed to bind
     */
    public void bind(SocketAddress firstLocalAddress, SocketAddress... addresses) throws IOException;

    /**
     * Binds to the specified local addresses and start to accept incoming
     * connections. If no address is given, bind on the default local address.
     * 
     * 绑定指定的本地Socket地址列表，并开始接收连接请求，如果参数列表为空，则绑定默认本地Socket地址。
     * 
     * @param addresses The SocketAddresses to bind to
     *
     * @throws IOException if failed to bind
     */
    public void bind(SocketAddress... addresses) throws IOException;

    /**
     * Binds to the specified local addresses and start to accept incoming
     * connections.
     * 
     * 绑定指定的本地Socket地址列表，并开始接收连接请求。
     *
     * @param localAddresses The local address we will be bound to
     * @throws IOException if failed to bind
     */
    public void bind(Iterable<? extends SocketAddress> localAddresses) throws IOException;
    
    /**
     * Unbinds from all local addresses that this service is bound to and stops
     * to accept incoming connections.  All managed connections will be closed
     * if {@link #setCloseOnDeactivation(boolean) disconnectOnUnbind} property
     * is <tt>true</tt>.  This method returns silently if no local address is
     * bound yet.
     * 
     * 解绑所有已绑定的本地Socket地址，并停止接收连接请求。
     * 如果setCloseOnDeactivation(boolean)方法设置了true，关闭所有已建立的连接。
     * 如果当前没有绑定任何地址，这个方法会默默的返回。
     * 
     */
    public void unbind();

    /**
     * Unbinds from the specified local address and stop to accept incoming
     * connections.  All managed connections will be closed if
     * {@link #setCloseOnDeactivation(boolean) disconnectOnUnbind} property is
     * <tt>true</tt>.  This method returns silently if the default local
     * address is not bound yet.
     * 
     * 解绑所有指定的本地Socket地址，并停止接收这个地址上的连接请求。
     * 如果setCloseOnDeactivation(boolean)方法设置了true，关闭这个地址上所有已建立的连接。
     * 如果指定的Socket地址并没有被绑定，这个方法会默默的返回。
     * 
     * @param localAddress The local address we will be unbound from
     */
    public void unbind(SocketAddress localAddress);

    /**
     * Unbinds from the specified local addresses and stop to accept incoming
     * connections.  All managed connections will be closed if
     * {@link #setCloseOnDeactivation(boolean) disconnectOnUnbind} property is
     * <tt>true</tt>.  This method returns silently if the default local
     * addresses are not bound yet.
     * 
     * 解绑所有指定的本地Socket地址列表，并停止接收这些地址上的连接请求。
     * 如果setCloseOnDeactivation(boolean)方法设置了true，关闭这些地址上所有已建立的连接。
     * 如果指定的Socket地址并没有被绑定，这个方法会默默的返回。
     * 
     * @param firstLocalAddress The first local address to be unbound from
     * @param otherLocalAddresses The other local address to be unbound from
     */
    public void unbind(SocketAddress firstLocalAddress, SocketAddress... otherLocalAddresses);

    /**
     * Unbinds from the specified local addresses and stop to accept incoming
     * connections.  All managed connections will be closed if
     * {@link #setCloseOnDeactivation(boolean) disconnectOnUnbind} property is
     * <tt>true</tt>.  This method returns silently if the default local
     * addresses are not bound yet.
     * 
     * 解绑所有指定的本地Socket地址列表，并停止接收这些地址上的连接请求。
     * 如果setCloseOnDeactivation(boolean)方法设置了true，关闭这些地址上所有已建立的连接。
     * 如果指定的Socket地址并没有被绑定，这个方法会默默的返回。
     * 
     * @param localAddresses The local address we will be unbound from
     */
    public void unbind(Iterable<? extends SocketAddress> localAddresses);
    
    /**
     * (Optional) Returns an {@link IoSession} that is bound to the specified
     * <tt>localAddress</tt> and the specified <tt>remoteAddress</tt> which
     * reuses the local address that is already bound by this service.
     * <p>
     * This operation is optional.  Please throw {@link UnsupportedOperationException}
     * if the transport type doesn't support this operation.  This operation is
     * usually implemented for connectionless transport types.
     * 
     * 返回一个绑定指定的远程Socket地址和本地Socket地址的IoSession
     * 这个方法是可选择的，如果传输方式(transport type)不支持，请抛出UnsupportedOperationException，
     * 这个方法通常用来实现无连接的传输方式。
     *
     * @param remoteAddress The remote address bound to the service
     * @param localAddress The local address the session will be bound to
     * @throws UnsupportedOperationException if this operation is not supported
     * @throws IllegalStateException if this service is not running.
     * @throws IllegalArgumentException if this service is not bound to the
     *                                  specified <tt>localAddress</tt>.
     * @return The session bound to the the given localAddress and remote address
     */
    public IoSession newSession(SocketAddress remoteAddress, SocketAddress loAddress);
}
