package nablarch.fw.web.handler;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import nablarch.core.util.Base64Util;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.handler.secure.CacheControlHeader;
import nablarch.fw.web.handler.secure.ContentSecurityPolicyHeader;
import nablarch.fw.web.handler.secure.ContentTypeOptionsHeader;
import nablarch.fw.web.handler.secure.FrameOptionsHeader;
import nablarch.fw.web.handler.secure.ReferrerPolicyHeader;
import nablarch.fw.web.handler.secure.SecureResponseHeader;
import nablarch.fw.web.handler.secure.XssProtectionHeader;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * Webアプリケーションのセキュリティに関する処理やヘッダ設定を行うハンドラ。
 * <p>
 * レスポンスヘッダに設定する値は、{@link #setSecureResponseHeaderList(List)}に設定された、値から取得する。
 * 特定条件の場合に出力を抑制する場合は、{@link SecureResponseHeader#isOutput(HttpResponse, ServletExecutionContext)}で、{@code false}を返すこと。
 *
 * @author Hisaaki Shioiri
 */
public class SecureHandler implements HttpRequestHandler {

    /**
     * CSP nonce生成の要求を表す値をリクエストスコープに設定する際に使用するキー
     */
    public static String CSP_NONCE_KEY = ExecutionContext.FW_PREFIX + "csp_nonce";

    /**
     * セキュリティ関連のレスポンスヘッダを構築するオブジェクト
     */
    private List<? extends SecureResponseHeader> secureResponseHeaderList =
            Arrays.asList(
                    new FrameOptionsHeader(),
                    new XssProtectionHeader(),
                    new ContentTypeOptionsHeader(),
                    new ReferrerPolicyHeader(),
                    new CacheControlHeader());

    /**
     * nonceを自動生成するかどうか
     */
    private boolean generateCspNonce = false;

    /**
     * nonceの生成用の乱数ジェネレータ
     */
    private final SecureRandom random = new SecureRandom();

    /**
     * nonceを自動生成するかどうか。
     *
     * @return trueの場合は、自動生成する
     */
    public boolean isGenerateCspNonce() {
        return generateCspNonce;
    }

    /**
     * nonceを自動生成するかどうかの設定
     * デフォルト値はfalseである。
     *
     * @param generateCspNonce nonceを自動生成するかどうか
     *
     */
    public void setGenerateCspNonce(boolean generateCspNonce) {
        this.generateCspNonce = generateCspNonce;
    }

    @Override
    public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {
        if (generateCspNonce) {
            // nonceは128ビット以上の暗号的乱数生成器を使用して、リクエストの都度生成することが推奨されている
            // https://www.w3.org/TR/CSP3/#security-nonces
            byte[] bytes = new byte[16];
            random.nextBytes(bytes);
            String nonce = Base64Util.encode(bytes);

            context.setRequestScopedVar(CSP_NONCE_KEY, nonce);
        }

        final HttpResponse response = context.handleNext(request);

        final ServletExecutionContext servletExecutionContext = (ServletExecutionContext) context;
        for (final SecureResponseHeader responseHeader : secureResponseHeaderList) {
            if (responseHeader.isOutput(response, servletExecutionContext)) {
                if (responseHeader instanceof ContentSecurityPolicyHeader) {
                    response.setHeader(
                            responseHeader.getName(),
                            ((ContentSecurityPolicyHeader) responseHeader).getFormattedValue(servletExecutionContext)
                    );
                } else {
                    response.setHeader(responseHeader.getName(), responseHeader.getValue());
                }
            }
        }
        return response;
    }

    /**
     * セキュリティ関連のヘッダ情報を生成する{@link SecureResponseHeader}を設定する。
     *
     * @param secureResponseHeaderList {@code SecureResponseHeader}のリスト
     */
    public void setSecureResponseHeaderList(
            final List<? extends SecureResponseHeader> secureResponseHeaderList) {
        this.secureResponseHeaderList = Collections.unmodifiableList(secureResponseHeaderList);
    }
}
