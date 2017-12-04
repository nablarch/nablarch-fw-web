package nablarch.fw.web.handler.secure;

import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * Content-Security-Policyレスポンスヘッダを設定するクラス。
 * 
 * @author Taichi Uragami
 *
 */
public class ContentSecurityPolicyHeader implements SecureResponseHeader {

    /** ポリシー */
    private String policy;
    /** report-onlyモード */
    private boolean reportOnly;

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
     * 常に出力する。
     */
    @Override
    public boolean isOutput(final HttpResponse response, final ServletExecutionContext context) {
        return true;
    }
}
