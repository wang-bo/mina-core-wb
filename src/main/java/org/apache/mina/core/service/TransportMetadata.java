package org.apache.mina.core.service;

import java.net.SocketAddress;
import java.util.Set;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;

/**
 * Provides meta-information that describes an {@link IoService}.
 * 
 * 描述{@link IoService}的元数据
 * 
 * @date	2016年5月27日 下午5:28:25	completed
 */
public interface TransportMetadata {

	/**
	 * @return the name of the service provider (e.g. "nio", "apr" and "rxtx").
	 * 
	 * 返回service provider名称 (如："nio", "apr", "rxtx")
	 */
	public String getProviderName();
	
	/**
	 * @return the name of the service.
	 * 
	 * 返回service名称
	 */
	public String getName();
	
	/**
	 * @return <tt>true</tt> if the session of this transport type is
     * <a href="http://en.wikipedia.org/wiki/Connectionless">connectionless</a>.
     * 
	 * 如果当前session的传输类型是无连接(connectionless)的就返回true
	 */
	public boolean isConnectionless();
	
	/**
	 * @return {@code true} if the messages exchanged by the service can be
     * <a href="http://en.wikipedia.org/wiki/IPv4#Fragmentation_and_reassembly">fragmented
     * or reassembled</a> by its underlying transport.
     * 
	 * 如果底层传输通道支持对service传输的数据进行切片和重组就返回true
	 */
	public boolean hasFragmentation();
	
	/**
	 * @return the address type of the service.
	 * 
	 * 返回service的address的类型
	 */
	public Class<? extends SocketAddress> getAddressType();
	
	/**
	 * @return the set of the allowed message type when you write to an
     * {@link IoSession} that is managed by the service.
     * 
	 * 返回service管理的IoSession允许写入的message类型的集合
	 */
	public Set<Class<? extends Object>> getEnvelopeTypes();
	
	/**
	 * @return the type of the {@link IoSessionConfig} of the service
	 * 
	 * 返回service的IoSessionConfig的类型
	 */
	public Class<? extends IoSessionConfig> getSessionConfigType();
}
