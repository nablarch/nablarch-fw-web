package nablarch.common.web.handler.threadcontext;

import nablarch.core.util.annotation.Published;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * HTTPセッションを使用してタイムゾーンの保持を行うクラス。
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public class TimeZoneAttributeInHttpSession extends TimeZoneAttributeInHttpSupport {

    /** タイムゾーンが格納されるセッション上のキー名 */
    private String sessionKey = null;

    /**
     * タイムゾーンが格納されるセッション上のキー名を設定する。
     * @param sessionKey タイムゾーンが格納されるセッション上のキー名
     */
    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    @Override
    protected void keepTimeZone(HttpRequest req, ServletExecutionContext ctx, String timeZone) {
        ctx.setSessionScopedVar(getSessionKey(), timeZone);
    }

    @Override
    protected String getKeepingTimeZone(HttpRequest req, ServletExecutionContext ctx) {
        return ctx.getSessionScopedVar(getSessionKey());
    }

    /**
     * タイムゾーンが格納されるセッション上のキー名を取得する。
     * <p/>
     * {@link #sessionKey}プロパティが設定されていない場合は、
     * nablarch.common.handler.threadcontext.TimeZoneAttribute#getKey()の値を使用する。
     * @return タイムゾーンが格納されるセッション上のキー名
     */
    protected String getSessionKey() {
        return sessionKey != null ? sessionKey : getKey();
    }
}
