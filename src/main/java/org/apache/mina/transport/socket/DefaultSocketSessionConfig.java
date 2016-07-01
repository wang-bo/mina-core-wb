package org.apache.mina.transport.socket;

import org.apache.mina.core.service.IoService;

/**
 * A default implementation of {@link SocketSessionConfig}.
 * 
 * SocketSessionConfig接口的默认实现。
 * 
 * @date	2016年6月20日 上午11:42:12	completed
 */
public class DefaultSocketSessionConfig extends AbstractSocketSessionConfig {

	/** 默认不重用本地Socket地址，SocketService不使用这个默认值 */
	private static final boolean DEFAULT_REUSE_ADDRESS = false;

	/** 默认设置IP包头部的流量类别为0 */
    private static final int DEFAULT_TRAFFIC_CLASS = 0;

    /** 默认不keepAlive */
    private static final boolean DEFAULT_KEEP_ALIVE = false;

    /** 默认不起用TCP紧急数据接收 */
    private static final boolean DEFAULT_OOB_INLINE = false;

    /** 默认禁用SO_LINGER， 即关闭连接时，发送完FIN后会立即进行一些清理工作并返回，而不等待一段时间，让socket缓冲区中的数据发送完成。 */
    private static final int DEFAULT_SO_LINGER = -1;

    /** 默认禁用TCP_NODELAY，即使用Negale算法，发送数据时不立即发出，先放在缓冲区，等缓存区满了再发出。
     *  发送完一批数据后，会等待接收方对这批数据的回应，然后再发送下一批数据。  */
    private static final boolean DEFAULT_TCP_NO_DELAY = false;

    /** 管理session的service */
    protected IoService parent;

    /**  */
    private boolean defaultReuseAddress;

    /**  */
    private boolean reuseAddress;

    /** The SO_RCVBUF parameter. Set to -1 (ie, will default to OS default) 
     *  默认的接收缓冲区大小，系统默认大小 */
    private int receiveBufferSize = -1;

    /** The SO_SNDBUF parameter. Set to -1 (ie, will default to OS default) 
     *  默认的发送缓冲区大小，系统默认大小 */
    private int sendBufferSize = -1;

    private int trafficClass = DEFAULT_TRAFFIC_CLASS;

    private boolean keepAlive = DEFAULT_KEEP_ALIVE;

    private boolean oobInline = DEFAULT_OOB_INLINE;

    private int soLinger = DEFAULT_SO_LINGER;

    private boolean tcpNoDelay = DEFAULT_TCP_NO_DELAY;
    
    /**
     * Creates a new instance.
     * 
     * 构造方法
     */
    public DefaultSocketSessionConfig() {
        // Do nothing
    }
    
    /**
     * 初始化，当IoService是SocketAcceptor时，默认启用本地Socket地址重用。
     * @param parent
     */
    public void init(IoService parent) {
        this.parent = parent;
        if (parent instanceof SocketAcceptor) {
            defaultReuseAddress = true;
        } else {
            defaultReuseAddress = DEFAULT_REUSE_ADDRESS;
        }
        reuseAddress = defaultReuseAddress;
    }
    
    public boolean isReuseAddress() {
        return reuseAddress;
    }

    public void setReuseAddress(boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }

    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    public void setReceiveBufferSize(int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
    }

    public int getSendBufferSize() {
        return sendBufferSize;
    }

    public void setSendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
    }

    public int getTrafficClass() {
        return trafficClass;
    }

    public void setTrafficClass(int trafficClass) {
        this.trafficClass = trafficClass;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public boolean isOobInline() {
        return oobInline;
    }

    public void setOobInline(boolean oobInline) {
        this.oobInline = oobInline;
    }

    public int getSoLinger() {
        return soLinger;
    }

    public void setSoLinger(int soLinger) {
        this.soLinger = soLinger;
    }

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }
    
    @Override
    protected boolean isKeepAliveChanged() {
        return keepAlive != DEFAULT_KEEP_ALIVE;
    }

    @Override
    protected boolean isOobInlineChanged() {
        return oobInline != DEFAULT_OOB_INLINE;
    }

    @Override
    protected boolean isReceiveBufferSizeChanged() {
        return receiveBufferSize != -1;
    }

    @Override
    protected boolean isReuseAddressChanged() {
        return reuseAddress != defaultReuseAddress;
    }

    @Override
    protected boolean isSendBufferSizeChanged() {
        return sendBufferSize != -1;
    }

    @Override
    protected boolean isSoLingerChanged() {
        return soLinger != DEFAULT_SO_LINGER;
    }

    @Override
    protected boolean isTcpNoDelayChanged() {
        return tcpNoDelay != DEFAULT_TCP_NO_DELAY;
    }

    @Override
    protected boolean isTrafficClassChanged() {
        return trafficClass != DEFAULT_TRAFFIC_CLASS;
    }
}
