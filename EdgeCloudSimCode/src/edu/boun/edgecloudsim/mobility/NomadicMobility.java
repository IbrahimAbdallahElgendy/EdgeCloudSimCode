/*
 * Title:        EdgeCloudSim - Nomadic Mobility model implementation
 * 
 * Description: 
 * MobilityModel implements basic nomadic mobility model where the
 * place of the devices are changed from time to time instead of a
 * continuous location update.
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.mobility;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;

public class NomadicMobility extends MobilityModel {
	//*Mine* Java TreeMap provides an efficient way of storing key-value pairs in sorted order.
	// Double is key and Location is the value
	// The time and associated location for each mobile device through the simulation time is stored in treeMapArray variable
	private List<TreeMap<Double, Location>> treeMapArray;
	
	public NomadicMobility(int _numberOfMobileDevices, double _simulationTime) {
		super(_numberOfMobileDevices, _simulationTime);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void initialize() {
		treeMapArray = new ArrayList<TreeMap<Double, Location>>();
		
		//*Mine* this variable contains the values for the waiting time for each edge data center (14 edge data center)
		ExponentialDistribution[] expRngList = new ExponentialDistribution[SimSettings.getInstance().getNumOfEdgeDatacenters()];

		//create random number generator (waiting time) for each place (edge data center)
		Document doc = SimSettings.getInstance().getEdgeDevicesDocument();//*Mine* this variable contains the parsing for Edge_Devices.xml file 
		NodeList datacenterList = doc.getElementsByTagName("datacenter");
		for (int i = 0; i < datacenterList.getLength(); i++) {
			Node datacenterNode = datacenterList.item(i);
			Element datacenterElement = (Element) datacenterNode;
			Element location = (Element)datacenterElement.getElementsByTagName("location").item(0);
			String attractiveness = location.getElementsByTagName("attractiveness").item(0).getTextContent();
			int placeTypeIndex = Integer.parseInt(attractiveness);
			//*Mine* Generate a random waiting time for each edge data center(14 edge data center) from {480, 300, 120 in minutes} values which exist in mobilityLookUpTable variables
			expRngList[i] = new ExponentialDistribution(SimSettings.getInstance().getMobilityLookUpTable()[placeTypeIndex]);
		}
		
		//initialize tree maps and position of mobile devices 
		//*Mine* assign each mobile device to a random edge data center
		for(int i=0; i<numberOfMobileDevices; i++) {
			treeMapArray.add(i, new TreeMap<Double, Location>());
			//*Mine* generate a random edge data center from 0-13
			int randDatacenterId = SimUtils.getRandomNumber(0, SimSettings.getInstance().getNumOfEdgeDatacenters()-1);
			// Read the information of this edge data center (location)
			Node datacenterNode = datacenterList.item(randDatacenterId);
			Element datacenterElement = (Element) datacenterNode;
			Element location = (Element)datacenterElement.getElementsByTagName("location").item(0);
			String attractiveness = location.getElementsByTagName("attractiveness").item(0).getTextContent();
			int placeTypeIndex = Integer.parseInt(attractiveness);
			int wlan_id = Integer.parseInt(location.getElementsByTagName("wlan_id").item(0).getTextContent());
			int x_pos = Integer.parseInt(location.getElementsByTagName("x_pos").item(0).getTextContent());
			int y_pos = Integer.parseInt(location.getElementsByTagName("y_pos").item(0).getTextContent());
			//*Mine* put the mobile device at this edge data center and each mobile should start after 10 seconds of simulation time
			//start locating user shortly after the simulation started (e.g. 10 seconds)
			treeMapArray.get(i).put(SimSettings.CLIENT_ACTIVITY_START_TIME, new Location(placeTypeIndex, wlan_id, x_pos, y_pos));
		}
		
		//*Mine* The mobility of each mobile device (Move from one Edge Data Center to another Randomly until the simulation time finished)
		for(int i=0; i<numberOfMobileDevices; i++) {
			TreeMap<Double, Location> treeMap = treeMapArray.get(i);
			//*Mine* this loop to guarantee that each mobile device remains moving till the Simulation Time is finished
			//*Mine* treeMap.lastKey() return the highest key in the treeMap (last time this mobile moved)
			while(treeMap.lastKey() < SimSettings.getInstance().getSimulationTime()) {				
				boolean placeFound = false;
				//*Mine* treeMap.lastEntry() return key, value pairs for the highest key in the treeMap (current location of the mobile device)
				int currentLocationId = treeMap.lastEntry().getValue().getServingWlanId();//*Mine* return the id for the edge data center
				double waitingTime = expRngList[currentLocationId].sample();
				//*Mine* this loop to iterate until the mobile should find another edge data center which is different from the current one
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
					}
				}
				if(!placeFound){
					SimLogger.printLine("impossible is occured! location cannot be assigned to the device!");
			    	System.exit(0);
				}
			}
		}

	}
	
	//*Mine* this method is print each device locations and its associated edge data center through the simulation time
	public void printDeviceLocations(){
		
		String FileLocation="E:\\HIT\\OneDrive - Computer and Information Technology (Menofia University)\\My Papers\\Git Code\\Other codes\\Cloud_EdgeSim_Code\\cloudsim-3.0.3\\scripts\\sample_app3\\config\\Files\\DeviceLocations.txt"; 
		BufferedWriter writer=null;
		try {
			writer = new BufferedWriter(new FileWriter(FileLocation,true));
			for(int i=0; i<numberOfMobileDevices; i++) {
				/* Display content using Iterator*/
			    Set set = treeMapArray.get(i).entrySet();
			    Iterator iterator = set.iterator();
			    while(iterator.hasNext()) {
			       Map.Entry mentry = (Map.Entry)iterator.next();
			       //System.out.println("Mobile Device"+i+" at "+ mentry.getKey() + " associated with "+((Location)mentry.getValue()).getServingWlanId()+" Edge Datacenter at ("+((Location)mentry.getValue()).getXPos()+","+((Location)mentry.getValue()).getYPos()+")");
			       writer.append("Mobile Device "+i+" at time: "+ mentry.getKey() + " is associated with "+((Location)mentry.getValue()).getServingWlanId()+" Edge Datacenter with location ("+((Location)mentry.getValue()).getXPos()+","+((Location)mentry.getValue()).getYPos()+")");	
					writer.newLine();
			    }
			}
			writer.newLine();
			writer.close();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	public Location getLocation(int deviceId, double time) {
		TreeMap<Double, Location> treeMap = treeMapArray.get(deviceId);
		
		//*Mine* treeMap.floorEntry(time) return the key,value pair associated with the greatest key less than or equal to time 		
		Entry<Double, Location> e = treeMap.floorEntry(time);
	    
	    if(e == null){
	    	SimLogger.printLine("impossible is occured! no location is found for the device '" + deviceId + "' at " + time);
	    	System.exit(0);
	    }
	    
		return e.getValue();
	}

}
