package nablarch.fw.web.handler.secure;

import nablarch.core.util.annotation.Published;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * 単純な{@link SecureResponseHeader}の実装を提供するサポートクラス。
 *
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public abstract class SecureResponseHeaderSupport implements SecureResponseHeader {

    /** レスポンスヘッダの名前 */
    private String name;

    /** レスポンスヘッダに指定する値 */
    private String value;

    /**
     * コンストラクタ。
     *
     * @param name レスポンスヘッダの名前
     * @param defaultValue レスポンスヘッダに指定する値のデフォルト
     */
    protected SecureResponseHeaderSupport(String name, String defaultValue) {
        this.name = name;
        this.value = defaultValue;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public boolean isOutput(HttpResponse response, ServletExecutionContext context) {
        return true;
    }

    /**
     * レスポンスヘッダに指定する値を設定する。
     *
     * @param value レスポンスヘッダに指定する値
     */
    public void setValue(String value) {
        this.value = value;
    }
}
