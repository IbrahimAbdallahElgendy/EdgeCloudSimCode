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

import edu.boun.edgecloudsim.cloud_server.CloudVmAllocationPolicy_Custom;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.mobile_processing_unit.MobileHost;
import edu.boun.edgecloudsim.edge_client.mobile_processing_unit.MobileVmAllocationPolicy_Custom;
import edu.boun.edgecloudsim.utils.SimLogger;

public class TestMobileServerManager {

	public static void main(String[] args) {

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
			System.out
					.println("Simulation setting file, output folder and iteration number are not provided! Using default ones...");
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

		DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Date SimulationStartDate = Calendar.getInstance().getTime();
		String now = df.format(SimulationStartDate);
		SimLogger.printLine("Simulation started at " + now);
		SimLogger
				.printLine("----------------------------------------------------------------------");

		Datacenter localDatacenter = null;

		int index = SS.MOBILE_DATACENTER_ID;

		// ------------------------Create Cloud
		// DataCenter-------------------------
		String arch = "x86";
		String os = "Linux";
		String vmm = "Xen";
		double costPerBw = 0;
		double costPerSec = 0;
		double costPerMem = 0;
		double costPerStorage = 0;
		System.out.println("Mobile DataCenter:" + index + " arch: " + arch
				+ " os: " + os + " vmm: " + vmm + " costPerBw: " + costPerBw
				+ " costPerSec: " + costPerSec + " costPerMem: " + costPerMem
				+ " costPerStorage: " + costPerStorage);

		List<MobileHost> hostList1 = null;
		// ---------------------Create Cloud Hosts---------------------------

		// Here are the steps needed to create a PowerDatacenter:
		// 1. We need to create a list to store one or more Machines
		List<MobileHost> hostList = new ArrayList<MobileHost>();

		for (int i = 0; i < 10; i++) {

			int numOfCores = SS.getCoreForMobileVM();
			double mips = SS.getMipsForMobileVM();
			int ram = SS.getRamForMobileVM();
			long storage = SS.getStorageForMobileVM();
			long bandwidth = 0;

			// 2. A Machine contains one or more PEs or CPUs/Cores. Therefore,
			// should
			// create a list to store these PEs before creating
			// a Machine.
			List<Pe> peList = new ArrayList<Pe>();

			// 3. Create PEs and add these into the list.
			// for a quad-core machine, a list of 4 PEs is required:
			for (int j = 0; j < numOfCores; j++) {
				peList.add(new Pe(j, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating
			}

			// 4. Create Hosts with its id and list of PEs and add them to the
			// list of machines
			MobileHost host = new MobileHost(
					// Hosts should have unique IDs, so create Mobile Hosts
					// after Edge+Cloud Hosts
					i + SimSettings.getInstance().getNumOfEdgeHosts()
							+ SimSettings.getInstance().getNumOfCoudHost(),
					new RamProvisionerSimple(ram), new BwProvisionerSimple(
							bandwidth), // kbps
					storage, peList, new VmSchedulerSpaceShared(peList));

			host.setMobileDeviceId(i);
			System.out.println("Mobile Host:"+i+" numofCores: "+numOfCores+" mips: "+mips+" ram: "+ram+" storage: "+storage+" bandwidth: "+bandwidth);

			hostList.add(host);
		}
		hostList1 = hostList;

		String name = "MobileDatacenter_" + Integer.toString(index);
		double time_zone = 3.0; // time zone this resource located
		LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN devices by now

		// 5. Create a DatacenterCharacteristics object that stores the
		// properties of a data center: architecture, OS, list of
		// Machines, allocation policy: time- or space-shared, time zone
		// and its price (G$/Pe time unit).
		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
				arch, os, vmm, hostList, time_zone, costPerSec, costPerMem,
				costPerStorage, costPerBw);

		// 6. Finally, we need to create a PowerDatacenter object.
		Datacenter datacenter = null;

		VmAllocationPolicy vm_policy = new MobileVmAllocationPolicy_Custom(
				hostList, index);

		try {
			int num_user = 2; // number of grid users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false; // mean trace events

			// Initialize the CloudSim library
			CloudSim.init(num_user, calendar, trace_flag, 0.01);
			datacenter = new Datacenter(name, characteristics, vm_policy,
					storageList, 0);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		localDatacenter = datacenter;

	}

}
