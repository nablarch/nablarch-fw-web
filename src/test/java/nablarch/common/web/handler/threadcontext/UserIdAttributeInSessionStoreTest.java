package nablarch.common.web.handler.threadcontext;

import nablarch.common.handler.threadcontext.ThreadContextHandler;
import nablarch.common.handler.threadcontext.UserIdAttribute;
import nablarch.common.web.session.SessionUtil;
import nablarch.core.ThreadContext;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.Request;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class UserIdAttributeInSessionStoreTest {

    /**
     * セッションストアからユーザIDが取得できない場合
     * {@link UserIdAttribute#setAnonymousId(String)}で設定した値がスレッドコンテキストに設定されること
     */
    @Test
    public void testUserIdAttributeOnGuestContext() {
        ExecutionContext context = buildExecutionContext();
        context.handleNext(new MockRequest());
        assertThat(ThreadContext.getUserId(), is("guest"));

    }

    /**
     * セッションストアからユーザIDを取得できた場合
     * 取得した値がスレッドコンテキストに設定されること
     */
    @Test
    public void testUserIdAttributeOnGuestContextOnLoginUserContext() {
        final ExecutionContext context = buildExecutionContext();
        
        try (final MockedStatic<SessionUtil> mocked = Mockito.mockStatic(SessionUtil.class)) {
            mocked.when(() -> SessionUtil.orNull(context, "user.id")).thenReturn("user-id");
            context.handleNext(new MockRequest());
        }
        
        assertThat(ThreadContext.getUserId(), is("user-id"));
    }

    private ExecutionContext buildExecutionContext() {
        ThreadContextHandler handler = new ThreadContextHandler(
                new UserIdAttributeInSessionStore() {{
                    setSessionKey("user.id");
                    setAnonymousId("guest");
                }}
        );

        return new ExecutionContext()
                .clearHandlers()
                .addHandler(handler)
                .addHandler(new FinalHandler());
    }

    private static class MockRequest implements Request<String> {
        @Override
        public String getRequestPath() {
            return null;
        }

        @Override
        public Request<String> setRequestPath(String s) {
            return null;
        }

        @Override
        public String getParam(String s) {
            return null;
        }

        @Override
        public Map<String, String> getParamMap() {
            return null;
        }
    }

    private static class FinalHandler implements Handler<Request<String>, String> {
        @Override
        public String handle(final Request<String> request, final ExecutionContext context) {
            return request.getParam("param");
        }
    }
}
