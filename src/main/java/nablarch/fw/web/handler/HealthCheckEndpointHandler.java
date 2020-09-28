package nablarch.fw.web.handler;

import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.handler.health.HealthCheckResponseBuilder;
import nablarch.fw.web.handler.health.HealthCheckResult;
import nablarch.fw.web.handler.health.HealthChecker;

import java.util.*;

/**
 * ヘルスチェックを行うエンドポイントとなるハンドラ。
 *
 * DBやRedisなどの対象ごとのヘルスチェックは{@link HealthChecker}が行う。
 * ヘルスチェック結果からレスポンスの作成は{@link HealthCheckResponseBuilder}が行う。
 *
 * @author Kiyohito Itoh
 */
public class HealthCheckEndpointHandler implements HttpRequestHandler {

    private List<HealthChecker> healthCheckers = Collections.emptyList();
    private HealthCheckResponseBuilder healthCheckResponseBuilder = new HealthCheckResponseBuilder();

    @Override
    public HttpResponse handle(HttpRequest request, ExecutionContext context) {
        boolean isMergedHealthy = true;
        List<HealthCheckResult.Target> targets = new ArrayList<HealthCheckResult.Target>();
        for (HealthChecker healthChecker : healthCheckers) {
            final boolean isHealthy = healthChecker.check(request, context);
            isMergedHealthy = isMergedHealthy && isHealthy;
            targets.add(new HealthCheckResult.Target(healthChecker.getName(), isHealthy));
        }
        return healthCheckResponseBuilder.build(
                request, context, new HealthCheckResult(isMergedHealthy, targets));
    }

    /**
     * DBやRedisなどの対象ごとのヘルスチェックを行う{@link HealthChecker}を設定する。
     * @param healthCheckers DBやRedisなどの対象ごとのヘルスチェックを行う{@link HealthChecker}
     */
    public void setHealthCheckers(List<HealthChecker> healthCheckers) {
        this.healthCheckers = healthCheckers;
    }

    /**
     * ヘルスチェック結果からレスポンスを作成する{@link HealthCheckResponseBuilder}を設定する。
     * @param healthCheckResponseBuilder ヘルスチェック結果からレスポンスを作成する{@link HealthCheckResponseBuilder}
     */
    public void setHealthCheckResponseBuilder(HealthCheckResponseBuilder healthCheckResponseBuilder) {
        this.healthCheckResponseBuilder = healthCheckResponseBuilder;
    }
}
