package nablarch.core.util.map;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 排他ロックを内蔵したMapのラッパークラス。
 *
 * @param <K> キーの型
 * @param <V> 値の型
 * 
 * @author Iwauo Tajima
 */
public class LockableMap<K, V> extends MapWrapper<K, V> implements Lock {
    /**
     * コンストラクタ。
     * @param baseMap ラップ対象のMap
     */
    public LockableMap(Map<K, V> baseMap) {
        this(baseMap, new ReentrantLock());
    }
    
    /**
     * コンストラクタ
     * @param baseMap ラップ対象のMap
     * @param lock    ロックオブジェクト
     */
    public LockableMap(Map<K, V> baseMap, ReentrantLock lock) {
        this.baseMap = baseMap;
        this.lock = lock;
    }
    
    @Override
    public Map<K, V> getDelegateMap() {
        return baseMap;
    }
    /** ラップ対象のMap */
    private final Map<K, V> baseMap;
    
    /** このインスタンスに対する排他ロック */
    private final ReentrantLock lock;
    
    /** {@inheritDoc} */
    public void unlock() {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
    
    /** {@inheritDoc} */
    public void lock() {
        if (!lock.isHeldByCurrentThread() && active) {
            lock.lock();
        }
    }
    
    /**
     * 排他ロックを無効化する。
     * 以降、どのスレッドも新たなロックを獲得することはできない。
     * （開放は可能。）
     */
    public void deactivate() {
        unlock();
        active = false;
    }
    
    /**
     * 排他ロック機能が有効かどうか。
     */
    private boolean active = true;
    
    /** {@inheritDoc} */
    public void lockInterruptibly() throws InterruptedException {
        throw new UnsupportedOperationException(
        "ExclusiveAccessMap#lockInterruptibly has not supported yet."
        );
    }

    /** {@inheritDoc} */
    public Condition newCondition() {
        throw new UnsupportedOperationException(
        "ExclusiveAccessMap#newCondition has not supported yet."
        );
    }

    /** {@inheritDoc} */
    public boolean tryLock() {
        throw new UnsupportedOperationException(
        "ExclusiveAccessMap#tryLock has not supported yet."
        );
    }

    /** {@inheritDoc} */
    public boolean tryLock(long time, TimeUnit unit) {
        throw new UnsupportedOperationException(
        "ExclusiveAccessMap#tryLock has not supported yet."
        );
    }
}
