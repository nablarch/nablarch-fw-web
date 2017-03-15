package nablarch.fw.web.sample;

import java.util.Map;

import nablarch.core.db.connection.AppDbConnection;
import nablarch.core.db.connection.DbConnectionContext;
import nablarch.core.db.statement.SqlPStatement;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpServer;
import nablarch.test.support.tool.Hereis;

/**
 * Nablarchで構築された単純なアプリケーションの例
 * 
 * @author Iwauo Tajima
 */
public class SampleWebApp {
    /**
     * 本サンプルアプリケーションをデプロイしたサーバを
     * ポート8090上で起動する。
     */
    public static final void main (String... args) {
        new HttpServer()
            .setPort(8090)
            .setWarBasePath("classpath://nablarch/fw/web/sample/")
            .addHandler("/app/", new HttpRequestHandler() {
                public HttpResponse handle(HttpRequest request, ExecutionContext ctx) {
                    Map<String, String[]> bookData =  request.getParamMap();
                    String title     = bookData.get("title")[0];
                    String publisher = bookData.get("publisher")[0];
                    String authors   = bookData.get("authors").toString();
                    
                    AppDbConnection conn = DbConnectionContext.getConnection();
                    SqlPStatement stmt = conn.prepareStatement(Hereis.string());
                    /******************
                    INSERT INTO Book
                    VALUES (
                        ?, -- title
                        ?, -- publisher
                        ?  -- authors
                    )
                    ******************/
                    stmt.setString(0, title    );
                    stmt.setString(1, publisher);
                    stmt.setString(2, authors  );
                    stmt.execute();
                    return new HttpResponse(200, "servlet://jsp/index.jsp");
                }
                
            })
            .start()
            .join();
    }
}