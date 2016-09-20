package nablarch.common.web.session;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nablarch.common.web.session.encoder.JavaSerializeStateEncoder;
import nablarch.fw.ExecutionContext;

/**
 * セッションストアの管理および、セッションオブジェクトの生成を行うクラス。
 *
 * @author kawasima
 * @author tajima
 */
public class SessionManager {

    /** デフォルトのストア名 */
    private String defaultStoreName;

    /** このマネージャから利用可能なセッションストアの一覧 */
    private Map<String, SessionStore> availableStores;

    /** 設定時の順序を保持したセッションストアの一覧 */
    private List<SessionStore> orderedStores;

    /** 明示的に指定されなかった場合に使用する{@link StateEncoder} */
    private StateEncoder defaultEncoder;

    /**
     * コンストラクタ。
     */
    public SessionManager() {
        defaultEncoder = new JavaSerializeStateEncoder();
    }

    /**
     * セッションを生成する。
     *
     * @param executionContext コンテキスト
     * @return 生成したセッション
     */
    public Session create(ExecutionContext executionContext) {
        Session session = new Session(executionContext);
        session.setSessionFactory(this);
        return session;
    }

    /**
     * デフォルトのストア名を設定する。
     * 
     * @param defaultStoreName デフォルトのストア名
     */
    public void setDefaultStoreName(String defaultStoreName) {
        this.defaultStoreName = defaultStoreName;
    }

    /**
     * セッションストアを設定する。
     *
     * @param sessionStores 設定するセッションストア
     */
    public void setAvailableStores(List<SessionStore> sessionStores) {
        if (sessionStores == null || sessionStores.isEmpty()) {
            throw new IllegalArgumentException("availableStores must not be empty.");
        }
        availableStores = new HashMap<String, SessionStore>();
        for (SessionStore store : sessionStores) {
            availableStores.put(store.getName(), store);
            if (store.getStateEncoder() == null) {
                store.setStateEncoder(defaultEncoder);
            }
        }
        availableStores = Collections.unmodifiableMap(availableStores);
        orderedStores = Collections.unmodifiableList(sessionStores);
    }

    /**
     * セッションストアを取得する。
     *
     * @return セッションストア
     */
    public List<SessionStore> getAvailableStores() {
        if (availableStores == null) {
            throw new IllegalStateException("must be set availableStores property.");
        }
        return orderedStores;
    }

    /**
     * セッションストアを検索する。
     *
     * @param storeName ストア名
     * @return セッションストア
     */
    public SessionStore findSessionStore(String storeName) {
        if (!availableStores.containsKey(storeName)) {
            throw new IllegalStateException("not found session store. storeName=[" + storeName + "]");
        }
        return availableStores.get(storeName);
    }

    /**
     * デフォルトのセッションストアを取得する。
     *
     * @return セッションストア
     */
    public SessionStore getDefaultStore() {
        if (defaultStoreName == null) {
            throw new IllegalStateException("must be set defaultStoreName property.");
        }
        return findSessionStore(defaultStoreName);
    }

    /**
     * デフォルトエンコーダを取得する。
     *
     * @return デフォルトエンコーダ
     */
    public StateEncoder getDefaultEncoder() {
        return defaultEncoder;
    }

    /**
     * デフォルトエンコーダを設定する。
     *
     * @param defaultEncoder デフォルトエンコーダ
     */
    public void setDefaultEncoder(StateEncoder defaultEncoder) {
        this.defaultEncoder = defaultEncoder;
    }

}
