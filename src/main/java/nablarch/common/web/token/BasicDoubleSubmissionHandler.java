package nablarch.common.web.token;

import nablarch.core.message.ApplicationException;
import nablarch.core.message.Message;
import nablarch.core.message.MessageLevel;
import nablarch.core.message.MessageUtil;
import nablarch.core.util.StringUtil;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpErrorResponse;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
/**
 * {@link DoubleSubmissionHandler}の基本実装クラス。
 * @author Kiyohito Itoh
 */
public class BasicDoubleSubmissionHandler implements DoubleSubmissionHandler {

    /** 二重サブミットと判定した場合の遷移先のリソースパス */
    private String path;
    
    /** 二重サブミットと判定した場合の遷移先画面に表示するエラーメッセージに使用するメッセージID */
    private String messageId;
    
    /** 二重サブミットと判定した場合のレスポンスステータス */
    private int statusCode = 400;
    
    /**
     * 二重サブミットと判定した場合の遷移先のリソースパスを設定する。<br>
     * {@link OnDoubleSubmission}アノテーションで個別に指定していない場合は、ここに指定したリソースパスを使用する。
     * @param path 二重サブミットと判定した場合の遷移先のリソースパス
     */
    public void setPath(String path) {
        this.path = path;
    }
    
    /**
     * 二重サブミットと判定した場合の遷移先画面に表示するエラーメッセージに使用するメッセージIDを設定する。
     * {@link OnDoubleSubmission}アノテーションで個別に指定していない場合は、ここに指定したメッセージIDを使用する。
     * @param messageId 二重サブミットと判定した場合の遷移先画面に表示するエラーメッセージに使用するメッセージID
     */
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    /**
     * 二重サブミットと判定した場合のレスポンスステータスを設定する。
     * {@link OnDoubleSubmission}アノテーションで個別に指定していない場合は、ここに指定したレスポンスステータスを使用する。
     * デフォルトは400。
     * @param statusCode 二重サブミットと判定した場合のレスポンスステータス
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
    
    /**
     * {@inheritDoc}
     * <pre>
     * {@link HttpErrorResponse}を生成して返す。
     * 
     * {@link OnDoubleSubmission}アノテーションの属性が指定されている場合は、アノテーションの属性を使用する。
     * アノテーションの属性が指定されていない場合は、自身に設定されている値を使用する。
     * 
     * メッセージIDが指定されていない場合は、メッセージの取得を行わない。
     * </pre>
     */
    public HttpResponse
    handle(HttpRequest request, ExecutionContext context,
           Handler<HttpRequest, HttpResponse>  originalHandler,
           OnDoubleSubmission annotation) {
        
        if (!TokenUtil.isValidToken(request, context)) {
            
            String usingPath = annotation.path();
            if (StringUtil.isNullOrEmpty(usingPath)) {
                usingPath = path;
            }
            
            String usingMessageId = annotation.messageId();
            if (StringUtil.isNullOrEmpty(usingMessageId)) {
                usingMessageId = messageId;
            }
            
            int usingStatusCode = annotation.statusCode();
            if (usingStatusCode == -1) {
                usingStatusCode = statusCode;
            }
            
            if (usingMessageId == null) {
                throw new HttpErrorResponse(usingStatusCode, usingPath);
            } else {
                Message message = MessageUtil.createMessage(MessageLevel.ERROR, usingMessageId);
                throw new HttpErrorResponse(usingStatusCode, usingPath, new ApplicationException(message));
            }
        }
        return originalHandler.handle(request, context);
    }
}
