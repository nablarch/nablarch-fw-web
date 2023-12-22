package nablarch.fw.web.servlet;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Kiyohito Itoh
 */
public class WebFrontControllerTest {
    @SuppressWarnings("unchecked")
    public Handler<Object, ?> handler = mock(Handler.class);
    public HttpServletResponse response = mock(HttpServletResponse.class);
    public FilterChain filterChain = mock(FilterChain.class);

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

        final ArgumentCaptor<ExecutionContext> captor = ArgumentCaptor.forClass(ExecutionContext.class);
        verify(handler, atLeastOnce()).handle(any(), captor.capture());

        final ExecutionContext context = captor.getValue();
        ServletRequest servletRequest = ((ServletExecutionContext) context).getServletRequest().getRequest();
        assertThat(servletRequest, is(notNullValue()));
        assertThat(servletRequest, is(not(instanceOf(PreventSessionCreationHttpServletRequestWrapper.class))));
    }

    @Test
    public void testPreventedSessionCreationIfEnable() throws Exception {
        WebFrontController sut = new WebFrontController();
        sut.setServletFilterConfig(new MockServletFilterConfig());
        sut.setHandlerQueue(Collections.singletonList(handler));

        sut.setPreventSessionCreation(true);
        sut.doFilter(new MockHttpServletRequest().getMockInstance(), response, filterChain);

        final ArgumentCaptor<ExecutionContext> captor = ArgumentCaptor.forClass(ExecutionContext.class);
        verify(handler, atLeastOnce()).handle(any(), captor.capture());
        
        final ExecutionContext context = captor.getValue();
        ServletRequest servletRequest = ((ServletExecutionContext) context).getServletRequest().getRequest();
        assertThat(servletRequest, is(instanceOf(PreventSessionCreationHttpServletRequestWrapper.class)));
    }

    @After
    public void clearRepository() {
        SystemRepository.clear();
    }
}
