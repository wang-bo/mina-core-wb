package org.apache.mina.core.session;

import java.util.Set;

/**
 * Stores the user-defined attributes which is provided per {@link IoSession}.
 * All user-defined attribute accesses in {@link IoSession} are forwarded to
 * the instance of {@link IoSessionAttributeMap}. 
 * 
 * 保存IoSession中的用户自定义属性。
 * 所有IoSession的用户自定义属性都保存在本接口的一个实例中。
 * 
 * @date	2016年6月17日 上午10:05:32	completed
 */
public interface IoSessionAttributeMap {

	/**
     * @return the value of user defined attribute associated with the
     * specified key.  If there's no such attribute, the specified default
     * value is associated with the specified key, and the default value is
     * returned.  This method is same with the following code except that the
     * operation is performed atomically.
     * <pre>
     * if (containsAttribute(key)) {
     *     return getAttribute(key);
     * } else {
     *     setAttribute(key, defaultValue);
     *     return defaultValue;
     * }
     * </pre>
     * 
     * 返回指定session关联的指定key的属性值，如果没有这个key，就先设置key-defaultValue，并返回defaultValue。
     * 
     * @param session the session for which we want to get an attribute
     * @param key The key we are looking for
     * @param defaultValue The default returned value if the attribute is not found
     */
	public Object getAttribute(IoSession session, Object key, Object defaultValue);

    /**
     * Sets a user-defined attribute.
     * 
     * 设置指定session关联的用户自定义属性key-value。
     *
     * @param session the session for which we want to set an attribute
     * @param key the key of the attribute
     * @param value the value of the attribute
     * @return The old value of the attribute.  <tt>null</tt> if it is new.
     */
	public Object setAttribute(IoSession session, Object key, Object value);

    /**
     * Sets a user defined attribute if the attribute with the specified key
     * is not set yet.  This method is same with the following code except
     * that the operation is performed atomically.
     * <pre>
     * if (containsAttribute(key)) {
     *     return getAttribute(key);
     * } else {
     *     return setAttribute(key, value);
     * }
     * </pre>
     * 
     * 设置指定session关联的用户自定义属性key-value，当指定的key不存在时。
     * 
     * @param session the session for which we want to set an attribute
     * @param key The key we are looking for
     * @param value The value to inject
     * @return The previous attribute
     */
	public Object setAttributeIfAbsent(IoSession session, Object key, Object value);

    /**
     * Removes a user-defined attribute with the specified key.
     * 
     * 根据key删除指定session关联的用户自定义属性。
     *
     * @return The old value of the attribute.  <tt>null</tt> if not found.
     * @param session the session for which we want to remove an attribute
     * @param key The key we are looking for
     */
    Object removeAttribute(IoSession session, Object key);

    /**
     * Removes a user defined attribute with the specified key if the current
     * attribute value is equal to the specified value.  This method is same
     * with the following code except that the operation is performed
     * atomically.
     * <pre>
     * if (containsAttribute(key) &amp;&amp; getAttribute(key).equals(value)) {
     *     removeAttribute(key);
     *     return true;
     * } else {
     *     return false;
     * }
     * </pre>
     * 
     * 根据key-value删除指定session关联的用户自定义属性。
     * 
     * @param session the session for which we want to remove a value
     * @param key The key we are looking for
     * @param value The value to remove
     * @return <tt>true</tt> if the value has been removed, <tt>false</tt> if the key was
     * not found of the value not removed
     */
    public boolean removeAttribute(IoSession session, Object key, Object value);

    /**
     * Replaces a user defined attribute with the specified key if the
     * value of the attribute is equals to the specified old value.
     * This method is same with the following code except that the operation
     * is performed atomically.
     * <pre>
     * if (containsAttribute(key) &amp;&amp; getAttribute(key).equals(oldValue)) {
     *     setAttribute(key, newValue);
     *     return true;
     * } else {
     *     return false;
     * }
     * </pre>
     * 
     * 根据key-oldValue替换指定session关联的用户自定义属性为key-newValue。
     * 
     * @param session the session for which we want to replace an attribute
     * @param key The key we are looking for
     * @param oldValue The old value to replace
     * @param newValue The new value to set
     * @return <tt>true</tt> if the value has been replaced, <tt>false</tt> if the key was
     * not found of the value not replaced
     */
    public boolean replaceAttribute(IoSession session, Object key, Object oldValue, Object newValue);

    /**
     * @return <tt>true</tt> if this session contains the attribute with
     * the specified <tt>key</tt>.
     * 
     * 返回指定session关联的用户自定义属性是否包含key。
     * 
     * @param session the session for which wa want to check if an attribute is present
     * @param key The key we are looking for
     */
    boolean containsAttribute(IoSession session, Object key);

    /**
     * @return the set of keys of all user-defined attributes.
     * 
     * 返回指定session关联的用户自定义属性的key集合。
     * 
     * @param session the session for which we want the set of attributes
     */
    public Set<Object> getAttributeKeys(IoSession session);

    /**
     * Disposes any releases associated with the specified session.
     * This method is invoked on disconnection.
     * 
     * 释放指定session关联的所有用户自定义属性，这个方法在session关闭连接时调用。
     *
     * @param session the session to be disposed
     * @throws Exception If the session can't be disposed 
     */
    public void dispose(IoSession session) throws Exception;
}
