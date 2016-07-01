package org.apache.mina.transport.socket;

import java.net.Socket;

import org.apache.mina.core.session.IoSessionConfig;

/**
 * An {@link IoSessionConfig} for socket transport type.
 * 
 * socket传输方式(TCP/IP)的IoSessionConfig接口。
 * 
 * @date	2016年6月20日 上午10:58:37	completed
 */
public interface SocketSessionConfig extends IoSessionConfig {

	/**
     * @see Socket#getReuseAddress()
     * 
     * 返回监听的本地Socket地址是否可重用标识。
     * 
     * @return <tt>true</tt> if SO_REUSEADDR is enabled.
     */
	public boolean isReuseAddress();

    /**
     * @see Socket#setReuseAddress(boolean)
     * 
     * 设置监听的本地Socket地址是否可重用标识。
     * 
     * @param reuseAddress Tells if SO_REUSEADDR is enabled or disabled
     */
	public void setReuseAddress(boolean reuseAddress);

    /**
     * @see Socket#getReceiveBufferSize()
     * 
     * 获取此 socket的 SO_RCVBUF选项的值，该值是平台在 socket上输入时使用的缓冲区大小。
     * 
     * @return the size of the receive buffer
     */
	public int getReceiveBufferSize();

    /**
     * @see Socket#setReceiveBufferSize(int)
     * 
     * 将此 socket的 SO_RCVBUF选项设置为指定的值。设置平台中此socket的接收区缓冲。 
     * 
     * @param receiveBufferSize The size of the receive buffer
     */
	public void setReceiveBufferSize(int receiveBufferSize);

    /**
     * @see Socket#getSendBufferSize()
     * 
     * 获取此 socket的 SO_SNDBUF选项的值，该值是平台在socket上输出时使用的缓冲区大小。
     * 
     * @return the size of the send buffer
     */
	public int getSendBufferSize();

    /**
     * @see Socket#setSendBufferSize(int)
     * 
     * 将此 socket的 SO_SNDBUF选项设置为指定的值。设置平台中此socket的发送区缓冲。 
     * 
     * @param sendBufferSize The size of the send buffer
     */
	public void setSendBufferSize(int sendBufferSize);

    /**
     * @see Socket#getTrafficClass()
     * 
     * 获取此socket上发送的包的IP头中的流量类别或服务类型。
     * 
     * @return the traffic class
     */
	public int getTrafficClass();

    /**
     * @see Socket#setTrafficClass(int)
     * 
     * 设置此socket上发送的包的IP头中的流量类别或服务类型。由于底层网络实现可能忽略此值，应用程序应该将其视为一种提示。
     * 
     * @param trafficClass The traffic class to set, one of <tt>IPTOS_LOWCOST</tt> (0x02)
     * <tt>IPTOS_RELIABILITY</tt> (0x04), <tt>IPTOS_THROUGHPUT</tt> (0x08) or <tt>IPTOS_LOWDELAY</tt> (0x10)
     */
	public void setTrafficClass(int trafficClass);

    /**
     * @see Socket#getKeepAlive()
     * 
     * 是否启用SO_KEEPALIVE。
     * 
     * @return <tt>true</tt> if <tt>SO_KEEPALIVE</tt> is enabled.
     */
	public boolean isKeepAlive();

    /**
     * @see Socket#setKeepAlive(boolean)
     * 
     * 启用/禁用SO_KEEPALIVE。
     * 
     * @param keepAlive if <tt>SO_KEEPALIVE</tt> is to be enabled
     */
	public void setKeepAlive(boolean keepAlive);

    /**
     * @see Socket#getOOBInline()
     * 
     * 是否启用OOBINLINE（TCP紧急数据的接收者）。
     * 
     * @return <tt>true</tt> if <tt>SO_OOBINLINE</tt> is enabled.
     */
	public boolean isOobInline();

    /**
     * @see Socket#setOOBInline(boolean)
     * 
     * 启用/禁用 OOBINLINE（TCP紧急数据的接收者） 默认情况下，此选项是禁用的，即在套接字上接收的 TCP 紧急数据被静默丢弃。
     * 如果用户希望接收到紧急数据，则必须启用此选项。启用时，可以将紧急数据内嵌在普通数据中接收 。
     * 注意，仅为处理传入紧急数据提供有限支持。
     * 特别要指出的是，不提供传入紧急数据的任何通知并且不存在区分普通数据和紧急数据的功能（除非更高级别的协议提供）。
     * 
     * @param oobInline if <tt>SO_OOBINLINE</tt> is to be enabled
     */
	public void setOobInline(boolean oobInline);

    /**
     * Please note that enabling <tt>SO_LINGER</tt> in Java NIO can result
     * in platform-dependent behavior and unexpected blocking of I/O thread.
     * 
     * 返回 SO_LINGER的设置。返回 -1意味着禁用该选项。 该设置仅影响套接字关闭。
     * 
     * SO_LINGER选项用来设置延迟关闭的时间，等待套接字发送缓冲区中的数据发送完成。
     * 没有设置该选项时，在调用close()后，在发送完FIN后会立即进行一些清理工作并返回。
     * 如果设置了SO_LINGER选项，并且等待时间为正值，则在清理之前会等待一段时间。
     * 
     * 注意：在Java NIO方式中开启SO_LINGER会依赖于平台的行为以及I/O线程未预期的阻塞。
     *
     * @see Socket#getSoLinger()
     * @see <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6179351">Sun Bug Database</a>
     * 
     * @return The value for <tt>SO_LINGER</tt>
     */
	public int getSoLinger();

    /**
     * Please note that enabling <tt>SO_LINGER</tt> in Java NIO can result
     * in platform-dependent behavior and unexpected blocking of I/O thread.
     * 
     * 启用/禁用具有指定逗留时间（以秒为单位）的 SO_LINGER。最大超时值是特定于平台的。 该设置仅影响套接字关闭。
     * 
     * SO_LINGER选项用来设置延迟关闭的时间，等待套接字发送缓冲区中的数据发送完成。
     * 没有设置该选项时，在调用close()后，在发送完FIN后会立即进行一些清理工作并返回。
     * 如果设置了SO_LINGER选项，并且等待时间为正值，则在清理之前会等待一段时间。
     * 
     * 注意：在Java NIO方式中开启SO_LINGER会依赖于平台的行为以及I/O线程未预期的阻塞。
     *
     * @param soLinger Please specify a negative value to disable <tt>SO_LINGER</tt>.
     *
     * @see Socket#setSoLinger(boolean, int)
     * @see <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6179351">Sun Bug Database</a>
     */
	public void setSoLinger(int soLinger);

    /**
     * @see Socket#getTcpNoDelay()
     * 
     * 是否启用TCP_NODELAY。
     * 
     * 默认情况下, 发送数据采用Negale算法. Negale算法是指发送方发送的数据不会立即发出,
     * 而是先放在缓冲区, 等缓存区满了再发出. 发送完一批数据后, 会等待接收方对这批数据的回应,
     * 然后再发送下一批数据. Negale算法适用于发送方需要发送大批量数据, 并且接收方会及时作出
     * 回应的场合, 这种算法通过减少传输数据的次数来提高通信效率。
     * 
     * @return <tt>true</tt> if <tt>TCP_NODELAY</tt> is enabled.
     */
	public boolean isTcpNoDelay();

    /**
     * @see Socket#setTcpNoDelay(boolean)
     * 
     * 启用/禁用TCP_NODELAY（启用/禁用 Nagle算法）。 
     * 
     * 默认情况下, 发送数据采用Negale算法. Negale算法是指发送方发送的数据不会立即发出,
     * 而是先放在缓冲区, 等缓存区满了再发出. 发送完一批数据后, 会等待接收方对这批数据的回应,
     * 然后再发送下一批数据. Negale算法适用于发送方需要发送大批量数据, 并且接收方会及时作出
     * 回应的场合, 这种算法通过减少传输数据的次数来提高通信效率。
     * 
     * @param tcpNoDelay <tt>true</tt> if <tt>TCP_NODELAY</tt> is to be enabled
     */
	public void setTcpNoDelay(boolean tcpNoDelay);
}
