package nablarch.common.web.session;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import nablarch.fw.ExecutionContext;

/**
 * セッション全体を表すクラス。
 *
 * @author kawasima
 * @author tajima
 */
public class Session implements Serializable, Iterable<SessionEntry> {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** セッションID */
    private String sessionId;

    /** セッションファクトリ */
    private transient SessionManager factory;

    /** HTTPリクエストに付随する実行コンテキスト */
    private final transient ExecutionContext executionContext;

    /** セッション内の変数テーブルとなるMap */
   private final transient Map<String, SessionEntry> entryMap = new HashMap<String, SessionEntry>();

    /**
     * コンストラクタ。
     * 
     * @param executionContext 設定するコンテキスト
     */
    public Session(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    /**
     * セッション自体を削除する。
     */
    public void invalidate() {
        if (sessionId != null) {
            for (SessionStore store : factory.getAvailableStores()) {
                store.invalidate(sessionId, executionContext);
            }
            sessionId = null;
        }
    }

    /**
     * セッションIDを取得する。
     * セッションIDが存在しない場合は新しく生成する。
     * 
     * @return セッションID
     */
    public String getOrGenerateId() {
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
        }
        return sessionId;
    }

    /**
     * セッションIDを取得する。
     * 
     * @return セッションID
     */
    public String getId() {
        return sessionId;
    }

    /**
     * セッションをロードする。
     * 
     * @param sessionId セッションID
     */
    public void load(String sessionId) {
        this.sessionId = sessionId;
        for (SessionStore store : factory.getAvailableStores()) {
            List<SessionEntry> entries = store.load(sessionId, executionContext);
            for (SessionEntry entry : entries) {
                entryMap.put(entry.getKey(), entry);
            }
        }
    }

    /**
     * セッションを保存する。
     */
    public void save() {

        // 新規Sessionの場合はセッションIDが生成されていないため。
        if (sessionId == null) {
            getOrGenerateId();
        }

        // SessionEntryが存在するStoreに対する保存。
        Map<SessionStore, List<SessionEntry>> entriesByStorage = new HashMap<SessionStore, List<SessionEntry>>();
        for (SessionEntry entry : entryMap.values()) {
            List<SessionEntry> entries = entriesByStorage.get(entry.getStorage());
            if (entries == null) {
                entries = new ArrayList<SessionEntry>();
                entriesByStorage.put(entry.getStorage(), entries);
            }
            entries.add(entry);
        }
        List<SessionStore> stores = new ArrayList<SessionStore>(factory.getAvailableStores());
        for (Map.Entry<SessionStore, List<SessionEntry>> e : entriesByStorage.entrySet()) {
            e.getKey().save(sessionId, e.getValue(), executionContext);
            stores.remove(e.getKey());
        }

        // SessionEntryが存在しなくなったStoreに対する削除。
        for (SessionStore store : stores) {
            store.delete(sessionId, executionContext);
        }
    }

    /**
     * セッションキーからセッション値を取得する。
     * 
     * @param key セッションキー
     * @return セッション値
     */
    public Object get(String key) {
        SessionEntry entry = entryMap.get(key);
        if (entry == null) {
            return null;
        }
        return entry.getValue();
    }

    /**
     * 指定されたセッションキーに対応する値を返す。
     * 
     * @param <T> 総称型
     * @param key セッションキー
     * @param type クラスタイプ
     * @return セッション値
     */
    public <T> T get(String key, Class<T> type) {
        Object value = get(key);
        return type.cast(value);
    }

    /**
     * 指定されたセッションキーに対応する値を登録する。
     * 
     * @param key セッションキー
     * @param value セッション値
     * @param storeName セッション名
     */
    public void put(String key, Object value, String storeName) {
        for (SessionStore storage : factory.getAvailableStores()) {
            if (storage.getName().equals(storeName)) {
                entryMap.put(key, new SessionEntry(key, value, storage));
                return;
            }
        }
    }

    /**
     * 指定されたエントリをセッションに登録する。
     *
     * @param entry エントリ
     */
    public void put(SessionEntry entry) {
        entryMap.put(entry.getKey(), entry);
    }

    /**
     * 指定されたセッションキーに登録されている値を削除する。
     * 
     * @param key セッションキー
     */
    public void delete(String key) {
        entryMap.remove(key);
    }

    /**
     * クリアする。
     */
    public void deleteAll() {
        entryMap.clear();
    }

    /**
     * このセッションを生成したファクトリを設定する。
     *
     * @param factory セッションファクトリ
     */
    protected void setSessionFactory(SessionManager factory) {
        this.factory = factory;
    }

    /**
     * このセッションを生成したファクトリを取得する。
     *
     * @return セッションファクトリ
     */
    public SessionManager getSessionFactory() {
        return factory;
    }

    @Override
    public Iterator<SessionEntry> iterator() {
        return entryMap.values().iterator();
    }
}
