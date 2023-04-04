package nablarch.common.web.token;

import nablarch.common.web.WebConfig;
import nablarch.common.web.WebConfigFinder;
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
     * @param request リクエスト
     * @return 生成したトークン
     */
    public static String generateToken(NablarchHttpServletRequestWrapper request) {
        WebConfig webConfig = WebConfigFinder.getWebConfig();
        String token = (String) request
                .getAttribute(webConfig.getDoubleSubmissionTokenRequestAttributeName());
        if (token == null) {
            token = getTokenGenerator().generate();
            request.setAttribute(webConfig.getDoubleSubmissionTokenRequestAttributeName(), token);
            getTokenManager().saveToken(token, request);
        }
        return token;
    }

    /**
     * {@link TokenGenerator}をリポジトリから取得する。<br>
     * リポジトリに存在しない場合は{@link RandomTokenGenerator}を使用する。
     * @return {@link TokenGenerator}
     */
    public static TokenGenerator getTokenGenerator() {
        final TokenGenerator generator = (TokenGenerator) SystemRepository.getObject(TOKEN_GENERATOR_NAME);
        return generator != null ? generator : new UUIDV4TokenGenerator();
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
        WebConfig webConfig = WebConfigFinder.getWebConfig();
        final String[] tokenParam = request
                .getParam(webConfig.getDoubleSubmissionTokenParameterName());

        final boolean validToken;
        if (tokenParam != null && tokenParam.length == 1) {
            final String clientToken = tokenParam[0];
            validToken = getTokenManager().isValidToken(clientToken, (ServletExecutionContext) context);
        } else {
            validToken = false;
        }
        return validToken;
    }

    /**
     * {@link TokenManager}を取得する。
     * @return トークンマネージャ
     */
    private static TokenManager getTokenManager() {
        final TokenManager tokenManager = SystemRepository.get("tokenManager");
        if (tokenManager == null) {
            return new HttpSessionTokenManager();
        }
        return tokenManager;
    }
}
