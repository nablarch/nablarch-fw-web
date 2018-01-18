package nablarch.common.web.token;

import java.util.Locale;

import nablarch.core.ThreadContext;

import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;

import nablarch.test.support.message.MockStringResourceHolder;
import org.junit.BeforeClass;

/**
 * @author Kiyohito Itoh
 */
public class TokenTestUtil {

    private TokenTestUtil() {}

    private static final String[][] MESSAGES = {
        { "MSG00011", "ja", "メッセージ011"},
        { "MSG00022", "ja", "メッセージ022" },
        };

    @BeforeClass
    public static void classSetup() throws Exception {
        TokenTestUtil.setUpMessageBeforeClass();
    }

    static final String TOKEN = "token_test";

    static void setUpValidToken(HttpRequest request, ExecutionContext context) {
        setTokenParam(request, TOKEN);
        setTokenSession(context, TOKEN);
    }

    static void setUpInvalidToken(HttpRequest request, ExecutionContext context) {
        setTokenParam(request, TOKEN);
    }

    static void setTokenSession(ExecutionContext context, String token) {
        context.setSessionScopedVar("/nablarch_session_token", token);
    }

    static void setTokenParam(HttpRequest request, String token) {
        request.getParamMap().put(TokenUtil.KEY_HIDDEN_TOKEN, new String[] { token });
    }

    static void setUpMessageBeforeClass() throws Exception  {
    }

    @SuppressWarnings("unchecked")
    static void setUpMessage(boolean withHandler) {
        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                withHandler ? "nablarch/common/web/token/message-with-token-test.xml" : "nablarch/common/web/token/message-test.xml");
        DiContainer container = new DiContainer(loader);
        container.getComponentByType(MockStringResourceHolder.class).setMessages(MESSAGES);
        SystemRepository.load(container);
        ThreadContext.setLanguage(Locale.JAPANESE);
    }

    static void tearDownAfterClass() throws Exception {
    }

    static void setUpTokenGenerator() {
        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/common/web/token/token-generator-test.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
    }
}
