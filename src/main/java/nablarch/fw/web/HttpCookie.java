package nablarch.fw.web;

import com.sun.org.apache.xerces.internal.impl.xpath.regex.Match;
import nablarch.core.repository.SystemRepository;
import nablarch.core.util.annotation.Published;
import nablarch.core.util.map.MapWrapper;

import javax.servlet.http.Cookie;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Httpクッキーのパーサー及びその内容を保持するデータオブジェクト。
 *
 * @author Iwauo Tajima <iwauo@tis.co.jp>
 */
@Published(tag = "architect")
public class HttpCookie extends MapWrapper<String, String> {

    /** {@link Cookie}のisHttpOnlyメソッドのメタ情報 */
    private static final Method IS_HTTP_ONLY_METHOD;

    /** {@link Cookie}のsetHttpOnlyメソッドのメタ情報 */
    private static final Method SET_HTTP_ONLY_METHOD;

    /** Set-CookieヘッダからCookie名を取り出すためのパターン */
    private static final Pattern SET_COOKIE_NAME_PATTERN = Pattern.compile(
        "^Set-Cookie: ([^=]+?)=([^;]+?)(;|$)", Pattern.DOTALL);

    /** Set-CookieヘッダからPath属性を取り出すためのパターン */
    private static final Pattern SET_COOKIE_PATH_PATTERN = Pattern.compile(
        " [pP][aA][tT][hH]=([^;]+?)(;|$)", Pattern.DOTALL);

    /** Set-CookieヘッダからDomain属性を取り出すためのパターン */
    private static final Pattern SET_COOKIE_DOMAIN_PATTERN = Pattern.compile(
        " [dD][oO][mM][aA][iI][nN]=([^;]+?)(;|$)", Pattern.DOTALL);

    /** Set-CookieヘッダからMax-Age属性を取り出すためのパターン */
    private static final Pattern SET_COOKIE_MAX_AGE_PATTERN = Pattern.compile(
        " [mM][aA][xX]-[aA][gG][eE]=([^;]+?)(;|$)", Pattern.DOTALL);

    /** Set-CookieヘッダからSecure属性を取り出すためのパターン */
    private static final Pattern SET_COOKIE_SECURE_PATTERN = Pattern.compile(" [sS][eE][cC][uU][rR][eE](;|$)", Pattern.CASE_INSENSITIVE);

    /** Set-CookieヘッダからHttpOnly属性を取り出すためのパターン */
    private static final Pattern SET_COOKIE_HTTP_ONLY_PATTERN = Pattern.compile(" [hH][tT][tT][pP][oO][nN][lL][yY](;|$)", Pattern.CASE_INSENSITIVE);

    static {
        Method isHttpOnlyMethod = null;
        try {
            isHttpOnlyMethod = Cookie.class.getMethod("isHttpOnly");
        } catch (NoSuchMethodException ignore) {
            // NOP
        }
        IS_HTTP_ONLY_METHOD = isHttpOnlyMethod;

        Method setHttpOnlyMethod = null;
        try {
            setHttpOnlyMethod = Cookie.class.getMethod("setHttpOnly", boolean.class);
        } catch (NoSuchMethodException ignore) {
            // NOP
        }
        SET_HTTP_ONLY_METHOD = setHttpOnlyMethod;
    }

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

    static List<HttpCookie> convertHttpCookies(List<Cookie> servletCookies) {
        List<HttpCookie> httpCookies = new ArrayList<HttpCookie>();
        for (Cookie servletCookie : servletCookies) {
            HttpCookie httpCookie = new HttpCookie();

            if (servletCookie.getName() == null) {
                throw new IllegalArgumentException("Cookie name must not be null.");
            }
            httpCookie.put(servletCookie.getName(), servletCookie.getValue());

            if (servletCookie.getMaxAge() != -1) {
                httpCookie.setMaxAge(servletCookie.getMaxAge());
            }

            if (servletCookie.getPath() != null) {
                httpCookie.setPath(servletCookie.getPath());
            }

            if (servletCookie.getDomain() != null) {
                httpCookie.setDomain(servletCookie.getDomain());
            }

            httpCookie.setSecure(servletCookie.getSecure());
            if (IS_HTTP_ONLY_METHOD != null) {
                try {
                    httpCookie.setHttpOnly((Boolean) IS_HTTP_ONLY_METHOD.invoke(servletCookie));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }

            httpCookies.add(httpCookie);
        }
        return httpCookies;
    }

    static HttpCookie parseSetCookie(String cookieString) {
        HttpCookie httpCookie = new HttpCookie();

        Matcher cookieNameValue = SET_COOKIE_NAME_PATTERN.matcher(cookieString);
        if (!cookieNameValue.find()) {
            parseError(cookieString);
        }
        httpCookie.put(cookieNameValue.group(1), cookieNameValue.group(2));

        Matcher cookiePath = SET_COOKIE_PATH_PATTERN.matcher(cookieString);
        if (cookiePath.find()) {
            httpCookie.setPath(cookiePath.group(1));
        }

        Matcher cookieDomain = SET_COOKIE_DOMAIN_PATTERN.matcher(cookieString);
        if (cookieDomain.find()) {
            httpCookie.setDomain(cookieDomain.group(1));
        }

        Matcher cookieMaxAge = SET_COOKIE_MAX_AGE_PATTERN.matcher(cookieString);
        if (cookieMaxAge.find()) {
            httpCookie.setMaxAge(Integer.valueOf(cookieMaxAge.group(1)));
        }

        if (SET_COOKIE_SECURE_PATTERN.matcher(cookieString).find()) {
            httpCookie.setSecure(true);
        }

        if (httpCookie.supportsHttpOnly() && SET_COOKIE_HTTP_ONLY_PATTERN.matcher(cookieString).find()) {
            httpCookie.setHttpOnly(true);
        }
        return httpCookie;
    }

    private static void parseError(Object obj) {
        throw new RuntimeException(
            "Invalid Set-Cookie header: " + obj.toString()
        );
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
        if (IS_HTTP_ONLY_METHOD == null) {
            throw new UnsupportedOperationException("ServletAPI in use is unsupported the HttpOnly attribute. " +
                    "Please update version of ServletAPI to 3.0 or more.");
        }
        return httpOnly;
    }

    /**
     * HttpOnly Cookieか否かを設定する。
     *
     * @param httpOnly trueの場合は、HttpOnly Cookie
     * @return このオブジェクト自体
     */
    public HttpCookie setHttpOnly(final boolean httpOnly) {
        if (SET_HTTP_ONLY_METHOD == null) {
            throw new UnsupportedOperationException("ServletAPI in use is unsupported the HttpOnly attribute. " +
                    "Please update version of ServletAPI to 3.0 or more.");
        }
        this.httpOnly = httpOnly;
        return this;
    }

    /**
     * HttpOnly Cookieがサポートされている環境か否か。
     *
     * @return HttpOnlyがサポートされている場合true
     */
    public boolean supportsHttpOnly() {
        return SET_HTTP_ONLY_METHOD != null;
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

            if (SET_HTTP_ONLY_METHOD != null) {
                try {
                    SET_HTTP_ONLY_METHOD.invoke(servletCookie, isHttpOnly());
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
            servletCookies.add(servletCookie);
        }
        return servletCookies;
    }

    @Override
    public Map<String, String> getDelegateMap() {
        return cookies;
    }
}
