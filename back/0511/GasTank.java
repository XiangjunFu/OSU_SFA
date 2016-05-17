package edu.osu.sfal.util;

import static edu.osu.sfal.actors.SfpActor.FINISHED_CALCULATING_VAR_NAME;
import static edu.osu.sfal.actors.SfpActor.READY_TO_CALCULATE_VAR_NAME;

import edu.osu.lapis.Flags;
import edu.osu.lapis.LapisApi;
import edu.osu.lapis.util.Sleep;

public class GasTank {
	
		private static final String
		SIM_FUNCTION_NAME = "GasTank",
		COORDINATOR_ADDRESS = "http://localhost:8910",
		MY_NODE_ADDRESS = "http://localhost:8779";
		
		public static void main(String[] args) {
			// TODO Auto-generated method stub
			LapisApi lapisApi = new LapisApi("node-test3", COORDINATOR_ADDRESS, MY_NODE_ADDRESS);
			
			lapisApi.publishReadOnly("SIMULATION_FUNCTION_NAME", SIM_FUNCTION_NAME);

			double[] readyToCalculate = Flags.getFlag(false);
			lapisApi.publish(READY_TO_CALCULATE_VAR_NAME, readyToCalculate);

			double[] finishedCalculating = Flags.getFlag(false);
			lapisApi.publishReadOnly(FINISHED_CALCULATING_VAR_NAME, finishedCalculating);
			
			//input parameter
			int[] RPM=new int[1];
			RPM[0]=0;
			lapisApi.publish("RPM", RPM);
			
			
			//output parameter
			int[] gasInjection=new int[1];
			gasInjection[0]=0;
			lapisApi.publishReadOnly("gasInjection", gasInjection);
			
			lapisApi.ready();

			while (true) {
				while (!Flags.evaluateFlagValue(readyToCalculate)) {
					Sleep.sleep(200);
				}
				
				Flags.setFlagFalse(finishedCalculating);
				Flags.setFlagFalse(readyToCalculate);
				
				System.out.println("intput RPM:"+RPM[0]);
				gasInjection[0]=10*RPM[0];
				System.out.println("output gas injection:"+gasInjection[0]);	
				Flags.setFlagTrue(finishedCalculating);
			}
		}

}
