package org.apache.mina.core.filterchain;

/**
 * A {@link RuntimeException} which is thrown when {@link IoFilter#init()}
 * or {@link IoFilter#onPostAdd(IoFilterChain, String, org.apache.mina.core.filterchain.IoFilter.NextFilter)}
 * failed.
 * 
 * IoFilter生命周期异常：当IoFilter.init()、onPostAdd()方法失败时抛出的异常。
 * 
 * @date	2016年6月27日 下午2:49:23	completed
 */
public class IoFilterLifeCycleException extends RuntimeException {

	private static final long serialVersionUID = -5542098881633506449L;
	
	public IoFilterLifeCycleException() {
    }

    public IoFilterLifeCycleException(String message) {
        super(message);
    }

    public IoFilterLifeCycleException(String message, Throwable cause) {
        super(message, cause);
    }

    public IoFilterLifeCycleException(Throwable cause) {
        super(cause);
    }
}
