import java.io.Serializable;

/**
* This class stores information for a neighbor
*
* @author Karan Bhagat
* @version 09-Oct-2015
*/

public class Neighbor implements Serializable {
	private RouterAddress addr;
	private int distance;
	
	Neighbor(String actual){
		addr = new RouterAddress(actual);
		distance = 1;
	}
	
	Neighbor(String actual, int distance){
		addr = new RouterAddress(actual);
		this.distance = distance;
	}
	
	//setter getters
	
	public RouterAddress getAddr(){
		return addr;
	}
	
	public int getDistance(){
		return distance;
	}
	
	public void setDistance(int distance){
		this.distance = distance;
	}

}
