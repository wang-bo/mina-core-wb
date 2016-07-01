package org.apache.mina.core.session;

import java.io.Serializable;

/**
 * Creates a Key from a class name and an attribute name. The resulting Key will
 * be stored in the session Map.<br>
 * For instance, we can create a 'processor' AttributeKey this way :
 * 
 * <pre>
 * private static final AttributeKey PROCESSOR = new AttributeKey(
 * 	SimpleIoProcessorPool.class, &quot;processor&quot;);
 * </pre>
 * 
 * This will create the <b>SimpleIoProcessorPool.processor@7DE45C99</b> key
 * which will be stored in the session map.<br>
 * Such an attributeKey is mainly useful for debug purposes.
 * 
 * 使用Class名称、属性名称来创建一个本类的实例，这个实例会保存在session Map中。
 * 使用本例的主要用途是调试程序。
 * 
 * @date	2016年6月16日 上午10:02:56	completed
 */
public class AttributeKey implements Serializable {

	/** The serial version UID */
    private static final long serialVersionUID = -583377473376683096L;

    /** The attribute's name */
    private final String name;
    
    /**
     * Creates a new instance. It's built from :
     * <ul>
     * <li>the class' name</li>
     * <li>the attribute's name</li>
     * <li>this attribute hashCode</li>
     * </ul>
     * 
     * 构造方法：创建一个实例。
     * 使用类名、属性名、属性值的hashCode来构造。
     * 
     * @param source The class this AttributeKey will be attached to
     * @param name The Attribute name
     */
    public AttributeKey(Class<?> source, String name) {
        this.name = source.getName() + '.' + name + '@' + Integer.toHexString(this.hashCode());
    }
    
    /**
     * The String representation of this object.
     */
    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        int h = 17 * 37 + ((name == null) ? 0 : name.hashCode());
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AttributeKey)) {
            return false;
        }
        AttributeKey other = (AttributeKey) obj;
        return name.equals(other.name);
    }
}
