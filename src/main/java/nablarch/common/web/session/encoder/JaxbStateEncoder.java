package nablarch.common.web.session.encoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import javax.xml.bind.JAXB;

import nablarch.common.web.session.EncodeException;
import nablarch.common.web.session.StateEncoder;
import nablarch.core.util.FileUtil;

/**
 * JAXBを使用した{@link StateEncoder}実装クラス。
 * <p/>
 * XMLベースのためJVMに依存せずに直列化を行うことができる。<br/>
 * ただし、パフォーマンス及びデータサイズの面で{@link JavaSerializeStateEncoder}に劣るため、
 * 本クラスを使用する場面は限られる。
 *
 * @author kawasima
 * @author tajima
 */
public class JaxbStateEncoder implements StateEncoder {

    @Override
    public <T> byte[] encode(T obj) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(baos, "UTF-8");
            JAXB.marshal(obj, writer);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new EncodeException(ex);
        } finally {
            FileUtil.closeQuietly(writer);
        }
    }

    @Override
    public <T> T decode(byte[] dmp, Class<T> type) {
        ByteArrayInputStream bais = new ByteArrayInputStream(dmp);
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(bais, "UTF-8");
            return JAXB.unmarshal(reader, type);
        } catch (IOException ex) {
            throw new EncodeException(ex);
        } finally {
            FileUtil.closeQuietly(reader);
        }
    }
}
