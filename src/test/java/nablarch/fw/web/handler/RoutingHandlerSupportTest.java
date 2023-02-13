package nablarch.fw.web.handler;

import static org.junit.Assert.*;

import org.hamcrest.CoreMatchers;

import nablarch.fw.ExecutionContext;
import nablarch.fw.MethodBinder;
import nablarch.fw.web.HttpRequest;

import org.junit.Test;

/**
 * {@link RoutingHandlerSupport}のテストクラス。
 */
public class RoutingHandlerSupportTest {

    /**
     * 設定した{@link MethodBinderFactory}が参照できることのみ確認する。
     */
    @Test
    public void testSetMethodBinderFactory() throws Exception {
        Stub stub = new Stub();
        stub.setMethodBinderFactory(new MethodBinderFactoryStub());

        Class<?> result = stub.getHandlerClass(null, null);
        assertThat("設定したクラスが利用可能であること", result, CoreMatchers.<Class<?>>is(MethodBinderFactoryStub.class));
    }

    /**
     * テスト対象の{@link RoutingHandlerSupport}の実装クラス。
     * <p/>
     * このクラスは、{@link RoutingHandlerSupport}が保持している、
     * {@link MethodBinderFactory}の実装クラスのクラスオブジェクトを
     * {@link #getHandlerClass(HttpRequest, ExecutionContext)}で返却する。
     */
    private static class Stub extends RoutingHandlerSupport {

        @Override
        protected Class<?> getHandlerClass(HttpRequest input, ExecutionContext context) throws ClassNotFoundException {
            return methodBinderFactory.getClass();
        }
    }

    private static class MethodBinderFactoryStub implements MethodBinderFactory<Object> {

        @Override
        public MethodBinder<HttpRequest, Object> create(String methodName) {
            return null;
        }
    }
}
