package org.apache.mina.transport.socket.nio;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.file.FileRegion;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.mina.transport.socket.AbstractSocketSessionConfig;
import org.apache.mina.transport.socket.SocketSessionConfig;

/**
 * An {@link IoSession} for socket transport (TCP/IP).
 * 
 * socket传输方式(TCP/IP)的IoSession接口实现类。
 * 
 * @date	2016年6月20日 下午2:11:32	completed
 */
class NioSocketSession extends NioSession {

	/** 描述{@link IoService}的元数据 */
	static final TransportMetadata METADATA = new DefaultTransportMetadata("nio", "socket", false, true,
            InetSocketAddress.class, SocketSessionConfig.class, IoBuffer.class, FileRegion.class);
	
	/** 获取socket通道上的socket对象 */
	private Socket getSocket() {
        return ((SocketChannel) channel).socket();
    }
	
	/**
     * 
     * Creates a new instance of NioSocketSession.
     * 
     * 构造方法
     *
     * @param service the associated IoService 
     * @param processor the associated IoProcessor
     * @param ch the used channel
     */
    public NioSocketSession(IoService service, IoProcessor<NioSession> processor, SocketChannel channel) {
        super(processor, service, channel);
        config = new SessionConfigImpl();
        this.config.setAll(service.getSessionConfig());
    }
    
    public TransportMetadata getTransportMetadata() {
        return METADATA;
    }
    
    /**
     * {@inheritDoc}
     */
    public SocketSessionConfig getConfig() {
        return (SocketSessionConfig) config;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    SocketChannel getChannel() {
        return (SocketChannel) channel;
    }
    
    /**
     * {@inheritDoc}
     */
    public InetSocketAddress getRemoteAddress() {
        if (channel == null) {
            return null;
        }
        Socket socket = getSocket();
        if (socket == null) {
            return null;
        }
        return (InetSocketAddress) socket.getRemoteSocketAddress();
    }

    /**
     * {@inheritDoc}
     */
    public InetSocketAddress getLocalAddress() {
        if (channel == null) {
            return null;
        }
        Socket socket = getSocket();
        if (socket == null) {
            return null;
        }
        return (InetSocketAddress) socket.getLocalSocketAddress();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public InetSocketAddress getServiceAddress() {
        return (InetSocketAddress) super.getServiceAddress();
    }
    
    /**
     * 供NioSocketSession使用的SessionConfig。所有内容都从当前session的socket中获取。
     */
    private class SessionConfigImpl extends AbstractSocketSessionConfig {
    	public boolean isKeepAlive() {
            try {
                return getSocket().getKeepAlive();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public void setKeepAlive(boolean on) {
            try {
                getSocket().setKeepAlive(on);
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public boolean isOobInline() {
            try {
                return getSocket().getOOBInline();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public void setOobInline(boolean on) {
            try {
                getSocket().setOOBInline(on);
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public boolean isReuseAddress() {
            try {
                return getSocket().getReuseAddress();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public void setReuseAddress(boolean on) {
            try {
                getSocket().setReuseAddress(on);
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public int getSoLinger() {
            try {
                return getSocket().getSoLinger();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public void setSoLinger(int linger) {
            try {
                if (linger < 0) {
                    getSocket().setSoLinger(false, 0);
                } else {
                    getSocket().setSoLinger(true, linger);
                }
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public boolean isTcpNoDelay() {
            if (!isConnected()) {
                return false;
            }

            try {
                return getSocket().getTcpNoDelay();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public void setTcpNoDelay(boolean on) {
            try {
                getSocket().setTcpNoDelay(on);
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        public int getTrafficClass() {
            try {
                return getSocket().getTrafficClass();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void setTrafficClass(int tc) {
            try {
                getSocket().setTrafficClass(tc);
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public int getSendBufferSize() {
            try {
                return getSocket().getSendBufferSize();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public void setSendBufferSize(int size) {
            try {
                getSocket().setSendBufferSize(size);
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public int getReceiveBufferSize() {
            try {
                return getSocket().getReceiveBufferSize();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public void setReceiveBufferSize(int size) {
            try {
                getSocket().setReceiveBufferSize(size);
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public final boolean isSecured() {
        // If the session does not have a SslFilter, we can return false
        IoFilterChain chain = getFilterChain();

        IoFilter sslFilter = chain.get(SslFilter.class);

        if (sslFilter != null) {
        // Get the SslHandler from the SslFilter
            return ((SslFilter) sslFilter).isSslStarted(this);
        } else {
            return false;
        }
    }
}
