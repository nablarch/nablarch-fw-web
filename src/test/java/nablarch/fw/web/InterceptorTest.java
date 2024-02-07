package nablarch.fw.web;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.Interceptor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class InterceptorTest {

    @After
    public void tearDown() throws Exception {
        SystemRepository.clear();
    }

    public static class GreetingHandler implements HttpRequestHandler {
        @SayHoge
        public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
            return new HttpResponse().write("Hello world.");
        }
    }
    
    @Documented
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Interceptor(SayHoge.Impl.class)
    public @interface SayHoge {
        public static class Impl extends Interceptor.Impl<HttpRequest, HttpResponse, SayHoge>{
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                HttpResponse res = getOriginalHandler().handle(req, ctx);
                return res.write("\nSay hoge!");
            }
        }
    }
    
    @Documented
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Interceptor(SayFuga.Impl.class)
    public @interface SayFuga {
        public static class Impl extends Interceptor.Impl<HttpRequest, HttpResponse, SayFuga>{
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                HttpResponse res = getOriginalHandler().handle(req, ctx);
                return res.write("\nSay fuga!");
            }
        }
    }

    @Documented
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Interceptor(SayFoo.Impl.class)
    public @interface SayFoo {
        public static class Impl extends Interceptor.Impl<HttpRequest, HttpResponse, SayFoo>{
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                HttpResponse res = getOriginalHandler().handle(req, ctx);
                return res.write("\nSay foo!");
            }
        }
    }
    
    @Test
    public void testGetInterceptorOf() {
        SayHoge annotation = new SayHoge(){
           public Class<? extends Annotation> annotationType() {
               return SayHoge.class;
           }
        };
        Assert.assertNotNull(Interceptor.Factory.getInterceptorOf(annotation));
    }
    
    @Test
    public void testWrap() {
        HttpRequest req = new MockHttpRequest();
        ExecutionContext ctx = new ExecutionContext();
        
        HttpRequestHandler handler = new GreetingHandler();
        Assert.assertEquals(
                "Hello world.",
                handler.handle(req, ctx)
                        .getBodyString()
        );
        
        Handler<HttpRequest, HttpResponse> wrapped = Interceptor.Factory.wrap(handler);
        
        Assert.assertEquals(
                "Hello world.\nSay hoge!",
                wrapped.handle(req, ctx)
                        .getBodyString()
        );
        
        // 無名クラスに対するインターセプト
        handler = new HttpRequestHandler() {
            @SayFuga //オーバーロードメソッドにはひっかからない。
            public HttpResponse handle() {
                return null;
            }

            @SayFuga
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse().write("Hello world.");
            }
        };
        
        Assert.assertEquals(
                "Hello world.",
                handler.handle(req, ctx)
                        .getBodyString()
        );
        
        wrapped = Interceptor.Factory.wrap(handler);
        
        Assert.assertEquals(
                "Hello world.\nSay fuga!",
                wrapped.handle(req, ctx)
                        .getBodyString()
        );
        
        
    }
    
    @Test
    public void testWrapWithMultipleInterceptors() {
        HttpRequest req = new MockHttpRequest();
        ExecutionContext ctx = new ExecutionContext();

        // 複数のインターセプターを使用した場合
        HttpRequestHandler handler = new HttpRequestHandler() {
            @SayHoge
            @SayFuga
            @After
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse().write("Hello world.");
            }
        };
        Assert.assertEquals(
                "Hello world.",
                handler.handle(req, ctx)
                        .getBodyString()
        );
        
        Handler<HttpRequest, HttpResponse> wrapped = Interceptor.Factory.wrap(handler);
        
        Assert.assertEquals(
                "Hello world.\nSay hoge!\nSay fuga!",
                wrapped.handle(req, ctx)
                        .getBodyString()
        );
    }
    
    @Test
    public void testOrder() {
        HttpRequest req = new MockHttpRequest();
        ExecutionContext ctx = new ExecutionContext();

        HttpRequestHandler handler = new HttpRequestHandler() {
            @SayHoge
            @SayFuga
            @After
            @SayFoo
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse().write("Hello world.");
            }
        };
        Assert.assertEquals(
                "Hello world.",
                handler.handle(req, ctx)
                        .getBodyString()
        );
        
        Handler<HttpRequest, HttpResponse> wrapped = Interceptor.Factory.wrap(handler);
        
        Assert.assertEquals(
                "Hello world.\nSay hoge!\nSay fuga!\nSay foo!",
                wrapped.handle(req, ctx)
                        .getBodyString()
        );
        
        SystemRepository.clear();
    }
    
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Interceptor(InterceptorWithoutConstructor.Impl.class)
    public @interface InterceptorWithoutConstructor {
        public static class Impl extends Interceptor.Impl<HttpRequest, HttpResponse, InterceptorWithoutConstructor> {
            public Impl(int value) {
                this.value = value;
            }
            final int value;
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                return getOriginalHandler().handle(req, ctx);
            }
        }
    }
    
    @Test
    public void testThatThrowsErrorWhenIntercepterDoesntHaveDefaultConstructor() {
        HttpRequestHandler handler = new HttpRequestHandler() {
            @InterceptorWithoutConstructor
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse();
            }
        };
        
        try {
            Interceptor.Factory.wrap(handler);
            Assert.fail();
        } catch(RuntimeException e) {
            Assert.assertEquals(NoSuchMethodException.class, e.getCause()
                    .getClass());
        } catch (Throwable e) {
            Assert.fail();
        }
    }
    
    
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Interceptor(InterceptorCannotInstantiate.Impl.class)
    public @interface InterceptorCannotInstantiate {
        public abstract static class Impl extends Interceptor.Impl<HttpRequest, HttpResponse, InterceptorCannotInstantiate> {
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                return getOriginalHandler().handle(req, ctx);
            }
        }
    }
    
    @Test
    public void testThatThrowsErrorWhenInterceptorCannotCreateInstance() {
        HttpRequestHandler handler = new HttpRequestHandler() {
            @InterceptorCannotInstantiate
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse();
            }
        };
        
        try {
            Interceptor.Factory.wrap(handler);
            Assert.fail();
        } catch (RuntimeException e) {
            Assert.assertEquals(InstantiationException.class, e.getCause()
                    .getClass());
        } catch (Throwable e) {
            Assert.fail();
        }
    }
    
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Interceptor(InterceptorWhoseConstructorThrowsIllegalAccessException.Impl.class)
    public @interface InterceptorWhoseConstructorThrowsIllegalAccessException {
        public static class Impl extends Interceptor.Impl<HttpRequest, HttpResponse, InterceptorWhoseConstructorThrowsIllegalAccessException> {
            Impl() {
            }
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse();
            }
        }
    }
    
    @Test
    public void testThatThrowsErrorWhenInterceptorsConstructorCannotAccess() {
        HttpRequestHandler handler = new HttpRequestHandler() {
            @InterceptorWhoseConstructorThrowsIllegalAccessException
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse();
            }
        };
        
        try {
            Interceptor.Factory.wrap(handler);
            Assert.fail();
        } catch (Throwable e) {
            Assert.assertEquals(RuntimeException.class, e.getClass());
            Assert.assertEquals(NoSuchMethodException.class, e.getCause()
                    .getClass());
        }
    }
    
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Interceptor(InterceptorWhoseConstructorThrowsRuntimeException.Impl.class)
    public @interface InterceptorWhoseConstructorThrowsRuntimeException {
        public static class Impl
        extends Interceptor.Impl<HttpRequest,
                HttpResponse,
                                 InterceptorWhoseConstructorThrowsRuntimeException> {
            public Impl() {
                throw new IllegalArgumentException();
            }
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse();
            }
        }
    }
    
    @Test
    public void testThatThrowsErrorWhenInterceptorThrowsRuntimeException() {
        HttpRequestHandler handler = new HttpRequestHandler() {
            @InterceptorWhoseConstructorThrowsRuntimeException
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse();
            }
        };
        
        try {
            Interceptor.Factory.wrap(handler);
            Assert.fail();
        } catch (Throwable e) {
            Assert.assertEquals(IllegalArgumentException.class, e.getClass());
        }
    }
    
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Interceptor(InterceptorWhoseConstructorThrowsError.Impl.class)
    public @interface InterceptorWhoseConstructorThrowsError {
        public static class Impl extends Interceptor.Impl<HttpRequest, HttpResponse, InterceptorWhoseConstructorThrowsError> {
            public Impl() {
                throw new IllegalAccessError();
            }
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse();
            }
        }
    }
    
    @Test
    public void testThatThrowsErrorWhenInterceptorThrowsError() {
        HttpRequestHandler handler = new HttpRequestHandler() {
            @InterceptorWhoseConstructorThrowsError
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse();
            }
        };
        
        try {
            Interceptor.Factory.wrap(handler);
            Assert.fail();
        } catch (Throwable e) {
            Assert.assertEquals(IllegalAccessError.class, e.getClass());
        }
    }
    
    
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Interceptor(InterceptorWhoseConstructorThrowsCheckedException.Impl.class)
    public @interface InterceptorWhoseConstructorThrowsCheckedException {
        public static class Impl extends Interceptor.Impl<HttpRequest, HttpResponse, InterceptorWhoseConstructorThrowsCheckedException> {
            public Impl() throws ParseException {
                throw new ParseException("parser error", 1);
            }
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse();
            }
        }
    }
    
    @Test
    public void testThatThrowCheckedExceptionWhenInterceptorThrowsError() {
        HttpRequestHandler handler = new HttpRequestHandler() {
            @InterceptorWhoseConstructorThrowsCheckedException
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse();
            }
        };
        
        try {
            Interceptor.Factory.wrap(handler);
            Assert.fail();
        } catch (Throwable e) {
            Assert.assertEquals(RuntimeException.class, e.getClass());
            Assert.assertEquals(ParseException.class, e.getCause()
                    .getClass());
        }
    }
    
    @Test
    public void testWrapByHttpMethodBinder() {
        // HttpMethodBinderによる委譲
        Object action = new Object() {
            @SayHoge
            @SayFuga
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse().write("<p>Hello world</p>");
            }
        };
        HttpRequestHandler handler = new HttpMethodBinding(action);

        HttpRequest req = new MockHttpRequest("GET /index.html HTTP/1.1");
        ExecutionContext ctx = new ExecutionContext();
        
        Assert.assertEquals(
                "<p>Hello world</p>\nSay hoge!\nSay fuga!",
                handler.handle(req, ctx)
                        .getBodyString()
        );
    }
    
    public void codeSampleCheck() {
        Handler<HttpRequest, HttpResponse> handler = new Interceptor.Impl<HttpRequest, HttpResponse, Annotation>() {
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                try {
                    doBeforeAdvice(); // インターセプタによる前処理
                    return getOriginalHandler().handle(req, ctx); // 本処理
                    
                } catch(RuntimeException e) {
                   doErrorHandling();  // インターセプタによる例外ハンドリング
                   throw e;
                   
                } finally {
                    doAfterAdvice();  // インターセプタによる終端処理
                }
            }
            
            public void doBeforeAdvice() {}
            public void doErrorHandling() {}
            public void doAfterAdvice() {}
        };
        
        Handler wrapped = Interceptor.Factory.wrap(handler);
    }

    @Test
    public void testGetDelegates() {
        Interceptor.Impl<Object, Object, AroundAdvice> impl = new Interceptor.Impl<Object, Object, AroundAdvice>() {
            @Override
            public Object handle(Object o, ExecutionContext context) {
                return null;
            }
        };

        assertThat(impl.getDelegates(null, null).size(), is(0));

        impl.setOriginalHandler(new Handler<Object, Object>() {
            @Override
            public Object handle(Object o, ExecutionContext context) {
                return null;
            }
        });

        assertThat(impl.getDelegates(null, null).size(), is(1));
    }

    /**
     * インターセプタの実行順を定義した場合、その順に実行されること
     */
    @Test
    public void testInterceptorOrders() throws Exception {
        final XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/fw/web/interceptorOrder.xml");
        SystemRepository.load(new DiContainer(loader));

        final Handler<String, String> handler = new Handler<String, String>() {
            @Override
            @BraceDecorator
            @CustomDecorator(c = '!')
            @HyphenDecorator
            @AsteriskDecorator
            public String handle(String input, ExecutionContext context) {
                return input;
            }
        };
        Handler<String, String> wrapHandler = Interceptor.Factory.wrap(handler);
        String result = wrapHandler.handle("input", new ExecutionContext());
        assertThat("想定した順序で実行されること", result, is("!-*[input]*-!"));


        // -------------------------------------------------------------------------------- 実行順を変更
        // ※複数の定義がある場合、より上位の順序が優先される
        SystemRepository.clear();
        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                final Map<String, Object> objects = new HashMap<String, Object>();
                objects.put("interceptorsOrder", new ArrayList<String>() {{
                    add(AsteriskDecorator.class.getName());
                    add(BraceDecorator.class.getName());
                    add(CustomDecorator.class.getName());
                    add(HyphenDecorator.class.getName());
                    add(AsteriskDecorator.class.getName());
                }});
                return objects;
            }
        });
        wrapHandler = Interceptor.Factory.wrap(handler);
        result = wrapHandler.handle("input", new ExecutionContext());
        assertThat("想定した順序で実行されること", result, is("-![*input*]!-"));
    }

    /**
     * 実行順に定義されていないインターセプタを使用した場合、実行時例外が送出されること。
     */
    @Test
    public void testInterceptorOrdersUnknownInterceptor() throws Exception {
        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                final Map<String, Object> objects = new HashMap<String, Object>();
                objects.put("interceptorsOrder", new ArrayList<String>() {{
                    add(AsteriskDecorator.class.getName());
                    add(BraceDecorator.class.getName());
                }});
                return objects;
            }
        });

        final Handler<String, String> handler = new Handler<String, String>() {
            @Override
            @HyphenDecorator
            public String handle(String input, ExecutionContext context) {
                return input;
            }
        };

        try {
            Interceptor.Factory.wrap(handler);
            fail();
        } catch (IllegalArgumentException e) {
            // Java 21での検証時に内部クラスのオブジェクト文字列表現が $内部クラス から .内部クラス（&HyphenDecoratorから.HyphenDecorator）に変わっていたため、バージョンに影響を受けないようにクラス名だけで検証する。
            assertThat(e.getMessage(), startsWith("interceptor is undefined in the interceptorsOrder."
                + " undefined interceptors="));

            String interceptorName = e.getMessage().replace("interceptor is undefined in the interceptorsOrder."
                + " undefined interceptors=", "");
            assertThat(interceptorName, containsString(HyphenDecorator.class.getSimpleName()));
        }
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Interceptor(BraceDecorator.Impl.class)
    public @interface BraceDecorator {
        public static class Impl extends Interceptor.Impl<String, String, BraceDecorator> {
            @Override
            public String handle(String s, ExecutionContext context) {
                return getOriginalHandler().handle('[' + s + ']', context);
            }
        }
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Interceptor(HyphenDecorator.Impl.class)
    public @interface HyphenDecorator {
        public static class Impl extends Interceptor.Impl<String, String, HyphenDecorator> {
            @Override
            public String handle(String s, ExecutionContext context) {
                return getOriginalHandler().handle('-' + s + '-', context);
            }
        }
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Interceptor(AsteriskDecorator.Impl.class)
    public @interface AsteriskDecorator {
        public static class Impl extends Interceptor.Impl<String, String, AsteriskDecorator> {
            @Override
            public String handle(String s, ExecutionContext context) {
                return getOriginalHandler().handle('*' + s + '*', context);
            }
        }
    }


    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Interceptor(CustomDecorator.Impl.class)
    public @interface CustomDecorator {
        char c();
        public static class Impl extends Interceptor.Impl<String, String, CustomDecorator> {
            @Override
            public String handle(String s, ExecutionContext context) {
                final CustomDecorator interceptor = getInterceptor();
                final char c = interceptor.c();
                return getOriginalHandler().handle(c + s + c, context);
            }
        }
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Interceptor(AroundAdvice.Impl.class)
    public @interface AroundAdvice {
        public static class Impl extends Interceptor.Impl<HttpRequest, HttpResponse, AroundAdvice> {
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                doBeforeAdvice(req, ctx);
                HttpResponse res =  getOriginalHandler().handle(req, ctx);
                doAfterAdvice(req, ctx);
                return res;
            }
            void doBeforeAdvice(HttpRequest req, ExecutionContext ctx) {
                //......
            }
            void doAfterAdvice(HttpRequest req, ExecutionContext ctx) {
                //......
           }
        }
    }
}
