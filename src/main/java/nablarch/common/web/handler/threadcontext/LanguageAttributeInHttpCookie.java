package nablarch.common.web.handler.threadcontext;

import javax.servlet.http.Cookie;

import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * クッキーを使用して言語の保持を行うクラス。
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public class LanguageAttributeInHttpCookie extends LanguageAttributeInHttpSupport {

    /** クッキー処理のサポート */
    private final CookieSupport cookieSupport = new CookieSupport(ExecutionContext.FW_PREFIX + "language");

    /**
     * 言語を保持するクッキーの名前を設定する。
     * @param cookieName 言語を保持するクッキーの名前
     */
    public void setCookieName(String cookieName) {
        cookieSupport.setCookieName(cookieName);
    }

    /**
     * 言語を保持するクッキーが送信されるURIのパス階層を設定する。
     * @param cookiePath 言語を保持するクッキーが送信されるURIのパス階層
     */
    public void setCookiePath(String cookiePath) {
        cookieSupport.setCookiePath(cookiePath);
    }

    /**
     * 言語を保持するクッキーが送信されるドメイン階層を設定する。
     * @param cookieDomain 言語を保持するクッキーが送信されるドメイン階層
     */
    public void setCookieDomain(String cookieDomain) {
        cookieSupport.setCookieDomain(cookieDomain);
    }

    /**
     * 言語を保持するクッキーの最長存続期間(秒単位)を設定する。
     * @param cookieMaxAge 言語を保持するクッキーの最長存続期間(秒単位)
     */
    public void setCookieMaxAge(Integer cookieMaxAge) {
        cookieSupport.setCookieMaxAge(cookieMaxAge);
    }

    /**
     * 言語を保持するクッキーのsecure属性有無を指定する。
     * （デフォルトではsecure属性を設定しない）
     * @param secure secure属性を設定するか否か（真の場合、secure属性を設定する）
     */
    public void setCookieSecure(boolean secure) {
        cookieSupport.setCookieSecure(secure);
    }

    @Override
    protected void keepLanguage(HttpRequest req, ServletExecutionContext ctx, String language) {
        Cookie cookie = cookieSupport.createCookie(ctx, language);
        ctx.getServletResponse().addCookie(cookie);
    }

    @Override
    protected String getKeepingLanguage(HttpRequest req, ServletExecutionContext ctx) {
        return cookieSupport.getCookieValue(ctx);
    }
}
