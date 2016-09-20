package nablarch.fw.web.handler;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.handler.normalizer.Normalizer;
import nablarch.fw.web.handler.normalizer.TrimNormalizer;

/**
 * リクエストパラメータの値をノーマライズするハンドラ。
 * <p>
 * このハンドラはデフォルトで、リクエストパラメータの前後のホワイトスペースを除去する。
 * <p>
 * もし、デフォルト実装以外のノーマライズ処理を行う必要がある場合は、{@link #setNormalizers(List)}を使用して、{@link Normalizer}を設定すること。
 * {@link #setNormalizers(List)}では、デフォルトの動作が上書きされるため、デフォルトで適用されている{@link TrimNormalizer}の設定も行う必要がある。
 *
 * @author Hisaaki Shioiri
 */
public class NormalizationHandler implements Handler<HttpRequest, Object> {

    /** このハンドラで行うノーマライザのリスト */
    private List<Normalizer> normalizers = null;

    /**
     * デフォルトの構成でハンドラオブジェクトを生成する。
     * <p>
     * デフォルト構成では、{@link TrimNormalizer}が有効となる。
     */
    public NormalizationHandler() {
        setNormalizers(Collections.<Normalizer>singletonList(new TrimNormalizer()));
    }

    @Override
    public Object handle(final HttpRequest request, final ExecutionContext context) {
        final Map<String, String[]> parameters = request.getParamMap();
        for (final Map.Entry<String, String[]> entry : parameters.entrySet()) {
            final String key = entry.getKey();
            String[] value = entry.getValue();
            for (final Normalizer normalizer : normalizers) {
                if (normalizer.canNormalize(key)) {
                    value = normalizer.normalize(value);
                }
            }
            parameters.put(key, value);

        }
        return context.handleNext(request);
    }

    /**
     * {@link Normalizer}を設定する。
     *
     * @param normalizers ノーマライザ
     */
    public void setNormalizers(final List<Normalizer> normalizers) {
        this.normalizers = Collections.unmodifiableList(normalizers);
    }
}

