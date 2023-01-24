package nablarch.fw.web.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpSession;

/**
 * {@link HttpSession} を生成できないようにした {@link HttpServletRequest} のラッパークラス。
 *
 * @author Tomoyuki Tanaka
 */
public class PreventSessionCreationHttpServletRequestWrapper extends HttpServletRequestWrapper {
    /**
     * コンストラクタ。
     * @param request ラップ対象のリクエストオブジェクト
     */
    public PreventSessionCreationHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    /**
     * このクラスは{@link HttpSession}を生成できないようにしているため、このメソッドは常に例外をスローします。
     * @return このメソッドが {@link HttpSession}を返すことはありません
     * @throws RuntimeException このメソッドを実行した場合
     */
    @Override
    public HttpSession getSession() {
        return this.getSession(true);
    }

    /**
     * このクラスは{@link HttpSession}を生成できないようにしているため、引数に{@code true}を渡した場合は例外をスローします。
     * <p/>
     * 引数に{@code false}を渡した場合は、常に{@code null}を返します。
     *
     * @param create {@code true} を渡した場合は例外をスローします
     * @return 引数に{@code false}を渡した場合のみ、{@code null}を返します
     * @throws RuntimeException 引数に{@code true}を渡した場合
     */
    @Override
    public HttpSession getSession(boolean create) {
        if (create) {
            throw new RuntimeException("Session creation is prevented.");
        }
        return null;
    }
}
