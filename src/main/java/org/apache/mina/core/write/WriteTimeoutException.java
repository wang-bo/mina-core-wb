package org.apache.mina.core.write;

import java.util.Collection;

/**
 * An exception which is thrown when write buffer is not flushed for
 * 
 * 当写缓冲区没有被flush(导致写超时)时抛出此异常。
 * 
 * @date	2016年6月17日 上午11:39:36	completed
 */
public class WriteTimeoutException extends WriteException {

	private static final long serialVersionUID = 3906931157944579121L;

    public WriteTimeoutException(Collection<WriteRequest> requests, String message, Throwable cause) {
        super(requests, message, cause);
    }

    public WriteTimeoutException(Collection<WriteRequest> requests, String s) {
        super(requests, s);
    }

    public WriteTimeoutException(Collection<WriteRequest> requests, Throwable cause) {
        super(requests, cause);
    }

    public WriteTimeoutException(Collection<WriteRequest> requests) {
        super(requests);
    }

    public WriteTimeoutException(WriteRequest request, String message, Throwable cause) {
        super(request, message, cause);
    }

    public WriteTimeoutException(WriteRequest request, String s) {
        super(request, s);
    }

    public WriteTimeoutException(WriteRequest request, Throwable cause) {
        super(request, cause);
    }

    public WriteTimeoutException(WriteRequest request) {
        super(request);
    }
}
