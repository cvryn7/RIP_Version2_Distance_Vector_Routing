import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
* This class converts and saves the router's address
* as this application of RIP2 works on local host
* router address are given in form 000.000.008.000
* which in decimal translate to port number 2048
*
* @author Karan Bhagat
* @version 09-Oct-2015
*/

public class RouterAddress implements Serializable {
	private String actualAddress;
	private InetAddress ipAddr;
	private int portNumber;
	
	RouterAddress(String actualAddress){
		try {
			ipAddr = InetAddress.getByName("127.0.0.1");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		this.actualAddress = ""+actualAddress;
		processAndStore(""+actualAddress);
	}
	
	
	//converts the weird looking port number to 
	//actual port number
	void processAndStore(String actualAddress){
		String[] s = actualAddress.split("\\.");
		int num = 0;
		int tempnum;
		int power = 8;
		num = num + Integer.parseInt(s[s.length-1]);
		for( int i = s.length-2; i >= 0; i--){
			tempnum = Integer.parseInt(s[i]);
			for( int j = 0; j < 8; j++){
				if( (tempnum & (1<<j)) == 1<<j){
					num = num + (int)Math.pow(2, power);
					
				}
				power++;
			}
		}
		
		portNumber = num;
	}
	
	//setter getters
	
	public String getActualAddress(){
		return actualAddress;
	}
	
	public InetAddress getInetAddress(){
		return ipAddr;
	}
	
	public int getPortNumber(){
		return portNumber;
	}
	
}
