package Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.commons.math3.distribution.ExponentialDistribution;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.EdgeTask;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;

public class TestTaskGenerator {

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
		
		
		//exponential number generator for file input size, file output size and task length
				ExponentialDistribution[][] expRngList = new ExponentialDistribution[SS.getTaskLookUpTable().length][3];

				//create random number generator for each place
				for(int i=0; i<SimSettings.getInstance().getTaskLookUpTable().length; i++) {
					if(SimSettings.getInstance().getTaskLookUpTable()[i][0] ==0)//*Mine*  [0] usage percentage (%)
						continue;
					
					expRngList[i][0] = new ExponentialDistribution(SS.getTaskLookUpTable()[i][5]);//*Mine* [5] avg data upload (KB)		    // 
					expRngList[i][1] = new ExponentialDistribution(SS.getTaskLookUpTable()[i][6]);//*Mine* [6] avg data download (KB)
					expRngList[i][2] = new ExponentialDistribution(SS.getTaskLookUpTable()[i][7]);//*Mine* [7] avg task length (MI)

				}
				
		
		List<EdgeTask> taskList = new ArrayList<EdgeTask>();
		
		int[] taskTypeOfDevices = new int[2];
		Vector<Double>startime_EdgeTask=new Vector<Double>();
		int[]numberoftask_permobile=new int[2];
		for (int i = 0; i < 2; i++) {
			int randomTaskType = -1;
			double taskTypeSelector = SimUtils.getRandomDoubleNumber(0, 100);
			double taskTypePercentage = 0;
			for (int j = 0; j < SS.getTaskLookUpTable().length; j++) {
				taskTypePercentage += SS.getTaskLookUpTable()[j][0];// *Mine* [0] usage percentage (%)
				if (taskTypeSelector <= taskTypePercentage) {
					randomTaskType = j;
					break;
				}
			}
			System.out.println("taskTypePercentage Value of Mobile"+i+" is "+taskTypePercentage);
			System.out.println("randomTaskType Value of Mobile"+i+" is "+randomTaskType);
		
			double poissonMean = SS.getTaskLookUpTable()[randomTaskType][2];//*Mine* [2] poisson mean (sec) 
			double activePeriod = SS.getTaskLookUpTable()[randomTaskType][3];//*Mine* [3] active period (sec)
			double idlePeriod = SS.getTaskLookUpTable()[randomTaskType][4];//*Mine* [4] idle period (sec)
			double activePeriodStartTime = SimUtils.getRandomDoubleNumber(
					SimSettings.CLIENT_ACTIVITY_START_TIME, 
					SimSettings.CLIENT_ACTIVITY_START_TIME + activePeriod);  //active period starts shortly after the simulation started (e.g. 10 seconds)
			double virtualTime = activePeriodStartTime;
			
			System.out.println("poissonMean Value of Mobile"+i+" is "+poissonMean);
			System.out.println("activePeriod Value of Mobile"+i+" is "+activePeriod);
			System.out.println("idlePeriod Value of Mobile"+i+" is "+idlePeriod);
			System.out.println("activePeriodStartTime Value of Mobile"+i+" is "+activePeriodStartTime);


			ExponentialDistribution rng = new ExponentialDistribution(poissonMean);
			//*Mine* Generate a set of EdgeTask for each mobile device where poissonMean is the period of time between two tasks  
			// Therefore, in the next loop, it generate random poissonMean value (interval) to be the start time for the each task
			// and the condition of loop (virtualTime < SS.getSimulationTime()) guarantee that the tasks start time doesn't exceed the Simulation Time 
			
			while(virtualTime < SS.getSimulationTime()) {
				double interval = rng.sample();
				//System.out.println("interval Value is "+interval);

				if(interval <= 0){
					SimLogger.printLine("Impossible is occured! interval is " + interval + " for device " + i + " time " + virtualTime);
					continue;
				}
				//SimLogger.printLine(virtualTime + " -> " + interval + " for device " + i + " time ");
				virtualTime += interval;
				//System.out.println("virtualTime New Value is "+virtualTime);

				// this if statement guarantee that the start time for each task doesn't exceed the total time for the application type
				// which is the summation of activePeriodStartTime and activePeriod
				//System.out.println("activePeriodStartTime + activePeriod Value is "+(activePeriodStartTime + activePeriod));
				if(virtualTime > activePeriodStartTime + activePeriod){
					activePeriodStartTime = activePeriodStartTime + activePeriod + idlePeriod;
					virtualTime = activePeriodStartTime;
					continue;
				}
				
				taskList.add(new EdgeTask(i,randomTaskType, virtualTime, expRngList));
				//*Mine* EdgeTask(int mobileDeviceId, APP_TYPES taskType, double startTime, ExponentialDistribution[][] expRngList)
				//*Mine* expRngList==> data upload (KB),avg data download (KB), avg task length (MI)
				numberoftask_permobile[i]++;
				startime_EdgeTask.add(virtualTime);
				
			}	
			startime_EdgeTask.add(0.0);
			System.out.println("Number of EdgeTasks of Mobile"+ i+" is "+numberoftask_permobile[i]);
			System.out.println("Simulation Time Value is "+SS.getSimulationTime()+" Seconds");

			System.out.println("*******************************");

	
		}
		
		for(int i=0;i<startime_EdgeTask.size();i++){
			if(startime_EdgeTask.get(i)==0.0){
				System.out.println(startime_EdgeTask.get(i));	
			}else{
				System.out.print(startime_EdgeTask.get(i)+", ");
			}
				
		}
	}
}
