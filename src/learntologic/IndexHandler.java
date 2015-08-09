
package learntologic;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Brad Minogue
 */
public class IndexHandler  implements HttpHandler{

        java.sql.Statement statement;
        MysqlDataSource datasrc = new MysqlDataSource();
        java.util.Date d = new java.util.Date();
        java.sql.Connection conn;
        java.sql.Timestamp date = new java.sql.Timestamp(d.getTime());
    @Override
    public void handle(HttpExchange he) throws IOException {
        JSONObject response = new JSONObject();
        response.put("success", false);
        response.put("reason", "unkown failure");
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
            //Check of validity
            if(!incomingRequest.has("action")){
                response.put("reason", "Missing Action");
            }
            else{
                switch(incomingRequest.getString("action")){
                    case "getvideo":
                        try{
                            JSONArray options = getAllVideos();
                            Random rand = new Random();
                            int randomNum = rand.nextInt(options.length());
                            response.put("value", ((JSONObject)options.get(randomNum)).get("video"));
                            response.put("success", true);
                            response.put("reason", "successfull");
                        }
                        catch(Exception e){
                            response.put("reason", "could not connect to server");
                            System.err.println(e);
                        }
                        break;
                    case "getAllVideos":
                        try{
                            JSONArray options = getAllVideos();
                            response.put("videos", options);
                            response.put("success", true);
                            response.put("reason", "successfull");
                        }
                        catch(Exception e){
                            response.put("reason", "could not connect to server");
                            System.err.println(e);
                        }
                        break;
                    case "enhanced/getmsgforuser":
                        if(incomingRequest.getString("user").contains(" ")){
                            response.put("reason", "bad user name");
                            return;
                        }
                        JSONArray temp = Learntologic.chat.getActionsForUser(incomingRequest.getString("user"));
                        if(temp == null){
                            response.put("reason", "no such user");
                            return;
                        }
                        response.put("actions", temp);
                        response.put("success", true);
                        response.put("reason", "got msges");
                        break;
                    case "enhanced/saveoption":
                        String name = incomingRequest.getString("user");
                        String option = incomingRequest.getString("option");
                        String modName = incomingRequest.getString("modName");
                        String channel = incomingRequest.getString("channel");
                        name = URLDecoder.decode(name, "UTF-8");
                        option = URLDecoder.decode(option, "UTF-8");
                        modName = URLDecoder.decode(modName, "UTF-8");
                        channel = URLDecoder.decode(channel, "UTF-8");
                        
                        if(name.contains(" ") || modName.contains(" ") || channel.contains(" ")){
                            response.put("reason", "bad input");
                            return;
                        }
                        String lstMsg= Learntologic.chat.getLastMsg(name, channel);
                        saveBan(modName, name, channel, option, lstMsg);
                        response.put("name",name);
                        response.put("success", true);
                        response.put("reason", "successfull");
                        break;
                    case "enhanced/getreasons":
                        channel = incomingRequest.getString("channel");
                        channel = URLDecoder.decode(channel, "UTF-8");
                        if(channel.contains(" ")){
                            response.put("reason", "bad input");
                            return;
                        }
                        response.put("reasons", getOptions(channel));
                        response.put("success", true);
                        response.put("reason", "successfull");
                        break;
                    case "enhanced/joinchannel":
                        channel = incomingRequest.getString("channel");
                        channel = URLDecoder.decode(channel, "UTF-8");
                        if(channel.contains(" ")){
                            response.put("reason", "bad channel");
                            return;
                        }
                        String user = incomingRequest.getString("user");
                        user = URLDecoder.decode(user, "UTF-8");
                        if(channel.contains(" ")){
                            response.put("reason", "bad user");
                            return;
                        }
                        if(Learntologic.chat.addUser(channel , user)){
                            response.put("success", true);
                            response.put("reason", "joined");
                        }
                        else{
                            response.put("reason", "could not join channel with user, maybe user already joined?");
                        }
                        break;
                    default:
                        response.put("reason", "unsuported action");
                        break;
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
    private JSONArray getAllVideos() throws Exception{
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
    private void saveBan(String mod, String name, String channel, String reason, String lastMsg) throws Exception{
        if(Learntologic.chat.hasBan(name, channel)){
            reason = URLEncoder.encode(reason, "UTF-8")
                         .replaceAll("\\+", "%20")
                         .replaceAll("\\%21", "!")
                         .replaceAll("\\%27", "'")
                         .replaceAll("\\%28", "(")
                         .replaceAll("\\%29", ")")
                         .replaceAll("\\%7E", "~");
            
            lastMsg = URLEncoder.encode(lastMsg, "UTF-8")
                         .replaceAll("\\+", "%20")
                         .replaceAll("\\%21", "!")
                         .replaceAll("\\%27", "'")
                         .replaceAll("\\%28", "(")
                         .replaceAll("\\%29", ")")
                         .replaceAll("\\%7E", "~");
            if(lastMsg.length() > 160)
                lastMsg = lastMsg.substring(0, 156) + "...";
            datasrc.setUser("user");
            datasrc.setServerName("localhost");
            datasrc.setPort(3306);
            datasrc.setDatabaseName("enhanced_mod");
            conn = datasrc.getConnection();
            String val = "INSERT INTO `ban` (`user`, `channel`, `reason`, `last_known_message`, `mod`) VALUES (?, ?, ?, ?, ?);";
            PreparedStatement ps = conn.prepareStatement(val);
            ps.setString(1, name);
            ps.setString(2, channel);
            ps.setString(3, reason);
            ps.setString(4, lastMsg);
            ps.setString(5, mod);
            ps.execute();
        }
    }
    private JSONArray getOptions(String channel){
        JSONArray array = new JSONArray();
        try {
            datasrc.setUser("user");
            datasrc.setServerName("localhost");
            datasrc.setPort(3306);
            datasrc.setDatabaseName("enhanced_mod");
            conn = datasrc.getConnection();
            String val = "SELECT * FROM reason WHERE channel = ?";
            PreparedStatement ps = conn.prepareStatement(val);
            ps.setString(1, "fryedman");
            //TODO:
            //ps.setString(1, channel);
            ResultSet set = ps.executeQuery();
            while(set.next()){
                JSONObject current = new JSONObject();
                current.put("reason", set.getString("reason"));
                array.put(current);
            }
        } 
        catch (Exception e) {
            System.out.println(e);
        }
        return array;
    }
}