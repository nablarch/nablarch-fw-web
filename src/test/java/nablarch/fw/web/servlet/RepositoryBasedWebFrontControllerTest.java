package nablarch.fw.web.servlet;

import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.test.support.reflection.ReflectionUtil;
import org.junit.Test;

import java.io.IOException;
import java.util.Enumeration;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * {@link RepositoryBasedWebFrontController}のテストクラス。
 * 
 * @author Kiyohito Itoh
 */
public class RepositoryBasedWebFrontControllerTest {
    
    /**
     * リポジトリに登録した{@link WebFrontController}に処理が委譲されること。
     */
    @Test
    public void testDelegateWebFrontControllerOnRepository() throws ServletException, IOException {
        
        SystemRepository.clear();
        String path = "classpath:nablarch/fw/web/servlet/repository-based-web-front-controller-test.xml";
        SystemRepository.load(new DiContainer(new XmlComponentDefinitionLoader(path)));
        
        WebFrontController webController = SystemRepository.get("webFrontController");
        TestHandler handler = webController.getHandlerOf(TestHandler.class);
        FilterConfig config = new TestFilterConfig();
        
        RepositoryBasedWebFrontController repoController = new RepositoryBasedWebFrontController();
        
        repoController.init(config);

        assertSame(webController.getServletFilterConfig(), config);
        
        MockServletRequest req = new MockServletRequest();
        req.setAttribute("key1", "value1");
        MockServletResponse res = new MockServletResponse();
        repoController.doFilter(req, res, null);
        
        assertEquals("value1", ((ServletExecutionContext)handler.context).getServletRequest().getScope().get("key1"));

        repoController.destroy();
        
        assertNull(webController.getServletFilterConfig());
    }
    
    public static final class TestHandler implements Handler<HttpRequestWrapper, Object> {
        private HttpRequestWrapper data;
        private ExecutionContext context;
        public Object handle(HttpRequestWrapper data, ExecutionContext context) {
            this.data = data;
            this.context = context;
            return null;
        }
    }
    
    /**
     * リポジトリに{@link WebFrontController}が登録されていない場合は{@link ServletException}をスローされる。
     */
    @Test
    public void testNoWebFrontControllerOnRepository() {

        SystemRepository.clear();
        String path = "classpath:nablarch/fw/web/servlet/repository-based-web-front-controller-nocontroller-test.xml";
        SystemRepository.load(new DiContainer(new XmlComponentDefinitionLoader(path)));

        FilterConfig config = new TestFilterConfig();
        
        RepositoryBasedWebFrontController repoController = new RepositoryBasedWebFrontController();
        
        try {
            repoController.init(config);
            fail();
        } catch (ServletException e) {
            assertThat(e.getMessage(), is("webFrontController must be configured in SystemRepository."));
        }
    }

    /**
     * デフォルト値と異なる値をinit-paramに設定すると移譲される{@link WebFrontController}は、指定したコントローラであること。
     * デフォルト値の名前で定義されたwebFrontControllerは使われないこと。
     */
    @Test
    public void testOtherNameWebFrontControllerOnRepository() throws ServletException {

        SystemRepository.clear();
        String path = "classpath:nablarch/fw/web/servlet/repository-based-web-front-controller-other-name-test.xml";
        SystemRepository.load(new DiContainer(new XmlComponentDefinitionLoader(path)));

        WebFrontController expectedWebController = SystemRepository.get("otherNameController");
        WebFrontController defaultWebController = SystemRepository.get("webFrontController");

        FilterConfig config = new TestFilterConfig("otherNameController");

        RepositoryBasedWebFrontController repoController = new RepositoryBasedWebFrontController();

        repoController.init(config);

        WebFrontController actualWebController = ReflectionUtil.getFieldValue(repoController,"controller");

        assertNotSame(defaultWebController,actualWebController);

        assertSame(expectedWebController,actualWebController);
    }

    private static final class TestFilterConfig implements FilterConfig {
        private String controllerName;

        public TestFilterConfig() {
        }

        public TestFilterConfig(String controllerName) {
            this.controllerName = controllerName;
        }

        @Override
        public String getFilterName() {
            return null;
        }

        @Override
        public ServletContext getServletContext() {
            return null;
        }

        @Override
        public String getInitParameter(String name) {
            return controllerName;
        }

        @Override
        public Enumeration getInitParameterNames() {
            return null;
        }
    }
}
