package nablarch.fw.web.servlet;

import java.util.Enumeration;

import jakarta.servlet.http.HttpServletRequest;

import nablarch.core.util.map.AttributeMap;

/**
 * {@link HttpServletRequest}オブジェクトに対してMapインターフェースを与えるラッパー。
 * @author Iwauo Tajima <iwauo@tis.co.jp>
 */
public class RequestAttributeMap extends AttributeMap<String, Object> {
    
    /**
     * HTTPサーブレットリクエストに対するMapインターフェースへのラッパーを作成する。
     * @param request HTTPサーブレットリクエストオブジェクト
     */
    public RequestAttributeMap(HttpServletRequest request) {
        this.request = request;
    }

    /** HTTPサーブレットリクエスト */
    private final HttpServletRequest request;

    /** {@inheritDoc} */
    public Object getAttribute(String name) {
        return request.getAttribute(name);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public Enumeration<String> getAttributeNames() {
        return request.getAttributeNames();
    }

    /** {@inheritDoc} */
    public void setAttribute(String name, Object value) {
        request.setAttribute(name, value);
    }

    /** {@inheritDoc} */
    public void removeAttribute(String name) {
        request.removeAttribute(name);
    }
}
