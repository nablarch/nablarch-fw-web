package nablarch.common.web.session;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;

/**
 * セッションの内容をストアに格納/読み込みするクラスが継承する共通実装。
 *
 * @author kawasima
 * @author tajima
 * @see nablarch.common.web.session.store
 */
@Published(tag = "architect")
public abstract class SessionStore {

    /** セッションストア名 */
    private String name;

    /** 有効期間(ミリ秒) */
    private Long expiresMilliSeconds;

    /** セッション内容の直列化モジュール */
    private StateEncoder stateEncoder;

    /**
     * コンストラクタ。
     * デフォルトのセッションストア名を設定する。
     * 
     * @param name セッションストア名
     */
    protected SessionStore(String name) {
        this.name = name;
    }
    
    /**
     * セッションストア名を設定する。
     * 
     * @param name セッションストア名
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * セッションストア名を取得する。
     * 
     * @return セッションストア名
     */
    public String getName() {
        return name;
    }

    /**
     * 有効期限(単位:秒)を設定する。
     * 
     * @param expires 有効期限(単位:秒)
     */
    public void setExpires(Long expires) {
        setExpires(expires, TimeUnit.SECONDS);
    }

    /**
     * 有効期限を設定する。
     * 
     * @param expires 有効期限
     * @param timeUnit 時間単位
     */
    public void setExpires(Long expires, TimeUnit timeUnit) {
        this.expiresMilliSeconds = timeUnit.toMillis(expires);
    }

    /**
     * セッションの内容をストアからロードする。
     * 
     * セッションの内容が存在しない場合は空リストを返す。
     * 
     * @param sessionId セッションID
     * @param executionContext コンテキスト
     * @return セッションエントリリスト
     */
    public abstract List<SessionEntry> load(String sessionId, ExecutionContext executionContext);

    /**
     * セッションの内容をストアに保存する。
     * 
     * @param sessionId セッションID
     * @param entries セッションエントリリスト
     * @param executionContext コンテキスト
     */
    public abstract void save(String sessionId, List<SessionEntry> entries, ExecutionContext executionContext);

    /**
     * セッションの内容をストアから削除する。
     *
     * @param sessionId セッションID
     * @param executionContext コンテキスト
     */
    public abstract void delete(String sessionId, ExecutionContext executionContext);

    /**
     * セッションストアを無効にする。
     *
     * @param sessionId セッションID
     * @param executionContext コンテキスト
     */
    public abstract void invalidate(String sessionId, ExecutionContext executionContext);

    /**
     * セッションエントリリストをエンコードする。
     * 
     * @param entries セッションエントリリスト
     * @return バイト配列
     */
    protected byte[] encode(List<SessionEntry> entries) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            for (SessionEntry entry : entries) {
                dos.writeUTF(entry.getKey());
                Object obj = entry.getValue();
                if (obj == null) {
                    dos.writeInt(0);
                } else {
                    byte[] encoded = stateEncoder.encode(obj);
                    dos.writeInt(encoded.length);
                    dos.writeUTF(obj.getClass().getName());
                    dos.write(encoded);
                }
            }
        } catch (IOException e) {
            throw new EncodeException(e);
        }
        return baos.toByteArray();
    }

    /**
     * セッションエントリリストをデコードする。
     * 
     * @param encoded エンコードされたバイト配列
     * @return セッションエントリリスト
     */
    protected List<SessionEntry> decode(byte[] encoded) {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(encoded));
        List<SessionEntry> entries = new ArrayList<SessionEntry>();
        try {
            while (dis.available() > 0) {
                String key = dis.readUTF();
                int length = dis.readInt();
                if (length == 0) {
                    entries.add(new SessionEntry(key, null, this));
                } else {
                    String typeName = dis.readUTF();
                    byte[] buf = new byte[length];
                    dis.read(buf);
                    Class<?> type = Class.forName(typeName);
                    entries.add(new SessionEntry(key, stateEncoder.decode(buf, type), this));
                }
            }
            return entries;
        } catch (IOException e) {
            throw new EncodeException(e);
        } catch (ClassNotFoundException e) {
            throw new EncodeException(e);
        }
    }

    /**
     * 有効期限(単位:ミリ秒)で取得する。
     * 
     * @return 有効期限(単位:ミリ秒)
     */
    public long getExpiresMilliSeconds() {
        return expiresMilliSeconds;
    }

    /**
     * セッション全体の有効期限に寄与するかを取得する。
     *
     * ストアの有効期限をセッショントラキングIDの保持期限に反映させない場合は、
     * 本メソッドをサブクラス側でオーバーライドしてfalseを返却するようにする。
     *
     * @return このストアの有効期限をセッションの維持期間に反映させる場合はtrue
     */
    public boolean isExtendable() {
        return true;
    }

    /**
     * セッション内容の直列化モジュールを取得する。
     * 
     * @return セッション内容の直列化モジュール
     */
    public StateEncoder getStateEncoder() {
        return stateEncoder;
    }

    /**
     * セッション内容の直列化モジュールを設定する。
     * 
     * @param stateEncoder セッション内容の直列化モジュール
     */
    public void setStateEncoder(StateEncoder stateEncoder) {
        this.stateEncoder = stateEncoder;
    }
}
