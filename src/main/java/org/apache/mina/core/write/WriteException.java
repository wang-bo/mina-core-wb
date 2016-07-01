package org.apache.mina.core.write;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.apache.mina.util.MapBackedSet;

/**
 * An exception which is thrown when one or more write operations failed.
 * 
 * 当写操作失败时抛出的异常。
 * 
 * @date	2016年6月16日 下午2:20:34	completed
 */
public class WriteException extends IOException {

	/** The mandatory serialVersionUUID */
    private static final long serialVersionUID = -4174407422754524197L;

    /** The list of WriteRequest stored in this exception. 当前exception保存的WriteRequest列表 */
    private final List<WriteRequest> requests;
    
    /**
     * Creates a new exception.
     * 
     * 构造方法
     * 
     * @param request The associated {@link WriteRequest}
     */
    public WriteException(WriteRequest request) {
        super();
        this.requests = asRequestList(request);
    }
    
    /**
     * Creates a new exception.
     * 
     * 构造方法
     * 
     * @param request The associated {@link WriteRequest}
     * @param message The detail message
     */
    public WriteException(WriteRequest request, String message) {
        super(message);
        this.requests = asRequestList(request);
    }

    /**
     * Creates a new exception.
     * 
     * 构造方法
     * 
     * @param request The associated {@link WriteRequest}
     * @param message The detail message
     * @param cause The Exception's cause
     */
    public WriteException(WriteRequest request, String message, Throwable cause) {
        super(message);
        initCause(cause);
        this.requests = asRequestList(request);
    }

    /**
     * Creates a new exception.
     * 
     * 构造方法
     * 
     * @param request The associated {@link WriteRequest}
     * @param cause The Exception's cause
     */
    public WriteException(WriteRequest request, Throwable cause) {
        initCause(cause);
        this.requests = asRequestList(request);
    }

    /**
     * Creates a new exception.
     * 
     * 构造方法
     * 
     * @param requests The collection of {@link WriteRequest}s
     */
    public WriteException(Collection<WriteRequest> requests) {
        super();
        this.requests = asRequestList(requests);
    }

    /**
     * Creates a new exception.
     * 
     * 构造方法
     * 
     * @param requests The collection of {@link WriteRequest}s
     * @param message The detail message
     */
    public WriteException(Collection<WriteRequest> requests, String message) {
        super(message);
        this.requests = asRequestList(requests);
    }

    /**
     * Creates a new exception.
     * 
     * 构造方法
     * 
     * @param requests The collection of {@link WriteRequest}s
     * @param message The detail message
     * @param cause The Exception's cause
     */
    public WriteException(Collection<WriteRequest> requests, String message, Throwable cause) {
        super(message);
        initCause(cause);
        this.requests = asRequestList(requests);
    }

    /**
     * Creates a new exception.
     * 
     * 构造方法
     * 
     * @param requests The collection of {@link WriteRequest}s
     * @param cause The Exception's cause
     */
    public WriteException(Collection<WriteRequest> requests, Throwable cause) {
        initCause(cause);
        this.requests = asRequestList(requests);
    }
    
    /**
     * @return the list of the failed {@link WriteRequest}, in the order of occurrence.
     * 
     * 返回失败的WriteRequest列表，按照发生失败操作的先后顺序排列。
     */
    public List<WriteRequest> getRequests() {
        return requests;
    }

    /**
     * @return the firstly failed {@link WriteRequest}. 
     * 
     * 返回第一个失败的WriteRequest对象。
     */
    public WriteRequest getRequest() {
        return requests.get(0);
    }
    
    /**
     * 将WriteRequest集合转成只读去重的list
     * @param requests
     * @return
     */
    private static List<WriteRequest> asRequestList(Collection<WriteRequest> requests) {
    	if (requests == null) {
            throw new IllegalArgumentException("requests");
        }
        if (requests.isEmpty()) {
            throw new IllegalArgumentException("requests is empty.");
        }
        // Create a list of requests removing duplicates.
        // 创建一个去重的list，这里为啥要自定义一个MapBackedSet来实现这个功能？而且这个MapBackedSet就用在这一个地方了...
        Set<WriteRequest> newRequests = new MapBackedSet<WriteRequest>(new LinkedHashMap<WriteRequest, Boolean>());
        for (WriteRequest request : requests) {
			newRequests.add(request.getOriginalRequest());
		}
        return Collections.unmodifiableList(new ArrayList<WriteRequest>(newRequests));
    }
    
    /**
     * 将WriteRequest包装成只读的list
     * @param request
     * @return
     */
    private static List<WriteRequest> asRequestList(WriteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request");
        }
        List<WriteRequest> requests = new ArrayList<WriteRequest>(1);
        requests.add(request.getOriginalRequest());
        return Collections.unmodifiableList(requests);
    }
}
