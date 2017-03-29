package nablarch.fw.web.handler;

import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.ComponentDefinitionLoader;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.fw.ExecutionContext;
import nablarch.fw.RequestHandlerEntry;
import nablarch.fw.handler.JavaPackageMappingEntry;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.MockHttpRequest;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/**
 * HttpRequestJavaPackageMappingのテスト。
 * @author Masato Inoue
 */
public class HttpRequestJavaPackageMappingTest {

    /**
     * mappingEntriesプロパティにリクエストパスのパターン文字列と マッピング先Javaパッケージの関連を保持するクラスを設定することにより、
     * 単一のJavaPackageMappingEntryで複数のリクエストパスへのディスパッチおよび、デフォルトパスへのマッピングへのディスパッチが可能になることのテスト。
     */
    @Test
    public void testMappingEntries() {
        HttpRequestJavaPackageMapping mapping = new HttpRequestJavaPackageMapping();

        @SuppressWarnings("serial")
        ArrayList<JavaPackageMappingEntry> list = 
            new ArrayList<JavaPackageMappingEntry>() {{
                add(new JavaPackageMappingEntry()
                    .setRequestPattern("/action/ss00A001/W11AC001Action/R0001*")
                    .setBasePackage("nablarch.fw.handler.dispatch.test1"));
                add(new JavaPackageMappingEntry()
                    .setRequestPattern("/action/ss00A001/W11AC001Action//")
                    .setBasePackage("nablarch.fw.handler.dispatch.test2"));
                add(new JavaPackageMappingEntry()
                    .setRequestPattern("/action/ss00A001//")
                    .setBasePackage("nablarch.fw.handler.dispatch.test3"));
                add(new JavaPackageMappingEntry()
                    .setRequestPattern("/action/ss00A002/test4/W11AC001")
                    .setBasePackage("nablarch.fw.handler.dispatch.test4.ss00A001.W11AC001Action"));
            }};
        
        mapping.setOptionalPackageMappingEntries(list);
        mapping.setBasePackage("nablarch.fw.handler.dispatch.base"); // mapping entryに合致しない場合に適用されるベースパス
        mapping.setBasePath("/action/");
        
        ExecutionContext ctx = new ExecutionContext();
        
        // nablarch.fw.handler.dispatch.test1.W11AC001Actionへのマッピング（リクエストIDでのマッピング）
        HttpRequest request = new MockHttpRequest().setRequestPath("/action/ss00A001/W11AC001Action/R00010000");
        mapping.handle(request, ctx);
        assertEquals("test1.W11AC001Action", ctx.getRequestScopedVar("executeAction"));

        // nablarch.fw.handler.dispatch.test2.W11AC001Actionへのマッピング（パッケージでのマッピング）
        request = new MockHttpRequest().setRequestPath("/action/ss00A001/W11AC001Action/R00020000");
        mapping.handle(request, ctx);
        assertEquals("test2.W11AC001Action", ctx.getRequestScopedVar("executeAction"));

        // nablarch.fw.handler.dispatch.test3.W11AC002Actionへのマッピング（パッケージでのマッピング）
        request = new MockHttpRequest().setRequestPath("/action/ss00A001/W11AC002Action");
        mapping.handle(request, ctx);
        assertEquals("test3.W11AC002Action", ctx.getRequestScopedVar("executeAction"));

        // nablarch.fw.handler.dispatch.test4.W11AC001Actionへのマッピング（パッケージでのマッピング + ベースパスがActionの完全修飾名）
        request = new MockHttpRequest().setRequestPath("/action/ss00A002/test4/W11AC001");
        mapping.handle(request, ctx);
        assertEquals("test4.W11AC001Action", ctx.getRequestScopedVar("executeAction"));

        // nablarch.fw.handler.dispatch.base.W11AC001Actionへのマッピング（どのエントリにも合致しないのでベースパスにディスパッチされる）
        request = new MockHttpRequest().setRequestPath("/action/ss00A002/W11AC001Action");
        mapping.handle(request, ctx);
        assertEquals("base.W11AC001Action", ctx.getRequestScopedVar("executeAction"));
        
    }

    
    /**
     * 設定ファイルのテスト。設定ファイルの内容は{@link #testMappingEntries()}のテストと全く同じ。<br/>
     * RequestHandlerEntryとHttpRequestJavaPackageMappingを結合して使用し、
     * 正常に動作することを確認する。
     */
    @Test
    public void testConfig() {
        
        // テスト用のリポジトリ構築
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/fw/handler/dispatch/config/entryWeb.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
        
        RequestHandlerEntry<HttpRequest, HttpResponse> entry = SystemRepository.get("webEntry");

        ExecutionContext ctx = new ExecutionContext();
        
        // nablarch.fw.handler.dispatch.test1.W11AC001Actionへのマッピング（リクエストIDでのマッピング）
        HttpRequest request = new MockHttpRequest().setRequestPath("/action/ss00A001/W11AC001Action/R00010000");
        entry.handle(request, ctx);
        assertEquals("test1.W11AC001Action", ctx.getRequestScopedVar("executeAction"));

        // nablarch.fw.handler.dispatch.test2.W11AC001Actionへのマッピング（パッケージでのマッピング）
        request = new MockHttpRequest().setRequestPath("/action/ss00A001/W11AC001Action/R00020000");
        entry.handle(request, ctx);
        assertEquals("test2.W11AC001Action", ctx.getRequestScopedVar("executeAction"));

        // nablarch.fw.handler.dispatch.test3.W11AC002Actionへのマッピング（パッケージでのマッピング）
        request = new MockHttpRequest().setRequestPath("/action/ss00A001/W11AC002Action");
        entry.handle(request, ctx);
        assertEquals("test3.W11AC002Action", ctx.getRequestScopedVar("executeAction"));

        // nablarch.fw.handler.dispatch.test4.W11AC001Actionへのマッピング（パッケージでのマッピング + ベースパスがActionの完全修飾名）
        request = new MockHttpRequest().setRequestPath("/action/ss00A002/test4/W11AC001");
        entry.handle(request, ctx);
        assertEquals("test4.W11AC001Action", ctx.getRequestScopedVar("executeAction"));

        // nablarch.fw.handler.dispatch.base.W11AC001Actionへのマッピング（どのエントリにも合致しないのでベースパスにディスパッチされる）
        request = new MockHttpRequest().setRequestPath("/action/ss00A002/W11AC001Action");
        entry.handle(request, ctx);
        assertEquals("base.W11AC001Action", ctx.getRequestScopedVar("executeAction"));
        
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetInvalidBasePath() {
        HttpRequestJavaPackageMapping packageMapping = new HttpRequestJavaPackageMapping();
        packageMapping.setBasePath("://");
    }
    
}
