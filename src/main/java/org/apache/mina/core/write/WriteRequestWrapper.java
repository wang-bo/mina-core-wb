package org.apache.mina.core.write;

import java.net.SocketAddress;

import org.apache.mina.core.future.WriteFuture;

/**
 * A wrapper for an existing {@link WriteRequest}.
 * 
 * 包装一个WriteRequest。
 * 
 * @date	2016年6月28日 下午7:00:51	completed
 */
public class WriteRequestWrapper implements WriteRequest {

	/** 被包装的写请求 */
	private final WriteRequest parentRequest;
	
	/**
     * Creates a new instance that wraps the specified request.
     * 
     * 构造方法
     * 
     * @param parentRequest The parent's request
     */
    public WriteRequestWrapper(WriteRequest parentRequest) {
        if (parentRequest == null) {
            throw new IllegalArgumentException("parentRequest");
        }
        this.parentRequest = parentRequest;
    }
    
    /**
     * {@inheritDoc}
     */
    public SocketAddress getDestination() {
        return parentRequest.getDestination();
    }

    /**
     * {@inheritDoc}
     */
    public WriteFuture getFuture() {
        return parentRequest.getFuture();
    }

    /**
     * {@inheritDoc}
     */
    public Object getMessage() {
        return parentRequest.getMessage();
    }

    /**
     * {@inheritDoc}
     */
    public WriteRequest getOriginalRequest() {
        return parentRequest.getOriginalRequest();
    }

    /**
     * @return the wrapped request object.
     */
    public WriteRequest getParentRequest() {
        return parentRequest;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "WR Wrapper" + parentRequest.toString();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEncoded() {
        return false;
    }
}
