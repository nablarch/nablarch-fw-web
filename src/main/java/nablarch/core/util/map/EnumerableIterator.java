package nablarch.core.util.map;

import java.util.Enumeration;
import java.util.Iterator;

/**
 * {@link Iterator}に対する{@link Enumeration}へのラッパー
 *
 * @param <K> 要素の型
 * @author Iwauo Tajima <iwauo@tis.co.jp>
 */
public class EnumerableIterator<K> implements Iterator<K>, Enumeration<K> {
    /**
     * コンストラクタ
     * @param iterator イテレーター
     */
    public EnumerableIterator(Iterator<K> iterator) {
        this.iterator = iterator;
    }
    /** イテレーター */
    private final Iterator<K> iterator;
    
    /** {@inheritDoc} */
    public boolean hasMoreElements() {
        return iterator.hasNext();
    }
    
    /** {@inheritDoc} */
    public K nextElement() {
        return iterator.next();
    }
    
    /** {@inheritDoc} */
    public boolean hasNext() {
        return iterator.hasNext();
    }
    
    /** {@inheritDoc} */
    public K next() {
        return iterator.next();
    }
    
    /** {@inheritDoc} */
    public void remove() {
        iterator.remove();
    }

}
