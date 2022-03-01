package nablarch.common.web.session;

import nablarch.fw.ExecutionContext;

/**
 * 内部利用のためのセッションユーティリティ。
 * @author Tanaka Tomoyuki
 */
public final class InternalSessionUtil {
    /**
     * リクエストスコープにセッションストアIDを保存するときに使用するキー。
     * <p>
     * 単体テストで使用するため、可視性をパッケージプライベートにしている。
     * </p>
     */
    static final String SESSION_STORE_ID_KEY = ExecutionContext.FW_PREFIX + "internal_session_store_id";

    /**
     * 実行コンテキストにセッションストアIDを記録する。
     * @param context 実行コンテキスト
     * @param sessionStoreId セッションストアID
     */
    public static void setId(ExecutionContext context, String sessionStoreId) {
        context.setRequestScopedVar(SESSION_STORE_ID_KEY, sessionStoreId);
    }

    /**
     * {@link #setId(ExecutionContext, String)}で保存したセッションストアIDを取得する。
     * <p>
     * 値が保存されていない場合、このメソッドは{@code null}を返す。
     * </p>
     * <p>
     * セッションストアIDの値は、{@code SessionStoreHandler}の往路で保存される。
     * したがって、それより前では値が取得できない。<br>
     * また、セッションストアIDの保存は１回のリクエストで一度だけ行われる。
     * このため、セッションストアの廃棄やIDの再生成が行われた場合であっても、
     * 同じリクエストの中では常に同じ値が返される。
     * </p>
     * @param context 実行コンテキスト
     * @return セッションストアID
     */
    public static String getId(ExecutionContext context) {
        return context.getRequestScopedVar(SESSION_STORE_ID_KEY);
    }

    // ユーティリティなのでインスタンス化はさせない
    private InternalSessionUtil() {}
}
