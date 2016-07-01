package org.apache.mina.filter.codec;

/**
 * An exception that is thrown when {@link ProtocolEncoder}
 * cannot understand or failed to validate the specified message object.
 * 
 * 当编码器无法处理指定的消息对象时抛出的异常。
 * 
 * @date	2016年6月28日 下午7:32:05	completed
 */
public class ProtocolEncoderException extends ProtocolCodecException {

	private static final long serialVersionUID = 8752989973624459604L;

    /**
     * Constructs a new instance.
     */
    public ProtocolEncoderException() {
        // Do nothing
    }

    /**
     * Constructs a new instance with the specified message.
     * 
     * @param message The detail message
     */
    public ProtocolEncoderException(String message) {
        super(message);
    }

    /**
     * Constructs a new instance with the specified cause.
     * 
     * @param cause The Exception's cause
     */
    public ProtocolEncoderException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new instance with the specified message and the specified
     * cause.
     * 
     * @param message The detail message
     * @param cause The Exception's cause
     */
    public ProtocolEncoderException(String message, Throwable cause) {
        super(message, cause);
    }
}
