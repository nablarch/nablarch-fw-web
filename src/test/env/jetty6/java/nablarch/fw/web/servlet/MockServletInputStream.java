package nablarch.fw.web.servlet;

import nablarch.core.util.StringUtil;

import jakarta.servlet.ServletInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * テスト用{@link ServletInputStream}クラス。<br/>
 *
 * @author T.Kawasaki
 */
public class MockServletInputStream extends ServletInputStream {
    /** 入力ストリーム */
    private final InputStream source;

    /** コンストラクタ。 */
    public MockServletInputStream() {
        this(new byte[0]);
    }

    /**
     * コンストラクタ。
     *
     * @param bytes バイト列
     */
    public MockServletInputStream(byte[] bytes) {
        this(new ByteArrayInputStream(bytes));
    }

    /**
     * コンストラクタ。
     *
     * @param source 入力ストリーム
     */
    public MockServletInputStream(InputStream source) {
        assert source != null;
        this.source = source;
    }

    /**
     * コンストラクタ。
     *
     * @param string  文字列
     * @param charset エンコーディング
     */
    public MockServletInputStream(String string, Charset charset) {
        this(StringUtil.getBytes(string, charset));
    }

    /** {@inheritDoc} */
    @Override
    public int read() throws IOException {
        return source.read();
    }

}
