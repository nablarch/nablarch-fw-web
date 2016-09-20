package nablarch.common.web.session.store;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import mockit.Mocked;
import nablarch.common.encryption.Encryptor;
import nablarch.common.web.session.MockHttpServletRequest;
import nablarch.common.web.session.SessionEntry;
import nablarch.common.web.session.encoder.JavaSerializeEncryptStateEncoder;
import nablarch.common.web.session.encoder.JavaSerializeStateEncoder;
import nablarch.common.web.session.encoder.JaxbStateEncoder;
import nablarch.core.util.StringUtil;
import nablarch.fw.web.servlet.ServletExecutionContext;

import org.junit.Test;

/**
 * {@link HiddenStore}のテスト。
 * @author Kiyohito Itoh
 */
public class HiddenStoreTest {

    @Mocked
    private ServletContext unusedHttpContext;

    @Mocked
    private HttpServletResponse unusedHttpResponse;

    /**
     * デフォルト設定で動作すること。
     */
    @Test
    public void testDefaultSettings() {

        HiddenStore store = new HiddenStore();
        store.setStateEncoder(new JavaSerializeStateEncoder());

        List<SessionEntry> inEntries = Arrays.asList(
                new SessionEntry("key1", "val1", store),
                new SessionEntry("key2", "val2", store),
                new SessionEntry("key3", "val3", store));

        String unusedId = createSessionId();
        ServletExecutionContext outCtxt = createExeCtxt();
        store.save(unusedId, inEntries, outCtxt);

        String value = outCtxt.getRequestScopedVar("nablarch_hiddenStore");
        System.out.println(value);
        assertNotNull(value);

        ServletExecutionContext inCtxt = createExeCtxt();
        inCtxt.getServletRequest().getParameterMap().put(
                "nablarch_hiddenStore", new String[] {value});
        List<SessionEntry> outEntries = store.load(unusedId, inCtxt);

        assertThat(outEntries.size(), is(3));
        Collections.sort(outEntries, keySort);
        int i = 1;
        for (SessionEntry outEntry : outEntries) {
            int num = i++;
            assertThat(outEntry.getKey(), is("key" + num));
            assertThat(outEntry.getValue().toString(), is("val" + num));
        }
    }
    
    /**
     * {@link JavaSerializeEncryptStateEncoder}を設定して動作すること。
     */
    @Test
    public void testEncryptSettings() {

        HiddenStore store = new HiddenStore();
        store.setStateEncoder(new JavaSerializeEncryptStateEncoder<Serializable>());

        List<SessionEntry> inEntries = Arrays.asList(
                new SessionEntry("key1", "val1", store),
                new SessionEntry("key2", "val2", store),
                new SessionEntry("key3", "val3", store));

        String unusedId = createSessionId();
        ServletExecutionContext outCtxt = createExeCtxt();
        store.save(unusedId, inEntries, outCtxt);

        String value = outCtxt.getRequestScopedVar("nablarch_hiddenStore");
        System.out.println(value);
        assertNotNull(value);

        ServletExecutionContext inCtxt = createExeCtxt();
        inCtxt.getServletRequest().getParameterMap().put(
                "nablarch_hiddenStore", new String[] {value});
        List<SessionEntry> outEntries = store.load(unusedId, inCtxt);

        assertThat(outEntries.size(), is(3));
        Collections.sort(outEntries, keySort);
        int i = 1;
        for (SessionEntry outEntry : outEntries) {
            int num = i++;
            assertThat(outEntry.getKey(), is("key" + num));
            assertThat(outEntry.getValue().toString(), is("val" + num));
        }
    }

    /**
     * 設定をカスタマイズして動作すること。
     */
    @Test
    public void testCustomSettings() {

        HiddenStore store = new HiddenStore();
        JaxbStateEncoder encoder = new JaxbStateEncoder();
        store.setStateEncoder(encoder);
        store.setParameterName("_HIDDEN_STORE_");

        List<SessionEntry> inEntries = Arrays.asList(
                new SessionEntry("key1", "val1", store),
                new SessionEntry("key2", "val2", store),
                new SessionEntry("key3", "val3", store));

        String unusedId = createSessionId();
        ServletExecutionContext outCtxt = createExeCtxt();
        store.save(unusedId, inEntries, outCtxt);

        String value = outCtxt.getRequestScopedVar("_HIDDEN_STORE_");
        System.out.println(value);
        assertNotNull(value);

        ServletExecutionContext inCtxt = createExeCtxt();
        inCtxt.getServletRequest().getParameterMap().put(
                "_HIDDEN_STORE_", new String[] {value});
        List<SessionEntry> outEntries = store.load(unusedId, inCtxt);

        assertThat(outEntries.size(), is(3));
        Collections.sort(outEntries, keySort);
        int i = 1;
        for (SessionEntry outEntry : outEntries) {
            int num = i++;
            assertThat(outEntry.getKey(), is("key" + num));
            assertThat(outEntry.getValue().toString(), is("val" + num));
        }
    }

    /**
     * 様々な入力値に対するロードのテスト。
     */
    @Test
    public void testLoad() {

        HiddenStore store = new HiddenStore();
        store.setStateEncoder(new JavaSerializeStateEncoder());

        String unusedId = createSessionId();

        ServletExecutionContext inCtxt;
        List<SessionEntry> outEntries;

        // パラメータなしの場合、空リストがロードされること。
        inCtxt = createExeCtxt();
        assertNull(inCtxt.getServletRequest().getParameter("nablarch_hiddenStore"));
        outEntries = store.load(unusedId, inCtxt);
        assertThat(outEntries.size(), is(0));

        // パラメータの値が空の場合、空リストがロードされること。
        inCtxt = createExeCtxt();
        inCtxt.getServletRequest().getParameterMap().put("nablarch_hiddenStore", new String[] {""});
        assertThat(inCtxt.getServletRequest().getParameter("nablarch_hiddenStore"), is(""));
        outEntries = store.load(unusedId, inCtxt);
        assertThat(outEntries.size(), is(0));
    }

    /**
     * 様々な入力値に対する保存のテスト。
     */
    @Test
    public void testSave() {

        HiddenStore store = new HiddenStore();
        store.setStateEncoder(new JavaSerializeStateEncoder());

        String unusedId = createSessionId();

        ServletExecutionContext outCtxt;
        List<SessionEntry> inEntries;

        // エントリが空の場合
        inEntries = Collections.emptyList();
        outCtxt = createExeCtxt();
        store.save(unusedId, inEntries, outCtxt);
        assertThat(outCtxt.getRequestScopedVar("nablarch_hiddenStore"), is(notNullValue()));
    }

    /**
     * delete呼び出しで、セッションの内容が削除されること。
     */
    @Test
    public void testDelete() {

        HiddenStore sut = new HiddenStore();
        sut.setParameterName("test-invalidate");

        final String unusedId = "unused";
        final ServletExecutionContext context = createExeCtxt();
        context.setRequestScopedVar("test-invalidate", "test-value");

        sut.delete(unusedId, context);

        assertThat(context.getRequestScopedVar("test-invalidate"), is(nullValue()));
    }

    /**
     * invalidate呼び出しで、セッションの内容が削除されること。
     */
    @Test
    public void testInvalidate() {

        HiddenStore sut = new HiddenStore();
        sut.setParameterName("test-invalidate");

        final String unusedId = "unused";
        final ServletExecutionContext context = createExeCtxt();
        context.setRequestScopedVar("test-invalidate", "test-value");

        sut.invalidate(unusedId, context);

        assertThat(context.getRequestScopedVar("test-invalidate"), is(nullValue()));
    }

    private static Comparator<SessionEntry> keySort = new Comparator<SessionEntry>() {
        @Override
        public int compare(SessionEntry o1, SessionEntry o2) {
            return o1.getKey().compareTo(o2.getKey());
        }
    };

    private ServletExecutionContext createExeCtxt() {
        return new ServletExecutionContext(
                new MockHttpServletRequest().getMockInstance(),
                unusedHttpResponse, unusedHttpContext);
    }

    private static final Charset utf8 = Charset.forName("UTF-8");

    private static Encryptor<Serializable> customEnc = new Encryptor<Serializable>() {
        @Override
        public Serializable generateContext() {
            return "enc-gen-ctxt";
        }
        @Override
        public byte[] encrypt(Serializable context, byte[] src)
                throws IllegalArgumentException {
            assertThat(context.toString(), is("enc-gen-ctxt"));
            String str = new String(src, utf8);
            System.out.println(str);
            return ("***" + str + "***").getBytes(utf8);
        }
        @Override
        public byte[] decrypt(Serializable context, byte[] src)
                throws IllegalArgumentException {
            assertThat(context.toString(), is("enc-gen-ctxt"));
            String str = new String(src, utf8);
            System.out.println(str);
            return (str.substring(3, str.length() - 3)).getBytes(utf8);
        }
    };
    
    private String createSessionId() {
        String uuid = UUID.randomUUID().toString();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.reset();
            md.update(StringUtil.getBytes(uuid, Charset.forName("UTF-8")));
            
            byte[] digest = md.digest();
            
            StringBuilder sb = new StringBuilder();
            int length = digest.length;
            for (int i = 0; i < length; i++) {
                int b = (0xFF & digest[i]);
                  if (b < 16) {
                      sb.append("0");
                  }
                sb.append(Integer.toHexString(b));
            }
            return uuid + sb;
        } catch (NoSuchAlgorithmException e) {
            // NOP
        }
        return null;
    }
}
