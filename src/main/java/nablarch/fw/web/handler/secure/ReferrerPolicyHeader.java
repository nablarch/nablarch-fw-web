package nablarch.fw.web.handler.secure;

import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.servlet.ServletExecutionContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Referrer-Policyレスポンスヘッダを設定するクラス。
 * <p>
 * デフォルトは"strict-origin-when-cross-origin"。
 *
 * @author Kiyohito Itoh
 */
public class ReferrerPolicyHeader implements SecureResponseHeader {

    /**
     * Referrer-Policyレスポンスヘッダに指定できる値
     */
    private static final Set<String> VALUES = new HashSet<String>(Arrays.asList(
            "no-referrer",
            "no-referrer-when-downgrade",
            "origin",
            "origin-when-cross-origin",
            "same-origin",
            "strict-origin",
            "strict-origin-when-cross-origin",
            "unsafe-url"));

    /**
     * Referrer-Policyレスポンスヘッダに指定する値
     */
    private String value = "strict-origin-when-cross-origin";

    /**
     * Referrer-Policyレスポンスヘッダに指定する値を設定する。
     *
     * @param value Referrer-Policyレスポンスヘッダに指定する値
     */
    public void setValue(String value) {
        if (!VALUES.contains(value)) {
            throw new IllegalArgumentException(String.format(
                    "The value specified in the Referrer-Policy header is invalid. value = [%s]", value));
        }
        this.value = value;
    }

    @Override
    public String getName() {
        return "Referrer-Policy";
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public boolean isOutput(HttpResponse response, ServletExecutionContext context) {
        return true;
    }
}
