
package learntologic.enhanced;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jibble.pircbot.*;
/**
 *
 * @author Brad Minogue
 */
public class Bot extends PircBot{
    final Chat OWNER;
    public Bot(Chat owner) throws Exception{
        this.setName(learntologic.config.BOT_NAME);
        OWNER = owner;
    }
    @Override
    public void onAction(String sender, String login, String hostname, String target, String action){
        onMessage(target, sender, login, hostname, action);
    }
    @Override
    public void onMessage(String channel, String sender,
                       String login, String hostname, String message) {
        try {
            this.OWNER.addMsg(channel.substring(1), sender, URLEncoder.encode(message, "UTF-8")
                         .replaceAll("\\+", "%20")
                         .replaceAll("\\%21", "!")
                         .replaceAll("\\%27", "'")
                         .replaceAll("\\%28", "(")
                         .replaceAll("\\%29", ")")
                         .replaceAll("\\%7E", "~"));
        } 
        catch (Exception e) {
            System.err.println(e);
        }
    }
    @Override
    public void onUnknown(String line){
        if(line.contains("CLEARCHAT")){
            String val = line.substring(line.indexOf("#"));
            String chan = val.substring(1,val.indexOf(" :"));
            val = val.substring(val.indexOf(" :")+2);
            this.OWNER.addBan(chan, val);
        }
        else{
            System.err.println(line);
        }
    }
    @Override
    public void onNotice(String sourceNick, String sourceLogin, String sourceHostname, String target, String notice){
        System.out.println("sourceNick " + sourceNick);
        System.out.println("sourceLogin " + sourceLogin);
        System.out.println("sourceHostname " + sourceHostname);
        System.out.println("target " + target);
        System.out.println("notice " + notice);
    }
}
