package org.apache.mina.core;

import java.io.IOException;

/**
 * A unchecked version of {@link IOException}.
 * <p>
 * Please note that {@link RuntimeIoException} is different from
 * {@link IOException} in that doesn't trigger force session close,
 * while {@link IOException} forces disconnection.
 * 
 * 一个IOException的未检查异常的版本。
 * 注意：与IOException的区别是，IOException会使IoSession关闭，但RuntimeIoException不会。
 * 
 * @date	2016年6月16日 上午9:25:02
 */
public class RuntimeIoException extends RuntimeException {

	private static final long serialVersionUID = 9029092241311939548L;
	
	public RuntimeIoException() {
        super();
    }

    public RuntimeIoException(String message) {
        super(message);
    }

    public RuntimeIoException(String message, Throwable cause) {
        super(message, cause);
    }

    public RuntimeIoException(Throwable cause) {
        super(cause);
    }
}
