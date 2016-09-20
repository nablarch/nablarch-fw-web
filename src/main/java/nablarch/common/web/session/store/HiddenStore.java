package nablarch.common.web.session.store;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import nablarch.common.web.session.EncodeException;
import nablarch.common.web.session.SessionEntry;
import nablarch.common.web.session.SessionStore;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * セッションの内容をHiddenに格納/読み込みする{@link SessionStore}。
 * <p/>
 * デフォルトのストア名は"hidden"。
 * 
 * @author kawasima
 * @author tajima
 */
public class HiddenStore extends SessionStore {

    /**
     * コンストラクタ。
     */
    @SuppressWarnings("unchecked")
    public HiddenStore() {
        super("hidden");
    }

    /**
     * HiddenStoreのロードに失敗した場合に送出する例外。
     * @author Kiyohito Itoh
     */
    public static class HiddenStoreLoadFailedException extends EncodeException {
        /**
         * コンストラクタ。
         * @param cause 起因例外
         */
        public HiddenStoreLoadFailedException(Exception cause) {
            super(cause);
        }
    }

    /** セッション内容を書きだすhidden要素のname属性(=POSTパラメータ名) */
    private String parameterName = ExecutionContext.FW_PREFIX + "hiddenStore";

    @Override
    public List<SessionEntry> load(String sessionId, ExecutionContext executionContext) {
        ServletExecutionContext servlet = (ServletExecutionContext) executionContext;
        String[] hiddenValue = servlet.getHttpRequest().getParam(parameterName);
        if (hiddenValue != null) {
            try {
                byte[] src = DatatypeConverter.parseBase64Binary(hiddenValue[0]);
                return decode(src);
            } catch (Exception e) {
                throw new HiddenStoreLoadFailedException(e);
            }
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void save(String sessionId, List<SessionEntry> entries, ExecutionContext executionContext) {
        byte[] src = encode(entries);
        String parameterValue = DatatypeConverter.printBase64Binary(src);
        executionContext.setRequestScopedVar(parameterName, parameterValue);
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
     * パラメータ名を設定する。
     *
     * @param parameterName 設定するパラメータ名
     */
    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }
}
