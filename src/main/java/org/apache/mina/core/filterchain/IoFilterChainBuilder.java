package org.apache.mina.core.filterchain;

import org.apache.mina.core.session.IoSession;

/**
 * An interface that builds {@link IoFilterChain} in predefined way
 * when {@link IoSession} is created.  You can extract common filter chain
 * modification logic to this interface.  For example, to add a filter
 * to the chain,
 * <pre>
 * public class MyFilterChainBuilder implements IoFilterChainBuilder {
 *     public void buildFilterChain( IoFilterChain chain ) throws Exception {
 *         chain.addLast( "myFilter", new MyFilter() );
 *     }
 * }
 * </pre>
 * 
 * 过滤器链生成器，当创建IoSession，使用预定义的方式生成IoFilterChain。
 * 可以修改本接口实现自定义的过滤器链生成逻辑
 * 
 * @date	2016年6月9日 下午5:29:03	completed
 */
public interface IoFilterChainBuilder {

	/**
     * An implementation which does nothing.
     * 一个空的实现，什么也不做
     */
	IoFilterChainBuilder NOOP = new IoFilterChainBuilder() {
		public void buildFilterChain(IoFilterChain chain) throws Exception {
		}
		@Override
		public String toString() {
			return "NOOP";
		}
	};
	
	/**
     * Modifies the specified <tt>chain</tt>.
     * 
     * 修改指定的过滤器链
     * 
     * @param chain The chain to modify
     * @throws Exception If the chain modification failed
     */
	public void buildFilterChain(IoFilterChain chain) throws Exception;
}
