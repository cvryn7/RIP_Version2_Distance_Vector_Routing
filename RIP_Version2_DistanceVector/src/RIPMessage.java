import java.io.Serializable;
import java.util.HashMap;

/**
*This class forms the wrapper for table containing
*RIPEnteries. This is RIP header
*
* @author Karan Bhagat
* @version 09-Oct-2015
*/

public class RIPMessage implements Serializable{
	int command;
	int version;
	HashMap<String,RIPEntry> routingTable = null;
	
	RIPMessage(int command,HashMap<String,RIPEntry> routingTable){
		this.command = command;
		version = 2;
		this.routingTable = routingTable;
	
	}
	
}
