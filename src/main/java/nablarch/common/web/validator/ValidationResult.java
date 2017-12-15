package nablarch.common.web.validator;

import nablarch.core.message.Message;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * {@link ValidationStrategy}のバリデーション結果を格納するクラス。
 *
 * @author Tsuyoshi Kawasaki
 */
public class ValidationResult {

    /** バリデーション対象となったオブジェクト */
    private final Serializable object;

    /** バリデーションエラー時のメッセージ */
    private final List<Message> messages;

    /**
     * バリデーション成功時の結果を生成する。
     *
     * @param object バリデーションが成功したオブジェクト
     * @return インスタンス
     */
    public static ValidationResult createValidResult(Serializable object) {
        return new ValidationResult(object, Collections.<Message>emptyList());
    }

    /**
     * バリデーション失敗時の結果を生成する。
     * バリデーション失敗時にはオブジェクトが生成されない実装の場合は、
     * {@link ValidationResult#createInvalidResult(Serializable, List)}ではなく、
     * こちらのメソッドを使用する。
     *
     * @param messages バリデーションエラー時のメッセージ
     * @return インスタンス
     */
    public static ValidationResult createInvalidResult(List<Message> messages) {
        return new ValidationResult(null, messages);
    }

    /**
     * バリデーション失敗時の結果を生成する。
     * バリデーション失敗時にはオブジェクトが生成されない実装の場合は、
     * 第1引数には{@code null}を指定すること。
     *
     * @param object バリデーションが失敗したオブジェクト
     * @param messages バリデーションエラー時のメッセージ
     * @return インスタンス
     */
    public static ValidationResult createInvalidResult(Serializable object, List<Message> messages) {
        return new ValidationResult(object, messages);
    }

    /**
     * コンストラクタ。
     * @param object バリデーション対象のオブジェクト
     * @param messages バリデーションエラー時のメッセージ
     */
    private ValidationResult(Serializable object, List<Message> messages) {
        this.object = object;
        this.messages = messages;
    }

    /**
     * バリデーション結果が妥当であったかどうか判定する。
     *
     * @return 妥当である場合、真
     */
    public boolean isValid() {
        return messages.isEmpty();
    }

    /**
     * バリデーションエラー時のメッセージを取得する。
     * バリデーション成功時は空のリストが返却される。
     *
     * @return バリデーションエラー時のメッセージ
     */
    public List<Message> getMessage() {
        return messages;
    }

    /**
     * バリデーション対象となったオブジェクトを取得する。
     * {@link ValidationStrategy}の実装クラスが、バリデーション実行時にオブジェクトを生成しない実装の場合は
     * {@code null}が返却される。
     *
     * @return バリデーション対象となったオブジェクト
     */
    public Serializable getObject() {
        return object;
    }
}
