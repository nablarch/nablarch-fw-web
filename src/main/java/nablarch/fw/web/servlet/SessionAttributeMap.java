package nablarch.fw.web.servlet;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import jakarta.servlet.http.HttpSession;

import nablarch.core.util.map.AttributeMap;
import nablarch.core.util.map.EnumerableIterator;

/**
 * {@link HttpSession}オブジェクトに対してMapインターフェースを与えるラッパー。
 *
 * @author Iwauo Tajima <iwauo@tis.co.jp>
 */
public class SessionAttributeMap extends AttributeMap<String, Object> {

    /**
     * HTTPセッションに対する、Mapインターフェースへのラッパーを作成する。
     * @param servletSession HTTPセッションオブジェクト
     */
    public SessionAttributeMap(HttpSession servletSession) {
        this.session = servletSession;
    }

    /** HTTPセッションオブジェクト */
    private HttpSession session;

    /**
     * このMapの実体であるHTTPSessionオブジェクトを返す。
     * @return このMapの実体であるHTTPSession
     */
    public HttpSession getBackend() {
        return session;
    }

    /**
     * このMapの実体であるHTTPSessionオブジェクトを設定する。
     * @param session このMapの実体であるHTTPSession
     * @return このオブジェクト自体
     */
    public SessionAttributeMap setBackend(HttpSession session) {
        this.session = session;
        return this;
    }


    /** {@inheritDoc} */
    @Override
    public Object getAttribute(String name) {
        try {
            return session.getAttribute(name);
        } catch (IllegalStateException ignored) {
            // 並行するスレッドによりセッションがinvalidate()された場合など、
            // 通常の操作で発生しうるため、この例外は無視する。
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public Enumeration<String> getAttributeNames() {
        try {
            return session.getAttributeNames();
        } catch (IllegalStateException ignored) {
            // 並行するスレッドによりセッションがinvalidate()された場合など、
            // 通常の操作で発生しうるため、この例外は無視する。
            List<String> empty = Collections.emptyList();
            return new EnumerableIterator<String>(empty.iterator());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setAttribute(String name, Object value) {
        try {
            session.setAttribute(name, value);
        } catch (IllegalStateException ignored) {
            // 並行するスレッドによりセッションがinvalidate()された場合など、
            // 通常の操作で発生しうるため、この例外は無視する。
            return;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void removeAttribute(String name) {
        try {
            session.removeAttribute(name);
        } catch (IllegalStateException ignored) {
            // 並行するスレッドによりセッションがinvalidate()された場合など、
            // 通常の操作で発生しうるため、この例外は無視する。
            return;
        }
    }
}
