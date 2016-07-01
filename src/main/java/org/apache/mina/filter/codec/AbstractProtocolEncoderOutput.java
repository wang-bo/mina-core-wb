package org.apache.mina.filter.codec;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.mina.core.buffer.IoBuffer;

/**
 * A {@link ProtocolEncoderOutput} based on queue.
 * 
 * 基于队列的实现ProtocolEncoderOutput接口的基类。
 * 
 * @date	2016年6月28日 下午5:47:27	completed
 */
public abstract class AbstractProtocolEncoderOutput implements ProtocolEncoderOutput {

	/** 编码好的消息队列 */
	private final Queue<Object> messageQueue = new ConcurrentLinkedQueue<Object>();

	/** 消息队列中是否只有IoBuffer */
    private boolean buffersOnly = true;

    public AbstractProtocolEncoderOutput() {
        // Do nothing
    }

    public Queue<Object> getMessageQueue() {
        return messageQueue;
    }
    
    /**
     * ${@inheritDoc}
     */
    public void write(Object encodedMessage) {
    	if (encodedMessage instanceof IoBuffer) {
			IoBuffer buffer = (IoBuffer) encodedMessage;
			if (buffer.hasRemaining()) {
				messageQueue.offer(buffer);
			} else {
                throw new IllegalArgumentException("buffer is empty. Forgot to call flip()?");
            }
		} else {
            messageQueue.offer(encodedMessage);
            buffersOnly = false;
        }
    }
    
    /**
     * ${@inheritDoc}
     */
    public void mergeAll() {
    	if (!buffersOnly) {
    		throw new IllegalStateException("the encoded message list contains a non-buffer.");
		}
    	
    	final int size = messageQueue.size();
    	if (size < 2) {
    		// no need to merge!
            return;
		}
    	
    	// Get the size of merged BB
    	// 1. 返回所有待merge的buffer的大小。
    	int sum = 0;
    	for (Object object : messageQueue) {
    		sum += ((IoBuffer) object).remaining();
    	}
    	
    	// Allocate a new BB that will contain all fragments
    	// 2. 分配一个所有待merge的buffer的大小的IoBuffer。
        IoBuffer newBuffer = IoBuffer.allocate(sum);
        
        // and merge all.
        // 3. 合并所有碎片。
        for (;;) {
        	IoBuffer buffer = (IoBuffer) messageQueue.poll();
        	if (buffer == null) {
				break;
			}
        	newBuffer.put(buffer);
        }
        
        // Push the new buffer finally.
        // 4. 将合并好的buffer放入队列中中
        newBuffer.flip();
        messageQueue.add(newBuffer);
    }
}
