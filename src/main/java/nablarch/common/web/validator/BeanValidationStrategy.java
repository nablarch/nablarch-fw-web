package nablarch.common.web.validator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import nablarch.common.web.interceptor.InjectForm;
import nablarch.core.beans.BeanUtil;
import nablarch.core.message.ApplicationException;
import nablarch.core.message.Message;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;
import nablarch.core.validation.ValidationResultMessage;
import nablarch.core.validation.ee.ConstraintViolationConverterFactory;
import nablarch.core.validation.ee.ValidatorUtil;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * BeanValidationを使用する場合のリクエスト内容のバリデーション、オブジェクト(Bean)生成ロジック.
 *
 * @author sumida
 */
public class BeanValidationStrategy implements ValidationStrategy {

    /**
     * {@code BeanValidationStrategy}を生成する。
     */
    @Published(tag = "architect")
    public BeanValidationStrategy() {
    }

    public Serializable validate(HttpRequest request, InjectForm annotation, boolean notUse,
            ServletExecutionContext context) {

        Map<String, String[]> rawRequestParamMap = request.getParamMap();
        Map<String, String[]> requestParamMap = getMapWithConvertedKey(annotation.prefix(), rawRequestParamMap);

        Serializable bean = BeanUtil.createAndCopy(annotation.form(), requestParamMap);
        Validator validator = ValidatorUtil.getValidator();
        Set<ConstraintViolation<Serializable>> results = validator.validate(bean);
        if (!results.isEmpty()) {
            // エラーのとき、リクエストスコープにbeanを設定する
            context.setRequestScopedVar(annotation.name(), bean);
            List<Message> messages = new ConstraintViolationConverterFactory().create(annotation.prefix()).convert(results);
            throw new ApplicationException(sortMessages(messages, context, annotation));
        }
        return bean;
    }

    /**
     * メッセージをソートする。
     * <p>
     * ソートされる順序は、{@link ServletRequest#getParameterNames()}の順となる。
     * {@link ServletRequest#getParameterNames()}に存在しない項目は、メッセージリストの末尾に移動する。
     *
     * @param messages ソート対象のメッセージリスト
     * @param context Servlet実行コンテキスト
     * @param injectForm {@code InjectForm}アノテーション
     * @return ソートしたメッセージリスト
     */
    @Published(tag = "architect")
    protected static List<Message> sortMessages(
            final List<Message> messages, final ServletExecutionContext context, final InjectForm injectForm) {
        final ServletRequest request = context.getServletRequest()
                                              .getRequest();

        @SuppressWarnings("unchecked")
        final List<String> parameterNames = Collections.list(request.getParameterNames());

        final List<Message> sortedMessage = new ArrayList<Message>(messages);
        Collections.sort(sortedMessage, new Comparator<Message>() {
            @Override
            public int compare(final Message m1, final Message m2) {
                final int index1 = getParameterIndex(parameterNames, m1);
                final int index2 = getParameterIndex(parameterNames, m2);
                
                if (index1 < index2) {
                    // m1のほうが小さい場合
                    return -1;
                } else if (index2 < index1) {
                    // m2のほうが小さい場合
                    return 1;
                } else {
                    // それ以外は同じと扱う
                    return 0;
                }
            }
        });
        return sortedMessage;
    }

    /**
     * メッセージが持つプロパティ名がパラメータ名の何番目の要素か返す。
     *
     * @param parameterNames パラメータ名のリスト
     * @param message メッセージ
     * @return パラメータ名の何番目か(パラメータ名にない場合はプロパティ名を持たない場合は{@link Integer#MAX_VALUE}) 
     */
    private static int getParameterIndex(final List<String> parameterNames, final Message message) {
        if (message instanceof ValidationResultMessage) {
            final int index = parameterNames.indexOf(((ValidationResultMessage) message).getPropertyName());
            return index == -1 ? Integer.MAX_VALUE : index;
        } else {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * {@link InjectForm}のprefixに指定した文字列を、キーの先頭から削除したMapオブジェクトを返す.<br>
     *
     * @param prefix 項目名から削除するプレフィックス
     * @param reqParamMap リクエストパラメータを格納したMap
     * @return キーからprefixが削除されたMap
     */
    private Map<String, String[]> getMapWithConvertedKey(String prefix, Map<String, String[]> reqParamMap) {
        if (StringUtil.isNullOrEmpty(prefix)) {
            // プレフィックスが指定されない場合、全てが対象とする
            return new HashMap<String, String[]>(reqParamMap);
        }

        final String prefixName = prefix + '.';
        final int prefixLength = prefixName.length();
        Map<String, String[]> convertedMap = new HashMap<String, String[]>();
        for (Map.Entry<String, String[]> entry : reqParamMap.entrySet()) {
            final String key = entry.getKey();
            final String[] value = entry.getValue();
            
            if (key.startsWith(prefixName)) {
                convertedMap.put(key.substring(prefixLength), value);
            }
        }
        return convertedMap;
    }
}
