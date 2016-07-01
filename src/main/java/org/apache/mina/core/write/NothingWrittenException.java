package org.apache.mina.core.write;

import java.util.Collection;

/**
 * An exception which is thrown when one or more write requests resulted
 * in no actual write operation.
 * 
 * 当往session写入时，发现没有真正写入东西的时候抛出此异常。
 * 
 * @date	2016年6月16日 下午2:41:16	completed
 */
public class NothingWrittenException extends WriteException {

	private static final long serialVersionUID = -6331979307737691005L;

    public NothingWrittenException(Collection<WriteRequest> requests, String message, Throwable cause) {
        super(requests, message, cause);
    }

    public NothingWrittenException(Collection<WriteRequest> requests, String s) {
        super(requests, s);
    }

    public NothingWrittenException(Collection<WriteRequest> requests, Throwable cause) {
        super(requests, cause);
    }

    public NothingWrittenException(Collection<WriteRequest> requests) {
        super(requests);
    }

    public NothingWrittenException(WriteRequest request, String message, Throwable cause) {
        super(request, message, cause);
    }

    public NothingWrittenException(WriteRequest request, String s) {
        super(request, s);
    }

    public NothingWrittenException(WriteRequest request, Throwable cause) {
        super(request, cause);
    }

    public NothingWrittenException(WriteRequest request) {
        super(request);
    }
}
