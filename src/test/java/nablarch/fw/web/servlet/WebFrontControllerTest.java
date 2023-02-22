package nablarch.fw.web.servlet;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import mockit.Mocked;
import mockit.Verifications;
import nablarch.TestUtil;
import nablarch.common.web.session.MockHttpServletRequest;
import nablarch.core.ThreadContext;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpServer;
import nablarch.fw.web.MockHttpRequest;
import nablarch.fw.web.handler.HttpErrorHandler;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;

/**
 * @author Kiyohito Itoh
 */
public class WebFrontControllerTest {
    @Mocked
    public Handler<Object, ?> handler;
    @Mocked
    public HttpServletResponse response;
    @Mocked
    public FilterChain filterChain;

    @BeforeClass
    public static void setUpClass() throws Exception {
        ThreadContext.clear();
    }

    @Test
    public void testInitForSpecifyDefaultPage() {
        
        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
            "nablarch/fw/web/servlet/specify-default-pages-test.xml"
        );
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
        
        WebFrontController controller = SystemRepository.get("webFrontController");
        
        assertThat(
            controller.getHandlerOf(HttpErrorHandler.class)
                      .getDefaultPageFor(500)
                      .getPath()
          , is("/ERROR.jsp")
        );
        assertThat(
            controller.getHandlerOf(HttpErrorHandler.class)
                      .getDefaultPageFor(404)
                      .getPath()
          , is("/NOT_FOUND.jsp")
        );

    }
    
    @Test
    public void testInitForNotSpecifyDefaultPage() {
        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
            "nablarch/fw/web/servlet/not-specify-default-pages-test.xml"
        );
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
        
        TestUtil.createHttpServer(); // 例外が発生しなければOK
    }
    
    @Test
    public void testCanBeAssignedFullHandlerQueue() {
        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
            "nablarch/fw/web/servlet/fullHandlerQueue.xml"
        );
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
        WebFrontController controller =  SystemRepository.get("webFrontController");
        assertNotNull(controller);
        
        HttpServer server = TestUtil.createHttpServer();
        server.setHandlerQueue(controller.getHandlerQueue());
        server.startLocal();
        
        HttpResponse res = server.handle(new MockHttpRequest("GET / HTTP/1.1"), new ExecutionContext());
        assertEquals(404, res.getStatusCode());
    }

    @Test
    public void testNotPreventedSessionCreationIfDisable() throws Exception {
        WebFrontController sut = new WebFrontController();
        sut.setServletFilterConfig(new MockServletFilterConfig());
        sut.setHandlerQueue(Collections.singletonList(handler));

        sut.setPreventSessionCreation(false);
        sut.doFilter(new MockHttpServletRequest().getMockInstance(), response, filterChain);

        new Verifications() {{
            ExecutionContext context;
            handler.handle(any, context = withCapture());

            ServletRequest servletRequest = ((ServletExecutionContext) context).getServletRequest().getRequest();
            assertThat(servletRequest, is(notNullValue()));
            assertThat(servletRequest, is(not(instanceOf(PreventSessionCreationHttpServletRequestWrapper.class))));
        }};
    }

    @Test
    public void testPreventedSessionCreationIfEnable() throws Exception {
        WebFrontController sut = new WebFrontController();
        sut.setServletFilterConfig(new MockServletFilterConfig());
        sut.setHandlerQueue(Collections.singletonList(handler));

        sut.setPreventSessionCreation(true);
        sut.doFilter(new MockHttpServletRequest().getMockInstance(), response, filterChain);

        new Verifications() {{
            ExecutionContext context;
            handler.handle(any, context = withCapture());

            ServletRequest servletRequest = ((ServletExecutionContext) context).getServletRequest().getRequest();
            assertThat(servletRequest, is(instanceOf(PreventSessionCreationHttpServletRequestWrapper.class)));
        }};
    }

    @After
    public void clearRepository() {
        SystemRepository.clear();
    }
}
