package nablarch.fw.web.handler.health;

import nablarch.core.util.annotation.Published;

import java.util.List;

/**
 * ヘルスチェック結果を保持するクラス。
 *
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public class HealthCheckResult {

    /**
     * 対象ごとのヘルスチェック結果
     */
    @Published(tag = "architect")
    public static class Target {

        private String name;
        private boolean healthy;

        /**
         * コンストラクタ。
         * @param name ヘルスチェック対象の名前
         * @param healthy ヘルスチェックが成功した場合はtrue
         */
        public Target(String name, boolean healthy) {
            this.name = name;
            this.healthy = healthy;
        }

        /**
         * ヘルスチェック対象の名前を取得する。
         * @return ヘルスチェック対象の名前
         */
        public String getName() {
            return name;
        }

        /**
         * ヘルスチェックが成功したか否かを判定する。
         * @return ヘルスチェックが成功した場合はtrue
         */
        public boolean isHealthy() {
            return healthy;
        }
    }

    private boolean healthy;
    private List<Target> targets;

    /**
     * コンストラクタ。
     * @param healthy 全てのヘルスチェックが成功した場合はtrue
     * @param targets 全てのヘルスチェック結果
     */
    public HealthCheckResult(boolean healthy, List<Target> targets) {
        this.healthy = healthy;
        this.targets = targets;
    }

    /**
     * 全てのヘルスチェックが成功したか否かを判定する。
     * @return 全てのヘルスチェックが成功した場合はtrue
     */
    public boolean isHealthy() {
        return healthy;
    }

    /**
     * 全てのヘルスチェック結果を取得する。
     * @return 全てのヘルスチェック結果
     */
    public List<Target> getTargets() {
        return targets;
    }
}
