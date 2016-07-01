package org.apache.mina.filter.codec;

/**
 * An exception that is thrown when {@link ProtocolEncoder} or
 * {@link ProtocolDecoder} cannot understand or failed to validate
 * data to process.
 * 
 * 当编码器或解码器无法处理数据时抛出的异常。
 * 
 * @date	2016年6月28日 下午7:18:43	completed
 */
public class ProtocolCodecException extends Exception {

	private static final long serialVersionUID = 5939878548186330695L;
	
	/**
     * Constructs a new instance.
     */
    public ProtocolCodecException() {
        // Do nothing
    }

    /**
     * Constructs a new instance with the specified message.
     * 
     * @param message The detail message
     */
    public ProtocolCodecException(String message) {
        super(message);
    }

    /**
     * Constructs a new instance with the specified cause.
     * 
     * @param cause The Exception's cause
     */
    public ProtocolCodecException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new instance with the specified message and the specified
     * cause.
     * 
     * @param message The detail message
     * @param cause The Exception's cause
     */
    public ProtocolCodecException(String message, Throwable cause) {
        super(message, cause);
    }
}
