package org.apache.mina.core.write;

import java.net.SocketAddress;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;

/**
 * Represents write request fired by {@link IoSession#write(Object)}.
 * 
 * 代表IoSession.write(Object)触发的一个写请求。
 * 
 * @date	2016年6月16日 上午10:22:26	completed
 */
public interface WriteRequest {

	/**
     * @return the {@link WriteRequest} which was requested originally,
     * which is not transformed by any {@link IoFilter}.
     * 
     * 返回原始的WriteRequest，没有被任何IoFilter转换过。
     */
	public WriteRequest getOriginalRequest();

    /**
     * @return {@link WriteFuture} that is associated with this write request.
     * 
     * 返回当前WriteRequest关联的WriteFuture。
     */
	public WriteFuture getFuture();

    /**
     * @return a message object to be written.
     * 
     * 返回要写入的message对象。
     */
	public Object getMessage();

    /**
     * Returns the destination of this write request.
     *
     * @return <tt>null</tt> for the default destination
     * 
     * 返回当前WriteRequest要发送的目的地Socket地址。
     */
	public SocketAddress getDestination();

    /**
     * Tells if the current message has been encoded
     * 
     * 返回当前message是否已被编码。
     *
     * @return true if the message has already been encoded
     */
	public boolean isEncoded();
}
