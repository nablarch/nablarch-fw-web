package nablarch.fw.web.handler.secure;

import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * X-Frame-Optionsレスポンスヘッダを設定するクラス。
 *
 * @author Hisaaki Shioiri
 */
public class FrameOptionsHeader implements SecureResponseHeader {

    /** X-Frame-Optionsヘッダの設定値リスト */
    private enum OPTIONS {
        /** DENY */
        DENY,
        /** SAMEORIGIN */
        SAMEORIGIN,
        /** X-Frame-Optionsを設定しない */
        NONE
    }

    /** X-Frame-Optionsヘッダーに設定する値 */
    private OPTIONS option;

    /**
     * デフォルトの設定でオブジェクトを構築する。
     */
    public FrameOptionsHeader() {
        option = OPTIONS.SAMEORIGIN;
    }

    /**
     * X-Frame-Optionsを設定する。
     *
     * @param xFrameOptions X-Frame-Optionsの値
     */
    public void setOption(final String xFrameOptions) {
        try {
            option = OPTIONS.valueOf(xFrameOptions);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("invalid X-Frame-Options. value = [%s]", xFrameOptions),
                    e);
        }
    }

    /**
     * {@link OPTIONS#NONE}以外の場合は出力する。
     */
    @Override
    public boolean isOutput(final HttpResponse response, final ServletExecutionContext context) {
        return option == OPTIONS.SAMEORIGIN || option == OPTIONS.DENY;
    }

    @Override
    public String getName() {
        return "X-Frame-Options";
    }

    @Override
    public String getValue() {
        return option.toString();
    }
}

