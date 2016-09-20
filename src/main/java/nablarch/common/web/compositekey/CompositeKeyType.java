package nablarch.common.web.compositekey;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import nablarch.core.util.annotation.Published;
import nablarch.core.validation.ConversionFormat;

/**
 * 特定文字で区切った複合キーの集合、またはその配列から構成されるパラメータを格納する、
 * {@link CompositeKey}型のプロパティに付与するアノテーション。
 * <p/>
 * CompositeKeyに展開できるパラメータの構造について
 * <ul>
 *     <li>特定文字で区切った複合キーの集合(例："user001,pk2001,pk3001")</li>
 *     <li>特定文字で区切った複合キーの集合の配列(例：{"user001,pk2001,pk3001","user002,pk2001,pk3001"})</li>
 * </ul>
 *
 * @author Koichi Asano
 *
 * @see CompositeKeyConvertor
 * @see CompositeKeyArrayConvertor
 */
@ConversionFormat
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Published
public @interface CompositeKeyType {
    /**
     * {@link CompositeKey}型のプロパティに格納する複合キーの数。
     * <p/>
     * CompositeKey型の配列に格納する場合は、各要素の複合キーの数を指定する。<br/>
     * ※要素ごとに複合キーの数を指定することはできない。従って、配列は同数の複合キーを持つ要素で構成する必要がある。
     */
    int keySize();
}
