import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
* 
* class for storing subnetted address.
* 
* @author Karan Bhagat
* @version 09-Oct-2015
*/

public class CidrAddress implements Serializable {
	private String actualAddr;
	private InetAddress address;
	private int subnet;
	
	CidrAddress(String actualAddr){
		processAndStore(actualAddr);
		this.actualAddr = actualAddr;
	}
	
	//extract the subnet from CIDR address
	void processAndStore(String actualAddr){
		
		String[] s = actualAddr.split("/");
		
		try {
			address = InetAddress.getByName(s[0]);
			subnet = Integer.parseInt(s[1]);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (NumberFormatException e){
			subnet = 0;
		}
		
		
	}
	
	//getter setters
	
	public InetAddress getAddress(){
		return address;
	}
	
	public int getMast(){
		return subnet;
	}
	
	public String getActualAddress(){
		return actualAddr;
	}
}
