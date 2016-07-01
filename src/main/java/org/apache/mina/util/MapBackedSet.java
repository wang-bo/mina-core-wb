package org.apache.mina.util;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A {@link Map}-backed {@link Set}.
 * 
 * 一个由Map转成的Set
 * 
 * @param <E>
 * 
 * @date	2016年6月16日 下午2:32:30
 */
public class MapBackedSet<E> extends AbstractSet<E> implements Serializable {

	private static final long serialVersionUID = -8347878570391674042L;

    protected final Map<E, Boolean> map;
    
    public MapBackedSet(Map<E, Boolean> map) {
        this.map = map;
    }

    public MapBackedSet(Map<E, Boolean> map, Collection<E> c) {
        this.map = map;
        addAll(c);
    }
    
    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public boolean add(E o) {
        return map.put(o, Boolean.TRUE) == null;
    }

    @Override
    public boolean remove(Object o) {
        return map.remove(o) != null;
    }

    @Override
    public void clear() {
        map.clear();
    }
}
