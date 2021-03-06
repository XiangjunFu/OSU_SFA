package edu.osu.sfal.util;

import static edu.osu.sfal.actors.SfpActor.FINISHED_CALCULATING_VAR_NAME;
import static edu.osu.sfal.actors.SfpActor.READY_TO_CALCULATE_VAR_NAME;

import edu.osu.lapis.Flags;
import edu.osu.lapis.LapisApi;
import edu.osu.lapis.util.Sleep;

public class Throttle {
	private static final String
	SIM_FUNCTION_NAME = "Throttle",
	COORDINATOR_ADDRESS = "http://localhost:8910",
	MY_NODE_ADDRESS = "http://localhost:8777";
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		LapisApi lapisApi = new LapisApi("node-test1", COORDINATOR_ADDRESS, MY_NODE_ADDRESS);
		
		lapisApi.publishReadOnly("SIMULATION_FUNCTION_NAME", SIM_FUNCTION_NAME);

		double[] readyToCalculate = Flags.getFlag(false);
		lapisApi.publish(READY_TO_CALCULATE_VAR_NAME, readyToCalculate);

		double[] finishedCalculating = Flags.getFlag(false);
		lapisApi.publishReadOnly(FINISHED_CALCULATING_VAR_NAME, finishedCalculating);
		
		//input parameter
		int[] footPressure=new int[1];
		footPressure[0]=0;
		lapisApi.publish("footPressure", footPressure);
		
		
		//output parameter
		int[] RPM=new int[1];
		RPM[0]=0;
		lapisApi.publishReadOnly("RPM", RPM);
		
		lapisApi.ready();

		while (true) {
			while (!Flags.evaluateFlagValue(readyToCalculate)) {
				Sleep.sleep(200);
			}
			
			Flags.setFlagFalse(finishedCalculating);
			Flags.setFlagFalse(readyToCalculate);
			
			System.out.println("intput foot pressure:"+footPressure[0]);
			RPM[0]=57*footPressure[0]+800;
			System.out.println("output RPM:"+RPM[0]);	
			Flags.setFlagTrue(finishedCalculating);
		}
	}
}
