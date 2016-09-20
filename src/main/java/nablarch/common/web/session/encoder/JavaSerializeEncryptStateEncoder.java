package nablarch.common.web.session.encoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import nablarch.common.encryption.AesEncryptor;
import nablarch.common.encryption.Encryptor;
import nablarch.common.web.session.EncodeException;
import nablarch.common.web.session.StateEncoder;

/**
 * Java標準のSerialize機構、暗号化を使用した{@link StateEncoder}実装クラス。
 * <p/>
 * デフォルトでは{@link AesEncryptor}による暗号化を行う。
 *
 * @author Naoki Yamamoto
 */
public class JavaSerializeEncryptStateEncoder<C extends Serializable> implements StateEncoder {
	
    /** 暗号化と復号に使用する{@link Encryptor} */
    private Encryptor<C> encryptor;

    /** 暗号化と復号に使用するコンテキスト情報 */
    private C encryptContext;
    
    /**
     * コンストラクタ。
     */
    @SuppressWarnings("unchecked")
    public JavaSerializeEncryptStateEncoder() {
        setEncryptor((Encryptor<C>) new AesEncryptor());
    }
    
    /**
     * 暗号化/復号に使用する{@link Encryptor}を設定する。
     * @param encryptor 暗号化/復号に使用する{@link Encryptor}
     */
    public void setEncryptor(Encryptor<C> encryptor) {
        this.encryptor = encryptor;
        encryptContext = encryptor.generateContext();
    }

    @Override
    public <T> byte[] encode(final T obj) {
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            final ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            return encryptor.encrypt(encryptContext, baos.toByteArray());
        } catch (IOException e) {
            throw new EncodeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T decode(final byte[] dmp, final Class<T> type) {
        try {
            final ByteArrayInputStream bais = new ByteArrayInputStream(encryptor.decrypt(encryptContext, dmp));
            final ObjectInputStream ois = new ObjectInputStream(bais);
            return (T) ois.readObject();
        } catch (Exception e) {
            throw new EncodeException(e);
        }
    }
}
