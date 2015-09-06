
package learntologic;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import java.sql.ResultSet;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Brad Minogue
 */
public class VideoCommander extends Commander {
    public VideoCommander(){
        commands = new String[2];
        commands[0]  = "getvideo";
        commands [1] = "getAllVideos";
    }

    @Override
    public JSONObject executeCommand(JSONObject request) {
        JSONObject retVal = new JSONObject();
        retVal.put("success", false);
        retVal.put("reason", "unkown command");
        switch(request.getString("action")){
            case "getvideo":
                return getRanVideo();
            case "getAllVideos":
                return getAllVideos();
            default:
                break;
        }
        return retVal;
    }
    private JSONObject getRanVideo(){
        JSONObject response = new JSONObject();
        try{
        JSONArray options = getAllVideosHelper();
        Random rand = new Random();
        int randomNum = rand.nextInt(options.length());
        response.put("value", ((JSONObject)options.get(randomNum)).get("video"));
        response.put("success", true);
        response.put("reason", "successfull");
        }
        catch(Exception e){
            response.put("success", false);
            response.put("reason", "could not connect to server");
        }
        return response;
    }
    private JSONObject getAllVideos(){
        JSONObject response = new JSONObject();
        try{
            response.put("videos", getAllVideosHelper());
            response.put("success", true);
            response.put("reason", "successfull");
        }
        catch(Exception e){
            response.put("success", false);
            response.put("reason", "could not connect to server");
        }
        return response;
    }
    private JSONArray getAllVideosHelper() throws Exception{
        JSONArray retVal = new JSONArray();
        java.sql.Statement statement;
        MysqlDataSource datasrc = new MysqlDataSource();
        datasrc.setUser("user");
        datasrc.setServerName("localhost");
        datasrc.setPort(3306);
        datasrc.setDatabaseName("video_db");
        java.sql.Connection conn = datasrc.getConnection();
        java.util.Date d = new java.util.Date();
        statement = conn.createStatement();
        ResultSet set = statement.executeQuery("SELECT * FROM `videos`;");
        while(set.next()){
            JSONObject current = new JSONObject();
            current.put("video", set.getString("name"));
            current.put("source", set.getString("source"));
            retVal.put(current);
        }
        if(conn != null){
            conn.close();
            conn = null;
        }
        return retVal;
    }
}
