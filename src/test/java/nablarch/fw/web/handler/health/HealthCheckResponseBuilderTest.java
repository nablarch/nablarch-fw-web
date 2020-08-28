package nablarch.fw.web.handler.health;

import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;

/**
 * {@link HealthCheckResponseBuilder}のテスト。
 */
public class HealthCheckResponseBuilderTest {

    /**
     * デフォルト設定でヘルスチェック結果が成功の確認。
     */
    @Test
    public void defaultSettingsForSuccess() {

        HealthCheckResult result = new HealthCheckResult(true, Arrays.asList(
                new HealthCheckResult.Target("DB", true),
                new HealthCheckResult.Target("Redis", true)
        ));

        HealthCheckResponseBuilder sut = new HealthCheckResponseBuilder();
        HttpResponse response = sut.build(null, null, result);

        assertThat(response.getStatusCode(), is(200));
        assertThat(response.getContentType(), is("application/json"));
        assertThat(response.getBodyString(), is("{\"status\":\"UP\",\"targets\":[{\"name\":\"DB\",\"status\":\"UP\"},{\"name\":\"Redis\",\"status\":\"UP\"}]}"));
    }

    /**
     * デフォルト設定でヘルスチェック結果が失敗の確認。
     */
    @Test
    public void defaultSettingsForFailure() {

        HealthCheckResult result = new HealthCheckResult(false, Arrays.asList(
                new HealthCheckResult.Target("DB", true),
                new HealthCheckResult.Target("Redis", false)
        ));

        HealthCheckResponseBuilder sut = new HealthCheckResponseBuilder();
        HttpResponse response = sut.build(null, null, result);

        assertThat(response.getStatusCode(), is(503));
        assertThat(response.getContentType(), is("application/json"));
        assertThat(response.getBodyString(), is("{\"status\":\"DOWN\",\"targets\":[{\"name\":\"DB\",\"status\":\"UP\"},{\"name\":\"Redis\",\"status\":\"DOWN\"}]}"));
    }

    /**
     * レスポンスボディを出さない設定の確認。
     */
    @Test
    public void noBody() {

        HealthCheckResult result = new HealthCheckResult(true, Arrays.asList(
                new HealthCheckResult.Target("DB", true),
                new HealthCheckResult.Target("Redis", true)
        ));

        HealthCheckResponseBuilder sut = new HealthCheckResponseBuilder();
        sut.setWriteBody(false);
        HttpResponse response = sut.build(null, null, result);

        assertThat(response.getStatusCode(), is(200));
        assertThat(response.getHeader("Content-Type"), is(nullValue()));
        assertThat(response.getBodyString(), is(""));
    }

    /**
     * カスタム設定でヘルスチェック結果が成功の確認。
     */
    @Test
    public void customSettingsForSuccess() {

        HealthCheckResult result = new HealthCheckResult(true, Arrays.asList(
                new HealthCheckResult.Target("DB", true),
                new HealthCheckResult.Target("Redis", true)
        ));

        HealthCheckResponseBuilder sut = new HealthCheckResponseBuilder();
        sut.setHealthyStatusCode(204);
        sut.setHealthyStatus("OK");
        sut.setUnhealthyStatusCode(500);
        sut.setUnhealthyStatus("NG");
        HttpResponse response = sut.build(null, null, result);

        assertThat(response.getStatusCode(), is(204));
        assertThat(response.getContentType(), is("application/json"));
        assertThat(response.getBodyString(), is("{\"status\":\"OK\",\"targets\":[{\"name\":\"DB\",\"status\":\"OK\"},{\"name\":\"Redis\",\"status\":\"OK\"}]}"));
    }

    /**
     * カスタム設定でヘルスチェック結果が失敗の確認。
     */
    @Test
    public void customSettingsForFailure() {

        HealthCheckResult result = new HealthCheckResult(false, Arrays.asList(
                new HealthCheckResult.Target("DB", true),
                new HealthCheckResult.Target("Redis", false)
        ));

        HealthCheckResponseBuilder sut = new HealthCheckResponseBuilder();
        sut.setHealthyStatusCode(204);
        sut.setHealthyStatus("OK");
        sut.setUnhealthyStatusCode(500);
        sut.setUnhealthyStatus("NG");
        HttpResponse response = sut.build(null, null, result);

        assertThat(response.getStatusCode(), is(500));
        assertThat(response.getContentType(), is("application/json"));
        assertThat(response.getBodyString(), is("{\"status\":\"NG\",\"targets\":[{\"name\":\"DB\",\"status\":\"OK\"},{\"name\":\"Redis\",\"status\":\"NG\"}]}"));
    }

    /**
     * ヘルスチェック対象がない場合の確認。
     */
    @Test
    public void noTargets() {

        HealthCheckResult result = new HealthCheckResult(true, Collections.<HealthCheckResult.Target>emptyList());

        HealthCheckResponseBuilder sut = new HealthCheckResponseBuilder();
        HttpResponse response = sut.build(null, null, result);

        assertThat(response.getStatusCode(), is(200));
        assertThat(response.getContentType(), is("application/json"));
        assertThat(response.getBodyString(), is("{\"status\":\"UP\"}"));
    }

    /**
     * レスポンスボディの作成処理のカスタマイズ確認。
     */
    @Test
    public void customResponseBody() {

        HealthCheckResult result = new HealthCheckResult(true, Arrays.asList(
                new HealthCheckResult.Target("DB", true),
                new HealthCheckResult.Target("Redis", true)
        ));

        HealthCheckResponseBuilder sut = new HealthCheckResponseBuilder() {
            @Override
            protected String getContentType() {
                return "text/plain";
            }
            @Override
            protected String buildResponseBody(HttpRequest request, ExecutionContext context, HealthCheckResult result) {
                return "test-test-test";
            }
        };
        HttpResponse response = sut.build(null, null, result);

        assertThat(response.getStatusCode(), is(200));
        assertThat(response.getContentType(), is("text/plain"));
        assertThat(response.getBodyString(), is("test-test-test"));
    }
}