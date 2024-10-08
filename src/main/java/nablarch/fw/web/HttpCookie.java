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
     * {@link Cookie}から{@link HttpCookie}を生成する。
     * @param cookie JavaEE サーブレットAPIのCookieオブジェクト
     * @return {@link HttpCookie}オブジェクト
     */
    public static HttpCookie fromServletCookie(Cookie cookie) {
        HttpCookie httpCookie = new HttpCookie();

        if (cookie.getValue() == null) {
            throw new IllegalArgumentException("Cookie value must not be null.");
        }
        httpCookie.put(cookie.getName(), cookie.getValue());

        if (cookie.getMaxAge() != -1) {
            httpCookie.setMaxAge(cookie.getMaxAge());
        }

        if (cookie.getPath() != null) {
            httpCookie.setPath(cookie.getPath());
        }

        if (cookie.getDomain() != null) {
            httpCookie.setDomain(cookie.getDomain());
        }

        httpCookie.setSecure(cookie.getSecure());

        httpCookie.setHttpOnly(cookie.isHttpOnly());

        return httpCookie;
    }

    /**
     * RFC6265に従い、Set-Cookieヘッダをパースして{@link HttpCookie}を生成する。
     * {@link HttpCookie}はPath、Domain、Max-Age、Secure、HttpOnly属性のみをサポートしているため、それ以外の属性はパース時に無視する。
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc6265#section-4.1.1">RFC6265 4.1.1. Syntax</a>
     * @param header　Set-Cookieヘッダ（Set-Cookie: を含む）
     * @return {@link HttpCookie} インスタンス
     */
    public static HttpCookie fromSetCookieHeader(String header) {

        if (header == null) {
            throw new IllegalArgumentException("Cookie string must not be null.");
        }
        if (!header.startsWith("Set-Cookie: ")) {
            throw new IllegalArgumentException("Cookie string must start with 'Set-Cookie: '.");
        }

        List<java.net.HttpCookie> cookies = java.net.HttpCookie.parse(header);

        // java.net.HttpCookie.parse()は、複数のクッキーを含み得るSet-Cookie2ヘッダにも対応しているため、List型の値を返却している。
        // ただし、ヘッダが"Set-Cookie: "から始まることを上で確認しているので、Listのサイズは必ず1となる。
        java.net.HttpCookie cookie = cookies.get(0);

        HttpCookie httpCookie = new HttpCookie();

        httpCookie.put(cookie.getName(), cookie.getValue());

        httpCookie.setPath(cookie.getPath());

        httpCookie.setDomain(cookie.getDomain());

        // HttpCookieクラスでは、JavaEEのCookieクラスに合わせて、Max-Age属性の値をInteger型で保持しているため、long型の値をint型にキャストしている。
        httpCookie.setMaxAge((int) cookie.getMaxAge());

        httpCookie.setSecure(cookie.getSecure());

        httpCookie.setHttpOnly(cookie.isHttpOnly());

        return httpCookie;
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
    @SuppressWarnings("UnusedReturnValue")
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
    @SuppressWarnings("UnusedReturnValue")
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
    @SuppressWarnings("UnusedReturnValue")
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
