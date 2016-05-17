package edu.osu.sfal.util;

import static edu.osu.sfal.actors.SfpActor.FINISHED_CALCULATING_VAR_NAME;
import static edu.osu.sfal.actors.SfpActor.READY_TO_CALCULATE_VAR_NAME;

import edu.osu.lapis.Flags;
import edu.osu.lapis.LapisApi;
import edu.osu.lapis.util.Sleep;

public class Wheel {

	private static final String
	SIM_FUNCTION_NAME = "Wheel",
	COORDINATOR_ADDRESS = "http://localhost:8910",
	MY_NODE_ADDRESS = "http://localhost:8800";
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		LapisApi lapisApi = new LapisApi("node-test4", COORDINATOR_ADDRESS, MY_NODE_ADDRESS);
		
		lapisApi.publishReadOnly("SIMULATION_FUNCTION_NAME", SIM_FUNCTION_NAME);

		double[] readyToCalculate = Flags.getFlag(false);
		lapisApi.publish(READY_TO_CALCULATE_VAR_NAME, readyToCalculate);

		double[] finishedCalculating = Flags.getFlag(false);
		lapisApi.publishReadOnly(FINISHED_CALCULATING_VAR_NAME, finishedCalculating);
		
		//input parameter
		int[] speed=new int[1];
		speed[0]=0;
		lapisApi.publish("speed", speed);
		
		
		//output parameter
		int[] roadSpeed=new int[1];
		roadSpeed[0]=0;
		lapisApi.publishReadOnly("roadSpeed", roadSpeed);
		
		lapisApi.ready();

		while (true) {
			while (!Flags.evaluateFlagValue(readyToCalculate)) {
				Sleep.sleep(200);
			}
			
			Flags.setFlagFalse(finishedCalculating);
			Flags.setFlagFalse(readyToCalculate);
			
			System.out.println("intput speed:"+speed[0]);
			roadSpeed[0]=1*speed[0]-2;
			System.out.println("output on road speed:"+roadSpeed[0]);	
			Flags.setFlagTrue(finishedCalculating);
		}
	}

}
