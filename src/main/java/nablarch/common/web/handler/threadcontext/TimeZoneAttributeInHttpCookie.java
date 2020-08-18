package nablarch.common.web.handler.threadcontext;

import javax.servlet.http.Cookie;

import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * クッキーを使用してタイムゾーンの保持を行うクラス。
 *
 * Servlet APIがサポートしていれば、クッキーにhttpOnly属性を設定する。
 *
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public class TimeZoneAttributeInHttpCookie extends TimeZoneAttributeInHttpSupport {

    /** クッキー処理のサポート */
    private final CookieSupport cookieSupport
            = new CookieSupport(ExecutionContext.FW_PREFIX + "timeZone");

    /**
     * タイムゾーンを保持するクッキーの名前を設定する。
     * @param cookieName タイムゾーンを保持するクッキーの名前
     */
    public void setCookieName(String cookieName) {
        cookieSupport.setCookieName(cookieName);
    }

    /**
     * タイムゾーンを保持するクッキーが送信されるURIのパス階層を設定する。
     * @param cookiePath タイムゾーンを保持するクッキーが送信されるURIのパス階層
     */
    public void setCookiePath(String cookiePath) {
        cookieSupport.setCookiePath(cookiePath);
    }

    /**
     * タイムゾーンを保持するクッキーが送信されるドメイン階層を設定する。
     * @param cookieDomain タイムゾーンを保持するクッキーが送信されるドメイン階層
     */
    public void setCookieDomain(String cookieDomain) {
        cookieSupport.setCookieDomain(cookieDomain);
    }

    /**
     * タイムゾーンを保持するクッキーの最長存続期間(秒単位)を設定する。
     * @param cookieMaxAge タイムゾーンを保持するクッキーの最長存続期間(秒単位)
     */
    public void setCookieMaxAge(Integer cookieMaxAge) {
        cookieSupport.setCookieMaxAge(cookieMaxAge);
    }

    /**
     * タイムゾーンを保持するクッキーのsecure属性を設定する。
     * （デフォルトではsecure属性を設定しない）
     * @param secure secure属性を設定するか否か（真の場合、secure属性を設定する）
     */
    public void setCookieSecure(boolean secure) {
        cookieSupport.setCookieSecure(secure);
    }

    @Override
    protected void keepTimeZone(HttpRequest req, ServletExecutionContext ctx, String timeZone) {
        Cookie cookie = cookieSupport.createCookie(ctx, timeZone);
        ctx.getServletResponse().addCookie(cookie);
    }

    @Override
    protected String getKeepingTimeZone(HttpRequest req, ServletExecutionContext ctx) {
        return cookieSupport.getCookieValue(ctx);
    }
}
