
package learntologic;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Brad Minogue
 */
public class EnhancedCommander extends Commander{

    java.sql.Statement statement;
    MysqlDataSource datasrc = new MysqlDataSource();
    java.util.Date d = new java.util.Date();
    java.sql.Connection conn;
    
    public EnhancedCommander(){
        commands = new String[4];
        commands[0]  = "enhanced/getmsgforuser";
        commands [1] = "enhanced/saveoption";
        commands [2] = "enhanced/getreasons";
        commands [3] = "enhanced/joinchannel";
    }

    @Override
    public JSONObject executeCommand(JSONObject request) {
        JSONObject retVal = new JSONObject();
        retVal.put("success", false);
        retVal.put("reason", "unkown command");
        switch(request.getString("action")){
            case "enhanced/getmsgforuser":
                return getMessageForAUser(request);
            case "enhanced/saveoption":
                return saveBan(request);
            case "enhanced/getreasons":
                return getOptions(request);
            case "enhanced/joinchannel":
                return joinChannel(request);
            default:
                break;
        }
        return retVal;
    }
    private JSONObject getMessageForAUser(JSONObject request){
        JSONObject retVal = new JSONObject();
        retVal.put("success", false);
        if(request.getString("user").contains(" ")){
            retVal.put("reason", "bad user name");
            return retVal;
        }
        JSONArray temp = Learntologic.chat.getActionsForUser(request.getString("user"));
        if(temp == null){
            retVal.put("reason", "no such user");
            return retVal;
        }
        retVal.put("actions", temp);
        retVal.put("success", true);
        retVal.put("reason", "got msges");
        return retVal;
    }
    private JSONObject saveBan(JSONObject request){
        JSONObject retVal = new JSONObject();
        try{
            String name = request.getString("user");
            String option = request.getString("option");
            String modName = request.getString("modName");
            String channel = request.getString("channel");
            name = URLDecoder.decode(name, "UTF-8");
            option = URLDecoder.decode(option, "UTF-8");
            modName = URLDecoder.decode(modName, "UTF-8");
            channel = URLDecoder.decode(channel, "UTF-8");

            if(name.contains(" ") || modName.contains(" ") || channel.contains(" ")){
                retVal.put("reason", "bad input");
                return retVal;
            }
            String lstMsg= Learntologic.chat.getLastMsg(name, channel);
            saveBanHelper(modName, name, channel, option, lstMsg);
            retVal.put("name",name);
            retVal.put("success", true);
            retVal.put("reason", "successfull");
        }
        catch(Exception e){
            System.err.println(e);
            retVal.put("success", false);
            retVal.put("reason", "ban does not exist or has already been saved");
        }
        return retVal;
    }
    private void saveBanHelper(String mod, String name, String channel, String reason, String lastMsg) throws Exception{
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
    private JSONObject getOptions(JSONObject request){
        JSONObject retVal = new JSONObject();
        String channel = request.getString("channel");
        try{
            channel = URLDecoder.decode(channel, "UTF-8");
        }
        catch(Exception e){
            retVal.put("success", false);
            retVal.put("reason", "bad input");
            return retVal;
        }
        if(channel.contains(" ")){
            retVal.put("success", false);
            retVal.put("reason", "bad input");
        }
        else{
            retVal.put("reasons", getOptionsHelper(channel));
            retVal.put("success", true);
            retVal.put("reason", "successfull");
        }
        return retVal;
    }
    private JSONArray getOptionsHelper(String channel){
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
    private JSONObject joinChannel(JSONObject request){
        JSONObject retVal = new JSONObject();
        String channel = request.getString("channel");
        String user = request.getString("user");
        try{
            channel = URLDecoder.decode(channel, "UTF-8");
            user = URLDecoder.decode(user, "UTF-8");
        }
        catch(Exception e){
            retVal.put("success", false);
            retVal.put("reason", "bad channel or user");
            return retVal;
        }
        if(channel.contains(" ")){
            retVal.put("success", false);
            retVal.put("reason", "bad channel");
            return retVal;
        }
        if(channel.contains(" ")){
            retVal.put("success", false);
            retVal.put("reason", "bad channel");
            return retVal;
        }
        if(Learntologic.chat.addUser(channel , user)){
            retVal.put("success", true);
            retVal.put("reason", "joined");
        }
        else{
            retVal.put("reason", "could not join channel with user, maybe user already joined?");
        }
        return retVal;
    }
}
