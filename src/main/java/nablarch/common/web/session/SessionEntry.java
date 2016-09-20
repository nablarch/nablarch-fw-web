package nablarch.common.web.session;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import nablarch.core.beans.BeanUtil;
import nablarch.core.beans.BeansException;
import nablarch.core.util.annotation.Published;

/**
 * セッションに登録するオブジェクト。
 *
 * JSPなどから値オブジェクトのプロパティを参照するために、Mapのインタフェースを実装している。
 * Mapインターフェースで操作する場合は、unmodifiableなMapとして振る舞う。
 * また、getter実行時に例外が発生する場合は、空のMapとして振る舞う。
 *
 * @author kawasima
 * @author tajima
 */
@Published(tag = "architect")
public class SessionEntry implements Map<String, Object> {

    /** セッションへの登録キー */
    private final String key;

    /** セッションに登録した値 */
    private final Object value;

    /** このエントリーを記録する際に使用する{@link SessionStore} */
    private final SessionStore storage;

    /** エントリーのJavaBeansプロパティのキー/値を格納するMap */
    private Map<String, Object> valueObjectMap;

    /**
     * コンストラクタ。
     *
     * @param key セッションキー
     * @param value セッション値
     * @param storage セッションストレージ
     */
    public SessionEntry(final String key, final Object value, final SessionStore storage) {
        this.key = key;
        this.value = value;
        this.storage = storage;
    }

    /**
     * セッションへの登録キーを取得する。
     *
     * @return セッションキー
     */
    public String getKey() {
        return key;
    }

    /**
     * セッションに登録された値を取得する。
     *
     * @return セッション値
     */
    public Object getValue() {
        return value;
    }

    /**
     * このエントリーを記録する際に使用する{@link SessionStore}を使用する。
     * 
     * @return セッションストレージ
     */
    public SessionStore getStorage() {
        return storage;
    }


    /**
     * プロパティからメソッドを読み込み、実施、値を取得する。
     */
    private void introspectValue() {
        if (valueObjectMap != null) {
            return;
        }

        if (value == null) {
            // valueがnullの場合は空のMapとして扱う
            valueObjectMap = Collections.emptyMap();
            return;
        }

        try {
            final PropertyDescriptor[] pds = BeanUtil.getPropertyDescriptors(value.getClass());
            valueObjectMap = new HashMap<String, Object>(pds.length);
            for (PropertyDescriptor pd : pds) {
                final Method getter = pd.getReadMethod();
                if (getter == null) { // setter only property
                    continue;
                }
                valueObjectMap.put(pd.getName(), getter.invoke(value));
            }
        } catch (IllegalAccessException ignored) {
            // 基本的には発生しえない。
            valueObjectMap = Collections.emptyMap();
        } catch (InvocationTargetException ignored) {
            // getterの実行時に例外が送出された場合は空のMapとして扱う。
            valueObjectMap = Collections.emptyMap();
        } catch (BeansException ignored) {
            // 基本的には発生しえない。
            valueObjectMap = Collections.emptyMap();
        }
    }

    @Override
    public int size() {
        introspectValue();
        return valueObjectMap.size();
    }

    @Override
    public boolean isEmpty() {
        introspectValue();
        return valueObjectMap.isEmpty();
    }

    @Override
    public boolean containsKey(final Object key) {
        introspectValue();
        return valueObjectMap.containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value) {
        introspectValue();
        return valueObjectMap.containsValue(value);
    }

    @Override
    public Object get(final Object key) {
        introspectValue();
        return valueObjectMap.get(key);
    }

    @Override
    public Object put(final String key, final Object value) {
        throw new UnsupportedOperationException("This object is immutable.");
    }

    @Override
    public Object remove(final Object key) {
        throw new UnsupportedOperationException("This object is immutable.");
    }

    @Override
    public void putAll(final Map<? extends String, ?> m) {
        throw new UnsupportedOperationException("This object is immutable.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("This object is immutable.");
    }

    @Override
    public Set<String> keySet() {
        introspectValue();
        return valueObjectMap.keySet();
    }

    @Override
    public Collection<Object> values() {
        introspectValue();
        return valueObjectMap.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        introspectValue();
        return valueObjectMap.entrySet();
    }
}
