package org.apache.mina.core.session;

/**
 * A {@link RuntimeException} that is thrown when the initialization of
 * an {@link IoSession} fails.
 * 
 * 初始化IoSession失败时抛出的异常
 * 
 * @date	2016年6月17日 下午1:36:42	completed
 */
public class IoSessionInitializationException extends RuntimeException {

	private static final long serialVersionUID = -1205810145763696189L;

    public IoSessionInitializationException() {
        super();
    }

    public IoSessionInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public IoSessionInitializationException(String message) {
        super(message);
    }

    public IoSessionInitializationException(Throwable cause) {
        super(cause);
    }
}
