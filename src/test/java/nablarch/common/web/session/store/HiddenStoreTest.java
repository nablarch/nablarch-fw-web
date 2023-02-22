package nablarch.common.web.session.store;

import nablarch.common.web.session.MockHttpServletRequest;
import mockit.Mocked;
import nablarch.common.encryption.AesEncryptor;
import nablarch.common.encryption.Encryptor;
import nablarch.common.web.session.EncodeException;
import nablarch.common.web.session.SessionEntry;
import nablarch.common.web.session.encoder.JavaSerializeEncryptStateEncoder;
import nablarch.common.web.session.encoder.JavaSerializeStateEncoder;
import nablarch.common.web.session.encoder.JaxbStateEncoder;
import nablarch.core.util.StringUtil;
import nablarch.fw.web.servlet.ServletExecutionContext;
import org.junit.Before;
import org.junit.Test;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.DatatypeConverter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * {@link HiddenStore}のテスト。
 *
 * 互換性を確認するため、5u13以前のHiddenStoreクラスのテストコードをそのまま流用している。
 * 5u14で独自に実装した部分のテストについては、本クラスの後半に記載している。
 *
 * @author Tsuyoshi Kawasaki
 */
public class HiddenStoreTest {

    @Mocked
    private ServletContext unusedHttpContext;

    @Mocked
    private HttpServletResponse unusedHttpResponse;

    private HiddenStore store;

    @Before
    public void setUp() {
        store = new HiddenStore();
        store.setStateEncoder(new JavaSerializeStateEncoder());
        store.setEncryptor(new AesEncryptor());
    }

    /**
     * デフォルト設定で動作すること。
     */
    @Test
    public void testDefaultSettings() {


        store.setStateEncoder(new JavaSerializeStateEncoder());

        List<SessionEntry> inEntries = Arrays.asList(
                new SessionEntry("key1", "val1", store),
                new SessionEntry("key2", "val2", store),
                new SessionEntry("key3", "val3", store),
                new SessionEntry("key4", "val\uD83C\uDF63\uD83C\uDF63\uD83C\uDF63", store));

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

        assertThat(outEntries.size(), is(4));
        Collections.sort(outEntries, keySort);
        for (int i = 0; i < outEntries.size(); i++) {
            final SessionEntry outEntry = outEntries.get(i);
            final SessionEntry inEntry = inEntries.get(i);
            assertThat(outEntry.getKey(), is(inEntry.getKey()));
            assertThat(outEntry.getValue().toString(), is(inEntry.getValue()));
        }
    }

    /**
     * {@link JavaSerializeEncryptStateEncoder}を設定して動作すること。
     */
    @Test
    public void testEncryptSettings() {


        store.setStateEncoder(new JavaSerializeEncryptStateEncoder());

        List<SessionEntry> inEntries = Arrays.asList(
                new SessionEntry("key1", "val1", store),
                new SessionEntry("key2", "val2", store),
                new SessionEntry("key3", "val3", store),
                new SessionEntry("key4", "\uD85A\uDE58\uD85A\uDE58\uD85A\uDE58", store));

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

        assertThat(outEntries.size(), is(4));
        Collections.sort(outEntries, keySort);
        for (int i = 0; i < outEntries.size(); i++) {
            final SessionEntry outEntry = outEntries.get(i);
            final SessionEntry inEntry = inEntries.get(i);
            assertThat(outEntry.getKey(), is(inEntry.getKey()));
            assertThat(outEntry.getValue().toString(), is(inEntry.getValue()));
        }
    }

    /**
     * 設定をカスタマイズして動作すること。
     */
    @Test
    public void testCustomSettings() {

        JaxbStateEncoder encoder = new JaxbStateEncoder();
        store.setStateEncoder(encoder);
        store.setParameterName("_HIDDEN_STORE_");

        List<SessionEntry> inEntries = Arrays.asList(
                new SessionEntry("key1", "val1", store),
                new SessionEntry("key2", "val2", store),
                new SessionEntry("key3", "val3", store),
                new SessionEntry("key4", "\uD866\uDCC6\uD866\uDCC6\uD866\uDCC6", store));

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

        assertThat(outEntries.size(), is(4));
        Collections.sort(outEntries, keySort);
        for (int i = 0; i < outEntries.size(); i++) {
            final SessionEntry outEntry = outEntries.get(i);
            final SessionEntry inEntry = inEntries.get(i);
            assertThat(outEntry.getKey(), is(inEntry.getKey()));
            assertThat(outEntry.getValue().toString(), is(inEntry.getValue()));
        }
    }

    /**
     * 様々な入力値に対するロードのテスト。
     */
    @Test
    public void testLoad() {

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

        store.setParameterName("test-invalidate");

        final String unusedId = "unused";
        final ServletExecutionContext context = createExeCtxt();
        context.setRequestScopedVar("test-invalidate", "test-value");

        store.delete(unusedId, context);

        assertThat(context.getRequestScopedVar("test-invalidate"), is(nullValue()));
    }

    /**
     * invalidate呼び出しで、セッションの内容が削除されること。
     */
    @Test
    public void testInvalidate() {

        store.setParameterName("test-invalidate");

        final String unusedId = "unused";
        final ServletExecutionContext context = createExeCtxt();
        context.setRequestScopedVar("test-invalidate", "test-value");

        store.invalidate(unusedId, context);

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

    //---- ここから5u14で新規に追加したテスト

    /** 保存時、データ全体に暗号化が施されること。*/
    @Test
    public void testEncryption() throws UnsupportedEncodingException {
        HiddenStore store = new HiddenStore();
        store.setStateEncoder(new JavaSerializeStateEncoder());
        // なんでもCAFEBABEに変換するEncryptor
        FixedBytesEncryptor fixedBytesEncryptor = new FixedBytesEncryptor();
        store.setEncryptor(fixedBytesEncryptor);

        List<SessionEntry> inEntries = Collections.singletonList(
                new SessionEntry("key1", "val1", store));

        String sessionId = createSessionId();
        ServletExecutionContext outCtxt = createExeCtxt();
        store.save(sessionId, inEntries, outCtxt);

        assertThat("暗号化部品が実行されていること", fixedBytesEncryptor.encryptionExecuted, is(true));

        String value = outCtxt.getRequestScopedVar("nablarch_hiddenStore");
        byte[] encryptedBytes = DatatypeConverter.parseBase64Binary(value);
        assertArrayEquals("データが暗号化されていること", FixedBytesEncryptor.FIXED_BYTES, encryptedBytes);
    }

    // なんでも固定のバイト列に置き換えるテスト用Encryptor
    private static class FixedBytesEncryptor implements Encryptor<Serializable> {

        private static final byte[] FIXED_BYTES = { (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE };

        private static final Serializable FIXED_CONTEXT = "FIXED_CONTEXT";

        boolean encryptionExecuted = false;

        @Override
        public Serializable generateContext() {
            return "unused"; // 本クラスはContextを使用しない
        }

        @Override
        public byte[] encrypt(Serializable context, byte[] src) throws IllegalArgumentException {
            encryptionExecuted = true;
            return FIXED_BYTES.clone();
        }

        @Override
        public byte[] decrypt(Serializable context, byte[] src) throws IllegalArgumentException {
            throw new AssertionError("本クラスのテストでは使用されないメソッド");
        }
    }

    /** セッションIDが不正な場合、例外が発生すること。*/
    @Test
    public void testInvalidSessionId() {
        store.setStateEncoder(new JavaSerializeStateEncoder());

        List<SessionEntry> inEntries = Collections.singletonList(
                new SessionEntry("key1", "val1", store));

        String sessionId = createSessionId();
        ServletExecutionContext outCtxt = createExeCtxt();
        store.save(sessionId, inEntries, outCtxt);

        String value = outCtxt.getRequestScopedVar("nablarch_hiddenStore");
        assertNotNull(value);

        ServletExecutionContext inCtxt = createExeCtxt();
        inCtxt.getServletRequest().getParameterMap().put(
                "nablarch_hiddenStore", new String[] {value});
        String invalidSessionId = createSessionId();
        assertNotEquals("保存時と復元時でセッションIDが異なる",
                invalidSessionId, sessionId);
        try {
            // 保存時と異なるセッションIDを用いて復元を試みる。
            store.load(invalidSessionId, inCtxt);
            fail();
        } catch (EncodeException e) {
            assertThat(e.getMessage(), containsString("Invalid Session ID detected."));
        }

    }

    /** 復元対象のデータがBase64として不正な場合、例外が発生すること*/
    @Test(expected = EncodeException.class)
    public void testInvalidBase64Data() {
        ServletExecutionContext inCtxt = createExeCtxt();
        inCtxt.getServletRequest().getParameterMap().put(
                "nablarch_hiddenStore", new String[] {"不正なHIDDENストアの値"});
        String sessionId = createSessionId();
        store.load(sessionId, inCtxt);
    }

    /** 復元対象のデータが復号化できない不正なデータである場合、例外が発生すること*/
    @Test(expected = EncodeException.class)
    public void testInvalidEncryptedData() {
        ServletExecutionContext inCtxt = createExeCtxt();
        inCtxt.getServletRequest().getParameterMap().put(
                "nablarch_hiddenStore", new String[] { DatatypeConverter.printBase64Binary("不正なHIDDENストアの値".getBytes(Charset.forName("UTF8")))});
        String sessionId = createSessionId();
        store.load(sessionId, inCtxt);
    }


    /** 復元対象のデータの鍵が合わない不正なデータである場合、例外が発生すること*/
    @Test(expected = EncodeException.class)
    public void testInvalidKeyData() {

        ServletExecutionContext outCtxt = createExeCtxt();
        {
            List<SessionEntry> inEntries = Collections.singletonList(
                    new SessionEntry("key1", "val1", store));

            String sessionId = createSessionId();

            // 別の鍵を持つHiddenStoreで暗号化
            HiddenStore another = new HiddenStore();
            another.setStateEncoder(new JavaSerializeStateEncoder());
            another.setEncryptor(new AesEncryptor());
            another.save(sessionId, inEntries, outCtxt);
        }

        String value = outCtxt.getRequestScopedVar("nablarch_hiddenStore");
        assertNotNull(value);

        ServletExecutionContext inCtxt = createExeCtxt();

        inCtxt.getServletRequest().getParameterMap().put(
                "nablarch_hiddenStore", new String[] { value });
        String sessionId = createSessionId();
        store.load(sessionId, inCtxt);
    }

}
