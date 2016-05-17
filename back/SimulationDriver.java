
package edu.osu.sfal.util;
import java.io.File;
import java.io.IOException; 
import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory; 
import javax.xml.parsers.ParserConfigurationException; 
import org.w3c.dom.Document; 
import org.w3c.dom.Element; 
import org.w3c.dom.Node; 
import org.w3c.dom.NodeList; 
import org.xml.sax.SAXException;

public class SimulationDriver {

	public Document configDoc;
	
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
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		
	}
/*
	public Element getSfpElement(String sfpName){
		
	}
*/
	public String setParameter(String sfpName, String paraName,int timeStamp){
		// todo: set the parameter outValue
		String outValue="";
		return "<parameter><name>"+paraName+"</name><type>integer</type><timeStamp>"+timeStamp+"</timeStamp><value>"+outValue+"</value></parameter>";
	}
	
	public String forkSfp(String sfpName, int timeStamp){
		String outName="";
		String outType="";
		String outTimeStamp="";
		String outValue="";
		String inputXML = "";// xml format input data to run a sfp once
		
		Element rootElement = configDoc.getDocumentElement(); 
		NodeList sfp = rootElement.getElementsByTagName(sfpName); 
		
		if(sfp!=null) {
			NodeList output=((Element)sfp.item(0)).getElementsByTagName("output");
		    NodeList input=((Element)sfp.item(0)).getElementsByTagName("input");
		    
		   outName=((Element)output.item(0)).getElementsByTagName("name").item(0).getNodeValue();
		   outType=((Element)output.item(0)).getElementsByTagName("type").item(0).getNodeValue();
		   outTimeStamp=((Element)output.item(0)).getElementsByTagName("timeStamp").item(0).getNodeValue();
		    
		   for(int i=0; i<input.getLength();i++){
			   String  parameterName=((Element) input.item(i)).getElementsByTagName("name").item(0).getNodeValue();
			   String  parameterType=((Element) input.item(i)).getElementsByTagName("type").item(0).getNodeValue();
			   String  parameterTimeStamp=((Element) input.item(i)).getElementsByTagName("timeStamp").item(0).getNodeValue();
			   String  parameterRes=((Element) input.item(i)).getElementsByTagName("resource").item(0).getNodeValue();
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
		   
		}
		//todo: parsing the parameter XML and call the sfp and get the result
		
		if(outTimeStamp.equals("n"))outTimeStamp=""+timeStamp;
		else if(outTimeStamp.equals("n-1")&&timeStamp>0)outTimeStamp=""+(timeStamp-1);
		return "<parameter><name>"+outName+"</name><type>"+outType+"</type><timeStamp>"+timeStamp+"</timeStamp><value>"+outValue+"</value>"+"</parameter>";
		
	}
	
}


