package Test;

import edu.boun.edgecloudsim.core.SimSettings;

public class TestNetwork {
	
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
		
		
		
	}

}
