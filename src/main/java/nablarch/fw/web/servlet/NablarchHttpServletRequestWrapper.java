package nablarch.fw.web.servlet;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.util.annotation.Published;
import nablarch.core.util.map.CaseInsensitiveMap;
import nablarch.core.util.map.EnumerableIterator;
import nablarch.fw.results.BadRequest;

/**
 * Nablarchのスコープオブジェクトを使用できるサーブレットリクエストのラッパー。
 * 
 * @author Iwauo Tajima
 */
public class NablarchHttpServletRequestWrapper extends HttpServletRequestWrapper {
    /**
     * コンストラクタ
     * @param delegate ラップ対象のリクエストオブジェクト
     */
    public NablarchHttpServletRequestWrapper(HttpServletRequest delegate) {
        super(delegate);
        origReq = delegate;
        scope = new RequestAttributeMap(delegate);
        createHeaderMap();
    }
    
    /**
     * 委譲対象のHttpServletRequest
     */
    private final HttpServletRequest origReq; 
    
    /**
     * HTTPヘッダーのMapを返す。
     * @return HTTPヘッダーのMap
     */
    public Map<String, String> getHeaderMap() {
        return headerMap;
    }
    
    /**
     * HTTPヘッダーを設定する。
     * @param headerMap HTTPヘッダーのMap
     * @return このオブジェクト自体。
     */
    public NablarchHttpServletRequestWrapper setHeaderMap(Map<String, String> headerMap) {
        this.headerMap = headerMap;
        return this;
    }
    
    /**
     * リクエストパラメータを設定する。
     * @param params リクエストパラメータ
     * @return このオブジェクト自体
     */
    public NablarchHttpServletRequestWrapper setParamMap(Map<String, String[]> params) {
        paramMap = params;
        return this;
    }

    /** リクエストスコープ */
    private Map<String, Object> scope;
    
    /**
     * リクエストスコープへの参照を返す。
     * @return リクエストスコープへの参照
     */
    public Map<String, Object> getScope() {
        return scope;
    }
    
    /**
     * リクエストスコープを設定する。
     * @param scope リクエストスコープ
     * @return このオブジェクト自体
     */
    public NablarchHttpServletRequestWrapper setScope(Map<String, Object> scope) {
        this.scope = scope;
        return this;
    }

    /** {@inheritDoc} */
    @Published(tag = "architect")
    public HttpSessionWrapper getSession() {
        return getSession(true);
    }
    
    /** {@inheritDoc} */
    @Published(tag = "architect")
    public HttpSessionWrapper getSession(boolean create) {
        HttpSession session = origReq.getSession(false);
        if (session == null) {
            if (create) {
                sessionWrapper = new HttpSessionWrapper(origReq.getSession(true));
                return sessionWrapper;
            }
            return null;
        }
        if (sessionWrapper == null) {
            sessionWrapper = new HttpSessionWrapper(session);
        }
        return sessionWrapper;
    }
    /** セッションラッパー */
    private HttpSessionWrapper sessionWrapper = null;
    
    /** {@inheritDoc} */
    @Published(tag = "architect")
    public String getHeader(String name) {
        return headerMap.get(name);
    }
    
    /** {@inheritDoc} */
    public Enumeration<String> getHeaderNames() {
        return new EnumerableIterator<String>(headerMap.keySet().iterator());
    }

    /**
     * HttpServletRequestのHTTPヘッダを格納したMapを作成する。
     */
    @SuppressWarnings("unchecked")
    private void createHeaderMap() {
        headerMap = new CaseInsensitiveMap<String>();
        Enumeration<String> headerNames = origReq.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = origReq.getHeader(headerName);
            headerMap.put(headerName, headerValue);
        }
    }
    /** HttpServletRequestのHTTPヘッダを格納したMap */
    private Map<String, String> headerMap;
    
    /** {@inheritDoc} */
    public String getParameter(String name) {
        String[] values = getParameterMap().get(name);
        return (values == null || values.length == 0) ? super.getParameter(name)
                                                      : values[0];
    }

    /** {@inheritDoc} */
    public Map<String, String[]> getParameterMap() {
        if (paramMap == null) {
            createParamMap();
        }
        return paramMap;
    }
    
    /** {@inheritDoc} */
    public Enumeration<String> getParameterNames() {
        return new EnumerableIterator<String>(getParameterMap().keySet().iterator());
    }

    /** {@inheritDoc} */
    public String[] getParameterValues(String name) {
        return getParameterMap().get(name);
    }

    /**
     * リクエストパラメータを格納したMapを作成する。
     * 特に、ポストパラメータの読み込み時に何らかのエラーが発生した場合は、
     * 内部ステータスコード400(BadRequest)の実行時例外を送出する。
     */
    @SuppressWarnings("unchecked")
    private void createParamMap() {
        paramMap = new HashMap<String, String[]>();
        Map<String, String[]> postParams = null;
        try {
            postParams = origReq.getParameterMap();
            
        } catch (Exception e) {
            // 一部のサーバではポストパラメータの初回読み込み時に実行時例外を
            // 送出するので、ここで捕捉して再送出する。
            if (LOGGER.isWarnEnabled()) {
                LOGGER.logWarn(POST_PARAM_READ_ERROR_MESSAGE, e);
            }
            throw new PostParameterReadError(POST_PARAM_READ_ERROR_MESSAGE, e);
        }
        paramMap.putAll(postParams);
    }
    
    /**
     * ポストパラメータが読み込めなかった場合のメッセージ
     */
    private static final String POST_PARAM_READ_ERROR_MESSAGE
        = "Could not read the post parameters"
        + " due to improperly formatted message body"
        + " or some network problems.";
        
    /** リクエストパラメータを格納したMap */
    private Map<String, String[]> paramMap;
    
    /**
     * {@inheritDoc}
     */
    public ServletRequest getRequest() {
        return origReq;
    }
    
    /** ロガー */
    private static final Logger
    LOGGER = LoggerManager.get(NablarchHttpServletRequestWrapper.class);
    
    // ------------------------------------------------------ Custom errors
    /**
     * ポストパラメータの読み込みに失敗したことを表す実行時例外。
     */
    @Published(tag = "architect")
    public static class PostParameterReadError extends BadRequest {
        /**
         * コンストラクタ。
         * @param message メッセージ
         * @param e 起因例外
         */
        public PostParameterReadError(String message, Throwable e) {
            super(message, e);
        }
    }

    // ---------------------------------------------- A wrapper to HttpSession
    /**
     * サーブレットセッションのラッパー
     */
    public static class HttpSessionWrapper implements HttpSession {
        /**
         * コンストラクター
         * @param delegate 委譲対象のセッションオブジェクト
         */
        public HttpSessionWrapper(HttpSession delegate) {
            this.delegate = delegate;
            scope = new SessionAttributeMap(delegate);
        }
        
        /** 委譲対象のセッションオブジェクト */
        private final HttpSession delegate;
        
        /** セッションスコープ */
        private Map<String, Object> scope;
        
        /**
         * セッションスコープへの参照を返す。
         * @return セッションスコープへの参照
         */
        public Map<String, Object> getScope() {
            return scope;
        }
        
        /**
         * セッションスコープを設定する。
         * @param scope セッションスコープ
         * @return このオブジェクト自体
         */
        public HttpSessionWrapper setScope(Map<String, Object> scope) {
            this.scope = scope;
            return this;
        }

        /** {@inheritDoc} */
        public Object getAttribute(String name) {
            return scope.get(name);
        }

        /** {@inheritDoc} */
        public Enumeration<String> getAttributeNames() {
            return new EnumerableIterator<String>(scope.keySet().iterator());
        }
        
        /** {@inheritDoc} */
        public void setAttribute(String name, Object value) {
            scope.put(name, value);
        }
        
        /** {@inheritDoc} */
        public void removeAttribute(String name) {
            scope.remove(name);
        }
        
        /** {@inheritDoc} */
        public Object getValue(String name) {
            return getAttribute(name);
        }
        
        /** {@inheritDoc} */
        public String[] getValueNames() {
            return scope.keySet().toArray(new String[]{});
        }
        
        /** {@inheritDoc} */
        public void putValue(String name, Object value) {
            setAttribute(name, value);
        }

        /** {@inheritDoc} */
        public void removeValue(String name) {
            removeAttribute(name);
        }
        
        /** {@inheritDoc} */
        public long getCreationTime() {
            return delegate.getCreationTime();
        }

        /** {@inheritDoc} */
        @Published(tag = "architect")
        public String getId() {
            return delegate.getId();
        }

        /** {@inheritDoc} */
        public long getLastAccessedTime() {
            return delegate.getLastAccessedTime();
        }

        /** {@inheritDoc} */
        public int getMaxInactiveInterval() {
            return delegate.getMaxInactiveInterval();
        }

        /** {@inheritDoc} */
        public ServletContext getServletContext() {
            return delegate.getServletContext();
        }

        /** {@inheritDoc} */
        @SuppressWarnings("deprecation")
        public javax.servlet.http.HttpSessionContext getSessionContext() {
            return delegate.getSessionContext();
        }

        /** {@inheritDoc} */
        public void invalidate() {
            delegate.invalidate();
        }

        /** {@inheritDoc} */
        public boolean isNew() {
            return delegate.isNew();
        }
        
        /** {@inheritDoc} */
        public void setMaxInactiveInterval(int interval) {
            delegate.setMaxInactiveInterval(interval);
        }
        
        /**
         * 委譲対象のセッションオブジェクトを取得する。
         * 
         * @return 委譲対象のセッションオブジェクト
         */
        public HttpSession getDelegate() {
            return delegate;
        }
    }
}
