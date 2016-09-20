package nablarch.fw.web.handler.secure;

import nablarch.core.util.StringUtil;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * X-XSS-Protectionレスポンスヘッダを設定するクラス。
 * <p>
 * デフォルトでは、X-XSS-Protectionが有効となる。
 * 無効化する場合には、{@link #setEnable(boolean)}に{@code false}を設定する。
 * また、モードを変更及びモードの出力を抑制したい場合には、{@link #setMode(String)}を使用して設定する。
 * ({@link #setMode(String)}に空文字列を設定すると、モードの出力が抑制される。
 *
 * @author Hisaaki Shioiri
 */
public class XssProtectionHeader implements SecureResponseHeader {

    /** 有効にするかどうか */
    private boolean enable = true;

    /** モード */
    private String mode = "block";

    @Override
    public String getName() {
        return "X-XSS-Protection";
    }

    @Override
    public String getValue() {
        final StringBuilder result = new StringBuilder(16);
        result.append(enable ? '1' : '0');
        if (StringUtil.hasValue(mode)) {
            result.append("; mode=")
                  .append(mode);
        }
        return result.toString();
    }

    /**
     * 常に出力するので、{@code true}を返す。
     */
    @Override
    public boolean isOutput(final HttpResponse response, final ServletExecutionContext context) {
        return true;
    }

    /**
     * X-XSS-Protectionを有効にするか否かを設定する。
     *
     * @param enable {@code true}を設定した場合有効となる
     */
    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    /**
     * モードを設定する。
     *
     * @param mode モード
     */
    public void setMode(String mode) {
        this.mode = mode;
    }
}
