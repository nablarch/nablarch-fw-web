package nablarch.fw.web.handler;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * HTTP エンコーディング制御ハンドラ。
 *
 * <pre>
 * #setDefaultEncoding(String) に指定されたエンコーディングを HttpServletRequest/HttpServletResponse に対して設定する。
 * 明示的に設定しない場合は"UTF-8"を使用する。
 * </pre>
 *
 * @author Toru Nagashima
 */
public class HttpCharacterEncodingHandler implements Handler<Object, Object> {

    /** デフォルトエンコーディング */
    private Charset defaultEncoding = Charset.forName("UTF-8");

    /** レスポンスのContent-Typeに「;charset=xx」を付加するかのフラグ(false:付加しない) */
    private boolean appendResponseCharacterEncoding = false;

    /**
     * デフォルトコンストラクタ。
     */
    @Published(tag = "architect")
    public HttpCharacterEncodingHandler() {
    }

    /**
     * デフォルトエンコーディングを設定する。<br />
     *
     * @param name エンコーディング名
     */
    public void setDefaultEncoding(String name) {
        setDefaultEncodingCharset(Charset.forName(name));
    }

    /**
     * デフォルトエンコーディングを設定する。<br />
     *
     * @param encoding エンコーディング
     */
    public void setDefaultEncodingCharset(Charset encoding) {
        this.defaultEncoding = encoding;
    }

    /**
     * デフォルトエンコーディングを取得する。<br />
     *
     * @return エンコーディング
     */
    public Charset getDefaultEncoding() {
        return defaultEncoding;
    }

    /**
     * レスポンスのContent-Typeに「;charset=xx」を付加するかのフラグを設定する。<br />
     * @param appendResponseCharacterEncoding フラグ
     */
    public void setAppendResponseCharacterEncoding(boolean appendResponseCharacterEncoding) {
        this.appendResponseCharacterEncoding = appendResponseCharacterEncoding;
    }

    /**
     * エンコーディングを設定する。<br />
     *
     * 本ハンドラは以下の手順で処理を行う。
     * <ol>
     * <li>{@link #resolveRequestEncoding(HttpServletRequest)} で解決したエンコーディングを HttpServletRequest に設定する。</li>
     * <li>{@link #resolveResponseEncoding(HttpServletRequest)} で解決したエンコーディングを HttpServletResponse に設定する。</li>
     * <li>後続のハンドラに処理を委譲する。</li>
     * </ol>
     *
     * @param data    入力データ
     * @param context 実行コンテキスト
     * @return 処理結果データ
     */
    public Object handle(Object data, ExecutionContext context) {

        ServletExecutionContext servletExecutionContext = (ServletExecutionContext) context;
        HttpServletRequest req = servletExecutionContext.getServletRequest();
        HttpServletResponse res = servletExecutionContext.getServletResponse();
        try {
            // ハンドラーに設定されているエンコーディング情報でリクエストとレスポンスのエンコーディングを設定。
            // レスポンスに関しては JSP でエンコーディング情報を記述し忘れた場合のフェールセーフとして設定。
            req.setCharacterEncoding(resolveRequestEncoding(req).name());
            if (appendResponseCharacterEncoding) {
                res.setCharacterEncoding(resolveResponseEncoding(req).name());
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Encoding is not supported.", e);
        }
        return context.handleNext(data);
    }

    /**
     * リクエストのエンコーディングを解決する。<br />
     * <pre>
     * 本ハンドラでは設定されているデフォルトエンコーディングを返却する。
     * URI 等のリクエスト情報によってエンコーディングを切り替える必要がある場合は、
     * 本メソッドを拡張し、エンコーディングを解決する処理を実装すること。
     *
     * ただし、本メソッド内ではリクエストパラメータの取得を行ってはならない。
     * リクエストパラメータの取得を行ってしまうと、エンコーディングの指定が行えなくなってしまうので、
     * 文字化けの原因となる。
     * </pre>
     *
     * @param request リクエスト
     * @return リクエストのエンコーディング
     */
    @Published(tag = "architect")
    protected Charset resolveRequestEncoding(HttpServletRequest request) {
        return defaultEncoding;
    }

    /**
     * レスポンスのエンコーディングを解決する。<br />
     * <pre>
     * 本ハンドラでは設定されているデフォルトエンコーディングを返却する。
     * URI 等のリクエスト情報によってエンコーディングを切り替える必要がある場合は、
     * 本メソッドを拡張し、エンコーディングを解決する処理を実装すること。
     *
     * ただし、本メソッド内ではリクエストパラメータの取得を行ってはならない。
     * リクエストパラメータの取得を行ってしまうと、エンコーディングの指定が行えなくなってしまうので、
     * 文字化けの原因となる。
     * </pre>
     *
     * @param request リクエスト
     * @return レスポンスのエンコーディング
     */
    @Published(tag = "architect")
    protected Charset resolveResponseEncoding(HttpServletRequest request) {
        return defaultEncoding;
    }

}
