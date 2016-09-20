package nablarch.common.web.handler.threadcontext;

import nablarch.core.util.annotation.Published;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * HTTPセッションを使用して言語の保持を行うクラス。
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public class LanguageAttributeInHttpSession extends LanguageAttributeInHttpSupport {
    
    /** 言語が格納されるセッション上のキー名 */
    private String sessionKey = null;
    
    /**
     * 言語が格納されるセッション上のキー名を設定する。
     * @param sessionKey 言語が格納されるセッション上のキー名
     */
    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    @Override
    protected void keepLanguage(HttpRequest req, ServletExecutionContext ctx, String language) {
        ctx.setSessionScopedVar(getSessionKey(), language);
    }

    @Override
    protected String getKeepingLanguage(HttpRequest req, ServletExecutionContext ctx) {
        return ctx.getSessionScopedVar(getSessionKey());
    }
    
    /**
     * 言語が格納されるセッション上のキー名を取得する。
     * <p/>
     * {@link #sessionKey}プロパティが設定されていない場合は、
     * nablarch.common.handler.threadcontext.LanguageAttribute#getKey()の値を使用する。
     * @return 言語が格納されるセッション上のキー名
     */
    protected String getSessionKey() {
        return sessionKey != null ? sessionKey : getKey();
    }
}
