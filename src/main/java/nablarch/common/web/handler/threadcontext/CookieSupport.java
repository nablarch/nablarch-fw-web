package nablarch.common.web.handler.threadcontext;

import javax.servlet.http.Cookie;

import nablarch.core.util.StringUtil;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpCookie;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * クッキーに対するアクセスをサポートするクラス。
 *
 * クッキーのhttpOnly属性はアプリケーションで使用しているServlet APIがサポートしている場合のみ設定する。
 *
 * @author Kiyohito Itoh
 */
public class CookieSupport {

    /** 値を保持するクッキーの名前 */
    private String cookieName = ExecutionContext.FW_PREFIX + "language";

    /** 値を保持するクッキーが送信されるURIのパス階層 */
    private String cookiePath = null;

    /** 値を保持するクッキーが送信されるドメイン階層 */
    private String cookieDomain = null;

    /** 値を保持するクッキーの最長存続期間(秒単位) */
    private Integer cookieMaxAge = null;

    /** secure属性の有無（デフォルト無し）*/
    private boolean secure = false;

    /** httpOnly属性の有無（デフォルトあり） */
    private boolean httpOnly = true;

    /**
     * コンストラクタ。
     * @param cookieName 値を保持するクッキーの名前
     */
    public CookieSupport(String cookieName) {
        this.cookieName = cookieName;
    }

    /**
     * 値を保持するクッキーの名前を設定する。
     * @param cookieName 値を保持するクッキーの名前
     */
    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    /**
     * 値を保持するクッキーが送信されるURIのパス階層を設定する。
     * @param cookiePath 値を保持するクッキーが送信されるURIのパス階層
     */
    public void setCookiePath(String cookiePath) {
        this.cookiePath = cookiePath;
    }

    /**
     * 値を保持するクッキーが送信されるドメイン階層を設定する。
     * @param cookieDomain 値を保持するクッキーが送信されるドメイン階層
     */
    public void setCookieDomain(String cookieDomain) {
        this.cookieDomain = cookieDomain;
    }

    /**
     * 値を保持するクッキーの最長存続期間(秒単位)を設定する。
     * @param cookieMaxAge 値を保持するクッキーの最長存続期間(秒単位)
     */
    public void setCookieMaxAge(Integer cookieMaxAge) {
        this.cookieMaxAge = cookieMaxAge;
    }

    /**
     * 保持するクッキーのsecure属性有無を指定する。
     * （デフォルトではsecure属性を設定しない）
     * @param secure secure属性を設定するか否か（真の場合、secure属性を設定する）
     */
    public void setCookieSecure(boolean secure) {
        this.secure = secure;
    }

    /**
     * 保持するクッキーのhttpOnly属性有無を指定する。
     * （デフォルトではサポートしていればhttpOnly属性を設定する）
     * @param httpOnly httpOnly属性を設定するか否か（真の場合、httpOnly属性を設定する）
     */
    public void setCookieHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    /**
     * 指定された値をクッキーに設定するための{@link Cookie}を作成する。
     * <p/>
     * クッキーのパス階層が指定されていない場合はコンテキストパスをパス階層に指定する。
     * @param ctx 実行コンテキスト
     * @param value クッキーに設定する値
     * @return {@link Cookie}
     */
    public Cookie createCookie(ServletExecutionContext ctx, String value) {
        HttpCookie httpCookie = new HttpCookie();
        httpCookie.put(cookieName, value);
        if (cookiePath != null) {
            httpCookie.setPath(cookiePath);
        } else {
            String contextPath = ctx.getServletRequest().getContextPath();
            if (StringUtil.isNullOrEmpty(contextPath)) {
                contextPath = "/";
            }
            httpCookie.setPath(contextPath);
        }
        if (cookieDomain != null) {
            httpCookie.setDomain(cookieDomain);
        }
        if (cookieMaxAge != null) {
            httpCookie.setMaxAge(cookieMaxAge);
        }
        httpCookie.setSecure(secure);
        if (httpCookie.supportsHttpOnly()) {
            httpCookie.setHttpOnly(httpOnly);
        }

        return httpCookie.convertServletCookies().get(0);
    }
    
    /**
     * クッキーの値を取得する。
     * @param ctx 実行コンテキスト
     * @return クッキーの値。送信されてない場合はnull
     */
    public String getCookieValue(ServletExecutionContext ctx) {
        Cookie[] cookies = ctx.getServletRequest().getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
