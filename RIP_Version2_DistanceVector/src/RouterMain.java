import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;



/**
 * This class imitates a router working on a single machine
 * with ip as local host. Multiple instances of this RouterMain 
 * class act as individual routers. This class requires configuration
 * file as command line argument.
 *
 * @author Karan Bhagat
 * @version 09-Oct-2015
 */

public class RouterMain {
	//map for storing routing table entries
	HashMap<String,RIPEntry> routingTable = new HashMap<String,RIPEntry>();
	//stores neighbor attributes corresponding to neighbor port number
	HashMap<Integer,Neighbor> neighborTable = new HashMap<Integer,Neighbor>();
	//stores thread of each neighbor which receive message from neighbor
	HashMap<Integer,Thread> neighborThreads = new HashMap<Integer,Thread>();
	//This acts as queue..although doesn't act as queue..this receive messages from routers
	HashMap<Integer,RIPMessage> messageList = new HashMap<Integer,RIPMessage>();
	//port number address of current router
	RouterAddress myAddrs = null;
	//dummy value to use 0 port number for printing
	RouterAddress zeroPort = new RouterAddress("000.000.000.000");
	//This socket enable this router to send and receive UDP packets
	DatagramSocket dSocket = null;

	//Main program require Configuration File as parameter
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		//Create instance of this class so that non-static members can be called 
		//from static reference
		RouterMain rm = new RouterMain();

		//read command line arguments
		if( args.length != 1){
			throw new IllegalArgumentException();
		}

		File configFile = new File(args[0]);

		//readFile and build initial routing table and neighbor table
		rm.buildInitialTable(configFile);

		//bind socket to this routers port number address
		try {
			rm.dSocket = new DatagramSocket(rm.myAddrs.getPortNumber());
		} catch (SocketException e) {
			e.printStackTrace();
		}

		//Start message receiving thread corresponding to each neighbor 
		//and store in neighborThreads Map. 
		//These threads actually don't listen on port..these thread checks
		//messageList map for message which corresponds to neighbor port number
		Iterator<Integer> keyItr = rm.neighborTable.keySet().iterator();
		int n;
		while(keyItr.hasNext()){
			n = keyItr.next();
			rm.neighborThreads.put(n,new Thread(rm.new RecvFromNeighbor(n)));
			rm.neighborThreads.get(n).start();
		}

		//send initial routing table to neighbors
		//0 : print table on command line
		//1 : don't print
		rm.sendToNeighbors(2,0);
		//starting thread for receiving messages
		(new Thread( rm.new RecvMessage())).start();
		//starting thread for reading user input
		(new Thread( rm.new NewInput())).start();
		//starting thread for sending table periodically
		(new Thread( rm.new SendPeriodically())).start();


	}

	/**
	 * Build the initial routing table and neighbor table using
	 * input config file
	 *
	 * @param configFile
	 *            Input config file
	 *
	 */
	void buildInitialTable(File configFile) {
		
		//reading file line by line
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(configFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		String s;
		String[] sAry;

		try {
			//parsing each line.
			while((s = br.readLine()) != null){
				//System.out.println(s);
				sAry = s.split(" ");
				if( sAry[0].equals("ADDRESS:")){
					myAddrs = new RouterAddress(""+sAry[1]);
				}else if( sAry[0].equals("NEIGHBOR:")){
					neighborTable.put((new Neighbor(""+sAry[1])).getAddr().getPortNumber(),new Neighbor(""+sAry[1]));
				}else if( sAry[0].equals("NETWORK:")){
					routingTable.put(""+sAry[1],new RIPEntry(""+sAry[1],myAddrs,"0"));
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * This method sends current routing table to all the
	 * neighbors
	 *
	 * @param command
	 *            Specify which kind of msg to send Requent 1, Response 2; // I never used this although
	 * @param printFlag
	 * 				Specify either to print table or not 0 mean print 1 means not
	 *
	 */

	synchronized void sendToNeighbors(int command,int printFlag){
		//declare datapacket and related upd stuff
		DatagramPacket tablePacket = null;
		byte[] byteBuff;
		ByteArrayOutputStream byteSendStream;
		ObjectOutputStream objectSendStream;
		Neighbor destProps;
		InetAddress destAddr;
		int destPort;
		//forming RIPMessage
		RIPMessage ripMsg = new RIPMessage(command,routingTable);
		//iterate over neighborTable to send table to each neighbor
		Iterator<Integer> keyItr = neighborTable.keySet().iterator();
		try{
			while(keyItr.hasNext()){
				byteSendStream = new ByteArrayOutputStream(5000);
				objectSendStream = new ObjectOutputStream(new BufferedOutputStream(byteSendStream));
				///these two flush are important for working of data send of udp
				objectSendStream.flush();
				objectSendStream.writeObject(ripMsg);
				objectSendStream.flush();
				byteBuff = byteSendStream.toByteArray();
				//destinatio properties for getting address and port of destination
				destProps = neighborTable.get(keyItr.next());
				destAddr = destProps.getAddr().getInetAddress();
				destPort = destProps.getAddr().getPortNumber();
				tablePacket = new DatagramPacket(byteBuff,byteBuff.length,destAddr,destPort);
				dSocket.send(tablePacket);
				objectSendStream.close();
				byteSendStream.close();
			}
		}catch(IOException e){
			System.out.println(e);
		}
		// printFlag : 0 means print table otherwise don't print
		if( printFlag == 0){
			printRoutingTable();
		}
	}
	/**
	 * This method updates routing table when ever user
	 * updates any neighbor's link cost
	 *
	 * @param n
	 *            current neighbor which got updated
	 * @param oldDistance
	 *            old link cost between this router and neighbor
	 * 
	 * @return triggeredUpdate
	 * 				flag to check if to send table immediately
	 *
	 */
	boolean updateRoutingTable(Neighbor n,int oldDistance){
		boolean triggeredUpdate = false;

		//iterate over routingTable to check if updating link updates any route
		Iterator<String> keyItr = routingTable.keySet().iterator();
		String key;
		int currentMetric;
		int newMetric;
		while( keyItr.hasNext() ){
			key = keyItr.next();
			//if route to destination goes through neighbor then update cost of route
			if( routingTable.get(key).getNextHop().getPortNumber() == n.getAddr().getPortNumber()){
				currentMetric = routingTable.get(key).getMetric();
				newMetric = currentMetric-oldDistance+n.getDistance();
				if( newMetric > 16){
					newMetric = 16;
				}
				routingTable.get(key).setMetric(newMetric);
				triggeredUpdate = true;
			}
		}
		//return triggeredUpdate to send to neighbors immediately in case of change
		return triggeredUpdate;
	}

	/**
	 * Prints routing table
	 */
	public void printRoutingTable(){
		Iterator<String> keyItr = routingTable.keySet().iterator();

		System.out.println("    "+"Address"+"                "+"Next Hop"+"          "+"Cost");
		System.out.println("=====================================================");
		String key;
		String nActualAddrs;
		int cost;
		while(keyItr.hasNext()){
			key = keyItr.next();
			nActualAddrs = routingTable.get(key).getNextHop().getActualAddress();
			if( nActualAddrs.equals(myAddrs.getActualAddress())){
				nActualAddrs = zeroPort.getActualAddress();
			}
			cost = routingTable.get(key).getMetric();
			System.out.println("   "+key+"     "+nActualAddrs+"     "+cost);
		}
			System.out.println();
	}
	
	/**
	 * Remove all entries related to dead neighbor from
	 * all tables
	 *
	 * @param neighborPort
	 *            port number of dead neighbor          
	 *
	 */
	synchronized void deleteAndCleanNeighbor(int neighborPort){
		//iterate over routing table to delete destination reachable from dead router
		Iterator<String> keyItr = routingTable.keySet().iterator();
		String neighborAddr = neighborTable.get(neighborPort).getAddr().getActualAddress();
		String key;
		boolean triggeredUpdate = false;
		while(keyItr.hasNext()){
			key = keyItr.next();
			if( neighborAddr.equals(routingTable.get(key).getNextHop().getActualAddress())){	
				keyItr.remove();
				System.out.println("Deleted");
				triggeredUpdate = true;
			}
		}
		//delete dead neighbor entries from other tables
		neighborTable.remove(neighborPort);
		neighborThreads.remove(neighborPort);
		messageList.remove(neighborPort);
		if( triggeredUpdate ){
			sendToNeighbors(2,0);
		}
	}
	
	/**
	 * Inner runnable class which sends routing table to 
	 * all the neighbors after 2 seconds
	 * 
	 * @author Karan Bhagat
	 * @version 09-Oct-2015
	 */

	class SendPeriodically implements Runnable{

		public void run() {
			while(true){
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				sendToNeighbors(1,1);

			}
		}

	}

	
	/**
	 * Inner runnable class which listen for user input
	 * regarding updated link cost
	 * 
	 * @author Karan Bhagat
	 * @version 09-Oct-2015
	 */

	class NewInput implements Runnable{

		public void run() {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String[] sInput;
			String ip;
			String metric;
			Neighbor n;
			int oldDistance;
			boolean triggeredUpdate = false;

			while(true){
				try{
					sInput = br.readLine().split(" ");
					ip = sInput[0];
					metric = sInput[1];

					n = new Neighbor(ip,Integer.parseInt(metric));

					if( n.getDistance() > 16 ){
						n.setDistance(16);
					}
					//calculate new distance and update in neighbor table
					if( neighborTable.containsKey(n.getAddr().getPortNumber())){
						oldDistance = neighborTable.get(n.getAddr().getPortNumber()).getDistance();
						neighborTable.get(n.getAddr().getPortNumber()).setDistance(n.getDistance());
						triggeredUpdate = updateRoutingTable(n,oldDistance);
					}else{
						neighborTable.put(n.getAddr().getPortNumber(),new Neighbor(ip,n.getDistance()));
						triggeredUpdate = true;
					}
					if( triggeredUpdate ){
						sendToNeighbors(2,0);
						triggeredUpdate = false;
					}

				}catch(Exception e){
					System.out.println("wrong input..try again");
				}
			}


		}

	}

	
	/**
	 *Inner runnable class which constantly listens
	 *for messages from other routers
	 *
	 * 
	 * @author Karan Bhagat
	 * @version 09-Oct-2015
	 */

	class RecvMessage implements Runnable{

		RecvMessage(){
		}

		@SuppressWarnings("unchecked")
		public void run() {
			byte[] recvBuff = new byte[5000];
			ByteArrayInputStream byteRecvStream;
			ObjectInputStream objectRecvStream;
			DatagramPacket recvPacket;
			try{
				while(true){

					recvPacket = new DatagramPacket(recvBuff,recvBuff.length);
					dSocket.receive(recvPacket);
					byteRecvStream = new ByteArrayInputStream(recvBuff);
					objectRecvStream = new ObjectInputStream(new BufferedInputStream(byteRecvStream));
					RIPMessage ripMsg = (RIPMessage)objectRecvStream.readObject();
					//add newly receive packet to messageList with port number as sender port number
					messageList.put(recvPacket.getPort(), ripMsg );
				}
			}catch(IOException e){
				e.printStackTrace();
			}catch(ClassNotFoundException e){
				e.printStackTrace();
			}
		}

	}

	
	/**
	 * This method contains the actual distance vector algorithm logic
	 * This logic is not based on distance vector table but on routing table
	 * and its little complex
	 * 
	 * @param newTable
	 *            new routing table received from neighbor
	 * @param neigbhorPort
	 *            Neighbor's port number
	 *
	 */
	synchronized void processNewMsg(HashMap<String,RIPEntry> newTable, int neighborPort){
		
		//for iterating over each entery of received routing table
		Iterator<String> keyItr = newTable.keySet().iterator();
		String key;
		Boolean triggeredUpdate = false;
		int oldNeighborLink = neighborTable.get(neighborPort).getDistance();;
		int newNeighborLink =0;
		String neighborAddr = neighborTable.get(neighborPort).getAddr().getActualAddress();
		boolean linkChange = false;
		//iterate over new incoming table

		//first check for each destination that if nextHop of destination is same as current router
		//then check if for same destination next hop in this router's routing table is this router's address
		//this verfies that destination is directly attached this router and cost metric is actually link cost 
		//between this router and sender neighbor...hence from this we can check if link cost has changed 
		while(keyItr.hasNext()){
			key = keyItr.next();
			if( myAddrs.getActualAddress().equals(newTable.get(key).getNextHop().getActualAddress())){
				if( routingTable.get(key).getNextHop().getActualAddress().equals(myAddrs.getActualAddress())){
					newNeighborLink = newTable.get(key).getMetric();
					if( oldNeighborLink != newNeighborLink){
						linkChange = true;
						neighborTable.get(neighborPort).setDistance(newNeighborLink);
						System.out.println("link changes "+ newNeighborLink +" "+oldNeighborLink);
						break;
					}
				}
			}
		}


		//reset iterator over new routing table to check each and every entry of new routing table 
		//against current router's routing table
		keyItr = newTable.keySet().iterator();
		while(keyItr.hasNext()){
			key = keyItr.next();

			//check only if that destination is present in router's routing table
			//other wise add that entry directly into the routing table
			if(routingTable.containsKey(key)){
				//this if is a condition to implement poisoned reverse
				//if next hop of destination is address of this router then that entry should not be considered
				//as current router will then definitely have shorter path to that destination.
				if( !myAddrs.getActualAddress().equals(newTable.get(key).getNextHop().getActualAddress())){
					//calculate new cost
					int recvDistance = newTable.get(key).getMetric();
					int curntDistance = routingTable.get(key).getMetric();
					int distToNeighbor = neighborTable.get(neighborPort).getDistance();
					String nextHop;

					//compare new table cost with this router's routing table cost for that destination
					//if that does'nt match , then check if sender router is next hop of any destination.
					if((recvDistance + distToNeighbor) < curntDistance){
						triggeredUpdate = true;
						nextHop = neighborTable.get(neighborPort).getAddr().getActualAddress();
						routingTable.put(key ,new RIPEntry(key,nextHop,""+(recvDistance+distToNeighbor)));
					}else if( routingTable.get(key).getNextHop().getActualAddress().equals(neighborAddr)){

						//if sender is next hop then change in link will update the existing routes
						//in this routers routing table
						if(linkChange){
							System.out.println("setting new link");
							int currentCost = routingTable.get(key).getMetric();
							routingTable.get(key).setMetric(currentCost-oldNeighborLink+newNeighborLink);
							triggeredUpdate = true;
						}
						//check if the recvDistance is greater and the next hop is sender then 
						// curntDistance have to replaced with recvDistance anyway
						if( recvDistance >= curntDistance){
							nextHop = neighborTable.get(neighborPort).getAddr().getActualAddress();
							routingTable.put(key, new RIPEntry(key,nextHop,""+(recvDistance+distToNeighbor)));
							triggeredUpdate = true;
						}
					}
				}
			}else{
				triggeredUpdate = true;
				String destAddr = newTable.get(key).getNetworkAddress().getActualAddress();
				//System.out.println(destAddr);
				String nextHop = neighborTable.get(neighborPort).getAddr().getActualAddress();
				int newMetric =  newTable.get(key).getMetric() + neighborTable.get(neighborPort).getDistance();

				RIPEntry newEntry = new RIPEntry(destAddr,nextHop,(new Integer(newMetric)).toString());
				routingTable.put(key, newEntry);
			}

		}
		
		//iterate over existing routing table to delete unreachable entries
		keyItr = routingTable.keySet().iterator();

		while(keyItr.hasNext()){
			key = keyItr.next();
			if( neighborAddr.equals( routingTable.get(key).getNextHop().getActualAddress())){
				if( !(newTable.containsKey(key))){
					keyItr.remove();
					triggeredUpdate = true;
				}
			}
		}

		if( triggeredUpdate ){
			sendToNeighbors(2,0);
		}
	}


	/**
	 *Inner runnable class from which thread 
	 *against each neighbor is started to take 
	 *new messages for messageList and check 
	 *if that message belong to router and then 
	 *that router process that message 
	 * 
	 * @author Karan Bhagat
	 * @version 09-Oct-2015
	 */

	class RecvFromNeighbor implements Runnable{

		int neighborPort;
		HashMap<String,RIPEntry> nRoutingTable = null;

		RecvFromNeighbor(int n){
			neighborPort = n;
		}

		@SuppressWarnings("unchecked")
		public void run() {
			Long startTime = 0L;
			Long endTime;
			//state 0 of router means up and running
			//state 1 of router means havn't heard from router in 1.5secs
			//state 2 of router means router is dead;
			int state = 0;
			boolean firstTime = true;
			try{
				while(true){
					//check timeout and if its first time don't check time out
					if(!firstTime){
						endTime = System.nanoTime();
						if( state == 0 && ((endTime-startTime)/1000000) > 3000 ){
							state = 1;
							System.out.println();
							System.out.println("Router : "+neighborTable.get(neighborPort).getAddr().getActualAddress()+" is not responding");
							System.out.println();
						}else if( state == 1 && ((endTime-startTime)/1000000) > 9000){
							state = 2;
							System.out.println();
							System.out.println("Router : "+neighborTable.get(neighborPort).getAddr().getActualAddress()+" is dead");
							System.out.println();
							break;
						}
					}
					if(!messageList.containsKey(neighborPort)){
						Thread.sleep(500);
						continue;
					}
					
					//if found message form this threads neighbor then process that message
					nRoutingTable = messageList.get(neighborPort).routingTable;
					messageList.remove(neighborPort);
					processNewMsg(nRoutingTable,neighborPort);
					firstTime = false;
					startTime = System.nanoTime();
					state = 0;
				}
			}catch(InterruptedException e){
				e.printStackTrace();
			}
			//if timeout delete that neighbor
			deleteAndCleanNeighbor(neighborPort);
		}



	}

}
