package org.apache.mina.filter.codec;

/**
 * A special exception that tells the {@link ProtocolDecoder} can keep
 * decoding even after this exception is thrown.
 * <p>
 * Once {@link ProtocolCodecFilter} catches any other type of exception
 * than {@link RecoverableProtocolDecoderException}, it stops calling
 * the {@link ProtocolDecoder#decode(org.apache.mina.core.session.IoSession,
 *        org.apache.mina.core.buffer.IoBuffer, ProtocolDecoderOutput)}
 * immediately and fires an <tt>exceptionCaught</tt> event.
 * <p>
 * On the other hand, if {@link RecoverableProtocolDecoderException} is thrown,
 * it doesn't stop immediately but keeps calling the {@link ProtocolDecoder}
 * as long as the position of the read buffer changes.
 * <p>
 * {@link RecoverableProtocolDecoderException} is useful for a robust
 * {@link ProtocolDecoder} that can continue decoding even after any
 * protocol violation.
 * 
 * 一个特殊的异常，用来告诉解码器在捕获此异常的情况下继续尝试解码。
 * 
 * @date	2016年6月28日 下午7:26:09	completed
 */
public class RecoverableProtocolDecoderException extends ProtocolDecoderException {

	private static final long serialVersionUID = -8172624045024880678L;

    public RecoverableProtocolDecoderException() {
        // Do nothing
    }

    public RecoverableProtocolDecoderException(String message) {
        super(message);
    }

    public RecoverableProtocolDecoderException(Throwable cause) {
        super(cause);
    }

    public RecoverableProtocolDecoderException(String message, Throwable cause) {
        super(message, cause);
    }
}
