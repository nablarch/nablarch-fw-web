package nablarch.common.web.session;

import nablarch.common.web.session.store.HiddenStore.HiddenStoreLoadFailedException;
import nablarch.core.date.SystemTimeUtil;
import nablarch.core.util.StringUtil;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpCookie;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.servlet.ServletExecutionContext;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

/**
 * ストアを選択できるセッション保存機能のためのハンドラ。
 *
 * @author kawasima
 * @author tajima
 */
public class SessionStoreHandler implements Handler<Object, Object> {

    /** セッションマネージャ */
    private SessionManager sessionManager;

    /** クッキーの名称。 */
    private String cookieName = "NABLARCH_SID";

    /** クッキーのpath属性。 */
    private String cookiePath = "/";

    /** クッキーのdomain属性。 */
    private String cookieDomain;

    /** クッキーにsecure属性を指定するかどうか。 */
    private boolean cookieSecure = false;

    /**
     * セッションがinvalidateされたことを示すフラグを
     * リクエストスコープに設定する際に使用するキー
     */
    public static final String IS_INVALIDATED_KEY
            = ExecutionContext.FW_PREFIX + "sessionStore_is_invalidated";

    /** 有効期限 */
    private Expiration expiration = new HttpSessionManagedExpiration();

    /**
     * セッションがinvalidateされたかを取得する。
     *
     * @param ctx コンテキスト
     * @return セッションがinvalidateされた場合はtrue
     */
    private boolean isInvalidated(ExecutionContext ctx) {
        return ctx.getSessionStoredVar(IS_INVALIDATED_KEY) != null;
    }

    /**
     * セッションのロード時に発生した{@link RuntimeException}を処理する。
     * <p/>
     * 次の例外が発生した場合は、クライアントによる改竄の可能性があるため、
     * ステータスコード400のエラーレスポンスを返却する。
     * <pre>
     * ・{@link HiddenStoreLoadFailedException}
     * </pre>
     * それ以外は、指定された例外をそのまま再送出する。
     *
     * @param data    入力データ
     * @param context 実行コンテキスト
     * @param e       {@link RuntimeException}
     * @return レスポンスオブジェクト
     */
    protected HttpResponse handleLoadFailed(Object data, ExecutionContext context, RuntimeException e) {
        if (e instanceof HiddenStoreLoadFailedException) {
            context.setException(e);
            return HttpResponse.Status.BAD_REQUEST.handle((HttpRequest) data, context);
        } else {
            throw e;
        }
    }

    @Override
    public Object handle(final Object data, final ExecutionContext context) {

        Session session = null;
        List<String> entryKeys = new ArrayList<String>();

        long current = SystemTimeUtil.getDate().getTime();

        // セッションIDがあれば、ストアから読み出しセッションストアにセットする。
        final ServletExecutionContext servletContext = (ServletExecutionContext) context;
        final String sessionId = readId(servletContext, current);
        if (sessionId != null) {
            InternalSessionUtil.setId(context, sessionId);

            session = sessionManager.create(context);
            try {
                session.load(sessionId);
            } catch (RuntimeException e) {
                return handleLoadFailed(data, servletContext, e);
            }
            for (SessionEntry sessionEntry : session) {
                entryKeys.add(sessionEntry.getKey());
                context.setSessionStoredVar(sessionEntry.getKey(), sessionEntry);
            }
        }

        final Object res = context.handleNext(data);

        if (sessionId != null && !expiration.isDeterminable(sessionId, context)) {
            // 往路処理でセッションが存在していたが、復路処理までの間にセッションが破棄された場合は、
            // セッションストアの保存処理は行わない。
            return res;
        }

        if (isInvalidated(context)) {
            invalidateHttpSession(servletContext);
            if (session != null) {
                session.invalidate();
            }
        }

        // リクエストスコープに格納されているセッションエントリを取り出し、
        // セッションオブジェクトを更新する。
        for (Object sessionStoreVar : context.getSessionStoreMap().values()) {
            if (sessionStoreVar instanceof SessionEntry) {
                if (session == null) {
                    session = sessionManager.create(context);
                }
                SessionEntry entry = (SessionEntry) sessionStoreVar;
                session.put(entry);
            }
        }

        if (session != null) {

            // セッションエントリが往路で存在して、復路で存在しなくなったものを削除する。
            for (String entryKey : entryKeys) {
                Object entry = context.getSessionStoreMap().get(entryKey);
                if (null == entry || !(entry instanceof SessionEntry)) {
                    session.delete(entryKey);
                }
            }

            session.save();

            // セッションIDをCookieにセットする。
            writeId(session, servletContext);
        }
        return res;
    }

    /**
     * HttpSessionを無効化する。
     *
     * @param context Servlet実行コンテキスト
     */
    private static void invalidateHttpSession(final ServletExecutionContext context) {
        final HttpSession session = context.getNativeHttpSession(false);
        if (session == null) {
            return;
        }
        synchronized (session) {
            try {
                session.invalidate();
            } catch (IllegalStateException ignore) {
                // HttpSessionが既に無効化されている状態で再度無効化した時にIllegalStateExcepitonが発生する。
                // 不要な例外であるためここでキャッチして無視する。
            }
        }
    }

    /**
     * セッションIDを書き出す。
     *
     * @param session セッション
     * @param context 実行コンテキスト
     */
    protected void writeId(final Session session, final ServletExecutionContext context) {
        long maxAge = 0L;
        for (SessionStore store : session.getSessionFactory().getAvailableStores()) {
            if (store.isExtendable()) {
                maxAge = Math.max(maxAge, store.getExpiresMilliSeconds());
            }
        }
        long expirationDateTime = SystemTimeUtil.getDate().getTime() + maxAge;
        expiration.saveExpirationDateTime(session.getOrGenerateId(), expirationDateTime, context);
        setSessionTrackingCookie(session, context.getServletResponse());
    }

    /**
     * セッションIDを保持するためのクッキーをレスポンスのSet-Cookieヘッダに追加する。
     *
     * @param session  セッション
     * @param response サーブレットレスポンス
     */
    protected void setSessionTrackingCookie(Session session, HttpServletResponse response) {
        final HttpCookie cookie = new HttpCookie();
        cookie.put(cookieName, session.getOrGenerateId());
        cookie.setPath(cookiePath);
        if (!StringUtil.isNullOrEmpty(cookieDomain)) {
            cookie.setDomain(cookieDomain);
        }
        if (cookie.supportsHttpOnly()) {
            cookie.setHttpOnly(true);
        }
        cookie.setSecure(cookieSecure);
        for (Cookie c : cookie.convertServletCookies()) {
            response.addCookie(c);
        }
    }

    /**
     * クッキーからセッションIDを読み出す。
     *
     * @param context 実行コンテキスト
     * @return セッションID
     */
    protected String readId(final ServletExecutionContext context, long current) {
        String sessionId = getSessionId(context);
        if (sessionId == null) {
            return null;
        }
        if (expiration.isExpired(sessionId, current, context)) {
            return null;
        }
        return sessionId;
    }

    /**
     * セッションIDを取得する。
     *
     * @param context 実行コンテキスト
     * @return セッションID
     */
    private String getSessionId(final ServletExecutionContext context) {
        final Cookie[] cookies = context.getServletRequest().getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    /**
     * セッションマネージャを設定する。
     *
     * @param sessionManager セッションマネージャ
     */
    public void setSessionManager(final SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    /**
     * セッションIDを保持するクッキーの名称を設定する。
     * デフォルトは "NABLARCH_SID"
     *
     * @param cookieName クッキー名
     */
    public void setCookieName(final String cookieName) {
        this.cookieName = cookieName;
    }

    /**
     * セッションIDを保持するクッキーのpath属性を設定する。
     * デフォルトではホスト配下の全てのパスを送信対象とする。
     *
     * @param cookiePath クッキーパス
     */
    public void setCookiePath(final String cookiePath) {
        this.cookiePath = cookiePath;
    }

    /**
     * セッションIDを保持するクッキーのdomain属性を設定する。
     * デフォルトでは未指定。
     * この場合、当該のクッキーは発行元ホストのみに送信される。
     *
     * @param cookieDomain クッキードメイン
     */
    public void setCookieDomain(final String cookieDomain) {
        this.cookieDomain = cookieDomain;
    }

    /**
     * セッショントラッキングIDを保持するクッキーにsecure属性を指定するかどうかを設定する。
     * trueに設定した場合、当該のクッキーはSSL接続されたリクエストでのみ送信される。
     * デフォルトはfalse。
     *
     * @param cookieSecure セキュア属性を付けたいならばtrue
     */
    public void setCookieSecure(final boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    /**
     * 有効期限を設定する。
     *
     * @param expiration 有効期限
     */
    public void setExpiration(Expiration expiration) {
        this.expiration = expiration;
    }
}
