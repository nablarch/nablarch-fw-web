package nablarch.common.web.token;

import static nablarch.fw.ExecutionContext.FW_PREFIX;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import nablarch.core.repository.SystemRepository;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.servlet.NablarchHttpServletRequestWrapper;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * トークンを使用した二重サブミットの防止機能のユーティリティ。
 *
 * なお、トークンは ExecutionContext#getSessionScopeMap で獲得可能なMapではなく、
 * HttpSession に直接格納する。
 * これは、SessionConcurrentAccessHandler と併用した場合、トークンはリクエストスレッド毎の
 * スナップショット上に保持されるので、本来エラーとなるタイミングでも並行実行されてしまう可能性
 * が発生するためである。
 *
 * @author Kiyohito Itoh
 */
public final class TokenUtil {

    /** トークンをhiddenタグに設定する際に使用するキー */
    public static final String KEY_HIDDEN_TOKEN = FW_PREFIX + "token";

    /** トークンをリクエストスコープに設定する際に使用するキー */
    public static final String KEY_REQUEST_TOKEN = FW_PREFIX + "request_token";

    /**
     * トークンをセッションスコープに設定する際に使用するキー
     * <pre>
     * {@link nablarch.fw.web.handler.SessionConcurrentAccessHandler#handle(Object, ExecutionContext)}にて、
     * このキーと同じ値をリテラルで使用しているので、キーの値を変更した場合は合わせて修正すること。
     * リテラルを使用している理由は以下のとおり。
     * ・SessionConcurrentAccessHandlerが行う同期化の対象から除外するため
     * ・モジュールの依存関係から、このフィールドを直接参照できないため
     * </pre>
     */
    public static final String KEY_SESSION_TOKEN = "/" + FW_PREFIX + "session_token";

    /** {@link TokenGenerator}をリポジトリから取得する際に使用する名前 */
    private static final String TOKEN_GENERATOR_NAME = "tokenGenerator";

    /**
     * 隠蔽コンストラクタ。
     */
    private TokenUtil() {
    }

    /**
     * トークンを生成し、セッションスコープに設定する。<br>
     * トークンの生成は、リクエストスコープに対して一度だけ行い、リクエストスコープ内では一度生成したトークンを使いまわす。
     * @param pageContext ページコンテキスト
     * @return 生成したトークン
     */
    public static String generateToken(PageContext pageContext) {
        String token = (String) pageContext.getAttribute(KEY_REQUEST_TOKEN, PageContext.REQUEST_SCOPE);
        if (token == null) {
            token = getTokenGenerator().generate();
            pageContext.setAttribute(KEY_REQUEST_TOKEN, token, PageContext.REQUEST_SCOPE);
            final HttpSession session = getNativeSession(pageContext);
            synchronized (session) {
                session.setAttribute(KEY_SESSION_TOKEN, token);
            }
        }
        return token;
    }

    /**
     * {@link HttpSession}を取得する。
     * {@link HttpSession}が存在しない場合は生成する。
     * @param pageContext ページコンテキスト
     * @return * {@link HttpSession}
     */
    private static HttpSession getNativeSession(PageContext pageContext) {
        NablarchHttpServletRequestWrapper request = (NablarchHttpServletRequestWrapper) pageContext.getRequest();
        return request.getSession(true).getDelegate();
    }

    /**
     * {@link TokenGenerator}をリポジトリから取得する。<br>
     * リポジトリに存在しない場合は{@link RandomTokenGenerator}を使用する。
     * @return {@link TokenGenerator}
     */
    public static TokenGenerator getTokenGenerator() {
        TokenGenerator generator = (TokenGenerator) SystemRepository.getObject(TOKEN_GENERATOR_NAME);
        return generator != null ? generator : new RandomTokenGenerator();
    }

    /**
     * リクエストパラメータのトークンが有効であるかを判定する。
     *
     * (注意)
     * 本メソッドはVM単位での同期となる。
     * ただし、処理内容は軽微かつブロックするような箇所もないので、ボトルネックとなることは無い。
     *
     * @param request リクエスト
     * @param context コンテキスト
     * @return トークンが有効な場合はtrue、有効でない場合はfalse
     * @throws ClassCastException Webコンテナ外で本メソッドが実行された場合。
     */
    public static synchronized boolean isValidToken(HttpRequest request, ExecutionContext context)
    throws ClassCastException {
        String[] tokenParam = request.getParam(KEY_HIDDEN_TOKEN);
        boolean validToken = true;
        HttpSession session = getNativeSession(context);
        if (session == null) {
            return false;
        }
        if (tokenParam != null && tokenParam.length == 1) {
            String clientToken = tokenParam[0];
            String serverToken = (String) session.getAttribute(KEY_SESSION_TOKEN);
            validToken = serverToken != null && serverToken.equals(clientToken);
        } else {
            validToken = false;
        }
        session.removeAttribute(KEY_SESSION_TOKEN);
        return validToken;
    }

    /**
     * HTTPサーブレットセッションオブジェクトを獲得する。
     *
     * HTTPセッションが既にinvalidateされている場合などの理由で取得できない場合は
     * null を返す。
     *
     * @param ctx 実行コンテキスト
     * @return HttpSession オブジェクト
     */
    private static HttpSession getNativeSession(ExecutionContext ctx) {
        return ((ServletExecutionContext) ctx).getNativeHttpSession(false);
    }
}
