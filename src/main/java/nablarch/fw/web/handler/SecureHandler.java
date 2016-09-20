package nablarch.fw.web.handler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.handler.secure.ContentTypeOptionsHeader;
import nablarch.fw.web.handler.secure.FrameOptionsHeader;
import nablarch.fw.web.handler.secure.SecureResponseHeader;
import nablarch.fw.web.handler.secure.XssProtectionHeader;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * セキュリティ関連のレスポンスヘッダを設定するハンドラ。
 *
 * レスポンスヘッダに設定する値は、{@link #setSecureResponseHeaderList(List)}に設定された、値から取得する。
 * 特定条件の場合に出力を抑制する場合は、{@link SecureResponseHeader#isOutput(HttpResponse, ServletExecutionContext)}で、{@code false}を返すこと。
 *
 * @author Hisaaki Shioiri
 */
public class SecureHandler implements HttpRequestHandler {

    /** セキュリティ関連のレスポンスヘッダを構築するオブジェクト */
    private List<? extends SecureResponseHeader> secureResponseHeaderList =
            Arrays.asList(
                    new FrameOptionsHeader(),
                    new XssProtectionHeader(),
                    new ContentTypeOptionsHeader());

    @Override
    public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {
        final HttpResponse response = context.handleNext(request);

        final ServletExecutionContext servletExecutionContext = (ServletExecutionContext) context;
        for (final SecureResponseHeader responseHeader : secureResponseHeaderList) {
            if (responseHeader.isOutput(response, servletExecutionContext)) {
                response.setHeader(responseHeader.getName(), responseHeader.getValue());
            }
        }
        return response;
    }

    /**
     * セキュリティ関連のヘッダ情報を生成する{@link SecureResponseHeader}を設定する。
     * @param secureResponseHeaderList {@code SecureResponseHeader}のリスト
     */
    public void setSecureResponseHeaderList(
            final List<? extends SecureResponseHeader> secureResponseHeaderList) {
        this.secureResponseHeaderList = Collections.unmodifiableList(secureResponseHeaderList);
    }
}
