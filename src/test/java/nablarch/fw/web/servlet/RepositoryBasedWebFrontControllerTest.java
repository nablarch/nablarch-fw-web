package nablarch.fw.web.servlet;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Enumeration;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import org.junit.Test;

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
        FilterConfig config = new FilterConfig() {
                                    public ServletContext getServletContext() { return null; }
                                    public Enumeration getInitParameterNames() { return null; }
                                    public String getInitParameter(String arg0) { return null; }
                                    public String getFilterName() { return null; }};
        
        RepositoryBasedWebFrontController repoController = new RepositoryBasedWebFrontController();
        
        repoController.init(config);
        
        assertTrue(webController.getServletFilterConfig() == config);
        
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
        
        FilterConfig config = new FilterConfig() {
                                    public ServletContext getServletContext() { return null; }
                                    public Enumeration getInitParameterNames() { return null; }
                                    public String getInitParameter(String arg0) { return null; }
                                    public String getFilterName() { return null; }};
        
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
    public void testOtherNameWebFrontControllerOnRepository() throws ServletException, IOException {

        SystemRepository.clear();
        String path = "classpath:nablarch/fw/web/servlet/repository-based-web-front-controller-other-name-test.xml";
        SystemRepository.load(new DiContainer(new XmlComponentDefinitionLoader(path)));

        WebFrontController expectedWebController = SystemRepository.get("otherNameController");

        FilterConfig config = new FilterConfig() {
            public ServletContext getServletContext() { return null; }
            public Enumeration getInitParameterNames() { return null; }
            public String getInitParameter(String arg0) {
                if (arg0=="controllerName"){
                    return "otherNameController";
                } else {
                    return null;
                }
            }
            public String getFilterName() { return null; }};

        RepositoryBasedWebFrontController repoController = new RepositoryBasedWebFrontController();

        repoController.init(config);

        Class c= repoController.getClass();
        Field fld= null;
        try {
            fld = c.getDeclaredField("controller");
        } catch (NoSuchFieldException e) {
            fail();
        }
        fld.setAccessible(true);

        WebFrontController actualWebController = null;
        try {
            actualWebController = (WebFrontController)fld.get(repoController);
        } catch (IllegalAccessException e) {
            fail();
        }

        assertTrue(expectedWebController == actualWebController);
    }
}
