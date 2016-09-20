package nablarch.fw.web.handler.secure;

import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * X-Content-Type-Optionsレスポンスヘッダを設定するクラス。
 *
 * @author Hisaaki Shioiri
 */
public class ContentTypeOptionsHeader implements SecureResponseHeader {

    /** ヘッダに出力する値 */
    private String option = "nosniff";

    @Override
    public String getName() {
        return "X-Content-Type-Options";
    }

    @Override
    public String getValue() {
        return option;
    }

    /**
     * 常に出力する。
     */
    @Override
    public boolean isOutput(final HttpResponse response, final ServletExecutionContext context) {
        return true;
    }

    /**
     * X-Content-Type-Optionsレスポンスヘッダに出力する値を設定する。
     *
     * @param option X-Content-Type-Optionsレスポンスヘッダに出力する値
     */
    public void setOption(final String option) {
        this.option = option;
    }
}
