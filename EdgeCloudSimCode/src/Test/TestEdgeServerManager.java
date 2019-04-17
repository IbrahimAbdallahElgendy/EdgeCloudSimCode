package Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.VmSchedulerSpaceShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.boun.edgecloudsim.applications.sample_app3.SampleScenarioFactory;
import edu.boun.edgecloudsim.core.ScenarioFactory;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_server.EdgeHost;
import edu.boun.edgecloudsim.edge_server.EdgeServerManager;
import edu.boun.edgecloudsim.edge_server.EdgeVmAllocationPolicy_Custom;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;

public class TestEdgeServerManager {

	public static void main(String[]args) {
		
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
		System.out.println("Number of Edge DataCenter "+SS.getNumOfEdgeDatacenters());

		System.out.println("Number of Hosts "+SS.getNumOfEdgeHosts());

		System.out.println("Number of VMs per Host "+SS.getNumOfEdgeVMs());
		
		DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Date SimulationStartDate = Calendar.getInstance().getTime();
		String now = df.format(SimulationStartDate);
		SimLogger.printLine("Simulation started at " + now);
		SimLogger.printLine("----------------------------------------------------------------------");
		
//--------------------------------------------------------------------------
		
		Document doc = SimSettings.getInstance().getEdgeDevicesDocument();
		NodeList datacenterList = doc.getElementsByTagName("datacenter");
		for (int i = 0; i < datacenterList.getLength(); i++) {
			Node datacenterNode = datacenterList.item(i);
			Element datacenterElement = (Element) datacenterNode;
			//*Mine* Defined in the super class {protected List<Datacenter> localDatacenters;}
			List<Datacenter> localDatacenters=new ArrayList<Datacenter>();
			//-----------------Create Edge DataCenters-------------------
			
			String arch = datacenterElement.getAttribute("arch");
			String os = datacenterElement.getAttribute("os");
			String vmm = datacenterElement.getAttribute("vmm");
			double costPerBw = Double.parseDouble(datacenterElement.getElementsByTagName("costPerBw").item(0).getTextContent());
			double costPerSec = Double.parseDouble(datacenterElement.getElementsByTagName("costPerSec").item(0).getTextContent());
			double costPerMem = Double.parseDouble(datacenterElement.getElementsByTagName("costPerMem").item(0).getTextContent());
			double costPerStorage = Double.parseDouble(datacenterElement.getElementsByTagName("costPerStorage").item(0).getTextContent());
			System.out.println("Edge DataCenter:"+i+" arch: "+arch+" os: "+os+" vmm: "+vmm+" costPerBw: "+costPerBw+" costPerSec: "+costPerSec +" costPerMem: "+costPerMem+" costPerStorage: "+costPerStorage);
			//------------------Create Edge Hosts-----------------				
			int hostIdCounter=0;
			// Here are the steps needed to create a PowerDatacenter:
			// 1. We need to create a list to store one or more Machines
			List<EdgeHost> hostList = new ArrayList<EdgeHost>();
			
			Element location = (Element)datacenterElement.getElementsByTagName("location").item(0);
			String attractiveness = location.getElementsByTagName("attractiveness").item(0).getTextContent();
			int wlan_id = Integer.parseInt(location.getElementsByTagName("wlan_id").item(0).getTextContent());
			int x_pos = Integer.parseInt(location.getElementsByTagName("x_pos").item(0).getTextContent());
			int y_pos = Integer.parseInt(location.getElementsByTagName("y_pos").item(0).getTextContent());
			int placeTypeIndex = Integer.parseInt(attractiveness);

			NodeList hostNodeList = datacenterElement.getElementsByTagName("host");
			for (int j = 0; j < hostNodeList.getLength(); j++) {
				Node hostNode = hostNodeList.item(j);
				
				Element hostElement = (Element) hostNode;
				int numOfCores = Integer.parseInt(hostElement.getElementsByTagName("core").item(0).getTextContent());
				double mips = Double.parseDouble(hostElement.getElementsByTagName("mips").item(0).getTextContent());
				int ram = Integer.parseInt(hostElement.getElementsByTagName("ram").item(0).getTextContent());
				long storage = Long.parseLong(hostElement.getElementsByTagName("storage").item(0).getTextContent());
				//*Mine* Divide the BANDWITH_WLAN across the number of hosts 
				long bandwidth = SS.getWlanBandwidth() / hostNodeList.getLength();
				

				
				// 2. A Machine contains one or more PEs or CPUs/Cores. Therefore, should
				//    create a list to store these PEs before creating
				//    a Machine.
				//*Mine* PE (Processing Element) class represents CPU unit, defined in terms of Millions Instructions Per Second (MIPS) rating.
				List<Pe> peList = new ArrayList<Pe>();

				// 3. Create PEs and add these into the list.
				//for a quad-core machine, a list of 4 PEs is required:
				for(int ii=0; ii<numOfCores; ii++){
					peList.add(new Pe(ii, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating
				}
				
				//4. Create Hosts with its id and list of PEs and add them to the list of machines
				EdgeHost host = new EdgeHost(
						hostIdCounter,
						new RamProvisionerSimple(ram),
						new BwProvisionerSimple(bandwidth), //kbps
						storage,
						peList,
						new VmSchedulerSpaceShared(peList)
					);
				
				host.setPlace(new Location(placeTypeIndex, wlan_id, x_pos, y_pos));
				System.out.println("Edge Host:"+j+" numofCores: "+numOfCores+" mips: "+mips+" ram: "+ram+" storage: "+storage+" bandwidth: "+bandwidth+" placetypeIndex: "+placeTypeIndex+" wlan_id: "+wlan_id+" x_pos: "+x_pos+" y_pos: "+y_pos);
				hostList.add(host);
				hostIdCounter++;
			}
//---------------------Finished Host Creation----------------------------			
			String name = "Datacenter_" + Integer.toString(i);
			double time_zone = 3.0;         // time zone this resource located
			LinkedList<Storage> storageList = new LinkedList<Storage>();	//we are not adding SAN devices by now

			// 5. Create a DatacenterCharacteristics object that stores the
			//    properties of a data center: architecture, OS, list of
			//    Machines, allocation policy: time- or space-shared, time zone
			//    and its price (G$/Pe time unit).
			DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
	                arch, os, vmm, hostList, time_zone, costPerSec, costPerMem, costPerStorage, costPerBw);

			// 6. Finally, we need to create a PowerDatacenter object.
			Datacenter datacenter = null;		

			VmAllocationPolicy vm_policy = new EdgeVmAllocationPolicy_Custom(hostList,i);
			
			try {
				int num_user = 2;   // number of grid users
				Calendar calendar = Calendar.getInstance();
				boolean trace_flag = false;  // mean trace events
		
				// Initialize the CloudSim library
				CloudSim.init(num_user, calendar, trace_flag, 0.01);
				datacenter = new Datacenter(name, characteristics, vm_policy, storageList, 0);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
			localDatacenters.add(datacenter);
		}
		
		
		
//---------------------------------------------------------------------------		
		
		
	}
	
	public void startDatacenters() throws Exception{
		Document doc = SimSettings.getInstance().getEdgeDevicesDocument();
		NodeList datacenterList = doc.getElementsByTagName("datacenter");
		for (int i = 0; i < datacenterList.getLength(); i++) {
			Node datacenterNode = datacenterList.item(i);
			Element datacenterElement = (Element) datacenterNode;
			//*Mine* Defined in the super class {protected List<Datacenter> localDatacenters;}
			List<Datacenter> localDatacenters=new ArrayList<Datacenter>();
			
					
			
			
			localDatacenters.add(createDatacenter(i, datacenterElement));
		}
	}
	Datacenter createDatacenter(int index, Element datacenterElement) throws Exception{
		String arch = datacenterElement.getAttribute("arch");
		String os = datacenterElement.getAttribute("os");
		String vmm = datacenterElement.getAttribute("vmm");
		double costPerBw = Double.parseDouble(datacenterElement.getElementsByTagName("costPerBw").item(0).getTextContent());
		double costPerSec = Double.parseDouble(datacenterElement.getElementsByTagName("costPerSec").item(0).getTextContent());
		double costPerMem = Double.parseDouble(datacenterElement.getElementsByTagName("costPerMem").item(0).getTextContent());
		double costPerStorage = Double.parseDouble(datacenterElement.getElementsByTagName("costPerStorage").item(0).getTextContent());
		
		List<EdgeHost> hostList=createHosts(datacenterElement);
		
		String name = "Datacenter_" + Integer.toString(index);
		double time_zone = 3.0;         // time zone this resource located
		LinkedList<Storage> storageList = new LinkedList<Storage>();	//we are not adding SAN devices by now

		// 5. Create a DatacenterCharacteristics object that stores the
		//    properties of a data center: architecture, OS, list of
		//    Machines, allocation policy: time- or space-shared, time zone
		//    and its price (G$/Pe time unit).
		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, costPerSec, costPerMem, costPerStorage, costPerBw);


		// 6. Finally, we need to create a PowerDatacenter object.
		Datacenter datacenter = null;
	
		VmAllocationPolicy vm_policy = getVmAllocationPolicy(hostList,index);
		datacenter = new Datacenter(name, characteristics, vm_policy, storageList, 0);
		
		return datacenter;
	}
	
	private List<EdgeHost> createHosts(Element datacenterElement){

		int hostIdCounter=0;
		// Here are the steps needed to create a PowerDatacenter:
		// 1. We need to create a list to store one or more Machines
		List<EdgeHost> hostList = new ArrayList<EdgeHost>();
		
		Element location = (Element)datacenterElement.getElementsByTagName("location").item(0);
		String attractiveness = location.getElementsByTagName("attractiveness").item(0).getTextContent();
		int wlan_id = Integer.parseInt(location.getElementsByTagName("wlan_id").item(0).getTextContent());
		int x_pos = Integer.parseInt(location.getElementsByTagName("x_pos").item(0).getTextContent());
		int y_pos = Integer.parseInt(location.getElementsByTagName("y_pos").item(0).getTextContent());
		int placeTypeIndex = Integer.parseInt(attractiveness);

		NodeList hostNodeList = datacenterElement.getElementsByTagName("host");
		for (int j = 0; j < hostNodeList.getLength(); j++) {
			Node hostNode = hostNodeList.item(j);
			
			Element hostElement = (Element) hostNode;
			int numOfCores = Integer.parseInt(hostElement.getElementsByTagName("core").item(0).getTextContent());
			double mips = Double.parseDouble(hostElement.getElementsByTagName("mips").item(0).getTextContent());
			int ram = Integer.parseInt(hostElement.getElementsByTagName("ram").item(0).getTextContent());
			long storage = Long.parseLong(hostElement.getElementsByTagName("storage").item(0).getTextContent());
			//*Mine* Divide the BANDWITH_WLAN across the number of hosts 
			long bandwidth = SimSettings.getInstance().getWlanBandwidth() / hostNodeList.getLength();
			
			// 2. A Machine contains one or more PEs or CPUs/Cores. Therefore, should
			//    create a list to store these PEs before creating
			//    a Machine.
			//*Mine* PE (Processing Element) class represents CPU unit, defined in terms of Millions Instructions Per Second (MIPS) rating.
			List<Pe> peList = new ArrayList<Pe>();

			// 3. Create PEs and add these into the list.
			//for a quad-core machine, a list of 4 PEs is required:
			for(int i=0; i<numOfCores; i++){
				peList.add(new Pe(i, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating
			}
			
			//4. Create Hosts with its id and list of PEs and add them to the list of machines
			EdgeHost host = new EdgeHost(
					hostIdCounter,
					new RamProvisionerSimple(ram),
					new BwProvisionerSimple(bandwidth), //kbps
					storage,
					peList,
					new VmSchedulerSpaceShared(peList)
				);
			
			host.setPlace(new Location(placeTypeIndex, wlan_id, x_pos, y_pos));
			hostList.add(host);
			hostIdCounter++;
		}

		return hostList;
	}
	
	public VmAllocationPolicy getVmAllocationPolicy(List<? extends Host> hostList, int dataCenterIndex) {
		return new EdgeVmAllocationPolicy_Custom(hostList,dataCenterIndex);
	}
	
}
