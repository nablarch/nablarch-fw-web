package nablarch.common.web;

import nablarch.core.repository.SystemRepository;

/**
 * {@link WebConfig}を取得するためのユーティリティ。
 * 
 * @author Taichi Uragami
 *
 */
public final class WebConfigFinder {

    /** デフォルトの{@link WebConfig} */
    private static final WebConfig DEFAULT_WEB_CONFIG = new WebConfig();

    /**
     * インスタンス化しない
     */
    private WebConfigFinder() {
        // nop
    }

    /** {@link WebConfig}をリポジトリから取得する際に使用する名前 */
    private static final String WEB_CONFIG_NAME = "webConfig";

    /**
     * Webアプリケーションの設定を取得する。
     * 
     * @return Webアプリケーションの設定
     */
    public static WebConfig getWebConfig() {
        WebConfig webConfig = SystemRepository.get(WEB_CONFIG_NAME);
        if (webConfig != null) {
            return webConfig;
        }
        return DEFAULT_WEB_CONFIG;
    }
}
