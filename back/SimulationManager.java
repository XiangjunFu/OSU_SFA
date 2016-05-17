package edu.osu.sfal.util;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.Node;

import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Request;
import org.restlet.Restlet;
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SimulationManager  extends ServerResource{

	private	String 	configFileName="vehicleSimulationConfig.xml"; // for process description
	
	private	String	deployFileName; // for nodes deploy information, such as authority information for login remeote server to run a app
	private	Document		simuProcessConfigDoc;
	private static final String REQUESTS_URI="http://127.0.0.1:8183/vehicle";
	private static final ClientResource clientResource = new ClientResource(REQUESTS_URI);
	//store the original input and output description 
	private Document inputXML;
	private Document outputXML;
	
	//store the attained input output for each function;
	//key is funcitonName$timeStamp, value is the input outputXML
	Map<String, String>inputMap=new HashMap<>();
    Map<String, String> outputMap=new HashMap<>();
    
    List<String> finishedSfpList=new ArrayList<String>();
    List<String> sfpList=new ArrayList<String>();
    List<String> outputList=new ArrayList<String>();
    //  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    @Post
    public String handlePostRequest(StringRepresentation re){
    	
    String reqStr=re.toString();
    //return "wrong way";
    	return handleRequest(reqStr);
    	
    }
    public String handleRequest(String reqStr){
    	String uriString=reqStr;
    	int index1=uriString.indexOf("</input>");
    	int index2=uriString.indexOf("<output>");
    	int index3=uriString.indexOf("</output>");
    	String input="";
    	String output="";
    	int timeStamp=0;
    	int tiIndex1=uriString.indexOf("<timeStamp>");
    	int tiIndex2=uriString.indexOf("</timeStamp>");
    	
    	if(index1>=0&&index2>=0&&index3>=0){
    		input=uriString.substring(0, index1+8);
    		output=uriString.substring(index2,index3+9);
    	}else{ return "error";}
    	if(tiIndex1>=0&&tiIndex2>=0){
    		String timeStampS=uriString.substring(tiIndex1+11,tiIndex2);
    		timeStamp=Integer.parseInt(timeStampS);
    	}else{ return "error";}
    	inputXML=parseStrXML(input);
    	outputXML=parseStrXML(output);
    	
    	NodeList inputNodes=inputXML.getElementsByTagName("parameter");
    	for(int i=0; i<inputNodes.getLength();i++){
    		String name=((Element)inputNodes.item(i)).getElementsByTagName("name").item(0).getTextContent();
    		String value=((Element)inputNodes.item(i)).getElementsByTagName("value").item(0).getTextContent();
    		inputMap.put(name, value);
    	}
    	runSimuProcessbyInput(input,output,timeStamp);
   // 	StringBuilder strbuff=new StringBuilder();
    	
    	while(!isGetAllOutput()){
    		try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	
    	return outputMap.toString();
    }
    
    
    @Get 
    public String handleGetRequest(){  
    	 /* return "Resource URI  : " + getReference() + '\n' + "Root URI      : "
    	            + getRootRef() + '\n' + "Routed part   : "
    	            + getReference().getBaseRef() + '\n' + "Remaining part: "
    	            + getReference().getRemainingPart();
      */
    		return "wrong way";
    	  
    }  
   
    public SimulationManager(){
    		super();
    		this.simuProcessConfigDoc=parserFigXml(configFileName);
    		NodeList sfpNodeList= simuProcessConfigDoc.getElementsByTagName("sfpName");
    		for(int i=0; i<sfpNodeList.getLength();i++){
    			String name=((Element)sfpNodeList.item(i)).getTextContent();
    		    sfpList.add(name);
    		}
    		
    		NodeList outputNodeList=simuProcessConfigDoc.getElementsByTagName("output");
    		for(int j=0;j<outputNodeList.getLength();j++){
    			NodeList paraList=((Element)outputNodeList.item(j)).getElementsByTagName("parameter");
    			for(int k=0;k<paraList.getLength();k++){
    				String name= ((Element)paraList.item(k)).getElementsByTagName("name").item(0).getTextContent();
    				outputList.add(name);
    			}
    			
    		}
    }
    
	public SimulationManager(String configFileName){
		
		this.configFileName=configFileName;
		this.simuProcessConfigDoc=parserFigXml(configFileName);
		
	}
	
	
	
	
	public Document parseStrXML(String str){
		
		DocumentBuilderFactory dbf =DocumentBuilderFactory.newInstance();
	    DocumentBuilder db = null;
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    InputSource is = new InputSource();
	    is.setCharacterStream(new StringReader(str));
	    Document doc = null;
		try {
			doc = db.parse(is);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	    return doc;
	}
	
	
	
	public List<String> findSfpToRunByInput(){
		List<String> sfpList=new ArrayList<String>();
		//Element rootElement = simuProcessConfigDoc.getDocumentElement(); 
		NodeList sfps = simuProcessConfigDoc.getElementsByTagName("sfp"); 
		int sfpNum=sfps.getLength();
		
		for(int i=0;i<sfpNum;i++){//seach each sfp
			Element sfpElement=(Element)sfps.item(i);
			Element inputElement=(Element) (sfpElement.getElementsByTagName("input").item(0));
			NodeList inputNameList=inputElement.getElementsByTagName("name");
			String sfpName=sfpElement.getElementsByTagName("sfpName").item(0).getTextContent();
			if(finishedSfpList.contains(sfpName))break;
			boolean isMatch=true;
			for(int j=0;j<inputNameList.getLength();j++){
		    		String name= ((Element)inputNameList.item(j)).getTextContent();
		        if(inputMap.containsKey(name))continue;
		        else {
		        	      isMatch=false;
		        	      break;
		        }
		    }
		   if(isMatch)sfpList.add(sfpName); 
		}
		return sfpList;
	}
	
	
	
	//according output, find the sfps need to be invork;
	public List<String> findSfpByOutput(String output){
		
		List<String> sfpList=new ArrayList<String>();
		//Element rootElement = simuProcessConfigDoc.getDocumentElement(); 
		NodeList sfps = simuProcessConfigDoc.getElementsByTagName("sfp"); 
		int sfpNum=sfps.getLength();
		
		Document outputDoc=parseStrXML(output);
		NodeList outNodeList=outputDoc.getElementsByTagName("parameter");
		
		if(sfpList!=null) {
			 for(int i=0; i<sfps.getLength();i++){
				 NodeList outputNodes=((Element)(sfps.item(i))).getElementsByTagName("parameter"); 
				 for(int j=0;j<outputNodes.getLength();j++){
					 String outputName=((Element)outputNodes.item(j)).getElementsByTagName("name").item(0).getTextContent();
				     	for(int k=0;k<outNodeList.getLength();k++){
				     		String outputName1=((Element)outNodeList.item(k)).getElementsByTagName("name").item(0).getTextContent();
				     	    if(outputName1.equals(outputName)){
				     	    	     String sfpName=((Element)(sfps.item(i))).getElementsByTagName("sfpName").item(0).getTextContent();
				     	    		sfpList.add(sfpName);
				     	    }
				     	}
				 }
			 }
		}
		return sfpList;
	}
	
	class sfpWorker implements Runnable {

		String sfpName;
		
		int timeStamp;
		
		public sfpWorker(String sfpName,int timeStamp){
			this.sfpName=sfpName;
			this.timeStamp=timeStamp;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			finishedSfpList.add(sfpName); //todo in future: add timeStamp
			String re=oneRunSfp(sfpName,timeStamp);
			// store the output;
			re="<output>"+re+"</output>";
			Document reDoc=parseStrXML(re);
			NodeList outputNodeList=reDoc.getElementsByTagName("output");
			NodeList outputList=((Element)outputNodeList.item(0)).getElementsByTagName("parameter");
			for(int i=0;i<outputList.getLength();i++){
				String name=((Element)outputList.item(i)).getElementsByTagName("name").item(0).getTextContent();
				String value=((Element)outputList.item(i)).getElementsByTagName("value").item(0).getTextContent();
			    inputMap.put(name, value); // the output can be other sfp's input
			    outputMap.put(name, value);
			}
		}
	}
	
	
	public String oneRunSfp(String sfpName, int timeStamp){
		String outName="";
		String outType="";
		String outTimeStamp="";
		String outValue="";
		String inputXML ="";// xml format input data to run a sfp once
		String outputXML="";
		//Element rootElement = simuProcessConfigDoc.getDocumentElement(); 
		NodeList input = getSfpinputDes(sfpName);
		NodeList  output = getSfpoutputDes(sfpName);
		
		for(int j=0;j<output.getLength();j++){
			   outName=((Element)output.item(j)).getElementsByTagName("name").item(0).getTextContent();
			   outType=((Element)output.item(j)).getElementsByTagName("type").item(0).getTextContent();
			   outTimeStamp=((Element)output.item(j)).getElementsByTagName("timeStamp").item(0).getTextContent();
		       outputXML=outputXML+"<parameter><name>"+outName+"</name><type>"+outType+"</type><timeStamp>"+timeStamp+"</timeStamp></parameter>";
		   }
		
		for(int i=0; i<input.getLength();i++){
			   String  name=((Element) input.item(i)).getElementsByTagName("name").item(0).getTextContent();
			   String  type=((Element) input.item(i)).getElementsByTagName("type").item(0).getTextContent();
			   String  timeStampS=((Element) input.item(i)).getElementsByTagName("timeStamp").item(0).getTextContent();
			   String  pRes=((Element) input.item(i)).getElementsByTagName("resource").item(0).getTextContent();
			   String value=inputMap.get(name);
			   inputXML=inputXML+"<parameter><name>"+name+"</name><type>"+type+"</type><value>"+value+"</value></parameter>";  
			}
		
		String invorkXML="<input>"+inputXML+"</input><sfpName>"+sfpName+"</sfpName><output>"+outputXML+"</output>";
		StringRepresentation entity = new StringRepresentation(invorkXML);
		//ClientResource clientResource = new ClientResource(REQUESTS_URI);
		Representation result=clientResource.post(entity);
		try {
			return  result.getText();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "wrong";
	}
	
	public boolean isGetAllOutput(){
		for(int i=0;i<outputList.size();i++){
			if(!outputMap.containsKey(outputList.get(i)))return false;
		}
		return true;
	}
	
	public boolean isRunAllSfp(){
		for(int i=0; i<sfpList.size();i++){
			String name=sfpList.get(i);
			if(!finishedSfpList.contains(name))return false;
		}
		return true;
	}
	
	public void runSimuProcessbyInput(String inpuy,String output,int timeStamp){
		
		while(!isRunAllSfp()){
			List<String> initalSfpList=findSfpToRunByInput();	
			for(Iterator<String> iterator=initalSfpList.iterator();iterator.hasNext();) {
		            String sfpName=iterator.next();   
		            sfpWorker worker=new sfpWorker(sfpName,timeStamp);
		            Thread t=new Thread(worker);
		            t.start();
		            System.out.println("invork "+sfpName);
				}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public String runSimulationProcess(String input, String output, int timeStamp){
				
		List<String> initalSfpList=findSfpByOutput(output);
		String outputBuff="";
		 for(Iterator<String> iterator=initalSfpList.iterator();iterator.hasNext();) {
	            String sfpName=iterator.next();
	            String sfpOutput=forkSfp(sfpName,timeStamp);
	            outputBuff=outputBuff+sfpOutput;
	        }
		
		
		return outputBuff;	
	}
	
	public NodeList getSfpinputDes(String sfpName){
		NodeList sfps = simuProcessConfigDoc.getElementsByTagName("sfp"); 
		int sfpNum=sfps.getLength();
		 for(int i=0; i<sfps.getLength();i++){
			 NodeList sfpNameNodes=((Element)(sfps.item(i))).getElementsByTagName("sfpName"); 
			 if(sfpNameNodes.item(0).getTextContent().equals(sfpName)){
				 NodeList inputList= ((Element)(sfps.item(i))).getElementsByTagName("input");
			     return ((Element)inputList.item(0)).getElementsByTagName("parameter");
			 }
				 
		 }
		return null;	
	}
	public NodeList getSfpoutputDes(String sfpName){
		NodeList sfps = simuProcessConfigDoc.getElementsByTagName("sfp"); 
		int sfpNum=sfps.getLength();
		 for(int i=0; i<sfps.getLength();i++){
			 NodeList sfpNameNodes=((Element)(sfps.item(i))).getElementsByTagName("sfpName"); 
			 if(sfpNameNodes.item(0).getTextContent().equals(sfpName)){
				 NodeList outputList= ((Element)(sfps.item(i))).getElementsByTagName("output");
			     return ((Element)outputList.item(0)).getElementsByTagName("parameter");
			 }
				 
		 }
		return null;	
	}
	
	public String forkSfp(String sfpName,int timeStamp){
		String outName="";
		String outType="";
		String outTimeStamp="";
		String outValue="";
		String inputXML ="";// xml format input data to run a sfp once
		String outputXML="";
		//Element rootElement = simuProcessConfigDoc.getDocumentElement(); 
		NodeList input = getSfpinputDes(sfpName);
		NodeList  output = getSfpoutputDes(sfpName);
		
		for(int j=0;j<output.getLength();j++){
			   outName=((Element)output.item(j)).getElementsByTagName("name").item(0).getTextContent();
			   outType=((Element)output.item(j)).getElementsByTagName("type").item(0).getTextContent();
			   outTimeStamp=((Element)output.item(j)).getElementsByTagName("timeStamp").item(0).getTextContent();
		       outputXML=outputXML+"<parameter><name>"+outName+"</name><type>"+outType+"</type><timeStamp>"+timeStamp+"</timeStamp></parameter>";
		   }
		   
		      
		for(int i=0; i<input.getLength();i++){
			   String  parameterName=((Element) input.item(i)).getElementsByTagName("name").item(0).getTextContent();
			   String  parameterType=((Element) input.item(i)).getElementsByTagName("type").item(0).getTextContent();
			   String  parameterTimeStamp=((Element) input.item(i)).getElementsByTagName("timeStamp").item(0).getTextContent();
			   String  parameterRes=((Element) input.item(i)).getElementsByTagName("resource").item(0).getTextContent();
			   if(!parameterRes.equals("setable")){
				   int timeStep=0;
				   if(parameterTimeStamp.equals("n-1")&&timeStamp>0)timeStep=timeStamp-1;
				   else if(parameterTimeStamp.equals("n"))timeStep=timeStamp;
				   String re=forkSfp(parameterRes,timeStep);
				   inputXML=inputXML+re;
			   }else{
				   inputXML=inputXML+setParameter(sfpName,parameterName,timeStamp);
			   }   
			}
		
		String invorkXML="<input>"+inputXML+"</input><sfpName>"+sfpName+"</sfpName><output>"+outputXML+"</output>";
		StringRepresentation entity = new StringRepresentation(invorkXML);
		
		ClientResource clientResource = new ClientResource(REQUESTS_URI);
		
		StringRepresentation result=(StringRepresentation)clientResource.post(entity);
		
	
	// loop to detect if all the output have been attained
		
		
		if(outTimeStamp.equals("n"))outTimeStamp=""+timeStamp;
		else if(outTimeStamp.equals("n-1")&&timeStamp>0)outTimeStamp=""+(timeStamp-1);
		//return "<parameter><name>"+outName+"</name><type>"+outType+"</type><timeStamp>"+timeStamp+"</timeStamp><value>"+outValue+"</value>"+"</parameter>";
		
		return "<parameter>"+result.getText()+"<timeStamp>"+timeStamp+"</timeStamp></parameter>";
	}
	
	
	
	
	public Document parserFigXml(String filePath){
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance(); 
		Document document = null; 
	      try { 
	         //DOM parser instance 
	         DocumentBuilder builder = builderFactory.newDocumentBuilder(); 
	         //parse an XML file into a DOM tree 
	         document = builder.parse(new File(filePath)); 
	      } catch (ParserConfigurationException e) { 
	         e.printStackTrace();  
	      } catch (SAXException e) { 
	         e.printStackTrace(); 
	      } catch (IOException e) { 
	         e.printStackTrace(); 
	      } 
	      return document; 
	}
	
	public String setParameter(String sfpName, String paraName,int timeStamp){
		// set the parameter outValue
		// look for input value list in the inputxml string
		 
		NodeList inputList = inputXML.getElementsByTagName("parameter"); 
		for(int i=0; i<inputList.getLength();i++){
			String  parameterName=((Element) inputList.item(i)).getElementsByTagName("name").item(0).getTextContent();
			String  parameterType=((Element) inputList.item(i)).getElementsByTagName("type").item(0).getTextContent();
		//	String  parameterTimeStamp=((Element) inputList.item(i)).getElementsByTagName("timeStamp").item(0).getTextContent();
		    String  parameterValue=((Element) inputList.item(i)).getElementsByTagName("value").item(0).getTextContent();
			if(parameterName.equals(paraName)){
		    		return "<parameter><name>"+parameterName+"</name><type>"+parameterType+"</type><timeStamp>"+timeStamp+"</timeStamp><value>"+parameterValue+"</value></parameter>";
			}
		}	
		return "";
	}
	
	public static void main(String[] args) {
		Component component = new Component();
	    component.getServers().add(Protocol.HTTP, 8182);

	    // Then attach it to the local host
	    component.getDefaultHost().attach("", SimulationManager.class);

	    // Now, let's start the component!
	    // Note that the HTTP server connector is also automatically started.
	    try {
			component.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

 