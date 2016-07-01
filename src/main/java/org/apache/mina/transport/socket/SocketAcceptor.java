package org.apache.mina.transport.socket;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Set;

import org.apache.mina.core.service.IoAcceptor;

/**
 * {@link IoAcceptor} for socket transport (TCP/IP).  This class
 * handles incoming TCP/IP based socket connections.
 * 
 * socket传输方式(TCP/IP)的IoAcceptor接口。
 * 这个接口基于socket连接处理客户端请求的TCP/IP包。
 * 
 * @date	2016年6月20日 上午10:42:45	completed
 */
public interface SocketAcceptor extends IoAcceptor {

	/**
     * @return the local InetSocketAddress which is bound currently.  If more than one
     * address are bound, only one of them will be returned, but it's not
     * necessarily the firstly bound address.
     * This method overrides the {@link IoAcceptor#getLocalAddress()} method.
     * 
     * {@inheritDoc}
     */
    public InetSocketAddress getLocalAddress();

    /**
     * @return a {@link Set} of the local InetSocketAddress which are bound currently.
     * This method overrides the {@link IoAcceptor#getDefaultLocalAddress()} method.
     * 
     * {@inheritDoc}
     */
    public InetSocketAddress getDefaultLocalAddress();

    /**
     * Sets the default local InetSocketAddress to bind when no argument is specified in
     * {@link #bind()} method. Please note that the default will not be used
     * if any local InetSocketAddress is specified.
     * This method overrides the {@link IoAcceptor#setDefaultLocalAddress(java.net.SocketAddress)} method.
     * 
     * {@inheritDoc}
     * 
     * @param localAddress The local address
     */
    public void setDefaultLocalAddress(InetSocketAddress localAddress);

    /**
     * @see ServerSocket#getReuseAddress()
     * 
     * 返回监听的本地Socket地址是否可重用标识。
     * 
     * @return <tt>true</tt> if the <tt>SO_REUSEADDR</tt> is enabled
     */
    public boolean isReuseAddress();

    /**
     * @see ServerSocket#setReuseAddress(boolean)
     * 
     * 设置监听的本地Socket地址是否可重用标识。
     * 
     * @param reuseAddress tells if the <tt>SO_REUSEADDR</tt> is to be enabled
     */
    public void setReuseAddress(boolean reuseAddress);

    /**
     * @return the size of the backlog.
     * 
     * 返回能排队等待请求被接受的socket数，默认50个。
     */
    public int getBacklog();

    /**
     * Sets the size of the backlog.  This can only be done when this
     * class is not bound
     * 
     * 设置能排队等待请求被接受的socket数，默认50个。
     * 
     * @param backlog The backlog's size
     */
    public void setBacklog(int backlog);

    /**
     * @return the default configuration of the new SocketSessions created by 
     * this acceptor service.
     * 
     * {@inheritDoc}
     */
    public SocketSessionConfig getSessionConfig();
}
