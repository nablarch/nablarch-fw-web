package nablarch.fw.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.Cookie;

import nablarch.core.util.annotation.Published;
import nablarch.core.util.map.MapWrapper;

/**
 * Httpクッキーのパーサー及びその内容を保持するデータオブジェクト。
 *
 * @author Iwauo Tajima <iwauo@tis.co.jp>
 */
@Published(tag = "architect")
public class HttpCookie extends MapWrapper<String, String> {

    /** クッキー名と値のペアを格納したMap */
    private final Map<String, String> cookies;

    /** Max-Age属性 */
    private Integer maxAge;

    /** Path属性 */
    private String path;

    /** Domain属性 */
    private String domain;

    /** Secure属性 */
    private boolean secure;

    /** HttpOnly属性 */
    private boolean httpOnly;

    /**
     * デフォルトコンストラクタ。
     */
    public HttpCookie() {
        cookies = new HashMap<String, String>();
    }

    /**
     * このクッキーの最長の存続期間（秒）を返す。（未設定の場合はnull）
     *
     * @return このクッキーの最長の存続期間（秒）
     */
    public Integer getMaxAge() {
        return maxAge;
    }

    /**
     * このクッキーの最長の存続期間（秒）を指定する。
     *
     * @param maxAge
     *            このクッキーの最長の存続期間（秒）
     * @return このオブジェクト自体
     */
    public HttpCookie setMaxAge(final Integer maxAge) {
        this.maxAge = maxAge;
        return this;
    }

    /**
     * このクッキーが送信されるURIのパス階層を返す。
     *
     * @return このクッキーが送信されるURI階層
     */
    public String getPath() {
        return path;
    }

    /**
     * このクッキーが送信されるURIのパス階層を指定する。
     *
     * @param path
     *            このクッキーが送信されるURI階層
     * @return このオブジェクト自体
     */
    public HttpCookie setPath(String path) {
        this.path = path;
        return this;
    }

    /**
     * このクッキーが送信されるドメイン階層を返す。
     *
     * @return このクッキーが送信されるドメイン階層
     */
    public String getDomain() {
        return domain;
    }

    /**
     * このクッキーが送信されるドメイン階層を指定する。
     *
     * @param domain
     *            このクッキーが送信されるドメイン階層
     * @return このオブジェクト自体
     */
    public HttpCookie setDomain(String domain) {
        this.domain = domain;
        return this;
    }

    /**
     * Secure Cookieか否か。
     *
     * @return trueの場合は、Secure Cookie
     */
    public boolean isSecure() {
        return secure;
    }

    /**
     * Secure Cookieか否かを設定する。
     *
     * @param secure
     *            trueの場合は、Secure Cookie
     */
    public HttpCookie setSecure(boolean secure) {
        this.secure = secure;
        return this;
    }

    /**
     * HttpOnly Cookieか否か。
     *
     * @return trueの場合は、HttpOnly Cookie
     */
    public boolean isHttpOnly() {
        return httpOnly;
    }

    /**
     * HttpOnly Cookieか否かを設定する。
     *
     * @param httpOnly trueの場合は、HttpOnly Cookie
     * @return このオブジェクト自体
     */
    public HttpCookie setHttpOnly(final boolean httpOnly) {
        this.httpOnly = httpOnly;
        return this;
    }

    /**
     * {@link Cookie}オブジェクトのリストに変換して返す。
     * @return {@link Cookie}リスト
     */
    public List<Cookie> convertServletCookies() {
        final List<Cookie> servletCookies = new ArrayList<Cookie>();

        for (Map.Entry<String, String> entry : getDelegateMap().entrySet()) {
            final Cookie servletCookie = new Cookie(entry.getKey(), entry.getValue());

            if (getMaxAge() != null) {
                servletCookie.setMaxAge(getMaxAge());
            }

            if (getPath() != null) {
                servletCookie.setPath(getPath());
            }

            if (getDomain() != null) {
                servletCookie.setDomain(getDomain());
            }

            servletCookie.setSecure(isSecure());
            servletCookie.setHttpOnly(isHttpOnly());

            servletCookies.add(servletCookie);
        }
        return servletCookies;
    }

    @Override
    public Map<String, String> getDelegateMap() {
        return cookies;
    }
}
