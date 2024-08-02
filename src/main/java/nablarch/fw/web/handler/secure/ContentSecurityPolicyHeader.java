package nablarch.fw.web.handler.secure;

import nablarch.core.util.StringUtil;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.handler.SecureHandler;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * Content-Security-Policyレスポンスヘッダを設定するクラス。
 * <p>
 * {@link #setReportOnly(boolean)} に{@code true}を設定した場合は、
 * Content-Security-Policy-Report-Onlyレスポンスヘッダを出力する。
 * 
 * @author Taichi Uragami
 *
 */
public class ContentSecurityPolicyHeader implements SecureResponseHeader {

    /** ポリシー */
    private String policy;
    /** report-onlyモード */
    private boolean reportOnly;
    /** プレースホルダー文字列 */
    private static final String CSP_NONCE_SOURCE_PLACE_HOLDER = "$cspNonceSource$";

    /**
     * Content-Security-Policyを設定する。
     * 
     * @param policy Content-Security-Policyの値
     */
    public void setPolicy(final String policy) {
        this.policy = policy;
    }

    /**
     * reportOnlyを設定する。
     * 
     * @param reportOnly report-onlyモードで動作させるならtrueを設定する
     */
    public void setReportOnly(final boolean reportOnly) {
        this.reportOnly = reportOnly;
    }

    @Override
    public String getName() {
        if (reportOnly) {
            return "Content-Security-Policy-Report-Only";
        }
        return "Content-Security-Policy";
    }

    @Override
    public String getValue() {
        if (policy == null) {
            throw new IllegalStateException("invalid Content-Security-Policy. policy is null");
        } else if (policy.isEmpty()) {
            throw new IllegalStateException("invalid Content-Security-Policy. policy is empty");
        }

        return policy;
    }

    /**
     * セキュアハンドラでnonceが自動生成されている場合は、プレースホルダーをnonceに置換する。
     * 自動生成されていない場合は、プレースホルダーをそのまま返す。
     *
     * @param context 実行コンテキスト
     * @return レスポンスヘッダの値
     */
    public String getFormattedValue(ServletExecutionContext context) {
        String rawPolicy = getValue();

        String nonce = context.getRequestScopedVar(SecureHandler.CSP_NONCE_KEY);

        if (StringUtil.isNullOrEmpty(nonce)) {
            return rawPolicy;
        }

        return rawPolicy.replace(CSP_NONCE_SOURCE_PLACE_HOLDER, "nonce-" + nonce);
    }

    /**
     * 常に出力する。
     */
    @Override
    public boolean isOutput(final HttpResponse response, final ServletExecutionContext context) {
        return true;
    }
}
