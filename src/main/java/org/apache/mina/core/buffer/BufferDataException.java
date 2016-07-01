package org.apache.mina.core.buffer;

/**
 * A {@link RuntimeException} which is thrown when the data the {@link IoBuffer}
 * contains is corrupt.
 * 
 * 当IoBuffer中的数据损坏时抛出此异常。
 * 
 * @date	2016年6月29日 下午5:28:59	completed
 */
public class BufferDataException extends RuntimeException {

	private static final long serialVersionUID = -4138189188602563502L;

    public BufferDataException() {
        super();
    }

    public BufferDataException(String message) {
        super(message);
    }

    public BufferDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public BufferDataException(Throwable cause) {
        super(cause);
    }
}
