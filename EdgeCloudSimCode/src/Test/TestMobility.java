package Test;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;

public class TestMobility {
public static void main(String[]args){
	
	System.out.println("Strated");
	int iterationNumber = 1;
	String configFile = "";
	String outputFolder = "";
	String edgeDevicesFile = "";
	String applicationsFile = "";
	if (args.length == 5) {
		configFile = args[0];
		edgeDevicesFile = args[1];
		applicationsFile = args[2];
		outputFolder = args[3];
		iterationNumber = Integer.parseInt(args[4]);
	} else {
		System.out.println("Simulation setting file, output folder and iteration number are not provided! Using default ones...");
		configFile = "scripts/sample_app3/config/default_config.properties";
		applicationsFile = "scripts/sample_app3/config/applications.xml";
		edgeDevicesFile = "scripts/sample_app3/config/edge_devices.xml";
		outputFolder = "sim_results/ite" + iterationNumber;
	}

	// load settings from configuration file
	SimSettings SS = SimSettings.getInstance();
	if (SS.initialize(configFile, edgeDevicesFile, applicationsFile) == false) {
		System.out.println("cannot initialize simulation settings!");
		System.exit(0);
	}
	
	
	
	ExponentialDistribution[] expRngList = new ExponentialDistribution[SS.getNumOfEdgeDatacenters()];
	System.out.println("Number of Edge Datacenter: "+SS.getNumOfEdgeDatacenters());

	//create random number generator for each place
	Document doc = SS.getEdgeDevicesDocument();//*Mine* this variable contains the parsing for Edge_Devices.xml file 
	NodeList datacenterList = doc.getElementsByTagName("datacenter");
	for (int i = 0; i < datacenterList.getLength(); i++) {
		Node datacenterNode = datacenterList.item(i);
		Element datacenterElement = (Element) datacenterNode;
		Element location = (Element)datacenterElement.getElementsByTagName("location").item(0);
		String attractiveness = location.getElementsByTagName("attractiveness").item(0).getTextContent();
		int placeTypeIndex = Integer.parseInt(attractiveness);
		//System.out.println("SS.getMobilityLookUpTable()[placeTypeIndex]: "+SS.getMobilityLookUpTable()[placeTypeIndex]);
		expRngList[i] = new ExponentialDistribution(SS.getMobilityLookUpTable()[placeTypeIndex]);
		//System.out.println("Place Type "+placeTypeIndex+" expRngList: "+i+" is  "+expRngList[i].sample());
	}
	List<TreeMap<Double, Location>> treeMapArray = new ArrayList<TreeMap<Double, Location>>();

	//initialize tree maps and position of mobile devices
			for(int i=0; i<10; i++) {
				treeMapArray.add(i, new TreeMap<Double, Location>());
				
				int randDatacenterId = SimUtils.getRandomNumber(0, SimSettings.getInstance().getNumOfEdgeDatacenters()-1);
				//System.out.println("Random DatCenter ID "+randDatacenterId);

				Node datacenterNode = datacenterList.item(randDatacenterId);
				Element datacenterElement = (Element) datacenterNode;
				Element location = (Element)datacenterElement.getElementsByTagName("location").item(0);
				String attractiveness = location.getElementsByTagName("attractiveness").item(0).getTextContent();
				int placeTypeIndex = Integer.parseInt(attractiveness);
				int wlan_id = Integer.parseInt(location.getElementsByTagName("wlan_id").item(0).getTextContent());
				int x_pos = Integer.parseInt(location.getElementsByTagName("x_pos").item(0).getTextContent());
				int y_pos = Integer.parseInt(location.getElementsByTagName("y_pos").item(0).getTextContent());
				
				//System.out.println("Place Type "+placeTypeIndex+"  wlan_id  "+wlan_id+"  x_pos  "+x_pos+"  y_pos "+y_pos);
				//start locating user shortly after the simulation started (e.g. 10 seconds)
				treeMapArray.get(i).put(SimSettings.CLIENT_ACTIVITY_START_TIME, new Location(placeTypeIndex, wlan_id, x_pos, y_pos));
				System.out.println("Mobile Device "+i+" Data Center "+randDatacenterId+" Waiting time  "+treeMapArray.get(i).lastKey());
			}
					
			for(int i=0; i<10; i++) {
				TreeMap<Double, Location> treeMap = treeMapArray.get(i);
				//*Mine* this loop to guarantee that each mobile device remains moving till the Simulation Time is finished
				while(treeMap.lastKey() < SimSettings.getInstance().getSimulationTime()) {				
					boolean placeFound = false;
					int currentLocationId = treeMap.lastEntry().getValue().getServingWlanId();//*Mine* return the id for the edge data center
					double waitingTime = expRngList[currentLocationId].sample();
					//*Mine* this loop to guarantee that should move to another edge data center
					while(placeFound == false){
						//*Mine* generate a new random edge data center to move the mobile device to it
						int newDatacenterId = SimUtils.getRandomNumber(0,SimSettings.getInstance().getNumOfEdgeDatacenters()-1);
						//*Mine* this if statement to guarantee that the current and new edge data center aren't the same
						if(newDatacenterId != currentLocationId){
							placeFound = true;
							Node datacenterNode = datacenterList.item(newDatacenterId);
							Element datacenterElement = (Element) datacenterNode;
							Element location = (Element)datacenterElement.getElementsByTagName("location").item(0);
							String attractiveness = location.getElementsByTagName("attractiveness").item(0).getTextContent();
							int placeTypeIndex = Integer.parseInt(attractiveness);
							int wlan_id = Integer.parseInt(location.getElementsByTagName("wlan_id").item(0).getTextContent());
							int x_pos = Integer.parseInt(location.getElementsByTagName("x_pos").item(0).getTextContent());
							int y_pos = Integer.parseInt(location.getElementsByTagName("y_pos").item(0).getTextContent());
							//*Mine* add the waiting time for the mobile device according to its new edge data center
							treeMap.put(treeMap.lastKey()+waitingTime, new Location(placeTypeIndex, wlan_id, x_pos, y_pos));
							System.out.println("Mobile Device "+i+" Data Center "+wlan_id+" Waiting time  "+treeMapArray.get(i).lastKey());

						}
					}
					if(!placeFound){
						SimLogger.printLine("impossible is occured! location cannot be assigned to the device!");
				    	System.exit(0);
					}
				}

			}
			
			
	
}
}
