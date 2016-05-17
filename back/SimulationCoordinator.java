package edu.osu.sfal.util;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.Node;

import org.apache.log4j.Logger;
import org.restlet.Component;
import org.restlet.data.Protocol;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.restlet.routing.Router;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.gson.JsonObject;

import akka.actor.ActorRef;
import edu.osu.lapis.LapisApi;
import edu.osu.lapis.network.LapisNode;
import edu.osu.lapis.network.NetworkChangeCallback;
import edu.osu.lapis.util.Sleep;
import edu.osu.sfal.messages.sfp.NewSfpMsg;
import edu.osu.sfal.messages.sfp.RemoveSfp;
import edu.osu.sfal.messages.sfp.SfpStatusMessage;
import edu.osu.lapis.Flags;
import edu.osu.sfal.actors.*;



public class SimulationCoordinator extends ServerResource {

	private  String lapisNodeName="vehicleSimulation";
	private  String coordinatorAddress="http://localhost:8910";
	//private final String couchbaseUrl;
	private  static  long nodeReadyTimeoutMillis=20000;
	private  static  long requestCompletedTimeout=180000;
	private  static LapisApi lapisApi;
	//private  LapisNetworkCallback register;	
   
	public   static Map<String, String>  onlineSfpListMap=new HashMap<String,String>();;
	
	/*
	class httpServer extends ServerResource{
		public httpServer(){
			super();
		}
		@Post
	    public String handlePostRequest(StringRepresentation re){
			String uriString=re.toString();
			int ind1=uriString.indexOf("<input>");
			int ind2=uriString.indexOf("</input>");
			int ind3=uriString.indexOf("<sfpName>");
			int ind6=uriString.indexOf("</sfpName");
			int ind4=uriString.indexOf("<output>");
			int ind5=uriString.indexOf("</output>");
			
			String input=uriString.substring(ind1+7, ind2);
			String output=uriString.substring(ind4+8,ind5);
			
			String functionName=uriString.substring(ind3+5,ind6);
			while(!onlineSfpListMap.containsKey(functionName)){
				Sleep.sleep(300);
			}
		
			//String nodeName=onlineSfpListMap.get(functionName);
			String res= executeSfp(input,output,functionName);
			return res;
		}
		
		@Get  
	    public Representation get() {
		 	return null;
		}
	}
	*/
	@Post
    public String handlePostRequest(StringRepresentation re){
		String uriString=re.toString();
		int ind1=uriString.indexOf("<input>");
		int ind2=uriString.indexOf("</input>");
		int ind3=uriString.indexOf("<sfpName>");
		int ind6=uriString.indexOf("</sfpName");
		int ind4=uriString.indexOf("<output>");
		int ind5=uriString.indexOf("</output>");
		
		String input=uriString.substring(ind1+7, ind2);
		String output=uriString.substring(ind4+8,ind5);
		
		String functionName=uriString.substring(ind3+9,ind6);
		while(!onlineSfpListMap.containsKey(functionName)){
			Sleep.sleep(300);
		}
	
		//String nodeName=onlineSfpListMap.get(functionName);
		String res= executeSfp(input,output,functionName);
		return res;
	}
	
	@Get  
    public Representation get() {
	 	return null;
	}
	public SimulationCoordinator() throws IOException {
		super();
		/*
		lapisNodeName="vehicleSimulation";
		coordinatorAddress="http://localhost:8910";
		//private final String couchbaseUrl;
		
		nodeReadyTimeoutMillis=20000;
		requestCompletedTimeout=180000;
		onlineSfpListMap=new HashMap<String,String>();
		lapisApi=new LapisApi(lapisNodeName, coordinatorAddress);
        lapisApi.registerNetworkChangeCallback(new LapisNetworkCallback(lapisApi, nodeReadyTimeoutMillis));
	   */
	}
	public  void init(){
		
	//	lapisNodeName="vehicleSimulation";
	//	coordinatorAddress="http://localhost:8910";
		//private final String couchbaseUrl;
	//	nodeReadyTimeoutMillis=20000;
	//	requestCompletedTimeout=180000;
		//onlineSfpListMap=new HashMap<String,String>();
		lapisApi=new LapisApi(lapisNodeName, coordinatorAddress);
        //register=new LapisNetworkCallback(lapisApi, nodeReadyTimeoutMillis);
		lapisApi.registerNetworkChangeCallback(new LapisNetworkCallback(lapisApi, nodeReadyTimeoutMillis));
	
	}
	
	
	
	private String executeSfp(String input, String output, String functionName){
		

		String nodeName=onlineSfpListMap.get(functionName);		
		double[] readyToWork=lapisApi.getArrayOfDouble(nodeName, "readyToCalculate");	
		while (Flags.evaluateFlagValue(readyToWork)) {
			Sleep.sleep(200);
		}
	
		try {
	        DocumentBuilderFactory dbf =DocumentBuilderFactory.newInstance();
	        DocumentBuilder db = dbf.newDocumentBuilder();
	        InputSource is = new InputSource();
	        is.setCharacterStream(new StringReader(input));
	        Document doc = db.parse(is);
	        NodeList nodes = doc.getElementsByTagName("parameter");

	        // iterate the parameters
	        for (int i = 0; i < nodes.getLength(); i++) {
	           Element element = (Element) nodes.item(i);
              //name
	           NodeList nameNode = element.getElementsByTagName("name");
	           Element line = (Element) nameNode.item(0);
	           String name=line.getTextContent();
	           //type
	           NodeList typeNode = element.getElementsByTagName("type");
	           line = (Element) typeNode.item(0);
	           String paraType=line.getTextContent();
	           //timeStamp
	           NodeList timeStampNode = element.getElementsByTagName("timeStamp");
	           line = (Element) nameNode.item(0);
	           String timeStamp=line.getTextContent();
	           
	           NodeList valueNode=element.getElementsByTagName("value");
	           line=(Element) valueNode.item(0);
	           String value=line.getTextContent();
	           //get value from lapi node     	          
	           if(paraType.equals("int")){
	        	     int[]int1=new int[1];
	        	     int1[0]=Integer.parseInt(value.trim());
	        	     lapisApi.set(nodeName, name,int1);  
	           }else if(paraType.equals("long")){
	        	   	 long[]long1=new long[1];
	        	   	 long1[0]=Long.parseLong(value);
	        	     lapisApi.set(nodeName, name,long1);  
	           }else if(paraType.equals("double")){
	        	     double[] double1=new double[1];
	        	     double1[0]=Double.parseDouble(value);
	        	   	 lapisApi.set(nodeName, name,double1);
	           }else if(paraType.equals("string")){
	        	     lapisApi.set(nodeName, name,value);
	           }else if(paraType.equals("bool")){
	        	   	 boolean[] bool1=new boolean[1];
	        	   	 bool1[0]=Boolean.parseBoolean(value);
	        	     lapisApi.set(nodeName, name,bool1);
	        	     }
	        }
	          
	        
	    }
	    catch (Exception e) {
	        e.printStackTrace();
	    }
		double[] startToWork=Flags.getFlag(true);
		lapisApi.set(nodeName, "readyToCalculate", startToWork);
		//
		double[] isWorkfinished=lapisApi.getArrayOfDouble(nodeName, "finishedCalculating");
		
		while (!Flags.evaluateFlagValue(isWorkfinished)) {
			Sleep.sleep(200);
		}
	   try{
		   DocumentBuilderFactory dbf =DocumentBuilderFactory.newInstance();
		    DocumentBuilder db = dbf.newDocumentBuilder();
		    InputSource is = new InputSource();
		    is.setCharacterStream(new StringReader(output));
		    Document doc = db.parse(is);
		    NodeList nodes = doc.getElementsByTagName("parameter");

	        // iterate the parameters
	        for (int i = 0; i < nodes.getLength(); i++) {
	           Element element = (Element) nodes.item(i);
	          //name
	           NodeList nameNode = element.getElementsByTagName("name");
	           Element line = (Element) nameNode.item(0);
	           String name=line.getTextContent();
	           //type
	           NodeList typeNode = element.getElementsByTagName("type");
	           line = (Element) typeNode.item(0);
	           String paraType=line.getTextContent();
	         //get value from lapi node     	          
	           if(paraType.equals("int")){
	        	     int[]int1=lapisApi.getArrayOfInt(nodeName, name);  
	             String int1s=""; 
	        	     for(int m=0; m<int1.length;m++){
	        	    	 	int1s=int1s+int1[m]+" ";
	              }
	        	     return wrapReturnValue(name,output,int1s);
	           }else if(paraType.equals("long")){
	        	     long[]long1=lapisApi.getArrayOfLong(nodeName, name);  
	        	     String long1s=""; 
	        	     for(int m=0; m<long1.length;m++){
	        	    	 	long1s=long1s+long1[m]+" ";
	              }
	        	     return wrapReturnValue(name,output,long1s);
	           
	           }else if(paraType.equals("double")){
	        	   	 double[]double1=lapisApi.getArrayOfDouble(nodeName, name);
	        	   	 String double1s=""; 
	       	     for(int m=0; m<double1.length;m++){
	       	    	 	double1s=double1s+double1[m]+" ";
	             }
	       	     return wrapReturnValue(name,output,double1s);
	           
	           }else if(paraType.equals("string")){
	        	   
	        	     String string1=lapisApi.getString(nodeName, name);
	             return wrapReturnValue(name,output,string1);
	           }else if(paraType.equals("bool")){
	        	     boolean [] bool1=lapisApi.getArrayOfBoolean(nodeName, name);
	        	     String bool1s=""; 
	           	     for(int m=0; m<bool1.length;m++){
	           	    	 	bool1s=bool1s+bool1[m]+" ";
	                 }
	           	     return wrapReturnValue(name,output,bool1s);  
	           }
	  
	        } 
	   }catch(Exception e) {
	        e.printStackTrace();
	    }	
		return "";
	}
	
    private String  wrapReturnValue(String name,String output, String returnValue){
    	    String outputBuff=output; 
    	    int ind1=outputBuff.indexOf("</parameter>");
    	    while(ind1>0){
    	    		int ind2=outputBuff.indexOf("<name>");
        	    int ind3=outputBuff.indexOf("</name>");
        	    String name1=outputBuff.substring(ind2+6,ind3);
        	    if(name1.equals(name)){
        	    String output2=outputBuff.substring(ind3+6);	
        	    	String output1=outputBuff.substring(0, ind3+7)+"<value>"+returnValue+"</value>"+output2;
        	    return output1;
        	    }
        	    outputBuff=outputBuff.substring(ind1+12);
        	    ind1=outputBuff.indexOf("</parameter>");
    	    }
    	    return "";
    }
	public static void main(String[] args) {
		
		
		Component component = new Component();
	    component.getServers().add(Protocol.HTTP, 8183);
	    // Then attach it to the local host
	    component.getDefaultHost().attach("/vehicle", SimulationCoordinator.class);
	    
	    // Now, let's start the component!
	    // Note that the HTTP server connector is also automatically started.
	    try {
			component.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	    try {
			SimulationCoordinator co=new SimulationCoordinator();
		    co.init();
	    } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 
	}
      
	class LapisNetworkCallback implements NetworkChangeCallback{

		private String nodeName;
		private String simulationFunctionName;
		static final String SIMULATION_FUNCTION_NAME = "SIMULATION_FUNCTION_NAME";

		private final Logger logger = Logger.getLogger(getClass());

		private final LapisApi lapisApi;
		private final long waitForNodeMillis;

		
		
		public LapisNetworkCallback(LapisApi lapisApi,long waitForNodeMillis) {
			this.waitForNodeMillis = waitForNodeMillis;
			this.lapisApi = lapisApi;
		}
		
		
		@Override
		public void onNodeAdd(LapisNode lapisNode) {
			// TODO Auto-generated method stub
			logger.info("New node on network: " + lapisNode);
			waitForNode(lapisNode);
			String nodeName=lapisNode.getNodeName();
			String functionName=lapisApi.getString(nodeName, SIMULATION_FUNCTION_NAME);				
			if(!onlineSfpListMap.containsKey(functionName)){
				onlineSfpListMap.put(functionName,nodeName);}
		}

		@Override
		public void onNodeDelete(LapisNode lapisNode) {
			// TODO Auto-generated method stub
			logger.info("Node deleted from network: " + lapisNode);
			waitForNode(lapisNode);
			String nodeName=lapisNode.getNodeName();
			String functionName=lapisApi.getString(nodeName, SIMULATION_FUNCTION_NAME);
		    if(onlineSfpListMap.containsKey(functionName)){
		    	onlineSfpListMap.remove(functionName);
		  }
		}
		
		private void waitForNode(LapisNode lapisNode) {
			String nodeName = lapisNode.getNodeName();
			try {
				lapisApi.waitForReadyNode(nodeName, waitForNodeMillis);
			} catch (TimeoutException e) {
				logger.warn("Timed out while waiting for node " + lapisNode
						+ " to become ready. Waited for " + waitForNodeMillis + " millis.", e);
				throw new RuntimeException(e);
			}
		}
/*
		private SfpInformation getSfpInformation(LapisNode lapisNode) {
			SfpInformation info = new SfpInformation();
			info.nodeName = lapisNode.getNodeName();
			info.simulationFunctionName =
					new SimulationFunctionName(lapisApi.getString(info.nodeName, SIMULATION_FUNCTION_NAME));
			info.sfpName = getSfpName(info.simulationFunctionName, lapisNode);
			return info;
		}

		private SfpName getSfpName(SimulationFunctionName sfName, LapisNode lapisNode) {
			return new SfpName(lapisNode.getNodeName());
		}
*/		
	}
	
}



