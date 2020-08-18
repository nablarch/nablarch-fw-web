package nablarch.fw.web.handler.secure;

import nablarch.core.util.StringUtil;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * Cache-Controlレスポンスヘッダを設定するクラス。
 *
 * デフォルトは"no-store"。
 *
 * Cache-Controlレスポンスヘッダを個別に指定したいケースに対応するため、
 * Cache-Controlレスポンスヘッダが設定されてない場合のみ設定を行う。
 * 上書きは行わない。
 *
 * @author Kiyohito Itoh
 */
public class CacheControlHeader extends SecureResponseHeaderSupport {

    private static final String NAME = "Cache-Control";
    /**
     * コンストラクタ。
     */
    public CacheControlHeader() {
        super(NAME, "no-store");
    }

    @Override
    public boolean isOutput(HttpResponse response, ServletExecutionContext context) {
        return StringUtil.isNullOrEmpty(response.getHeader(NAME));
    }
}
