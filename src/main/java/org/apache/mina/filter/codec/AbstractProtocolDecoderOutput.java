package org.apache.mina.filter.codec;

import java.util.LinkedList;
import java.util.Queue;

/**
 * A {@link ProtocolDecoderOutput} based on queue.
 * 
 * 基于队列的实现ProtocolDecoderOutput接口的基类。
 * 
 * @date	2016年6月28日 下午5:44:33	completed
 */
public abstract class AbstractProtocolDecoderOutput implements ProtocolDecoderOutput {

	/** 解码好的消息队列 */
	private final Queue<Object> messageQueue = new LinkedList<Object>();
	
	public AbstractProtocolDecoderOutput() {
        // Do nothing
    }
	
	public Queue<Object> getMessageQueue() {
        return messageQueue;
    }

	/**
     * ${@inheritDoc}
     */
	public void write(Object message) {
		if (message == null) {
			throw new IllegalArgumentException("message");
		}
		messageQueue.add(message);
	}
}
