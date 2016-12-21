package nablarch.fw.web.handler;

import static nablarch.fw.web.handler.HttpAccessLogFormatter.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.MockHttpRequest;
import nablarch.fw.web.handler.normalizer.Normalizer;
import nablarch.fw.web.handler.normalizer.TrimNormalizer;

import org.junit.Test;

/**
 * {@link NormalizationHandler}のテストクラス。
 */
public class NormalizationHandlerTest {

    /** テスト対象 */
    private final NormalizationHandler sut = new NormalizationHandler();

    /**
     * デフォルト構成の場合、{@link TrimNormalizer}により、ノーマライズ処理が実行されること。
     *
     * @throws Exception
     */
    @Test
    public void testDefault() throws Exception {

        final ExecutionContext context = new ExecutionContext();
        context.addHandler(new Handler<HttpRequest, Object>() {
            @Override
            public Object handle(final HttpRequest request, final ExecutionContext context) {
                return request;
            }
        });
        final MockHttpRequest request = new MockHttpRequest();
        request.setParam("spaceonly", " \t\r\n　");
        request.setParam("someEmpty", "1", "  ", "3", "　　");
        request.setParam("trim1", " value ");
        request.setParam("trim2", "\t 　\r\naa\uD867\uDE3Dbb\t\r\n \t");         // サロゲートペア
        request.setParam("key", "v a l");
        request.setParam("surrogate_pair", "\uD867\uDE3D");                  // サロゲートペア

        sut.handle(request, context);

        assertThat("nullになること", request.getParam("spaceonly"), is(new String[] {null}));
        assertThat("空文字列になった要素だけnullに置き換えれること", request.getParam("someEmpty"),is(toArray("1", null, "3", null)));
        assertThat("前後のスペースがトリムされること", request.getParam("trim1"), is(toArray("value")));
        assertThat("サロゲートペアがあっても問題なくトリムできること", request.getParam("trim2"), is(toArray("aa\uD867\uDE3Dbb")));

        assertThat("文字列中のスペースはそのまま", request.getParam("key"), is(toArray("v a l")));
        assertThat("トリムされないケース（サロゲートペア有り）", request.getParam("surrogate_pair"), is(toArray("\uD867\uDE3D")));
    }

    /**
     * カスタム構成の場合、指定したノーマライザによりノーマライズ処理が実行されること。
     *
     * @throws Exception
     */
    @Test
    public void testCustom() throws Exception {
        sut.setNormalizers(Arrays.asList(
                new TrimNormalizer(),
                new Normalizer() {
                    @Override
                    public boolean canNormalize(final String key) {
                        return key.contains("target");
                    }

                    @Override
                    public String[] normalize(final String[] value) {
                        final String[] result = new String[value.length];
                        for (int i = 0; i < value.length; i++) {
                            result[i] = '[' + value[i] + ']';
                        }
                        return result;
                    }
                }
        ));

        final ExecutionContext context = new ExecutionContext();
        context.addHandler(new Handler<HttpRequest, Object>() {
            @Override
            public Object handle(final HttpRequest request, final ExecutionContext context) {
                return request;
            }
        });
        final MockHttpRequest request = new MockHttpRequest();
        request.setParam("trim1", " value ");
        request.setParam("target", " value ");
        request.setParam("param", "value");

        sut.handle(request, context);

        assertThat("trimのみ実行される", request.getParam("trim1"), is(toArray("value")));
        assertThat("trimと編集が実行される", request.getParam("target"), is(toArray("[value]")));

        assertThat("なにも実行されない", request.getParam("param"), is(toArray("value")));
    }

    /**
     * 配列に変換する。
     */
    public String[] toArray(String... values) {
        return values;
    }
}