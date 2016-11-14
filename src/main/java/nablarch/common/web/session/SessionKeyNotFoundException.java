package nablarch.common.web.session;

import java.util.NoSuchElementException;

import nablarch.core.util.annotation.Published;

/**
 * セッションに指定したキーが存在しないことを示す例外クラス。
 *
 * @author siosio
 */
@Published
public class SessionKeyNotFoundException extends NoSuchElementException {

    /**
     * 指定されたキーが存在しないことを示す例外を生成する。
     *
     * @param key キー
     */
    public SessionKeyNotFoundException(final String key) {
        super("specified key was not found in session store. key: " + key);
    }
}
