package nablarch.fw.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

/**
 * {@link HttpSession} を生成できないようにした {@link HttpServletRequest} のラッパークラス。
 *
 * @author Tomoyuki Tanaka
 */
public class PreventSessionCreationHttpServletRequestWrapper extends HttpServletRequestWrapper {
    /**
     * コンストラクタ。
     * @param request ラップ対象のリクエストオブジェクト
     */
    public PreventSessionCreationHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public HttpSession getSession() {
        return this.getSession(true);
    }

    @Override
    public HttpSession getSession(boolean create) {
        if (create) {
            throw new RuntimeException("Session creation is prevented.");
        }
        return null;
    }
}
