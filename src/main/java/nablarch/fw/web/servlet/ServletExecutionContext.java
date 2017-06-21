package nablarch.fw.web.servlet;

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpMethodBinding;
import nablarch.fw.web.servlet.NablarchHttpServletRequestWrapper.HttpSessionWrapper;

/**
 * サーブレットコンテナ上でのリクエスト処理における実行コンテキスト
 */
public class ServletExecutionContext extends ExecutionContext {
    /**
     * コンストラクタ。
     * @param servletReq サーブレットリクエスト
     * @param servletRes サーブレットレスポンス
     * @param servletCtx サーブレットコンテキスト
     */
    public ServletExecutionContext(HttpServletRequest  servletReq,
                                   HttpServletResponse servletRes,
                                   ServletContext      servletCtx) {
        this.servletReq = new NablarchHttpServletRequestWrapper(servletReq);
        this.servletRes = servletRes;
        this.servletCtx = servletCtx;
        setMethodBinder(new HttpMethodBinding.Binder());

        request = new HttpRequestWrapper(this.servletReq);
    }

    /** サーブレットリクエスト */
    private final NablarchHttpServletRequestWrapper servletReq;
    /** サーブレットレスポンス */
    private final HttpServletResponse servletRes;
    /** サーブレットコンテキスト */
    private final ServletContext servletCtx;
    /** HTTPリクエスト */
    private final HttpRequestWrapper request;

    /**
     * HTTPリクエストオブジェクトを返す。
     * @return HTTPリクエストオブジェクト
     */
    public HttpRequestWrapper getHttpRequest() {
        return this.request;
    }

    /**
     * サーブレットリクエストを返す。
     * @return サーブレットリクエスト
     */
    @Published(tag = "architect")
    public NablarchHttpServletRequestWrapper getServletRequest() {
        return this.servletReq;
    }

    /**
     * サーブレットレスポンスを返す。
     * @return サーブレットレスポンス
     */
    @Published(tag = "architect")
    public HttpServletResponse getServletResponse() {
        return this.servletRes;
    }

    /**
     * サーブレットコンテキストを返す。
     * @return サーブレットコンテキスト
     */
    public ServletContext getServletContext() {
        return this.servletCtx;
    }


    /** {@inheritDoc} */
    @Override
    public ExecutionContext invalidateSession() {
        HttpSession session = servletReq.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isNewSession() {
        return servletReq.getSession(true).isNew();
    }

    /**
     * {@inheritDoc}
     * <p />
     * HttpSessionの生成を抑制したい場合には、本メソッドで判定する。
     */
    @Override
    public boolean hasSession() {
        return servletReq.getSession(false) != null;
    }

    @Override
    public Map<String, Object> getRequestScopeMap() {
        return servletReq.getScope();
    }

    @Override
    public ExecutionContext setRequestScopeMap(Map<String, Object> scope) {
        servletReq.setScope(scope);
        return this;
    }

    /**
     * {@inheritDoc}
     *<p />
     * 本メソッドではHttpSessionが存在しない場合、新規に作成する。<br />
     * HttpSessionを生成したくない場合には{@link #hasSession()}にて判定すること。
     *
     * @see #getSessionScopedVar(String)
     * @see #hasSession()
     */
    @Override
    public Map<String, Object> getSessionScopeMap() {
        return servletReq.getSession().getScope();
    }

    /**
     * {@inheritDoc}
     * <p />
     * 本メソッドではHttpSessionがない場合は、生成せずにnullを返却する。
     *
     * @see #getSessionScopeMap()
     */
    @Override
    public <T> T getSessionScopedVar(String varName) throws ClassCastException {
        if(!hasSession()) {
            return null;
        }
        return super.getSessionScopedVar(varName);
    }

    @Override
    public ExecutionContext setSessionScopeMap(Map<String, Object> scope) {
        servletReq.getSession().setScope(scope);
        return this;
    }

    /**
     * サーブレットコンテナが提供する{@link HttpSession}を取得する。
     * 明示的に{@link HttpSession}を使用したい場合は、本メソッドから取得する。
     *
     * @param create セッションを生成するかどうか
     * @return {@link HttpSession}
     * @see HttpServletRequest#getSession(boolean)
     */
    @Published(tag = "architect")
    public HttpSession getNativeHttpSession(boolean create) {
        HttpSessionWrapper wrapper = servletReq.getSession(create);
        return wrapper == null ? null : wrapper.getDelegate();
    }
}
