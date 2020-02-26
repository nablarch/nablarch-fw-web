package nablarch.fw.web.servlet;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.log.app.FailureLogUtil;
import nablarch.core.log.app.LogInitializationHelper;
import nablarch.core.log.app.PerformanceLogUtil;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.ComponentDefinitionLoader;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.DuplicateDefinitionPolicy;
import nablarch.core.repository.di.config.externalize.CompositeExternalizedLoader;
import nablarch.core.repository.di.config.externalize.ExternalizedComponentDefinitionLoader;
import nablarch.core.repository.di.config.externalize.OsEnvironmentVariableExternalizedLoader;
import nablarch.core.repository.di.config.externalize.SystemPropertyExternalizedLoader;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.fw.web.handler.HttpAccessLogUtil;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Arrays;

/**
 * コンテキストの初期化を行う。<br/>
 * <br/>
 * 本クラスにおけるロガーの取得処理は、アプリケーションの起動時にログの初期処理を確実に行う意図があるため、削除しないこと。
 *
 * @author Koichi Asano
 */
public class NablarchServletContextListener implements ServletContextListener {

    /** ロガー */
    private static final Logger LOGGER = LoggerManager.get(NablarchServletContextListener.class);

    /** DIコンテナの設定ファイル名の設定キー。 */
    private static final String DI_CONTAINER_CONFIG_FILE_KEY = "di.config";

    /** 設定値に重複する設定が存在した場合の動作ポリシーの設定キー。 */
    private static final String DI_CONTAINER_DUPLICATE_DEFINITION_CONFIG_KEY = "di.duplicate-definition-policy";

    /** staticプロパティインジェクションの許可設定キー。 */
    private static final String DI_CONTAINER_ALLOW_STATIC_PROPERTY_KEY = "di.allow-static-property";

    /**
     * {@inheritDoc}<br/>
     * <br/>
     * リポジトリの初期化処理を行う。<br/>
     * 初期化処理完了後にINFOレベルでログを出力する。
     * リクエスト単体テスト時にはリポジトリの初期化は行わない（自動テストフレームワークにて実施）。
     */
    public void contextInitialized(ServletContextEvent event) {
        try {
            initializeLog();
            if (!isRequestTest()) {
                initializeRepository(event);
            }
            LOGGER.logInfo("[" + NablarchServletContextListener.class.getName()
                    + "#contextInitialized] initialization completed.");
        } catch (RuntimeException e) {
            FailureLogUtil.logFatal(e, (Object) null, null);
            throw e;
        }
    }

    /**
     * リポジトリの初期化を行う。
     *
     * @param event ServletContextEvent
     */
    private void initializeRepository(ServletContextEvent event) {

        ServletContext servletContext = event.getServletContext();
        // コンポーネント設定ファイル
        String configFile = servletContext.getInitParameter(DI_CONTAINER_CONFIG_FILE_KEY);

        // 設定値に重複する設定が存在した場合の動作ポリシー
        String duplicateDefinitionPolicy =
                servletContext.getInitParameter(DI_CONTAINER_DUPLICATE_DEFINITION_CONFIG_KEY);
        DuplicateDefinitionPolicy policy = evaluateDuplicateDefinitionPolicy(duplicateDefinitionPolicy);

        // staticプロパティへのインジェクションを許可するか
        String allowStaticProperty = servletContext.getInitParameter(DI_CONTAINER_ALLOW_STATIC_PROPERTY_KEY);
        boolean isStaticPropertyAllowed = Boolean.parseBoolean(allowStaticProperty);

        // リポジトリ初期化
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(configFile, policy);
        ExternalizedComponentDefinitionLoader externalizedComponentDefinitionLoader
            = new CompositeExternalizedLoader(Arrays.asList(
                new OsEnvironmentVariableExternalizedLoader(),
                new SystemPropertyExternalizedLoader()
            ));
        DiContainer container = new DiContainer(loader, isStaticPropertyAllowed, externalizedComponentDefinitionLoader);
        SystemRepository.load(container);
    }

    /**
     * 文字列から、設定値重複時の動作ポリシーを評価する。
     *
     * @param stringExpression 文字列表現
     * @return 設定値重複時の動作ポリシー
     */
    private DuplicateDefinitionPolicy evaluateDuplicateDefinitionPolicy(String stringExpression) {
        if (stringExpression == null) {
            return DuplicateDefinitionPolicy.OVERRIDE;
        }
        try {
            return DuplicateDefinitionPolicy.valueOf(stringExpression);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Illegal duplicate definition policy was specified."
                    + DI_CONTAINER_DUPLICATE_DEFINITION_CONFIG_KEY + " = " + stringExpression, e);
        }
    }

    /**
     * リクエスト単体テストであるか判定する。
     *
     * @return 判定結果
     */
    private boolean isRequestTest() {
        Object o = SystemRepository.get("httpTestConfiguration");
        return (o != null);
    }

    /** 各種ログの初期化を行う。 */
    private void initializeLog() {
        FailureLogUtil.initialize();
        PerformanceLogUtil.initialize();
        HttpAccessLogUtil.initialize();
        LogInitializationHelper.initialize();
    }

    /**
     * {@inheritDoc}<br/>
     * <br/>
     * ログの終了処理を行う。<br/>
     * ログの終了処理の直前にINFOレベルでログを出力する。
     */
    public void contextDestroyed(ServletContextEvent event) {

        LOGGER.logInfo("[" + NablarchServletContextListener.class.getName() + "#contextDestroyed]");

        LoggerManager.terminate();
    }
}
