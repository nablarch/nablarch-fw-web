package nablarch.common.web.compositekey;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import nablarch.core.util.StringUtil;
import nablarch.core.validation.Convertor;
import nablarch.core.validation.ValidationContext;
import nablarch.core.validation.ValidationResultMessageUtil;

/**
 * 値を{@link CompositeKey}の配列に変換するクラス。
 * 
 * @author Koichi Asano 
 * @see CompositeKey
 */
public class CompositeKeyArrayConvertor implements Convertor {

    /**
     * 変換失敗時のデフォルトのエラーメッセージのメッセージID。
     */
    private String conversionFailedMessageId;

    /**
     * {@inheritDoc}
     */
    public Class<?> getTargetClass() {
        return CompositeKey[].class;
    }

    /**
     * 変換失敗時のデフォルトのエラーメッセージのメッセージIDを設定する。<br/>
     * デフォルトメッセージの例 : "{0}が正しくありません"
     *
     * @param conversionFailedMessageId 変換失敗時のデフォルトのエラーメッセージのメッセージID
     */
    public void setConversionFailedMessageId(String conversionFailedMessageId) {

        this.conversionFailedMessageId = conversionFailedMessageId;
    }

    /**
     * {@inheritDoc}
     */
    public <T> boolean isConvertible(ValidationContext<T> context,
            String propertyName, Object propertyDisplayName, Object value,
            Annotation format) {
        boolean convertible = false;

        if (format == null) {
            throw new IllegalArgumentException("annotation was not specified."
                    + " conversion of " + CompositeKey.class.getName() 
                    + " requires annotation " +  CompositeKeyType.class.getName() + "."
                    + " propertyName = [" + propertyName + "]");
        }

        if (!(format instanceof CompositeKeyType)) {
            throw new IllegalArgumentException("illegal annotation type was specified."
                    + " conversion of " + CompositeKey.class.getName() 
                    + " requires annotation " +  CompositeKeyType.class.getName() + "."
                    + " propertyName = [" + propertyName + "]");
        }

        CompositeKeyType compositeKeyType = (CompositeKeyType) format;
        if (value == null) {
            return true;
        } else if (value instanceof String[]) {
            String[] array = (String[]) value;
            convertible = true;
            for (int i = 0; i < array.length; i++) {
                int keySize = StringUtil.split(array[i], ",").size();
                if (compositeKeyType.keySize() != keySize) {
                    convertible = false;
                    break;
                }
            }
        }

        if (!convertible) {
            ValidationResultMessageUtil.addResultMessage(context, propertyName,
                                                        conversionFailedMessageId, propertyDisplayName);
        }
        
        return convertible;
    }

    /**
     * {@inheritDoc}
     */
    public <T> Object convert(ValidationContext<T> context,
            String propertyName, Object value, Annotation format) {

        if (value == null) {
            return null;
        }

        String[] keys = (String[]) value;
        List<CompositeKey> ret = new ArrayList<CompositeKey>();
        for (String key : keys) {
            List<String> split = StringUtil.split(key, ",");
            ret .add(new CompositeKey(StringUtil.toArray(split)));
        }
        
        return ret.toArray(new CompositeKey[ret.size()]);
    }

}
