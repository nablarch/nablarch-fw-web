package nablarch.fw.web;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.util.FileUtil;

/**
 * HTTPレスポンスのボディ内容を格納するオブジェクト。
 * <pre>
 * レスポンスボディの内容を以下の3つの方式のいずれかによって指定する。
 * (排他利用)
 * 1. コンテンツパスによる静的リソースの指定。
 *     {@link HttpResponse#setContentPath(ResourceLocator)}
 * 2. 入力ストリームによる指定。
 *     {@link HttpResponse#setBodyStream(InputStream)}
 * 3. 内部バッファへの書き込み。
 *     {@link HttpResponse#write(byte[])}
 *     {@link HttpResponse#write(CharSequence)}
 * </pre>
 *
 * 内部バッファは一定サイズまではヒープ領域上に置かれるが、それを超えた
 * 場合は一時ファイルに出力される。
 *
 * 入力ストリーム一時ファイルは、レスポンス処理終了後に
 * {@link nablarch.fw.web.handler.HttpResponseHandler} が {@link ResponseBody#cleanup()} を呼ぶことで
 * 自動的に削除される。
 *
 * @see    HttpResponse
 * @see    nablarch.fw.web.handler.HttpResponseHandler
 * @author Iwauo Tajima <iwauo@tis.co.jp>
 */
public class ResponseBody {
    /** ロガー */
    private static final Logger LOGGER = LoggerManager.get(ResponseBody.class);

    /** HTTPレスポンスの動作設定 */
    private static final HttpResponseSetting CONF = new HttpResponseSetting();

    /** ボディバッファの初期サイズ(16KB) */
    private static final int BSIZE = 16 * 1024;

    /**
     * コンストラクタ
     * @param response HTTPレスポンス本体
     */
    public ResponseBody(HttpResponse response) {
        this.response = response;
    }

    /** HTTPレスポンス本体 */
    private final HttpResponse response;

    /** コンテンツパス */
    private ResourceLocator contentPath = null;

    /** 入力ストリーム */
    private InputStream input = null;

    /** 内部バッファ(オンヒープ) */
    private ByteBuffer buffer = null;

    /** 内部バッファ(一時ファイル) */
    private File tempFile = null;

    /** 一時ファイルへの出力チャネル */
    private FileChannel tempFileWriteChannel = null;

    /** 削除しなければならない一時ファイル */
    private static final ThreadLocal<Collection<File>>
    TEMP_FILE_HOLDER = new ThreadLocal<Collection<File>>() {
        @Override protected Collection<File> initialValue() {
            return new ArrayList<File>();
        }
    };

    /** 閉じなければならない入力ストリーム */
    private static final ThreadLocal<Collection<Closeable>>
    STREAM_HOLDER = new ThreadLocal<Collection<Closeable>>() {
        @Override protected Collection<Closeable> initialValue() {
            return new ArrayList<Closeable>();
        }
    };

    /**
     * ボディの内容が設定されていなければtrueを返す。
     * @return ボディの内容が設定されていなければtrue
     */
    public boolean isEmpty() {
        return contentPath == null
            && input       == null
            && tempFile    == null
            && buffer      == null;
    }

    /**
     * ボディの内容をヒープバッファに保持している場合、そのサイズを返す。
     * <pre>
     * 内部バッファを使用していない場合、もしくは、
     * 内部バッファとして一時ファイルを使用している場合は null を返す。
     * </pre>
     * @return ヒープバッファのサイズ
     */
    public Long length() {
        if (contentPath != null || input != null || tempFile != null) {
            return null; // unknown
        }
        if (buffer != null) {
            return Long.valueOf(buffer.position());
        }
        return Long.valueOf(0);
    }

    /**
     * ボディのコンテンツパスを設定する。
     * @param path コンテンツパス
     * @return このオブジェクト自体
     */
    public ResponseBody setContentPath(ResourceLocator path) {
        contentPath = path;
        return this;
    }

    /**
     * コンテンツパスを取得する。
     * @return コンテンツパス
     */
    public ResourceLocator getContentPath() {
        return contentPath;
    }


    /**
     * 内部バッファにバイナリデータを書き込む。
     * @param bytes バイナリデータ
     * @return このオブジェクト自体
     */
    public ResponseBody write(byte[] bytes) {
        return write(ByteBuffer.wrap(bytes));
    }

    /**
     * 内部バッファにキャラクタデータを書き込む。
     * @param text キャラクタデータ
     * @return このオブジェクト自体
     */
    public ResponseBody write(CharSequence text) {
        return write(response.getCharset().encode(CharBuffer.wrap(text)));
    }

    /**
     * 内部バッファにバイナリデータを書き込む。
     * <pre>
     * 要求サイズがヒープバッファの上限値を超過した場合、
     * 一時ファイルへの書き出しが許可されていれば、
     * ヒープバッファを破棄し、以降は一時ファイルをバッファとして使用する。
     * (ヒープバッファは直近のGCにより開放される。)
     * 一時ファイルへの書き出しが許可されていないばあい、
     * レスポンスコード400に相当するHTTPエラーを送出する。
     * </pre>
     * @param bytes バイナリデータ
     * @return このオブジェクト自体
     */
    public ResponseBody write(ByteBuffer bytes) {
        if (tempFile != null) {
            writeToFile(bytes);
            return this;
        }
        if (buffer == null) {
            buffer = ByteBuffer.allocate(BSIZE);
        }

        int newLimit = buffer.limit() + bytes.remaining();

        if (newLimit > CONF.getBufferLimitSizeKb() * 1024) {
            writeToFile(bytes);
        } else {
            writeToHeapBuffer(bytes);
        }
        return this;
    }

    /**
     * ヒープ上のバッファにデータを書き込む
     * @param bytes データ
     */
    private void writeToHeapBuffer(ByteBuffer bytes) {
        if (buffer.remaining() < bytes.remaining()) {
            expandTo(buffer.capacity() + bytes.remaining());
        }
        buffer.put(bytes);
    }

    /**
     * 一時ファイル上のバッファにデータを書き込む
     * @param bytes データ
     */
    private void writeToFile(ByteBuffer bytes) {
        if (tempFile == null) {
            useTempFile();
        }
        try {
            tempFileWriteChannel.write(bytes);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * ヒープバッファのサイズを拡張する。
     * @see nablarch.fw.web.HttpResponseSetting#getBufferLimitSizeKb()
     * @see nablarch.fw.web.HttpResponseSetting#usesTempFile()
     * @param requestedSize 要求サイズ
     */
    private void expandTo(long requestedSize) {
        int currentCapacity = buffer.capacity();
        int maxBufferSize   = CONF.getBufferLimitSizeKb() * 1024;

        int newSize = (requestedSize > currentCapacity * 2)
                    ? (int) requestedSize
                    : currentCapacity * 2;
        newSize = (maxBufferSize > newSize) ? newSize : maxBufferSize;
        buffer = ByteBuffer.allocate(newSize).put((ByteBuffer) buffer.flip());
    }

    /** {@inheritDoc}
     * この実装ではボディの内容を表す文字列を返す。
     * <pre>
     * 1. コンテンツパスが指定されている場合。
     *     パス文字列を返す。
     *
     * 2. 入力ストリームもしくは一時ファイル上にバッファリングしている場合。
     *     先頭16KBの内容をデコードした文字列を返す。
     *
     * 3. ヒープ上にバッファリングしている場合。
     *     バッファの内容をデコードした文字列を返す。
     * </pre>
     * @see HttpResponse#getCharset()
     *         デコードに使用するコンバータ
     */
    @Override
    public String toString() {
        if (contentPath != null) {
            return "Content-Path: " + contentPath.toString();
        }
        try {
            if (input != null) {
                return peek(input);
            }
            if (tempFile != null) {
                InputStream istream = new BufferedInputStream(new FileInputStream(tempFile));
                try {
                    return peek(istream);
                } finally {
                    FileUtil.closeQuietly(istream);
                }


            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new StringBuilder(
            (buffer == null)
            ? CharBuffer.allocate(0)
            : response.getCharset().decode((ByteBuffer) buffer.duplicate().flip())
        ).toString();
    }

    /**
     * 渡された入力ストリームの先頭16KBの内容をデコードした文字列を返す。
     * <pre>
     * 入力ストリームが先読みをサポートしていない場合はクラス名のみを返す。
     * </pre>
     * @param input 入力ストリーム
     * @return 先頭16KBの内容をデコードした文字列
     * @throws IOException I/O例外
     */
    private String peek(InputStream input) throws IOException {
        if (!input.markSupported()) {
            return "InputStream: " + input.getClass().getName();
        }
        byte[] peeked = new byte[BSIZE];
        input.mark(BSIZE);
        int readSize = 0;
        try {
            readSize = input.read(peeked);

        } finally {
            input.reset();
        }
        return new StringBuilder(
            response.getCharset().decode(ByteBuffer.wrap(peeked, 0, readSize))
        ).append((readSize == BSIZE) ? "..." : "").toString();
    }

    /**
     * ボディの内容を格納した入力ストリームを返す。
     *
     * なお、コンテントパスを使用している場合、以下のケースではnullを返却する。
     * <pre>
     * 1. コンテントパスのスキームが file:// もしくは classpath:// のいずれでもない場合
     * 2. コンテントパスのスキームが file:// もしくは classpath:// だが、その参照先のファイルが存在しない場合。
     * </pre>
     *
     * @return 入力ストリーム
     */
    public InputStream getInputStream() {
        if (contentPath != null) {
            try {
                return contentPath.getInputStream();
            } catch (FileNotFoundException e) {
                return null;
            }
        }

        if (input != null) {
            return input;
        }

        if (tempFile != null) {
            try {
                return new FileInputStream(tempFile);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        if (buffer == null) {
            buffer =  ByteBuffer.wrap(new byte[]{});
        }
        return new InputStreamWrapper((ByteBuffer) buffer.flip());
    }

    /**
     * ボディの内容を格納した入力ストリームを設定する。
     * @param input 入力ストリーム
     * @return このオブジェクト自体
     */
    public ResponseBody setInputStream(InputStream input) {
        this.input = input;
        STREAM_HOLDER.get().add(input);
        return this;
    }

    /**
     * 削除しなければならない一時ファイルに追加する。
     * <p/>
     * {@link #cleanup()}メソッドが呼ばれた際に指定された一時ファイルが削除される。
     * @param file 一時ファイル
     */
    public static void addTempFileToDelete(File file) {
        TEMP_FILE_HOLDER.get().add(file);
    }

    /**
     * カレントスレッドがIO用に確保しているリソースを全て開放する。
     */
    public static void cleanup() {
        Collection<Closeable> streams = STREAM_HOLDER.get();
        for (Closeable stream : streams) {
            FileUtil.closeQuietly(stream);
        }
        Collection<File> tempFiles = TEMP_FILE_HOLDER.get();
        for (File file : tempFiles) {
            if (!file.delete()) {
                LOGGER.logWarn(
                    "could not delete a temporary file: "
                   + file.getAbsolutePath()
                );
            }
        }
        streams.clear();
        tempFiles.clear();
    }

    /**
     * {@link ByteBuffer} を {@link InputStream} に変換するラッパー
     * @author Iwauo Tajima
     */
    private static class InputStreamWrapper extends InputStream {

        /**
         * コンストラクタ。
         * @param buffer 内部バッファ
         */
        public InputStreamWrapper(ByteBuffer buffer) {
            this.buffer = buffer;
        }

        /**
         * 内部バッファ。
         */
        private final ByteBuffer buffer;

        /**
         * 内部バッファを読み込む。
         * @return 1バイト
         * @throws IOException I/O例外
         */
        @Override
        public synchronized int read() throws IOException {
            if (!buffer.hasRemaining()) {
                return -1;
            }
            return buffer.get();
        }

        /**
         * 内部バッファを読み込む。
         * @param bytes バイト配列
         * @param off 開始位置
         * @param len 読込レングス
         * @return 残バイトレングス
         * @throws IOException I/O例外
         */
        @Override
        public synchronized int read(byte[] bytes, int off, int len)
        throws IOException {
            if (!buffer.hasRemaining()) {
                return -1;
            }
            len = Math.min(len, buffer.remaining());
            buffer.get(bytes, off, len);
            return len;
        }
    }

    /**
     * 内部バッファを一時ファイルに書き出す。
     */
    private void useTempFile() {
        File dir = getDownloadDir();
        try {
            tempFile = File.createTempFile("nablarch_temp_", null, dir);
            tempFileWriteChannel = new FileOutputStream(tempFile).getChannel();

            TEMP_FILE_HOLDER.get().add(tempFile);
            STREAM_HOLDER.get().add(tempFileWriteChannel);

            tempFileWriteChannel.write((ByteBuffer) buffer.flip());
            buffer = null;

        } catch (IOException e) {
            throw new RuntimeException("download temp file create failed. ", e);
        }
    }

    /**
     * 一時ファイルを格納するディレクトリを取得する。
     * <pre>
     * ディレクトリが存在しなかった場合、新規に作成する。
     * </pre>
     * @return 一時ディレクトリ
     */
    private File getDownloadDir() {
        String tempDirRootPath = CONF.getTempDirPath();
        if (tempDirRootPath == null) {
            return null; //システム既定のTEMPフォルダに出力する。
        }
        File file = new File(tempDirRootPath);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw new RuntimeException(
                    "failed to create download dir."
                  + "  file=[" + file.toString() + "]");
            }
        }
        return file;
    }
}
