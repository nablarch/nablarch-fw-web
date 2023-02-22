package nablarch.fw.web.servlet;

import nablarch.fw.web.HttpCookie;
import nablarch.fw.web.HttpRequest;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * サーブレットのHTTPリクエスト処理に関連するオブジェクト
 * (HttpServletRequest/HttpServletResponse/ServletContext)への参照を
 * 集約するクラス。
 * 
 * @author Iwauo Tajima <iwauo@tis.co.jp>
 */
public class HttpRequestWrapper extends HttpRequest {

    /** サーブレットリクエスト */
    private final NablarchHttpServletRequestWrapper servletRequest;

    /** HttpCookie */
    private final HttpCookie httpCookie;

    /**
     * コンストラクタ
     * @param req サーブレットリクエスト
     */
    public HttpRequestWrapper(final NablarchHttpServletRequestWrapper req) {

        servletRequest  = req;
        
        final String contextPath = req.getContextPath();
        final String requestUri = req.getRequestURI().replaceFirst(Pattern.quote(contextPath), "");
        setRequestUri(requestUri);

        httpCookie = new HttpCookie();
        Cookie[] cookies = servletRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                httpCookie.put(cookie.getName(), cookie.getValue());
            }
        }
    }

    @Override
    public String getMethod() {
        return servletRequest.getMethod().trim();
    }

    @Override
    public String getHttpVersion() {
        return servletRequest.getProtocol().trim();
    }

    @Override
    public Map<String, String[]> getParamMap() {
        return servletRequest.getParameterMap();
    }

    @Override
    public String[] getParam(final String name) {
        return servletRequest.getParameterMap().get(name);
    }

    @Override
    public HttpRequest setParam(final String name, final String... params) {
        servletRequest.getParameterMap().put(name, params);
        return this;
    }

    @Override
    public HttpRequest setParamMap(final Map<String, String[]> params) {
        servletRequest.setParamMap(params);
        return this;
    }

    @Override
    public Map<String, String> getHeaderMap() {
        return servletRequest.getHeaderMap();
    }

    @Override
    public String getHeader(final String headerName) {
        return servletRequest.getHeader(headerName);
    }

    @Override
    public HttpCookie getCookie() {
        return httpCookie;
    }

    /**
     * 入力ストリームを取得する。<br/>
     *
     * @return 入力ストリーム
     */
    public ServletInputStream getInputStream() {
        try {
            return servletRequest.getInputStream();
        } catch (IOException e) {
            throw new nablarch.fw.results.InternalError(e);
        }
    }

    /**
     * Content-Typeを取得する。<br/>
     *
     * @return Content-Type
     */
    public String getContentType() {
        final String type1 = servletRequest.getHeader("Content-Type");
        final String type2 = servletRequest.getContentType();
        if (type1 == null) {
            return type2;
        }
        if (type2 == null) {
            return type1;
        }
        return type1.length() <= type2.length() ? type2 : type1;
    }

    /**
     * Content-Lengthを取得する。<br/>
     *
     * @return Content-Length
     */
    public int getContentLength() {
        return servletRequest.getContentLength();
    }

    /**
     * エンコーディングを取得する。<br/>
     *
     * @return エンコーディング
     */
    public String getCharacterEncoding() {
        return servletRequest.getCharacterEncoding();
    }
}
