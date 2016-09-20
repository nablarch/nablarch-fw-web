package nablarch.core.util.map;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 与えられたMapに対する自動排他制御を実装するラッパークラス。
 * アクセサを使用した時点で排他ロックを自動的に取得する。
 * @param <K> キーの型
 * @param <V> 値の型
 * 
 * @author Iwauo Tajima <iwauo@tis.co.jp>
 */
public class ExclusiveAccessMap<K, V> extends LockableMap<K, V> {
    /**
     * コンストラクタ。
     * @param baseMap ラップ対象のMap
     */
    public ExclusiveAccessMap(Map<K, V> baseMap) {
        super(baseMap);
    }
    
    /**
     * コンストラクタ
     * @param baseMap ラップ対象のMap
     * @param lock    ロックオブジェクト
     */
    public ExclusiveAccessMap(Map<K, V> baseMap, ReentrantLock lock) {
        super(baseMap, lock);
    }

    /** {@inheritDoc} */
    public void clear() {
        lock();
        super.clear();
    }

    /** {@inheritDoc} */
    public boolean containsKey(Object key) {
        lock();
        return super.containsKey(key);
    }

    /** {@inheritDoc} */
    public boolean containsValue(Object value) {
        lock();
        return super.containsValue(value);
    }
    
    /** {@inheritDoc} */
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        lock();
        return super.entrySet();
    }

    /** {@inheritDoc} */
    public V get(Object key) {
        lock();
        return super.get(key);
    }

    /** {@inheritDoc} */
    public boolean isEmpty() {
        lock();
        return super.isEmpty();
    }

    /** {@inheritDoc} */
    public Set<K> keySet() {
        lock();
        return super.keySet();
    }

    /** {@inheritDoc} */
    public V put(K key, V value) {
        lock();
        return super.put(key, value);
    }

    /** {@inheritDoc} */
    public void putAll(Map<? extends K, ? extends V> m) {
        lock();
        super.putAll(m);
    }

    /** {@inheritDoc} */
    public V remove(Object key) {
        lock();
        return super.remove(key);
    }

    /** {@inheritDoc} */
    public int size() {
        lock();
        return super.size();
    }

    /** {@inheritDoc} */
    public Collection<V> values() {
        lock();
        return super.values();
    }
}
