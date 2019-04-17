/*
 * Title:        EdgeCloudSim - Simulation Manager
 * 
 * Description: 
 * SimManager is an singleton class providing many abstract classeses such as
 * Network Model, Mobility Model, Edge Orchestrator to other modules
 * Critical simulation related information would be gathered via this class 
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.core;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.FutureQueue;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;














import edu.boun.edgecloudsim.applications.sample_app3.SampleNetworkModel;
import edu.boun.edgecloudsim.applications.sample_app3.SampleEdgeOrchestrator;
import edu.boun.edgecloudsim.edge_orchestrator.EdgeOrchestrator;
import edu.boun.edgecloudsim.edge_server.EdgeServerManager;
import edu.boun.edgecloudsim.edge_server.EdgeVmAllocationPolicy_Custom;
import edu.boun.edgecloudsim.cloud_server.CloudServerManager;
import edu.boun.edgecloudsim.edge_client.MobileDeviceManager;
import edu.boun.edgecloudsim.edge_client.mobile_processing_unit.MobileServerManager;
import edu.boun.edgecloudsim.mobility.MobilityModel;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import edu.boun.edgecloudsim.network.NetworkModel;
import edu.boun.edgecloudsim.utils.EdgeTask;
import edu.boun.edgecloudsim.utils.SimLogger;

public class SimManager extends SimEntity {
	private static final int CREATE_TASK = 0;
	private static final int CHECK_ALL_VM = 1;
	private static final int GET_LOAD_LOG = 2;
	private static final int PRINT_PROGRESS = 3;
	private static final int STOP_SIMULATION = 4;
	
	private String simScenario;
	private String orchestratorPolicy;
	private int numOfMobileDevice;
	private NetworkModel networkModel;
	private MobilityModel mobilityModel;
	private ScenarioFactory scenarioFactory;
	private EdgeOrchestrator edgeOrchestrator;
	private EdgeServerManager edgeServerManager;
	private CloudServerManager cloudServerManager;
	private MobileServerManager mobileServerManager;
	private LoadGeneratorModel loadGeneratorModel;
	private MobileDeviceManager mobileDeviceManager;
	
	private static SimManager instance = null;
	
	public SimManager(ScenarioFactory _scenarioFactory, int _numOfMobileDevice, String _simScenario, String _orchestratorPolicy) throws Exception {
		super("SimManager");
		simScenario = _simScenario;
		scenarioFactory = _scenarioFactory;
		numOfMobileDevice = _numOfMobileDevice;
		orchestratorPolicy = _orchestratorPolicy;

		SimLogger.printLine("Creating tasks...");
		loadGeneratorModel = scenarioFactory.getLoadGeneratorModel();
		//*Mine* generate different number of edge tasks for each mobile device at each simScenario
		loadGeneratorModel.initializeModel();
		loadGeneratorModel.printDeviceTasks(); //*Mine* print the number of edge tasks for each mobile device.
		SimLogger.printLine("Tasks are Created.");
		

		
		SimLogger.printLine("Creating device locations...");
		mobilityModel = scenarioFactory.getMobilityModel();
		mobilityModel.initialize();
		mobilityModel.printDeviceLocations(); //*Mine* print each device locations and its associated edge data center through the simulation time
		SimLogger.printLine("Device Locations are Created.");

		//Generate network model
		networkModel = scenarioFactory.getNetworkModel();
		networkModel.initialize();

		
		//Generate edge orchestrator
		edgeOrchestrator = scenarioFactory.getEdgeOrchestrator();
		edgeOrchestrator.initialize();


		//Create Physical Servers
		edgeServerManager = scenarioFactory.getEdgeServerManager();
		edgeServerManager.initialize();

		//Create Physical Servers on cloud
		cloudServerManager = scenarioFactory.getCloudServerManager();
		cloudServerManager.initialize();

		//Create Physical Servers on mobile devices
		mobileServerManager = scenarioFactory.getMobileServerManager();
		mobileServerManager.initialize();

		//Create Client Manager
		mobileDeviceManager = scenarioFactory.getMobileDeviceManager();
		mobileDeviceManager.initialize();

		instance = this;
	}
	
	public static SimManager getInstance(){
		return instance;
	}
	
	/**
	 * Triggering CloudSim to start simulation
	 */
	public void startSimulation() throws Exception{
		//Starts the simulation
		SimLogger.printLine(super.getName()+" is starting...");
		
		//Start Edge Datacenters & Generate VMs
		edgeServerManager.startDatacenters();
		edgeServerManager.createVmList(mobileDeviceManager.getId());
		SimLogger.printLine("DefaultEdgeServerManager Class:  Edge DataCenters and VMs are created");
		SimLogger.printLine("Number of Edge DataCenter "+edgeServerManager.getNumberOfEdgeDataCenter()+" Number of Hosts "+edgeServerManager.getNumberOfHostOnEdge()+" Number of VMs "+edgeServerManager.getNumberOfVMOnEdge());
		//Start Edge Datacenters & Generate VMs
		cloudServerManager.startDatacenters();
		cloudServerManager.createVmList(mobileDeviceManager.getId());
		SimLogger.printLine("DefaultCloudServerManager Class:  Cloud DataCenters and VMs are created");
		SimLogger.printLine("Number of Hosts "+cloudServerManager.getNumberOfHostOnCloud()+" Number of VMs on CloudServerManager "+cloudServerManager.getNumberOfVMOnCloud());
		//Start Mobile Datacenters & Generate VMs
		mobileServerManager.startDatacenters();
		mobileServerManager.createVmList(mobileDeviceManager.getId());
		SimLogger.printLine("SampleMobileServerManager Class:  Mobile DataCenters and VMs are created");
		//The number of hosts and VMs here is equal to number of mobile devices where for each mobile, one host with one VM is created  
		SimLogger.printLine("Number of Hosts "+mobileServerManager.getNumberOfHostOnMobile()+" Number of VMs on MobileServerManager "+mobileServerManager.getNumberOfVMOnMobile());


		SimLogger.printLine("Before CloudSim Start Simulation");
		CloudSim.startSimulation();
		SimLogger.printLine("After CloudSim Start Simulation");

	}

	public String getSimulationScenario(){
		return simScenario;
	}

	public String getOrchestratorPolicy(){
		return orchestratorPolicy;
	}
	
	public ScenarioFactory getScenarioFactory(){
		return scenarioFactory;
	}
	
	public int getNumOfMobileDevice(){
		return numOfMobileDevice;
	}
	
	public NetworkModel getNetworkModel(){
		return networkModel;
	}

	public MobilityModel getMobilityModel(){
		return mobilityModel;
	}
	
	public EdgeOrchestrator getEdgeOrchestrator(){
		return edgeOrchestrator;
	}
	
	public EdgeServerManager getEdgeServerManager(){
		return edgeServerManager;
	}
	
	public CloudServerManager getCloudServerManager(){
		return cloudServerManager;
	}
	
	public MobileServerManager getMobileServerManager(){
		return mobileServerManager;
	}

	public LoadGeneratorModel getLoadGeneratorModel(){
		return loadGeneratorModel;
	}
	
	public MobileDeviceManager getMobileDeviceManager(){
		return mobileDeviceManager;
	}
	
	@Override
	public void startEntity() {
		int hostCounter=0;

		//*Mine* Adding all the VMs in edge Server Manager to mobileDeviceManager object
		for(int i= 0; i<edgeServerManager.getDatacenterList().size(); i++) {
			List<? extends Host> list = edgeServerManager.getDatacenterList().get(i).getHostList();
			for (int j=0; j < list.size(); j++) {
				mobileDeviceManager.submitVmList(edgeServerManager.getVmList(hostCounter));
				hostCounter++;
			}
		}
		//*Mine* Adding all the VMs in cloud Server Manager to mobileDeviceManager object
		//*Mine* This number (VMs in cloud Server Manager) is defined in default_config.properties file)
		for(int i= 0; i<SimSettings.getInstance().getNumOfCoudHost(); i++) {
			mobileDeviceManager.submitVmList(cloudServerManager.getVmList(i));
		}

		//*Mine* Adding all the VMs in mobile Server Manager to mobileDeviceManager object
		for(int i=0; i<numOfMobileDevice; i++){
			if(mobileServerManager.getVmList(i) != null)
				mobileDeviceManager.submitVmList(mobileServerManager.getVmList(i));
		}

		//Creation of tasks are scheduled here!
		/**
		 * Send an event to another entity by id number, with data. Note that the tag <code>9999</code>
		 * is reserved.
		 * 
		 * @param dest The unique id number of the destination entity
		 * @param delay How long from the current simulation time the event should be sent
		 * @param tag An user-defined number representing the type of event.
		 * @param data The data to be sent with the event.
		 */		
		//*Mine* Adding the EdgeTask as SimEvent to the FutureQueue (SortedSet) object in CloudSim Class
		//*Mine* If I need to apply Partial Offloading, I can change in this loop to add only a specific tasks
		for(int i=0; i< loadGeneratorModel.getTaskList().size(); i++){
			//*Mine* schedule(int dest, double delay, int tag, Object data)
			schedule(getId(), loadGeneratorModel.getTaskList().get(i).startTime, CREATE_TASK, loadGeneratorModel.getTaskList().get(i));
		}
		
		
		//Periodic event loops starts from here!
		//*Mine*schedule(int dest, double delay, int tag)
		schedule(getId(), 5, CHECK_ALL_VM);
		schedule(getId(), SimSettings.getInstance().getSimulationTime()/1000, PRINT_PROGRESS);//*Mine* This line print Numbers in output 
		schedule(getId(), SimSettings.getInstance().getVmLoadLogInterval(), GET_LOAD_LOG);
		schedule(getId(), SimSettings.getInstance().getSimulationTime(), STOP_SIMULATION);

		SimLogger.printLine("StartEntity Method in SimManager Class Done.");
		//SimLogger.printLine("Number of Events in FutureQueue is: "+CloudSim.getFutureQueue().size());
		//WriteEventsStartTimeInFile();
	}
//*Mine* Defined by me to write the start time of all the event on the file 
	public void WriteEventsStartTimeInFile(){
		String FileLocation="E:\\HIT\\OneDrive - Computer and Information Technology (Menofia University)\\My Papers\\Git Code\\Other codes\\Cloud_EdgeSim_Code\\cloudsim-3.0.3\\scripts\\sample_app3\\config\\Files\\Events_Time.txt"; 
		BufferedWriter writer=null;
		try {
			
			writer = new BufferedWriter(new FileWriter(FileLocation,true));
			for(int i=0; i<loadGeneratorModel.getTaskList().size(); i++) {
				
				//System.out.println("The Mobile Device"+i+" has "+numberOfEdgeTaskPerDevcie[i]+" Edge Taks");
				writer.append("Time for Event "+i+" is "+loadGeneratorModel.getTaskList().get(i).startTime);	
				writer.newLine();
			}
			int x=loadGeneratorModel.getTaskList().size()+1;
			writer.append("Time for Event "+x+" is "+5);	
			writer.newLine();
			++x;
			writer.append("Time for Event "+x+" is "+SimSettings.getInstance().getSimulationTime()/100);	
			writer.newLine();
			++x;
			writer.append("Time for Event "+x+" is "+SimSettings.getInstance().getVmLoadLogInterval());	
			writer.newLine();
			++x;
			writer.append("Time for Event "+x+" is "+SimSettings.getInstance().getSimulationTime());	
			writer.newLine();
			
			writer.newLine();			
			writer.close();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void processEvent(SimEvent ev) {
		synchronized(this){
			switch (ev.getTag()) {
			case CREATE_TASK:
				try {
					EdgeTask edgeTask = (EdgeTask) ev.getData();
					mobileDeviceManager.submitTask(edgeTask);						
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(0);
				}
				break;
			case CHECK_ALL_VM:
				int totalNumOfVm = SimSettings.getInstance().getNumOfEdgeVMs();
				if(EdgeVmAllocationPolicy_Custom.getCreatedVmNum() != totalNumOfVm){
					SimLogger.printLine("All VMs cannot be created! Terminating simulation...");
					System.exit(0);
				}
				break;
			case GET_LOAD_LOG:
				SimLogger.getInstance().addVmUtilizationLog(
						CloudSim.clock(),
						edgeServerManager.getAvgUtilization(),
						cloudServerManager.getAvgUtilization(),
						mobileServerManager.getAvgUtilization());
				
				schedule(getId(), SimSettings.getInstance().getVmLoadLogInterval(), GET_LOAD_LOG);
				break;
			case PRINT_PROGRESS:
				int progress = (int)((CloudSim.clock()*100)/SimSettings.getInstance().getSimulationTime());
				if(progress % 10 == 0)
					SimLogger.print(Integer.toString(progress));
				else
					SimLogger.print(".");
				if(CloudSim.clock() < SimSettings.getInstance().getSimulationTime())
					schedule(getId(), SimSettings.getInstance().getSimulationTime()/100, PRINT_PROGRESS);

				break;
			case STOP_SIMULATION:
				SimLogger.printLine("100");
				CloudSim.terminateSimulation();
				try {
					SimLogger.getInstance().simStopped();
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(0);
				}
				break;
			default:
				Log.printLine(getName() + ": unknown event type");
				break;
			}
		}
	}

	@Override
	public void shutdownEntity() {
		edgeServerManager.terminateDatacenters();
		cloudServerManager.terminateDatacenters();
		mobileServerManager.terminateDatacenters();
	}
}
