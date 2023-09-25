package nablarch.fw.web.servlet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import nablarch.core.log.CustomClassLoader;
import nablarch.core.log.LogTestSupport;
import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.ConfigurationLoadException;
import nablarch.core.repository.di.ContainerProcessException;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.fw.web.servlet.staticprop.Bar;
import nablarch.fw.web.servlet.staticprop.Foo;
import nablarch.test.support.log.app.OnMemoryLogWriter;
import nablarch.test.support.tool.Hereis;

import org.junit.Test;

/**
 * {@link NablarchServletContextListener}のテストクラス。
 * 
 * @author Kiyohito Itoh
 */
public class NablarchServletContextListenerTest extends LogTestSupport {
    
    private void clear() {
        SystemRepository.clear();
        OnMemoryLogWriter.clear();
        Foo.setBar(null);
    }
    
    /**
     * 設定に応じてリポジトリが初期化されること。(classpath指定)
     */
    @Test
    public void testRepositoryInitializationForClasspathConfig() {
        
        // 設定値の上書き時の動作設定を指定しない場合
        // コンポーネント定義を重複させた設定ファイルを指定する。
        
        MockServletContext ctx = new MockServletContext();
        ctx.getInitParams().put("di.config", "classpath:nablarch/fw/web/servlet/nablarch-servlet-context-duplication-test.xml");
        ServletContextEvent ctxEvt = new ServletContextEvent(ctx);
        ServletContextListener listener = new NablarchServletContextListener();
        
        clear();
        listener.contextInitialized(ctxEvt);
        
        Object obj = SystemRepository.getObject("book");
        assertThat(((Book) obj).getName(), is("Nablarch入門Vol2"));
        
        // 設定値の上書き時の動作設定にOVERRIDEを指定した場合
        // コンポーネント定義を重複させた設定ファイルを指定する。
        
        ctx = new MockServletContext();
        ctx.getInitParams().put("di.config", "classpath:nablarch/fw/web/servlet/nablarch-servlet-context-duplication-test.xml");
        ctx.getInitParams().put("di.duplicate-definition-policy", "OVERRIDE");
        ctxEvt = new ServletContextEvent(ctx);
        listener = new NablarchServletContextListener();
        
        clear();
        listener.contextInitialized(ctxEvt);
        
        obj = SystemRepository.getObject("book");
        assertThat(((Book) obj).getName(), is("Nablarch入門Vol2"));
        
        // 設定値の上書き時の動作設定にDENYを指定した場合
        // コンポーネント定義を重複させた設定ファイルを指定する。
        
        ctx = new MockServletContext();
        ctx.getInitParams().put("di.config", "classpath:nablarch/fw/web/servlet/nablarch-servlet-context-duplication-test.xml");
        ctx.getInitParams().put("di.duplicate-definition-policy", "DENY");
        ctxEvt = new ServletContextEvent(ctx);
        listener = new NablarchServletContextListener();
        
        clear();
        try {
            listener.contextInitialized(ctxEvt);
            fail("設定値の上書き時の動作設定にDENYを指定した場合");
        } catch (ConfigurationLoadException e) {
            assertThat(e.getMessage(), is("file processing failed. file = classpath:nablarch/fw/web/servlet/nablarch-servlet-context-duplication-test.xml"));
            assertThat(e.getCause().getMessage(), is("component name was duplicated. name = book"));
        }
        
        // 設定値の上書き時の動作設定にDENYを指定した場合
        // コンポーネント定義が重複していない設定ファイルを指定する。
        
        ctx = new MockServletContext();
        ctx.getInitParams().put("di.config", "classpath:nablarch/fw/web/servlet/nablarch-servlet-context-test.xml");
        ctx.getInitParams().put("di.duplicate-definition-policy", "DENY");
        ctxEvt = new ServletContextEvent(ctx);
        listener = new NablarchServletContextListener();
        
        clear();
        listener.contextInitialized(ctxEvt);
        
        obj = SystemRepository.getObject("book");
        assertThat(((Book) obj).getName(), is("Nablarch入門"));
        
        // 設定値の上書き時の動作設定に規定していない動作ポリシー名が指定された場合

        ctx = new MockServletContext();
        ctx.getInitParams().put("di.config", "classpath:nablarch/fw/web/servlet/nablarch-servlet-context-test.xml");
        ctx.getInitParams().put("di.duplicate-definition-policy", "SILENCE");
        ctxEvt = new ServletContextEvent(ctx);
        listener = new NablarchServletContextListener();
        
        clear();
        try {
            listener.contextInitialized(ctxEvt);
            fail("設定値の上書き時の動作設定に規定していない動作ポリシー名が指定された場合");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Illegal duplicate definition policy was specified.di.duplicate-definition-policy = SILENCE"));
        }
        
        // リクエスト単体テストの場合(コンポーネント名「httpTestConfiguration」が含まれる場合)
        
        clear();
        String pathHttpTestConfig = "classpath:nablarch/fw/web/servlet/nablarch-servlet-context-requesttest-test.xml";
        SystemRepository.load(new DiContainer(new XmlComponentDefinitionLoader(pathHttpTestConfig)));
        
        ctx = new MockServletContext();
        ctx.getInitParams().put("di.config", "classpath:nablarch/fw/web/servlet/nablarch-servlet-context-test.xml");
        ctxEvt = new ServletContextEvent(ctx);
        listener = new NablarchServletContextListener();
        
        listener.contextInitialized(ctxEvt);
        
        obj = SystemRepository.getObject("httpTestConfiguration");
        assertNotNull(obj);
        obj = SystemRepository.getObject("book");
        assertNull(obj);

        // リクエスト単体テストの場合(コンポーネント名「restTestConfiguration」が含まれる場合)

        clear();
        String pathRestTestConfig = "classpath:nablarch/fw/web/servlet/nablarch-servlet-context-restrequesttest-test.xml";
        SystemRepository.load(new DiContainer(new XmlComponentDefinitionLoader(pathRestTestConfig)));

        ctx = new MockServletContext();
        ctx.getInitParams().put("di.config", "classpath:nablarch/fw/web/servlet/nablarch-servlet-context-test.xml");
        ctxEvt = new ServletContextEvent(ctx);
        listener = new NablarchServletContextListener();

        listener.contextInitialized(ctxEvt);

        obj = SystemRepository.getObject("restTestConfiguration");
        assertNotNull(obj);
        obj = SystemRepository.getObject("book");
        assertNull(obj);
    }
    
    /**
     * 設定に応じてリポジトリが初期化されること。(file指定)
     * @throws IOException IOException
     */
    @Test
    public void testRepositoryInitializationForFileConfig() throws IOException {
        
        File file = File.createTempFile("test", ".xml");
        file.deleteOnExit();

        Hereis.file(file.getAbsolutePath());
        /*
        <?xml version="1.0" encoding="UTF-8"?>
        <component-configuration xmlns="http://tis.co.jp/nablarch/component-configuration"
                                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        
            <component name="book" class="nablarch.fw.web.servlet.NablarchServletContextListenerTest$Book">
                <property name="name" value="Nablarch入門" />
            </component>
            
        </component-configuration>*/
        
        // 設定値の上書き時の動作設定にDENYを指定した場合
        // コンポーネント定義が重複していない設定ファイルを指定する。
        
        MockServletContext ctx = new MockServletContext();
        ctx.getInitParams().put("di.config", file.toURI().toString());
        ctx.getInitParams().put("di.duplicate-definition-policy", "DENY");
        ServletContextEvent ctxEvt = new ServletContextEvent(ctx);
        NablarchServletContextListener listener = new NablarchServletContextListener();
        
        clear();
        listener.contextInitialized(ctxEvt);
        
        Object obj = SystemRepository.getObject("book");
        assertThat(((Book) obj).getName(), is("Nablarch入門"));
    }
    
    /**
     * ログが出力されること。
     */
    @Test
    public void testLogging() {
        
        ClassLoader defaultCL = Thread.currentThread().getContextClassLoader();
        ClassLoader customCL1 = new CustomClassLoader(defaultCL);
        
        Thread.currentThread().setContextClassLoader(customCL1);
        
        LoggerManager.get(this.getClass()); // ログの初期化
        
        // 初期処理と終了処理で例外が発生せずに処理した場合
        
        MockServletContext ctx = new MockServletContext();
        ctx.getInitParams().put("di.config", "classpath:nablarch/fw/web/servlet/nablarch-servlet-context-test.xml");
        ServletContextEvent ctxEvt = new ServletContextEvent(ctx);
        ServletContextListener listener = new NablarchServletContextListener();
        
        clear();
        listener.contextInitialized(ctxEvt);
        
        assertThat(OnMemoryLogWriter.getMessages("writer.appLog").toString(),
                   containsString("[nablarch.fw.web.servlet.NablarchServletContextListener#contextInitialized] initialization completed."));
        
        OnMemoryLogWriter.clear();
        listener.contextDestroyed(ctxEvt);
        
        String actualLog = OnMemoryLogWriter.getMessages("writer.appLog").toString().replace(Logger.LS, "");
        
        assertThat(actualLog, containsString("nablarch.fw.web.servlet.NablarchServletContextListener#contextDestroyed"));
        
        // OnMemoryLogWriter#onTerminateで出力しているログのアサート
        assertThat("ログの終了処理が起動されていること", actualLog, containsString("@@@END@@@"));
        
        // 初期処理で例外が発生した場合
        
        ctx = new MockServletContext();
        ctx.getInitParams().put("di.config", "classpath:nablarch/fw/web/servlet/nablarch-servlet-context-duplication-test.xml");
        ctx.getInitParams().put("di.duplicate-definition-policy", "DENY");
        ctxEvt = new ServletContextEvent(ctx);
        listener = new NablarchServletContextListener();
        
        clear();
        try {
            listener.contextInitialized(ctxEvt);
            fail("初期処理で例外が発生した場合");
        } catch (ConfigurationLoadException e) {
            assertThat(e.getMessage(), is("file processing failed. file = classpath:nablarch/fw/web/servlet/nablarch-servlet-context-duplication-test.xml"));
            assertThat(e.getCause().getMessage(), is("component name was duplicated. name = book"));
        }
        
        actualLog = OnMemoryLogWriter.getMessages("writer.appLog").toString().replace(Logger.LS, "");
        
        assertThat(actualLog, not(containsString("nablarch.fw.web.servlet.NablarchServletContextListener#contextDestroyed")));
        assertThat(actualLog, not(containsString("@@@END@@@")));
        assertThat(actualLog, containsString("FATAL"));
        
        actualLog = OnMemoryLogWriter.getMessages("writer.appLog").toString().replace(Logger.LS, "");
        
        assertThat(actualLog, containsString("FATAL"));
        
        Thread.currentThread().setContextClassLoader(defaultCL);
    }

    /** 初期パラメータで、staticプロパティへのインジェクションを許可した場合、staticプロパティにインジェクションが行われること。 */
    @Test
    public void testAllowStaticProperty() {
        MockServletContext ctx = new MockServletContext();
        ctx.getInitParams().put("di.config", "classpath:nablarch/fw/web/servlet/staticprop/static-property-injection-autowire.xml");
        ctx.getInitParams().put("di.allow-static-property", "true");
        ServletContextEvent ctxEvt = new ServletContextEvent(ctx);
        ServletContextListener listener = new NablarchServletContextListener();

        clear();
        listener.contextInitialized(ctxEvt);

        Bar bar = SystemRepository.get("bar");
        assertThat(Foo.getBar(), is(sameInstance(bar)));
    }

    /** デフォルトでは、staticプロパティにインジェクションが行われず例外が発生すること。 */
    @Test
    public void testDisallowStaticPropertyByDefault() {
        MockServletContext ctx = new MockServletContext();
        ctx.getInitParams().put("di.config", "classpath:nablarch/fw/web/servlet/staticprop/static-property-injection-autowire.xml");

        ServletContextEvent ctxEvt = new ServletContextEvent(ctx);
        ServletContextListener listener = new NablarchServletContextListener();

        clear();
        try {
            listener.contextInitialized(ctxEvt);
            fail("staticを許容しない場合、staticプロパティインジェクション時に例外が発生しなければならない");
        } catch (ContainerProcessException e) {
            assertThat(e.getMessage(),
                       is("static property injection not allowed. component=[foo] property=[bar]"));
        }
    }

    /**
     * contextDestroyed() のときに {@link nablarch.core.repository.disposal.ApplicationDisposer} が実行されること。
     */
    @Test
    public void testApplicationDisposerIsInvokedAtContextDestroyed() {
        MockServletContext ctx = new MockServletContext();
        ctx.getInitParams().put("di.config", "classpath:nablarch/fw/web/servlet/nablarch-application-disposer-is-invoked-test.xml");
        ServletContextEvent ctxEvt = new ServletContextEvent(ctx);
        ServletContextListener listener = new NablarchServletContextListener();

        clear();
        listener.contextInitialized(ctxEvt);

        MockDisposable disposable1 = SystemRepository.get("disposable1");
        MockDisposable disposable2 = SystemRepository.get("disposable2");
        MockDisposable disposable3 = SystemRepository.get("disposable3");

        listener.contextDestroyed(ctxEvt);

        assertThat(disposable1.isDisposed(), is(true));
        assertThat(disposable2.isDisposed(), is(true));
        assertThat(disposable3.isDisposed(), is(true));
    }

    /**
     * {@link nablarch.core.repository.disposal.ApplicationDisposer} のコンポーネントが存在するときだけ、
     * 廃棄処理が実行されること。
     */
    @Test
    public void testApplicationDisposerIsInvokedOnlyExistsComponent() {
        MockServletContext ctx = new MockServletContext();
        ctx.getInitParams().put("di.config", "classpath:nablarch/fw/web/servlet/nablarch-application-disposer-not-exists-test.xml");
        ServletContextEvent ctxEvt = new ServletContextEvent(ctx);
        ServletContextListener listener = new NablarchServletContextListener();

        clear();
        listener.contextInitialized(ctxEvt);

        MockDisposable disposable1 = SystemRepository.get("disposable1");
        MockDisposable disposable2 = SystemRepository.get("disposable2");
        MockDisposable disposable3 = SystemRepository.get("disposable3");

        listener.contextDestroyed(ctxEvt);

        assertThat(disposable1.isDisposed(), is(false));
        assertThat(disposable2.isDisposed(), is(false));
        assertThat(disposable3.isDisposed(), is(false));
    }

    /**
     * 初期化に成功している場合は{@link NablarchServletContextListener#isInitializationCompleted()}でtrueが返却されること
     */
    @Test
    public void testInitializationCompleted_success() {
        MockServletContext ctx = new MockServletContext();
        ctx.getInitParams().put("di.config", "classpath:nablarch/fw/web/servlet/nablarch-servlet-context-duplication-test.xml");
        ServletContextEvent ctxEvt = new ServletContextEvent(ctx);
        ServletContextListener listener = new NablarchServletContextListener();

        clear();
        // 初期前はfalseになること
        assertFalse(NablarchServletContextListener.isInitializationCompleted());

        listener.contextInitialized(ctxEvt);

        // 初期化に成功していること
        assertTrue(NablarchServletContextListener.isInitializationCompleted());
    }

    /**
     * 初期化に失敗している場合は{@link NablarchServletContextListener#isInitializationCompleted()}でfalseが返却されること
     */
    @Test
    public void testInitializationCompleted_failure() {
        MockServletContext ctx = new MockServletContext();
        ctx.getInitParams().put("di.config", "classpath:nablarch/fw/web/servlet/nablarch-servlet-context-duplication-test.xml");
        ctx.getInitParams().put("di.duplicate-definition-policy", "DENY");
        ServletContextEvent ctxEvt = new ServletContextEvent(ctx);
        ServletContextListener listener = new NablarchServletContextListener();

        clear();
        // 初期前はfalseになること
        assertFalse(NablarchServletContextListener.isInitializationCompleted());
        try {
            listener.contextInitialized(ctxEvt);
            fail("初期化に失敗するはず");
        } catch (ConfigurationLoadException e) {
            // 初期化失敗を検知できること
            assertFalse(NablarchServletContextListener.isInitializationCompleted());
        }
    }

    public static final class Book {
        private String name;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
    }
}
