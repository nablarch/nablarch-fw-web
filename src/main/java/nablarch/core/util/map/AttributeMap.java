package nablarch.core.util.map;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 最小限のアクセサのみが公開されているkey-value構造体に対して、
 * Mapインターフェースを提供するラッパーを作成するための基底クラス。
 * 
 * @param <K> 属性名の型
 * @param <V> 属性値の型
 * 
 * @see nablarch.fw.web.servlet.SessionAttributeMap
 * @see nablarch.fw.web.servlet.RequestAttributeMap
 * 
 * @author Iwauo Tajima <iwauo@tis.co.jp>
 */
public abstract class AttributeMap<K, V> extends MapWrapper<K, V> {
    
    /**
     * 指定した属性名に対応する属性値を取得する。
     * @param name キー名
     * @return 属性値
     */
    public abstract V getAttribute(K name);

    /**
     * 属性名のイテレータを取得する。
     * @return 属性名のイテレータ
     */
    public abstract Enumeration<K> getAttributeNames();

    /**
     * 属性名に対応する属性値を設定する。
     * @param name  属性名
     * @param value 属性値
     */
    public abstract void setAttribute(K name, V value);

    /**
     * 指定された属性を削除する。
     * @param name 削除する属性名
     */
    public abstract void removeAttribute(K name);

    /* Following methods are implementation of java.util.Map API.*/
    /** {@inheritDoc} */
    public synchronized int size() {
        int result = 0;
        Enumeration<K> attrNames = getAttributeNames();
        while (attrNames.hasMoreElements()) {
            attrNames.nextElement();
            result++;
        }
        return result;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        return getAttribute((K) key);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public synchronized V remove(Object key) {
        Enumeration<K> attrNames = getAttributeNames();
        while (attrNames.hasMoreElements()) {
            K attrName = attrNames.nextElement();
            boolean found = (key == null) 
                          ? attrName == null
                          : key.equals(attrName);
            if (found) {
                V oldVal = getAttribute((K) key);
                removeAttribute((K) key);
                return oldVal;
            }
        }
        return null;
    }
    
    /** {@inheritDoc} */
    public synchronized void putAll(Map<? extends K, ? extends V> map) {
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            setAttribute(entry.getKey(), entry.getValue());
        }
    }

    /** {@inheritDoc} */
    public synchronized V put(K key, V value) {
        V oldVal = getAttribute(key);
        setAttribute(key, value);
        return oldVal;
    }

    /** {@inheritDoc} */
    public synchronized void clear() {
        Enumeration<K> attrNames = getAttributeNames();
        while (attrNames.hasMoreElements()) {
            K attrName = attrNames.nextElement();
            removeAttribute(attrName);
        }
    }

    /** {@inheritDoc} */
    public synchronized boolean containsKey(Object key) {
        Enumeration<K> attrNames = getAttributeNames();
        while (attrNames.hasMoreElements()) {
            K attrName = attrNames.nextElement();
            boolean found = (key == null) 
                          ? attrName == null
                          : key.equals(attrName);
            if (found) {
                return true;
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    public synchronized boolean containsValue(Object value) {
        Enumeration<K> attrNames = getAttributeNames();
        while (attrNames.hasMoreElements()) {
            K attrName  = attrNames.nextElement();
            V attrValue = getAttribute(attrName);
            boolean found = (value == null)
                          ? attrValue == null
                          : value.equals(attrValue);
            if (found) {
                return true;
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    public synchronized Map<K, V> getDelegateMap() {
        Map<K, V> result = new HashMap<K, V>();
        Enumeration<K> attrNames = getAttributeNames();
        while (attrNames.hasMoreElements()) {
            K attrName = attrNames.nextElement();
            result.put(attrName, getAttribute(attrName));
        }
        return Collections.unmodifiableMap(result);
    }
}
