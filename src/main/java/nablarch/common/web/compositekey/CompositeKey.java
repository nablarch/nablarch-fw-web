package nablarch.common.web.compositekey;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;

/**
 * 画面から送信された複合キーを格納するクラス。
 * <p/>
 * フォームのプロパティとして本クラスを定義し{@link CompositeKeyType}アノテーションを付与することで、
 * 送信された複合キーの解析・格納を自動で行うことができる。
 * <p/>
 * 以下のようなパラメータで送信された複合キーを格納する。
 * <ul>
 *     <li>特定文字で区切った複合キーの集合(例："user001,pk2001,pk3001")。
 *         フォームには、CompositeKey型のプロパティを定義する。</li>
 *     <li>特定文字で区切った複合キーの集合の配列(例：{"user001,pk2001,pk3001","user002,pk2001,pk3001"})。
 *         フォームには、CompositeKey[]型のプロパティを定義する。</li>
 * </ul>
 * <p/>
 *
 * @author Koichi Asano
 *
 * @see CompositeKeyConvertor
 * @see CompositeKeyArrayConvertor
 */
@Published
public class CompositeKey implements Serializable {

    /**
     * シリアルバージョンUID。
     */
    private static final long serialVersionUID = 4242392928992097582L;
    
    /** キー */
    private String[] keys;

    /**
     * キーを指定して{@code CompositeKey}を構築する。
     * @param keys 
     */
    public CompositeKey(String... keys) {
        this.keys = keys;
    }

    /**
     * 全てのキーを取得する。
     * @return 全てのキー
     */
    public String[] getKeys() {
        return (String[]) keys.clone();
    }

    /**
     * このオブジェクトのハッシュコード値を返す。
     * @return ハッシュコード値。同じ値のキーを保持しているオブジェクトは、同じハッシュコード値を返す
     * @see Arrays#hashCode(Object[])
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(keys);
        return result;
    }

    /**
     * このオブジェクトと等価であるかを返す。
     * <p/>
     * {@code obj}が以下の条件を全て満たす場合{@code true}を返す。
     * <ul>
     *     <li>{@code null}ではないこと。</li>
     *     <li>CompositeKey型のオブジェクトであること。</li>
     *     <li>保持しているキーの値が、このオブジェクトが保持しているキーの値と一致すること。</li>
     * </ul>
     *
     * @param obj 比較対象のオブジェクト
     * @return このオブジェクトと等価である場合{@code true}
     * @see Arrays#equals(Object[], Object[])
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CompositeKey other = (CompositeKey) obj;
        if (!Arrays.equals(keys, other.keys)) {
            return false;
        }
        return true;
    }

    /**
     * このオブジェクトが保持しているキーを「,(カンマ)」区切りで列挙した文字列を返す。
     * @return キーを列挙した文字列
     */
    @Override
    public String toString() {
        List<String> keyList = new ArrayList<String>();
        Collections.addAll(keyList, (keys));
        return StringUtil.join(",", keyList);
    }
}
