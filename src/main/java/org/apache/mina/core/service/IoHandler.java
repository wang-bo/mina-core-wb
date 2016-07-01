package org.apache.mina.core.service;

import java.io.IOException;

import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

/**
 * Handles all I/O events fired by MINA.
 * 
 * 处理mina触发的所有I/O事件
 * 
 * @date	2016年6月6日 下午6:17:23	completed
 */
public interface IoHandler {

	/**
     * Invoked from an I/O processor thread when a new connection has been created.
     * Because this method is supposed to be called from the same thread that
     * handles I/O of multiple sessions, please implement this method to perform
     * tasks that consumes minimal amount of time such as socket parameter
     * and user-defined session attribute initialization.
     * 
     * 当一个新连接建立时，由I/O processor线程调用。
     * 这个方法假定被同一个线程调用，而这个线程要负责处理多个session的I/O，所以实现这个方法时，请尽量少的占用时间,
     * 比如获取socket参数、使用自定义的session初始化属性。
     * 
     * @param session The session being created
     * @throws Exception If we get an exception while processing the create event
     */
	public void sessionCreated(IoSession session) throws Exception;
	
	/**
     * Invoked when a connection has been opened.  This method is invoked after
     * {@link #sessionCreated(IoSession)}.  The biggest difference from
     * {@link #sessionCreated(IoSession)} is that it's invoked from other thread
     * than an I/O processor thread once thread model is configured properly.
     * 
     * 当一个连接打开时被调用，这个方法在sessionCreated()方法执行后执行，和sessionCreated()的最大区别是：
     * 当thread mode(即ExecutorFilter)配置后，这个方法是由其它线程调用的，而不是I/O processor线程。
     * 
     * @param session The session being opened
     * @throws Exception If we get an exception while processing the open event
     */
	public void sessionOpened(IoSession session) throws Exception;
	
	/**
     * Invoked when a connection is closed.
     * 
     * 当一个连接关闭时被调用
     * 
     * @param session The session being closed
     * @throws Exception If we get an exception while processing the close event
     */
	public void sessionClosed(IoSession session) throws Exception;
	
	/**
     * Invoked with the related {@link IdleStatus} when a connection becomes idle.
     * This method is not invoked if the transport type is UDP; it's a known bug,
     * and will be fixed in 2.0.
     * 
     * 当一个连接对应的IdleStatus空闲时被调用
     * 
     * @param session The idling session 
     * @param status The session's status
     * @throws Exception If we get an exception while processing the idle event
     */
	public void sessionIdle(IoSession session, IdleStatus status) throws Exception;
	
	/**
     * Invoked when any exception is thrown by user {@link IoHandler}
     * implementation or by MINA.  If <code>cause</code> is an instance of
     * {@link IOException}, MINA will close the connection automatically.
     * 
     * 当mina或IoHandler实例抛出任何异常时被调用。如果是IOException时，mina会自动关闭这个连接。
     * 
     * @param session The session for which we have got an exception
     * @param cause The exception that has been caught
     * @throws Exception If we get an exception while processing the caught exception
     */
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception;
	
	/**
     * Invoked when a message is received.
     * 
     * 当一个连接收到消息时被调用
     * 
     * @param session The session that is receiving a message
     * @param message The received message
     * @throws Exception If we get an exception while processing the received message
     */
	public void messageReceived(IoSession session, Object message) throws Exception;
	
	/**
     * Invoked when a message written by {@link IoSession#write(Object)} is
     * sent out.
     * 
     * 当一个连接发送了消息时被调用
     * 
     * @param session The session that has sent a full message
     * @param message The sent message
     * @throws Exception If we get an exception while processing the sent message 
     */
	public void messageSent(IoSession session, Object message) throws Exception;
	
	/**
     * Handle the closure of an half-duplex TCP channel
     * 
     * 当TCP通道的input被关闭时(即收到远端节点发送的fin后，TCP通道无法读只能写了)被调用
     * 
     * @param session The session which input is being closed
     * @throws Exception If we get an exception while closing the input
     */
	public void inputClosed(IoSession session) throws Exception;
}
