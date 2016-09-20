package nablarch.common.web.session.encoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import nablarch.common.web.session.EncodeException;
import nablarch.common.web.session.StateEncoder;

/**
 * Java標準のSerialize機構を使用した{@link StateEncoder}実装クラス。
 *
 * @author kawasima
 * @author tajima
 */
public class JavaSerializeStateEncoder implements StateEncoder {

    @Override
    public <T> byte[] encode(final T obj) {
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            final ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new EncodeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T decode(final byte[] dmp, final Class<T> type) {
        try {
            final ByteArrayInputStream bais = new ByteArrayInputStream(dmp);
            final ObjectInputStream ois = new ObjectInputStream(bais);
            return (T) ois.readObject();
        } catch (Exception e) {
            throw new EncodeException(e);
        }
    }
}
