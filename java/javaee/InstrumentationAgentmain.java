package org.test;

public class MyThread extends Thread {

	private boolean broken = true;

	public void run() {
		while (broken) {
			try {
				Thread.sleep(10000);
				System.out.println("broken:" + broken);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void setBroken(boolean broken) {
		this.broken = broken;
	}

	public static void main(String[] args) {
		MyThread t = new MyThread();
		t.start();
	}
}

package org.test;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

public class VirtualMachineTest {

	@SuppressWarnings("rawtypes")
	public static void main(String[] args) {
		try {
			VirtualMachine vm = VirtualMachine.attach("107048");
			Properties props = vm.getSystemProperties();
			Enumeration en = props.propertyNames();
			while (en.hasMoreElements()) {
				Object key = en.nextElement();
				System.out.println(key+":" + props.get(key));
			}
		} catch (AttachNotSupportedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
