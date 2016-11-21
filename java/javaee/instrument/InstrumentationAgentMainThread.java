package org.test;

import org.aspectj.weaver.loadtime.Agent;

public class InstrumentationAgentMainThread extends Thread {

	private boolean broken = true;

	@SuppressWarnings("rawtypes")
	public void run() {
		while (broken) {
			try {
				Thread.sleep(10000);
				System.out.println("111broken:" + broken);

				if (Agent.getInstrumentation() != null) {
					Class[] clazzes = Agent.getInstrumentation().getAllLoadedClasses();

					int index = 1;
					for (Class class1 : clazzes) {
						System.out.println("Loaded Classes(" + (index++) + ")  " + class1.getName());
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void setBroken(boolean broken) {
		this.broken = broken;
	}

	public static void main(String[] args) {
		InstrumentationAgentMainThread t = new InstrumentationAgentMainThread();
		t.start();
	}
}
