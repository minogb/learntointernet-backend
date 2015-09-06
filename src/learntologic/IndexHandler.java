
package learntologic;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import org.json.JSONObject;

/**
 *
 * @author Brad Minogue
 */
public class IndexHandler  implements HttpHandler{
    private static final Commander[] COMMANDER_LIST = {
        new EnhancedCommander(),
        new VideoCommander()
    };
    java.sql.Statement statement;
    MysqlDataSource datasrc = new MysqlDataSource();
    java.util.Date d = new java.util.Date();
    java.sql.Connection conn;
    java.sql.Timestamp date = new java.sql.Timestamp(d.getTime());
    @Override
    public void handle(HttpExchange he) throws IOException {
        JSONObject response = new JSONObject();
        response.put("success", false);
        response.put("reason", "unkown command");
        try{
            //Need to parse stream to string to jsonobject
            StringBuilder out = new StringBuilder();
            BufferedReader  br = new BufferedReader (new InputStreamReader(he.getRequestBody()));
            String read= br.readLine();
            while(read != null){
                out.append(read);
                read = br.readLine();
            }

            JSONObject incomingRequest = new JSONObject(out.toString());
            String command = incomingRequest.getString("action");
            if(!incomingRequest.has("action")){
                response.put("reason", "Missing Action");
            }
            else{
                for(int i = 0; i < COMMANDER_LIST.length; i++){
                    if(COMMANDER_LIST[i].hasCommand(command)){
                       response = COMMANDER_LIST[i].executeCommand(incomingRequest);
                    }
                }
            }
        }
        catch(Exception e){
            System.err.println(e);
        }
        finally{
            he.sendResponseHeaders(200, response.toString().length());
            OutputStream oout = he.getResponseBody();
            oout.write(response.toString().getBytes());
            oout.close();
        }
    }
}