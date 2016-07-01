package org.apache.mina.filter.codec;

import org.apache.mina.core.buffer.IoBuffer;

/**
 * An exception that is thrown when {@link ProtocolDecoder}
 * cannot understand or failed to validate the specified {@link IoBuffer}
 * content.
 * 
 * 当解码器无法处理指定的IoBuffer里的内容时抛出的异常。
 * 
 * @date	2016年6月28日 下午7:20:06	completed
 */
public class ProtocolDecoderException extends ProtocolCodecException {

	private static final long serialVersionUID = 3545799879533408565L;
	
	private String hexdump;

    /**
     * Constructs a new instance.
     */
    public ProtocolDecoderException() {
        // Do nothing
    }

    /**
     * Constructs a new instance with the specified message.
     * 
     * @param message The detail message
     */
    public ProtocolDecoderException(String message) {
        super(message);
    }

    /**
     * Constructs a new instance with the specified cause.
     * 
     * @param cause The Exception's cause
     */
    public ProtocolDecoderException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new instance with the specified message and the specified
     * cause.
     * 
     * @param message The detail message
     * @param cause The Exception's cause
     */
    public ProtocolDecoderException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * @return the message and the hexdump of the unknown part.
     */
    @Override
    public String getMessage() {
        String message = super.getMessage();
        if (message == null) {
            message = "";
        }
        if (hexdump != null) {
            return message + (message.length() > 0 ? " " : "") + "(Hexdump: " + hexdump + ')';
        }
        return message;
    }

    /**
     * @return the hexdump of the unknown message part.
     */
    public String getHexdump() {
        return hexdump;
    }

    /**
     * Sets the hexdump of the unknown message part.
     * 
     * @param hexdump The hexadecimal String representation of the message 
     */
    public void setHexdump(String hexdump) {
        if (this.hexdump != null) {
            throw new IllegalStateException("Hexdump cannot be set more than once.");
        }
        this.hexdump = hexdump;
    }
}
