package nablarch.common.web.session.store;

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpSession;
import javax.xml.bind.DatatypeConverter;

import nablarch.common.web.session.SessionEntry;
import nablarch.common.web.session.SessionStore;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * セッションの内容をHttpSessionに格納/読み込みする{@link SessionStore}。
 * <p/>
 * デフォルトのストア名は"httpSession"。
 * 
 * @author kawasima
 * @author tajima
 */
public class HttpSessionStore extends SessionStore {

    /**
     * コンストラクタ。
     */
    public HttpSessionStore() {
        super("httpSession");
    }

    @Override
    public List<SessionEntry> load(String sessionId, ExecutionContext executionContext) {
        if (!executionContext.hasSession()) {
            return Collections.emptyList();
        }
        String entries;
        synchronized (getNativeSession(executionContext)) {
            entries = executionContext.getSessionScopedVar(sessionId);
        }

        if (entries != null) {
            return decode(DatatypeConverter.parseBase64Binary(entries));
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void save(String sessionId, List<SessionEntry> entries, ExecutionContext executionContext) {
        String entriesBinary = DatatypeConverter.printBase64Binary(encode(entries));
        synchronized (getNativeSession(executionContext)) {
            executionContext.setSessionScopedVar(sessionId, entriesBinary);
        }
    }

    @Override
    public void delete(String sessionId, ExecutionContext executionContext) {
        if (!executionContext.hasSession()) {
            return;
        }
        synchronized (getNativeSession(executionContext)) {
            executionContext.getSessionScopeMap().remove(sessionId);
        }
    }

    @Override
    public void invalidate(String sessionId, ExecutionContext executionContext) {
        if (!executionContext.hasSession()) {
            return;
        }
        synchronized (getNativeSession(executionContext)) {
            executionContext.invalidateSession();
        }
    }

    /**
     * HttpSessionを取得する。
     * 
     * HttpSessionが存在しない場合は新しく生成される。
     * 
     * @param ctx 実行コンテキスト
     * @return HttpSession オブジェクト
     */
    private HttpSession getNativeSession(ExecutionContext ctx) {
        return ((ServletExecutionContext) ctx).getNativeHttpSession(true);
    }
}
