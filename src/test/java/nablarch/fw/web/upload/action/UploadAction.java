package nablarch.fw.web.upload.action;

import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.upload.PartInfo;

import java.util.List;
import java.util.Map;

/**
 * {@link nablarch.fw.web.upload.MultipartHandlerTest}で使用するアクションクラス。
 *
 * @author hisaaki sioiri
 */
public class UploadAction {
    public HttpResponse doUpload(HttpRequest request, ExecutionContext context) {
        Map<String,List<PartInfo>> multipart = request.getMultipart();
        System.out.println("multipart = " + multipart);
        return new HttpResponse("forward:///test/action/ForwardAction/request");
    }
}
