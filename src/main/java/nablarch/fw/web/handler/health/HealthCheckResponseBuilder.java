package nablarch.fw.web.handler.health;

import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;

import java.util.List;

/**
 * ヘルスチェック結果からレスポンスを作成するビルダ。
 *
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public class HealthCheckResponseBuilder {

    private int healthyStatusCode = HttpResponse.Status.OK.getStatusCode();
    private String healthyStatus = "UP";
    private int unhealthyStatusCode = HttpResponse.Status.SERVICE_UNAVAILABLE.getStatusCode();
    private String unhealthyStatus = "DOWN";
    private boolean writeBody = true;

    /**
     * ヘルスチェック結果からレスポンスを作成する。
     *
     * デフォルトではJSONのレスポンスを作成する。
     *
     * ヘルスチェックが成功した場合
     * {"status":"UP","targets":[{"name":"DB","status":"UP"},{"name":"Redis","status":"UP"}]}
     *
     * ヘルスチェックが失敗した場合
     * {"status":"DOWN","targets":[{"name":"DB","status":"UP"},{"name":"Redis","status":"DOWN"}]}
     *
     * @param request リクエスト
     * @param context コンテキスト
     * @param result ヘルスチェック結果
     * @return レスポンス
     */
    public HttpResponse build(HttpRequest request, ExecutionContext context, HealthCheckResult result) {
        HttpResponse response = new HttpResponse();
        response.setStatusCode(result.isHealthy() ? healthyStatusCode : unhealthyStatusCode);
        response.setContentType(getContentType());
        if (writeBody) {
            response.write(buildResponseBody(request, context, result));
        }
        return response;
    }

    /**
     * コンテンツタイプを取得する。
     * @return コンテンツタイプ
     */
    protected String getContentType() {
        return "application/json";
    }

    /**
     * レスポンスボディを作成する。
     *
     * @param request リクエスト
     * @param context コンテキスト
     * @param result ヘルスチェック結果
     * @return レスポンスボディ
     */
    protected String buildResponseBody(HttpRequest request, ExecutionContext context, HealthCheckResult result) {
        if (result.getTargets().isEmpty()) {
            return String.format("{\"status\":\"%s\"}", status(result.isHealthy()));
        } else {
            return String.format("{\"status\":\"%s\",\"targets\":[%s]}",
                    status(result.isHealthy()), targets(result.getTargets()));
        }

    }

    private String targets(List<HealthCheckResult.Target> targets) {
        StringBuilder sb = new StringBuilder();
        for (HealthCheckResult.Target target : targets) {
            if (sb.length() != 0) {
                sb.append(",");
            }
            sb.append(String.format("{\"name\":\"%s\",\"status\":\"%s\"}",
                    target.getName(), status(target.isHealthy())));
        }
        return sb.toString();
    }

    private String status(boolean isHealthy) {
        return isHealthy ? healthyStatus : unhealthyStatus;
    }

    /**
     * ヘルスチェックが成功した場合のステータスコードを設定する。
     *
     * デフォルトは"200"。
     *
     * @param healthyStatusCode ヘルスチェックが成功した場合のステータスコード
     */
    public void setHealthyStatusCode(int healthyStatusCode) {
        this.healthyStatusCode = healthyStatusCode;
    }

    /**
     * ヘルスチェックが成功した場合のステータスの表現を設定する。
     *
     * デフォルトは"UP"。
     *
     * @param healthyStatus ヘルスチェックが成功した場合のステータスの表現
     */
    public void setHealthyStatus(String healthyStatus) {
        this.healthyStatus = healthyStatus;
    }

    /**
     * ヘルスチェックが失敗した場合のステータスコードを設定する。
     *
     * デフォルトは"503"。
     *
     * @param unhealthyStatusCode ヘルスチェックが失敗した場合のステータスコード
     */
    public void setUnhealthyStatusCode(int unhealthyStatusCode) {
        this.unhealthyStatusCode = unhealthyStatusCode;
    }

    /**
     * ヘルスチェックが失敗した場合のステータスの表現を設定する。
     *
     * デフォルトは"DOWN"。
     *
     * @param unhealthyStatus ヘルスチェックが失敗した場合のステータスの表現
     */
    public void setUnhealthyStatus(String unhealthyStatus) {
        this.unhealthyStatus = unhealthyStatus;
    }

    /**
     * レスポンスボディを書き込むか否かを設定する。
     *
     * デフォルトは"true"。
     * ステータスコードだけでよい場合は"false"を指定する。
     *
     * @param writeBody レスポンスボディを書き込む場合はtrue
     */
    public void setWriteBody(boolean writeBody) {
        this.writeBody = writeBody;
    }
}
