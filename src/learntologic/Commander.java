
package learntologic;

import org.json.JSONObject;

/**
 *
 * @author Brad Minogue
 */
public abstract class Commander {
    protected String[] commands;
    public boolean hasCommand(String command){
        for(int i = 0; i < commands.length; i++){
            if(commands[i].equalsIgnoreCase(command))
                return true;
        }
        return false;
    }
    public abstract  JSONObject executeCommand(JSONObject request);
}
