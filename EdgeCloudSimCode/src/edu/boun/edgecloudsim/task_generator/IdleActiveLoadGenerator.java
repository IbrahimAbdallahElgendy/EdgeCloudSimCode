/*
 * Title:        EdgeCloudSim - Idle/Active Load Generator implementation
 * 
 * Description: 
 * IdleActiveLoadGenerator implements basic load generator model where the
 * mobile devices generate task in active period and waits in idle period.
 * Task interarrival time (load generation period), Idle and active periods
 * are defined in the configuration file.
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.task_generator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.math3.distribution.ExponentialDistribution;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.EdgeTask;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;

public class IdleActiveLoadGenerator extends LoadGeneratorModel{
	int taskTypeOfDevices[];
	//*Mine* this variable defiend by me to show number of tasks per each user 
	int numberOfEdgeTaskPerDevcie[];
	public IdleActiveLoadGenerator(int _numberOfMobileDevices, double _simulationTime, String _simScenario) {
		super(_numberOfMobileDevices, _simulationTime, _simScenario);
	}

	@Override
	public void initializeModel() {
		taskList = new ArrayList<EdgeTask>();		
		numberOfEdgeTaskPerDevcie=new int[numberOfMobileDevices];
		//exponential number generator for file input size, file output size and task length
		//*Mine* the length of this exponential number is [number of application][input size, file output size and task length] i.e. [4][3]
		ExponentialDistribution[][] expRngList = new ExponentialDistribution[SimSettings.getInstance().getTaskLookUpTable().length][3];

		//create random number generator (file input size, file output size and task length) for each app type
		for(int i=0; i<SimSettings.getInstance().getTaskLookUpTable().length; i++) {
			if(SimSettings.getInstance().getTaskLookUpTable()[i][0] ==0)//*Mine*  [0] usage percentage (%)
				continue;
			
			expRngList[i][0] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][5]);//*Mine* [5] avg data upload (KB)		    // 
			expRngList[i][1] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][6]);//*Mine* [6] avg data download (KB)
			expRngList[i][2] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][7]);//*Mine* [7] avg task length (MI)

		}
		
		
		//Each mobile device utilizes an app type (task type)
		//*Mine* taskTypeOfDevices can be 0 for AUGMENTED_REALITY, 1 for HEALTH_APP, 2 for HEAVY_COMP_APP and 3 for INFOTAINMENT_APP
		taskTypeOfDevices = new int[numberOfMobileDevices];
		for(int i=0; i<numberOfMobileDevices; i++) {
			int randomTaskType = -1;
			double taskTypeSelector = SimUtils.getRandomDoubleNumber(0,100);//Mine* This method generate a double random number betwee1 and 100
			double taskTypePercentage = 0;
			for (int j=0; j<SimSettings.getInstance().getTaskLookUpTable().length; j++) {
				taskTypePercentage += SimSettings.getInstance().getTaskLookUpTable()[j][0];//*Mine*  [0] usage percentage (%)
				if(taskTypeSelector <= taskTypePercentage){
					randomTaskType = j;
					break;
				}
			}
			if(randomTaskType == -1){
				SimLogger.printLine("Impossible is occured! no random task type!");
				continue;
			}
			
			taskTypeOfDevices[i] = randomTaskType;
			
			double poissonMean = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][2];//*Mine* [2] poisson mean (sec) 
			//*Mine* active period is the period of time that the mobile device can generate or offload task
			double activePeriod = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][3];//*Mine* [3] active period (sec)
			//*Mine* idle period is the period of time that the mobile device can work on another application or just idle 
			double idlePeriod = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][4];//*Mine* [4] idle period (sec)
			double activePeriodStartTime = SimUtils.getRandomDoubleNumber(
					SimSettings.CLIENT_ACTIVITY_START_TIME, 
					SimSettings.CLIENT_ACTIVITY_START_TIME + activePeriod);  //active period starts shortly after the simulation started (e.g. 10 seconds)
			double virtualTime = activePeriodStartTime;

			ExponentialDistribution rng = new ExponentialDistribution(poissonMean);
			//*Mine* Generate a set of EdgeTask for each mobile device where poissonMean is the period of time between two tasks  
			// Therefore, in the next loop, it generate random poissonMean value (interval) to be the start time for the each task
			// and the condition of loop (virtualTime < SS.getSimulationTime()) guarantee that the tasks start time doesn't exceed the Simulation Time
			
			while(virtualTime < simulationTime) {
				double interval = rng.sample();

				if(interval <= 0){
					SimLogger.printLine("Impossible is occured! interval is " + interval + " for device " + i + " time " + virtualTime);
					continue;
				}
				//SimLogger.printLine(virtualTime + " -> " + interval + " for device " + i + " time ");
				virtualTime += interval;
				
				// this if statement guarantee that the start time for each task (virtualTime) doesn't exceed the active period time 
				// which is the summation of activePeriodStartTime and activePeriod
				// if the start time exceed active period, then it add an idle period and go up in the start of the loop to check with the simulationTime
				if(virtualTime > activePeriodStartTime + activePeriod){
					activePeriodStartTime = activePeriodStartTime + activePeriod + idlePeriod;
					virtualTime = activePeriodStartTime;
					continue;
				}
				//*Mine* if the start time does not exceed the active period, then a new edge task is added to this mobile device with this value as a start time				
				taskList.add(new EdgeTask(i,randomTaskType, virtualTime, expRngList));
				numberOfEdgeTaskPerDevcie[i]++;
				//*Mine* EdgeTask(int mobileDeviceId, APP_TYPES taskType, double startTime, ExponentialDistribution[][] expRngList)
				//*Mine* expRngList==> data upload (KB),avg data download (KB), avg task length (MI)
			}
		}
	}

	@Override
	public int getTaskTypeOfDevice(int deviceId) {
		// TODO Auto-generated method stub
		return taskTypeOfDevices[deviceId];
	}

	//*Mine* this method is used to print the number of edge tasks for each mobile device
	public void printDeviceTasks(){
	    String FileLocation="E:\\HIT\\OneDrive - Computer and Information Technology (Menofia University)\\My Papers\\Git Code\\Other codes\\Cloud_EdgeSim_Code\\cloudsim-3.0.3\\scripts\\sample_app3\\config\\Files\\TaskswithDevice.txt"; 
		BufferedWriter writer=null;
		try {
			writer = new BufferedWriter(new FileWriter(FileLocation,true));
			for(int i=0; i<numberOfMobileDevices; i++) {
				
				//System.out.println("The Mobile Device"+i+" has "+numberOfEdgeTaskPerDevcie[i]+" Edge Taks");
				writer.append("The Mobile Device"+i+" has "+numberOfEdgeTaskPerDevcie[i]+" Edge Taks");	
				writer.newLine();
			}
			writer.newLine();
			writer.close();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
