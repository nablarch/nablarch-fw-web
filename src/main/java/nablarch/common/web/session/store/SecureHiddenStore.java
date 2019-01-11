package nablarch.common.web.session.store;

import nablarch.common.encryption.AesEncryptor;
import nablarch.common.encryption.Encryptor;
import nablarch.common.web.session.EncodeException;
import nablarch.common.web.session.SessionEntry;
import nablarch.common.web.session.SessionStore;
import nablarch.common.web.session.store.HiddenStore.HiddenStoreLoadFailedException;
import nablarch.core.util.FileUtil;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.servlet.ServletExecutionContext;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

/**
 * HTMLのinputタグ(type="hidden")を格納先とする{@link SessionStore}拡張クラス。
 * {@link nablarch.common.web.session.store.HiddenStore}に以下の追加変更を行っている。
 * <pre>
 *     <li>保存するデータをひとつづつ暗号化するのではなく、保存するデータ全体をまとめて暗号化する</li>
 *     <li>保存するデータにセッションIDを含めておき、復元時のセッションIDと一致することを確認する</li>
 * </pre>
 * {@link nablarch.common.web.session.store.HiddenStore}とは直列化、暗号化の方法など
 * 内部的な処理やデータ構造は異なるが、外部的な振る舞いは互換性がある。
 *
 * デフォルトのストア名は"hidden"である。
 *
 * 以下の構造でデータをストアに格納する。
 * <pre>
 * | 項目                                             | 長さ  |
 * |--------------------------------------------------+-------|
 * | セッションIDバイト長                         | 4Byte |
 * | セッションID                                     | 可変  |
 * | セッションエントリ({@link SessionEntry}のリスト) | 可変  |
 * </pre>
 *
 * 保存({@link #save(String, List, ExecutionContext)}時にセッションIDを格納しておき、
 * 復元({@link #load(String, ExecutionContext)})時にセッションIDが一致することを確認する。
 * これにより、他ユーザのデータを流用することを防止する。
 *
 * 復元時点でセッションタイムアウトが発生している場合は、
 * 復元処理がスキップされるため({@link #save(String, List, ExecutionContext)} save}が呼ばれない)
 * セッションIDの変更が改ざんに誤検知されることはない。
 *
 * @author TIS
 */
public class SecureHiddenStore extends SessionStore {

    /** セッションIDのエンコーディングに使用する{@link Charset} */
    private static final Charset SESSION_ID_ENCODING = Charset.forName("UTF-8");

    /** 「セッションIDバイト長」領域のバイト長(4Byte) */
    private static final int SESSION_ID_LENGTH_BYTES = Integer.SIZE / Byte.SIZE;

    /** 暗号化クラス */
    @SuppressWarnings("rawtypes")
    private Encryptor encryptor;

    /** 暗号化のコンテキスト */
    private Serializable context;

    /** セッション内容を書きだすhidden要素のname属性(=POSTパラメータ名) */
    private String parameterName = ExecutionContext.FW_PREFIX + "hiddenStore";

    /**
     * コンストラクタ。
     */
    public SecureHiddenStore() {
        super("hidden");
        @SuppressWarnings("rawtypes")
        Encryptor defaultEncryptor = new AesEncryptor();
        setEncryptor(defaultEncryptor);
    }

    /**
     * {@inheritDoc}
     * 本クラスでは、セッションIDとセッションエントリ全体を暗号化した結果を保存する。
     */
    @Override
    public void save(String sessionId, List<SessionEntry> entries, ExecutionContext executionContext) {
        byte[] serialized = serialize(sessionId, entries);
        @SuppressWarnings("unchecked")
        byte[] encrypted = encryptor.encrypt(context, serialized);
        String base64Encoded = DatatypeConverter.printBase64Binary(encrypted);
        executionContext.setRequestScopedVar(parameterName, base64Encoded);
    }

    @Override
    public List<SessionEntry> load(String currentSessionId, ExecutionContext executionContext) {
        ServletExecutionContext servlet = (ServletExecutionContext) executionContext;
        String[] hidden = servlet.getHttpRequest().getParam(parameterName);
        if (hidden == null || hidden.length == 0) {
            return Collections.emptyList();
        }
        try {
            String base64Encoded = hidden[0];
            byte[] encrypted = DatatypeConverter.parseBase64Binary(base64Encoded);
            @SuppressWarnings("unchecked")
            byte[] serialized = encryptor.decrypt(context, encrypted);
            return deserializeAndValidateSessionId(serialized, currentSessionId);
        } catch (Exception e) {
            // SessionStoreHandlerの例外判定でこの例外の型を使用するので、HiddenStoreにある例外を使用する。
            throw new HiddenStoreLoadFailedException(e);
        }
    }

    /**
     * セッションID、セッションエントリのシリアライズを行う。
     *
     * @param sessionId 現在のセッションID
     * @param entries セッションエントリ
     * @return シリアライズ結果
     */
    private byte[] serialize(String sessionId, List<SessionEntry> entries)  {
        // セッションID
        byte[] sidBytes = sessionId.getBytes(SESSION_ID_ENCODING);
        // セッションIDバイト長
        byte[] sidLengthBytes = ByteBuffer.allocate(SESSION_ID_LENGTH_BYTES).putInt(sidBytes.length).array();
        // セッションエントリ
        byte[] entriesBytes = encode(entries);
        return concat(sidLengthBytes, sidBytes, entriesBytes);
    }

    /**
     * セッションエントリのデシリアライズを行う。
     * その際に、保存時のセッションIDと復元時のセッションIDの比較を行う。
     *
     * @param serialized シリアライズされたデータ
     * @param currentSessionId 現在のセッションID
     * @return セッションエントリ(セッションIDが一致しない場合、空のリスト)
     */
    private List<SessionEntry> deserializeAndValidateSessionId(byte[] serialized, String currentSessionId) {
        if (serialized.length == 0) {
            return Collections.emptyList();
        }
        // セッションIDバイト長
        byte[] sidLengthBytes = copy(serialized, 0, SESSION_ID_LENGTH_BYTES);
        int sidByteLength = ByteBuffer.wrap(sidLengthBytes).getInt();

        // セッションID
        byte[] sidBytes = copy(serialized, SESSION_ID_LENGTH_BYTES, sidByteLength);
        String sessionIdFromHidden = new String(sidBytes, SESSION_ID_ENCODING);

        // セッションIDの比較
        validate(currentSessionId, sessionIdFromHidden);

        // 残りのデータを復元
        int sessionIdSectionByteLength = SESSION_ID_LENGTH_BYTES + sidByteLength;  // 既に読み取ったバイト長
        int entriesByteLength = serialized.length - sessionIdSectionByteLength;    // 残りのバイト長
        byte[] entriesBytes = copy(serialized, sessionIdSectionByteLength, entriesByteLength);
        return decode(entriesBytes);
    }

    /**
     * セッションIDのバリデーションを行う。
     *
     * {@link nablarch.common.web.session.store.HiddenStore}との互換性を保つため、
     * 新規の例外ではなく、{@link EncodeException}をスローしている。
     *
     * @param currentSessionId 現在のセッションID
     * @param sessionIdFromHidden HIDDENストアから復元したセッションID
     * @throws EncodeException 保存時のセッションIDと復元時のセッションIDが一致しない場合
     */
    private void validate(String currentSessionId, String sessionIdFromHidden) {
        if (currentSessionId.equals(sessionIdFromHidden)) {
            return;  // セッションIDが一致
        }
        throw new EncodeException(new RuntimeException("Invalid Session ID detected."));
    }

    /**
     * バイト配列のコピーを行う。
     * @param src コピー元のバイト配列
     * @param srcPos コピー開始位置
     * @param length 長さ
     * @return コピー後のバイト配列
     */
    private byte[] copy(byte[] src, int srcPos, int length) {
        byte[] dest = new byte[length];
        System.arraycopy(src, srcPos, dest, 0, length);
        return dest;
    }

    /**
     * バイト配列の結合を行う。
     * @param byteArrays 結合対象のバイト配列
     * @return 結合後のバイト配列
     */
    private byte[] concat(byte[]... byteArrays) {
        int totalLenght = 0;
        for (byte[] bs : byteArrays) {
            totalLenght += bs.length;
        }
        ByteArrayOutputStream dest = new ByteArrayOutputStream(totalLenght);
        try {
            for (byte[] byteArray : byteArrays) {
                dest.write(byteArray);
            }
        } catch (IOException e) {
            throw new EncodeException(e);  // 発生しない
        } finally {
            FileUtil.closeQuietly(dest);
        }
        return dest.toByteArray();
    }

    @Override
    public void delete(String sessionId, ExecutionContext executionContext) {
        executionContext.getRequestScopeMap().remove(parameterName);
    }

    @Override
    public void invalidate(String sessionId, ExecutionContext executionContext) {
        executionContext.getRequestScopeMap().remove(parameterName);
    }

    /**
     * 暗号化クラスを設定する。
     * デフォルトでは{@link AesEncryptor}が使用される。
     * 暗号化のカスタマイズを行う場合は、このプロパティに{@link Encryptor}実装を設定する。
     *
     * @param encryptor 暗号化クラス
     */
    @SuppressWarnings("rawtypes")
    public void setEncryptor(Encryptor encryptor) {
        this.encryptor = encryptor;
        context = encryptor.generateContext();
    }

    /**
     * パラメータ名を設定する。
     *
     * @param parameterName 設定するパラメータ名
     */
    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

}
