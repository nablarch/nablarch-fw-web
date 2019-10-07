package nablarch;

import nablarch.core.exception.IllegalConfigurationException;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.fw.web.HttpServer;
import nablarch.fw.web.HttpServerFactory;

/**
 * ユニットテスト用のユーティリティクラス。
 */
public class TestUtil {
    private static final String HTTP_SERVER_FACTORY_KEY = "httpServerFactory";

    private TestUtil() {
    }

    /**
     * HttpServerの具象クラスのインスタンスを返却する。
     *
     * @return HttpServerのインスタンス
     */
    static public HttpServer createHttpServer() {
        final String HTTP_SERVER_FACTORY_KEY = "httpServerFactory";
        HttpServerFactory factory = SystemRepository.get(HTTP_SERVER_FACTORY_KEY);
        if (factory != null) {
            return factory.create();
        }

        //システムリポジトリにまだ存在してなかったら読み込む
        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader("jetty-config.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);

        factory = SystemRepository.get(HTTP_SERVER_FACTORY_KEY);
        if (factory == null) {
            throw new IllegalConfigurationException("could not find component. name=[" + HTTP_SERVER_FACTORY_KEY + "].");
        }
        return factory.create();
    }
}
