
package learntologic.enhanced;
import java.util.ArrayList;
import org.jibble.pircbot.*;
import org.json.JSONArray;
import org.json.JSONObject;
/**
 * Keeps track of all chat activates, when the chat bot sees a message or ban it 
 * informs this class which in turn can notify users
 * @author Brad Minogue
 */
public class Chat {
    /**
     * A simple class to record a message
     */
    private class Message{
        
        public String message;
        public final String USER;
        public final String CHANNEL;
        
        /**
         * constructor
         * @param msg that was sent
         * @param usr whom sent the msg
         * @param chn to which channel does the msg belong
         */
        Message(String msg, String usr, String chn){
            message = msg;
            USER = usr;
            CHANNEL = chn;
        }
    };
    
    /**
     * A class to allow us to track users and allow us to notify them
     */
    private class User{
        
        public ArrayList<String> channels;
        public JSONArray actions;
        public final String NAME;
        
        /**
         * Constructor to create a user without joining a channel
         * @param name of user
         */
        User(String name){
            channels = new ArrayList();
            actions = new JSONArray();
            NAME = name;
        }
        /**
         * constructor to create a user with a channel, this should be the case
         * most of the time
         * @param channel the user is joining
         * @param name of the user
         */
        User(String channel, String name){
            NAME = name;
            channels = new ArrayList();
            channels.add(channel);
            actions = new JSONArray();
        }
        
    };
    /**
     * list of users using this program, not those in a channel
     */
    private ArrayList<User> users;
    /**
     * the last message any given user sent. Used to help identify why someone
     * was banned by a mod. This is due to twitch not having a fully functional
     * irc system
     */
    private ArrayList<Message> lastKnownMessage;
    /**
     * list of people banned across all listening channels
     */
    private ArrayList<User> banList;
    /**
     * irc bot
     */
    private Bot bot;
    /**
     * 
     * @throws Exception If creation of the chat system fail, we need to fail out
     */
    public Chat() throws Exception{
        bot = new Bot(this);
        bot.connect(learntologic.config.T_SERVER, learntologic.config.T_PORT, learntologic.config.BOT_OATH);
        users = new ArrayList();
        lastKnownMessage = new ArrayList();
        banList = new ArrayList();
        bot.sendRawLine("CAP REQ :twitch.tv/commands");
        bot.sendRawLine("CAP REQ :twitch.tv/membership");
    }
    /**
     * Remove a user from a channel's ban list
     * @param channel
     * @param name 
     */
    public void removeFromBanList(String channel, String name){
        for(User current : banList){
            if(current.NAME.equalsIgnoreCase(name) && current.channels.contains(channel))
                banList.remove(current);
        }
    }
    /**
     * Notify all users of a ban
     * @param chan of the ban
     * @param name whom got baned
     */
    public void addBan(String chan, String name){
        JSONObject cObj = new JSONObject();
        cObj.put("action", "ban");
        cObj.put("user", name);
        cObj.put("channel", chan);
        for(User current : users){
            if(current.channels.contains(chan)){
                current.actions.put(cObj);
                banList.add(new User(name,chan));
                String msg = getLastMsg(name,chan);
                if(msg == null){
                    cObj.put("last_msg", "err::unkown_msg");
                }
                else{
                    cObj.put("last_msg", msg);
                }
            }
        }
    }
    /**
     * check to see if a user is banned
     * @param usr
     * @param chan
     * @return 
     */
    public boolean hasBan(String usr, String chan){
        for(User current : banList){
            if(current.NAME.equalsIgnoreCase(usr) && current.channels.contains(chan))
                return true;
        }
        return false;
    }
    /**
     * notify all users of the system of a message in a channel, if they are in
     * that channel
     * @param chan
     * @param name
     * @param msg 
     */
    public void addMsg(String chan, String name, String msg){
        JSONObject cObj = new JSONObject();
        cObj.put("action", "msg");
        cObj.put("user", name);
        cObj.put("channel", chan);
        cObj.put("msg", msg);
        for(User current : users){
            if(current.channels.contains(chan))
                current.actions.put(cObj);
        }
        //We need to save last known message for a user in a channel to save when
        //user is banned for the reason. (not the best setup, but twitch won't allow
        //us to do much more
        boolean msgSet = false;
        for(Message current : lastKnownMessage){
            if(current.CHANNEL.equalsIgnoreCase(chan) && 
                    current.USER.equalsIgnoreCase(name)){
                current.message = msg;
                msgSet = true;
                break;
            }
        }
        if(!msgSet)
            lastKnownMessage.add(new Message(msg, name, chan));
    }
    /**
     * add user to the system
     * @param channel
     * @param user
     * @return 
     */
    public boolean addUser(String channel, String user){
        if(user != null){
            //if a user is in the system
            for(User current : users){
                if(current.NAME.equalsIgnoreCase(user)){
                    if(current.channels.contains(channel)){
                       return true;
                    }
                    else{
                        if(channel != null){
                            current.channels.add(channel);
                            boolean flag = false;
                            //verify that we are already not in that channel
                            //bot will throw if we attempt to join a channel
                            //that we are currently in
                            for(int i = 0; i < bot.getChannels().length; i++){
                                if(bot.getChannels()[i].equalsIgnoreCase(channel)){
                                    flag = true;
                                    break;
                                }
                            }
                            if(!flag)
                                bot.joinChannel("#" + channel);
                            return true;
                        }
                    }
                }
            }
            //if a user is not already in the system
            if(channel != null){
                users.add(new User(channel, user));
                boolean flag = false;
                for(int i = 0; i < bot.getChannels().length; i++){
                    if(bot.getChannels()[i].equalsIgnoreCase(channel)){
                        flag = true;
                        break;
                    }
                }
                if(!flag)
                    bot.joinChannel("#" + channel);
                return true;
            }
            else{
                return false;
            }
        }
        return false;
    }
    /**
     * 
     * get all msgs and bans the user needs to know about
     * @param user
     * @return 
     */
    public JSONArray getActionsForUser(String user){
        for(User current : users)
            if(current.NAME.equalsIgnoreCase(user)){
                JSONArray retVal = current.actions;
                current.actions = new JSONArray();
                return retVal;
            }
        return null;
    }
    /**
     * get the last sent message, usually because they were banned
     * @param usr
     * @param channel
     * @return 
     */
    public String getLastMsg(String usr, String channel){
            for(Message lastMsg : lastKnownMessage){
                if(lastMsg.CHANNEL.equalsIgnoreCase(channel) && lastMsg.USER.equalsIgnoreCase(usr)){
                    return lastMsg.message;
                }
            }
        return null;
    }
}