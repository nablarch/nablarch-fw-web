package nablarch.fw.web.handler.secure;

import nablarch.core.util.annotation.Published;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * セキュリティに関連したレスポンスヘッダを返すインタフェース
 *
 * @author Hisaaki Shioiri
 */
@Published(tag = "architect")
public interface SecureResponseHeader {

    /**
     * レスポンスヘッダの名前を返す。
     *
     * @return レスポンスヘッダの名前
     */
    String getName();

    /**
     * レスポンスヘッダの値を返す。
     *
     * @return レスポンスヘッダの値
     */
    String getValue();

    /**
     * このヘッダを出力するか否かを返す。
     *
     * @param response レスポンスオブジェクト
     * @param context Servlet APIの情報を持つコンテキスト
     * @return 出力する場合は{@code true}
     */
    boolean isOutput(HttpResponse response, ServletExecutionContext context);

}
