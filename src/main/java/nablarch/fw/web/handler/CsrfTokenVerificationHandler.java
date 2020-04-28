package nablarch.fw.web.handler;

import nablarch.common.web.WebConfig;
import nablarch.common.web.WebConfigFinder;
import nablarch.common.web.csrf.CsrfTokenVerificationFailureException;
import nablarch.common.web.session.SessionUtil;
import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.handler.csrf.CsrfTokenGenerator;
import nablarch.fw.web.handler.csrf.HttpMethodVerificationTargetMatcher;
import nablarch.fw.web.handler.csrf.UUIDv4CsrfTokenGenerator;
import nablarch.fw.web.handler.csrf.VerificationTargetMatcher;

/**
 * CSRFトークンの検証を行うハンドラ。
 * 
 * <p>
 * 本ハンドラの処理は次の順番で行われる。
 * </p>
 * 
 * <ol>
 * <li>セッションストアからCSRFトークンを取得する</li>
 * <li>取得できなかった場合はCSRFトークンを生成してセッションストアへ保存する</li>
 * <li>HTTPリクエストが検証対象かどうかを判定する</li>
 * <li>検証対象の場合はHTTPリクエストヘッダ、またはHTTPリクエストパラメータからCSRFトークンを取得して検証を行う</li>
 * <li>検証に失敗した場合は{@link CsrfTokenVerificationFailureException}をスローする</li>
 * <li>検証に成功した場合は次のハンドラへ処理を移す</li>
 * </ol>
 * 
 * @author Uragami Taichi
 *
 */
public class CsrfTokenVerificationHandler implements HttpRequestHandler {

    /**
     * CSRFトークン再生成の要求を表す値をリクエストスコープに設定する際に使用するキー
     */
    public static final String REQUEST_REGENERATE_KEY = ExecutionContext.FW_PREFIX
            + "request_for_csrf_token_to_be_regenerated";

    /**
     * ロガー
     */
    private static final Logger LOGGER = LoggerManager.get(CsrfTokenVerificationHandler.class);

    /**
     * CSRFトークンの生成を行うインターフェース
     */
    private CsrfTokenGenerator csrfTokenGenerator = new UUIDv4CsrfTokenGenerator();

    /**
     * HTTPリクエストがCSRFトークンの検証対象となるか判定を行うインターフェース
     */
    private VerificationTargetMatcher verificationTargetMatcher = new HttpMethodVerificationTargetMatcher();

    @Override
    public HttpResponse handle(HttpRequest request, ExecutionContext context) {
        String sessionAssociatedToken = getSessionAssociatedToken(context);
        if (isTargetOfVerification(request)) {
            String userSentToken = getUserSentToken(request);
            if (!verifyToken(userSentToken, sessionAssociatedToken)) {
                throw new CsrfTokenVerificationFailureException();
            }
        }
        HttpResponse response = context.handleNext(request);

        // REGENERATE_KEYがBoolean.TRUEであれば再生成を行う。
        // Note: ExecutionContext.handleNextよりも後で再生成を行うため、再生成後のCSRFトークンを
        //       HttpResponseへ含めることができない制約があるが実運用上は問題ないと考えている。
        //       JSPへフォワードする場合はリクエストスコープ(※)から再生成後のCSRFトークンを取り出して
        //       書き出すことができる。
        //       ※HttpResponseHandlerによってセッションスコープ変数はリクエストスコープへコピーされる。
        //       また、RESTfulウェブサービスの場合はCSRFトークンを取得するAPIを用意すれば事足りる。
        //       どうしても再生成直後のレスポンスに再生成後のCSRFトークンを含めたい場合は、
        //       ExecutionContext.handleNextから返却されたHttpResponseをカスタマイズするハンドラを
        //       実装して、本ハンドラの直前に差し込めば良い。
        Object requestToRegenerate = context.getRequestScopedVar(REQUEST_REGENERATE_KEY);
        if (requestToRegenerate != null) {
            if (Boolean.TRUE.equals(requestToRegenerate)) {
                generateAndSaveToken(context);
            } else {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.logWarn("A request scoped variable named '" + REQUEST_REGENERATE_KEY
                            + "' has unexpected value. '" + REQUEST_REGENERATE_KEY
                            + "' must be Bolean.TRUE.");
                }
            }
        }
        return response;
    }

    private boolean isTargetOfVerification(HttpRequest request) {
        return verificationTargetMatcher.match(request);
    }

    private boolean verifyToken(String userSentToken, String sessionAssociatedToken) {
        return userSentToken != null && userSentToken.equals(sessionAssociatedToken);
    }

    private String getUserSentToken(HttpRequest request) {
        WebConfig webConfig = WebConfigFinder.getWebConfig();
        String header = request.getHeader(webConfig.getCsrfTokenHeaderName());
        if (header != null) {
            return header;
        }
        String[] params = request.getParam(webConfig.getCsrfTokenParameterName());
        if (params != null && params.length > 0) {
            return params[0];
        }
        return null;
    }

    private String getSessionAssociatedToken(ExecutionContext context) {
        WebConfig webConfig = WebConfigFinder.getWebConfig();
        String name = webConfig.getCsrfTokenSessionStoredVarName();
        String csrfToken = SessionUtil.orNull(context, name);
        if (csrfToken == null) {
            csrfToken = generateAndSaveToken(context);
        }
        return csrfToken;
    }

    private String generateAndSaveToken(ExecutionContext context) {
        WebConfig webConfig = WebConfigFinder.getWebConfig();
        String name = webConfig.getCsrfTokenSessionStoredVarName();
        String csrfToken = csrfTokenGenerator.generateToken();
        String storeName = webConfig.getCsrfTokenSavedStoreName();
        if (storeName != null) {
            SessionUtil.put(context, name, csrfToken, storeName);
        } else {
            SessionUtil.put(context, name, csrfToken);
        }
        return csrfToken;
    }

    /**
     * CSRFトークンの生成を行うインターフェースを設定する。
     * 
     * @param csrfTokenGenerator CSRFトークンの生成を行うインターフェース
     */
    public void setCsrfTokenGenerator(CsrfTokenGenerator csrfTokenGenerator) {
        this.csrfTokenGenerator = csrfTokenGenerator;
    }

    /**
     * HTTPリクエストがCSRFトークンの検証対象となるか判定を行うインターフェースを設定する。
     * 
     * @param verificationTargetMatcher HTTPリクエストがCSRFトークンの検証対象となるか判定を行うインターフェース
     */
    public void setVerificationTargetMatcher(VerificationTargetMatcher verificationTargetMatcher) {
        this.verificationTargetMatcher = verificationTargetMatcher;
    }
}
