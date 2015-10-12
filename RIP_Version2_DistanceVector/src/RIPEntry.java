import java.io.Serializable;


/**
* This class defines single RIP entery 
*
* @author Karan Bhagat
* @version 09-Oct-2015
*/


public class RIPEntry implements Serializable {
	
	//for saving subnetted address basically destination address
	private CidrAddress networkAddress;
	private RouterAddress nextHop;
	private int metric; // cost
		
	//overriden constructor for various inputs
	RIPEntry(String netAddr, String nextHop){
		networkAddress = new CidrAddress(netAddr);
		this.nextHop = new RouterAddress(nextHop);
		metric = 1;
	}
	
	RIPEntry(String netAddr, String nextHop, String metric){
		networkAddress = new CidrAddress(netAddr);
		this.nextHop = new RouterAddress(nextHop);
		try{
		this.metric = Integer.parseInt(metric) ;
		}catch(NumberFormatException e){
			this.metric = 1;
		}
	}
	
	RIPEntry(String netAddr, RouterAddress nextHop, String metric){
		networkAddress = new CidrAddress(netAddr);
		this.nextHop = new RouterAddress(nextHop.getActualAddress());
		try{
		this.metric = Integer.parseInt(metric) ;
		}catch(NumberFormatException e){
			this.metric = 1;
		}
	}
	
	//getter setters

	public CidrAddress getNetworkAddress(){
		return networkAddress;
	}
	
	public RouterAddress getNextHop(){
		return nextHop;
	}
	
	public int getMetric(){
		return metric;
	}
	
	public void setMetric(int newMetric){
		metric = newMetric;
	}
}
