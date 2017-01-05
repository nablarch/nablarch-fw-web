package nablarch.core.util.map;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * スレッド毎のスナップショットを作成することで、並行アクセスに対する一貫読み取りおよび、
 * 楽観ロック方式による書き込みを行うラッパークラス。
 * <pre>
 * リクエストスレッドがセッション上の変数にアクセスした時点で、
 * セッションのスナップショット（ディープコピー）をスレッドローカル変数上に作成する。
 * 以降、当該リクエストスレッドからのアクセスはこのスナップショットに対して行われる。
 * リクエスト終了時にスナップショット上の変更をもとのセッションにマージするが、
 * この際、他のリクエストスレッドによってセッションが既に書き換えられていた場合は
 * {@link ConcurrentModificationException} を送出する。
 * また、セッションの直列化に失敗した場合は {@link CopyOnReadMap.SnapshotCreationError}
 * を送出する。
 * </pre>
 * @param <K> キーの型
 * @param <V> 値の型
 *
 * @author Iwauo Tajima
 */
public class CopyOnReadMap<K, V> extends MapWrapper<K, V> {
    /**
     * デフォルトコンストラクタ
     * <pre>
     * このコンストラクタの処理は以下のソースコードと等価である。
     *     new ConcurrentReadMap(new ConcurrentHashMap<K, V>);
     * </pre>
     */
    public CopyOnReadMap() {
        this(new ConcurrentHashMap<K, V>());
    }

    /**
     * 指定されたMapオブジェクトに対する並行アクセスラッパーを作成する。
     * @param baseMap ラップ対象のMap
     */
    public CopyOnReadMap(Map<K, V> baseMap) {
        this.baseMap = baseMap;
    }
    /** ベースマップ */
    private final Map<K, V> baseMap;

    /**{@inheritDoc}
     *
     * 本クラスの実装では、変更点を一時的に保持するためのスナップショットが返される。
     * スナップショットに対する変更は{@link #save()}で明示的に反映させる必要がある。
     *
     * @throws CopyOnReadMap.SnapshotCreationError
     *          セッション内にserialize不可能なオブジェクトが存在するなどの理由で、
     *          セッションのスナップショットの作成に失敗した場合。
     */
    @Override
    public Map<K, V> getDelegateMap() throws CopyOnReadMap.SnapshotCreationError {
        if (!active) {
            return baseMap;
        }
        return getSnapshot();
    }

    /**
     * 同期処理を停止する。
     * 以降はベースとなるMapに単に処理を委譲するのみ。
     * @return このオブジェクト自体。
     */
    public CopyOnReadMap<K, V> deactivate() {
        active = false;
        return this;
    }

    /** 同期処理が有効であればtrue */
    private boolean active = true;

    /** 同期処理の対象外とするエントリのキー名 */
    @SuppressWarnings("unchecked")
    private K[] ignoredEntries = (K[]) new Object[0];

    /**
     * 同期処理の対象外とするエントリを追加する。
     * @param entryNames  同期処理の対象外とするエントリのキー名
     * @return このオブジェクト自体
     */
    public CopyOnReadMap<K, V> setIgnoredEntries(K... entryNames) {
        ignoredEntries = entryNames;
        return this;
    }

    /**
     * カレントスレッドが保持しているスナップショットを実体のマップに反映する。
     * 処理の結果によらず、現行スレッドのスナップショットは破棄する。
     * @return このオブジェクト自体。
     * @throws ConcurrentModificationException
     *          スナップショットを作成してからこれまでの間に、
     *          カレントスレッド以外によるアクセスがあった場合。
     * @throws CopyOnReadMap.SnapshotCreationError
     *          セッション内にserialize不可能なオブジェクトが存在するなどの理由で、
     *          セッションのスナップショットの作成に失敗した場合。
     */
    public CopyOnReadMap<K, V> save()
    throws ConcurrentModificationException, CopyOnReadMap.SnapshotCreationError {
        try {
            Snapshot<K, V> snapshot = snapshotOnCurrentThread.get();
            if (snapshot == null) {
                return this;
            }
            synchronized (baseMap) {
                snapshot.flush();
            }
        } finally {
            refresh();
        }
        return this;
    }

    /**
     * 現在のスナップショットを破棄する。
     */
    public void refresh() {
        snapshotOnCurrentThread.remove();
    }

    /**
     * ベースマップのスナップショットを作成し、スレッドローカル変数上に格納する。
     * <pre>
     * 既にスナップショットを作成していた場合はその参照を返す。
     * </pre>
     * @return スナップショット
     */
    private Snapshot<K, V> getSnapshot() {
        if (snapshotOnCurrentThread.get() == null) {
            snapshotOnCurrentThread.set(Snapshot.take(baseMap, ignoredEntries));
        }
        return snapshotOnCurrentThread.get();
    }

    /**
     * カレントスレッドが保持しているスナップショット
     */
    private final ThreadLocal<Snapshot<K, V>>
    snapshotOnCurrentThread = new ThreadLocal<Snapshot<K, V>>() {
        @Override
        protected Snapshot<K, V> initialValue() {
            return null;
        }
    };

    /**
     * あるMapのスナップショットとそこからの変更差分を管理するためのMap
     */
    private static final class Snapshot<K, V> extends HashMap<K, V> {
        /** このスナップショットを作成した時点でのMD5ハッシュ値 */
        private transient byte[] digest = new byte[16];
        /** このスナップショットの元となったMap */
        private transient Map<K, V> original;
        /** スナップショットの対象としないエントリーのキー名 */
        @SuppressWarnings("unchecked")
        private transient K[]  ignoredEntries = (K[]) new Object[0];

        /**
         * デフォルトコンストラクタ。
         */
        private Snapshot() {
        }

        /**
         * 与えられたMapのスナップショットを作成する。
         * @param <K> キーの型
         * @param <V> 値の型
         * @param original       スナップショットをとるMap
         * @param ignoredEntries 同期の対象外とするエントリのキー名
         * @return スナップショット
         */
        @SuppressWarnings("unchecked")
        private static <K, V> Snapshot<K, V>
        take(Map<K, V> original, K[]  ignoredEntries) {
            Map<K, V> snapshot = new Snapshot<K, V>();
            snapshot.putAll(original);
            for (K ignored : ignoredEntries) {
                snapshot.remove(ignored);
            }
            byte[] serialized = serialize(snapshot);
            Snapshot<K, V> clone = null;
            try {
                clone = (Snapshot<K, V>) new ObjectInputStream(
                                           new ByteArrayInputStream(serialized)
                                         ).readObject();
                clone.original       = original;
                clone.digest         = new byte[16];
                clone.ignoredEntries = ignoredEntries;
                System.arraycopy(
                    calcDigest(serialized), 0
                  , clone.digest          , 0
                  , 16
                );
                return clone;
            } catch (IOException e) {
                throw new SnapshotCreationError(e);
            } catch (ClassNotFoundException e) {
                throw new SnapshotCreationError(e);
            }
        }

        /**
         * このスナップショットに対する変更が行われたか?
         * @return 変更がある場合はtrue
         */
        private boolean isDirty() {
            byte[] currentDigest = Snapshot.take(this, ignoredEntries).digest;
            return !MessageDigest.isEqual(digest, currentDigest);
        }

        /**
         * 元のMapに対してスナップショット上の内容を反映する。
         * @throws ConcurrentModificationException
         *     指定されたMapが他の並行スレッドによって既に書き換えられていた場合。
         */
        private void flush()
        throws ConcurrentModificationException {
            synchronized (original) {
                if (!isDirty()) {
                    return;
                }
                byte[] currentDigest = Snapshot.take(original, ignoredEntries)
                                               .digest;
                if (!MessageDigest.isEqual(digest, currentDigest)) {
                    throw new ConcurrentModificationException(
                      "this map was touched concurrently."
                    );
                }
                Map<K, V> reserved = new HashMap<K, V>();
                for (K ignored : ignoredEntries) {
                    V value = original.get(ignored);
                    if (value != null) {
                        reserved.put(ignored, value);
                    }
                }
                original.clear();
                original.putAll(reserved);
                original.putAll(this);
            }
        }

        /**
         * 与えられたバイト列のMD5ハッシュ値を算出する。
         * @param data バイト列
         * @return MD5ハッシュ(16byte)
         */
        private static byte[] calcDigest(byte[] data) {
            try {
                return MessageDigest.getInstance("MD5").digest(data);

            } catch (NoSuchAlgorithmException wontHappen) {
                throw new RuntimeException(wontHappen);
            }
        }

        /**
         * 与えられたオブジェクトをシリアライズする。
         * @param obj 処理対象となるオブジェクト
         * @return シリアライズ結果
         * @throws CopyOnReadMap.SnapshotCreationError
         *     与えられたオブジェクトがシリアライズできない。
         */
        private static byte[] serialize(Object obj) throws CopyOnReadMap.SnapshotCreationError {
            try {
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                ObjectOutputStream objWriter = new ObjectOutputStream(byteStream);
                objWriter.writeObject(obj);
                objWriter.close();
                return byteStream.toByteArray();
            } catch (IOException e) {
                throw new SnapshotCreationError(e);
            }
        }
    }

    /**
     * マップ内にserialize不可能なオブジェクトが存在するなどの理由により、
     * スナップショットの作成に失敗したことを表す実行じ例外。
     */
    public static class SnapshotCreationError extends IllegalStateException {
        /**
         * コンストラクタ。
         * @param e 起因例外
         */
        public SnapshotCreationError(Throwable e) {
            super("unable to create snapshot of this map.", e);
        }
    }
}
