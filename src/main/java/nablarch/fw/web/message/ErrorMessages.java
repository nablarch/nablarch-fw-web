package nablarch.fw.web.message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nablarch.core.message.ApplicationException;
import nablarch.core.message.Message;
import nablarch.core.validation.ValidationResultMessage;

/**
 * エラーメッセージを保持するクラス。
 *
 * @author Hisaaki Sioiri
 */
public class ErrorMessages {

    /** 空のインスタンス */
    private static final ErrorMessages EMPTY_INSTANCE = new ErrorMessages();

    /** 全てのメッセージのリスト */
    private final List<String> allMessages = new ArrayList<String>();

    /** グローバルなメッセージのリスト */
    private final List<String> globalMessages = new ArrayList<String>();

    /** プロパティ名に紐づくメッセージを保持するオブジェクト **/
    private final PropertyMessages propertyMessages = new PropertyMessages();

    /**
     * /**
     * {@link ApplicationException}からオブジェクトを構築する。
     *
     * @param applicationException エラーメッセージを持つアプリケーション例外
     */
    public ErrorMessages(final ApplicationException applicationException) {
        for (final Message message : applicationException.getMessages()) {
            final String messageText = message.formatMessage();

            // 全てのメッセージ
            allMessages.add(messageText);

            if (message instanceof ValidationResultMessage) {
                // プロパティに紐づくメッセージ
                propertyMessages.addMessage(((ValidationResultMessage) message).getPropertyName(), messageText);
            } else {
                // プロパティに紐付かないグローバルなメッセージ
                globalMessages.add(messageText);
            }
        }
    }

    /**
     * 空の{@link ErrorMessages}を構築するコンストラクタ。
     */
    private ErrorMessages() {
        //nop
    }

    /**
     * 空の{@link ErrorMessages}インスタンスを返す。
     * {@link nablarch.fw.web.handler.HttpErrorHandler HttpErrorHandler}から使用されることを想定。
     * 
     * @return 空のインスタンス
     */
    public static ErrorMessages empty() {
        return EMPTY_INSTANCE;
    }

    /**
     * プロパティ名に対応したメッセージを返す。
     * <p>
     * プロパティ名に対応したメッセージが複数存在した場合には、最後に追加されたものを返す。
     * プロパティ名に対応したメッセージが存在しない場合は、{@code null}を返す。
     *
     * @param propertyName プロパティ名
     * @return プロパティ名に対応したメッセージ(存在しない場合は{@code null})
     */
    public String getMessage(final String propertyName) {
        verifyPropertyName(propertyName);
        return propertyMessages.getMessage(propertyName);
    }

    /**
     * 指定されたプロパティ名に対応したエラーがあるかを返す。
     *
     * @param propertyName プロパティ名
     * @return プロパティ名に対応したエラーがある場合は{@code true}
     */
    public boolean hasError(final String propertyName) {
        verifyPropertyName(propertyName);
        return propertyMessages.contains(propertyName);
    }

    /**
     * プロパティ名の検証を行う。
     * <p>
     * プロパティ名が{@code null}の場合は、{@link IllegalArgumentException}を送出する。
     *
     * @param propertyName プロパティ名
     */
    private void verifyPropertyName(final String propertyName) {
        if (propertyName == null) {
            throw new IllegalArgumentException("propertyName is null.");
        }
    }

    /**
     * プロパティに対応したメッセージをすべて返す。
     *
     * @return プロパティ対応したメッセージのリスト
     */
    public List<String> getPropertyMessages() {
        return Collections.unmodifiableList(propertyMessages.messages);
    }

    /**
     * グローバルなメッセージ(プロパティに紐付かないメッセージ)をすべて返す。
     *
     * @return グローバルなメッセージのリスト
     */
    public List<String> getGlobalMessages() {
        return Collections.unmodifiableList(globalMessages);
    }

    /**
     * 全てのメッセージを返す。
     *
     * @return 全てのメッセージのリスト
     */
    public List<String> getAllMessages() {
        return Collections.unmodifiableList(allMessages);
    }

    /**
     * プロパティに紐づくメッセージを保持するクラス。
     */
    private static class PropertyMessages {

        /** メッセージのリスト */
        private final List<String> messages = new ArrayList<String>();

        /** プロパティ名とメッセージ */
        private final Map<String, Integer> messageIndex = new HashMap<String, Integer>();

        /**
         * プロパティ名に対応したメッセージを追加する。
         *
         * @param propertyName プロパティ名
         * @param messageText メッセージテキスト
         */
        private void addMessage(final String propertyName, final String messageText) {
            messages.add(messageText);
            messageIndex.put(propertyName, messages.size() - 1);
        }

        /**
         * プロパティ名に対応したメッセージを返す。
         *
         * @param propertyName プロパティ名
         * @return プロパティ名に対応したメッセージ
         */
        private String getMessage(final String propertyName) {
            final Integer index = messageIndex.get(propertyName);
            return index != null ? messages.get(index) : null;
        }

        /**
         * プロパティ名に対応したメッセージを含むかどうかを判定する。
         *
         * @param propertyName プロパティ名
         * @return 指定されたプロパティ名を含む場合{@code true}
         */
        private boolean contains(final String propertyName) {
            return messageIndex.containsKey(propertyName);
        }
        
    }

}
