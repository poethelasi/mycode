package org.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

public class VirtualMachineLoadAgent {

	private static String agentJar = "D:/mvn/org/aspectj/aspectjweaver/1.8.7/aspectjweaver-1.8.7.jar";

	public static void main(String[] args) {
		if (args == null || args.length == 0 || args[0] == null || args[0].length() == 0) {
			System.out.println("请指定程序名称。");
			return;
		}

		try {
			Process proc = Runtime.getRuntime().exec("jps");
			BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));

			String processId = null;
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.contains(args[0])) {
					processId = line.split(" ")[0];
					break;
				}
			}

			VirtualMachine vm = VirtualMachine.attach(processId);
			vm.loadAgent(agentJar);

			vm.detach();

			/**
			 * <pre>
			 * 下面这样写是不对的，Agent.getInstrumentation()会报错。
			 *1) vm.loadAgent(agentJar)是在目标JVM中加载了agentJar，Agent是用目标JVM加载器进行加载的。
			 *2) 执行下面的代码，是在本地JVM中，该Agent是由本地JVM加载的。
			 *3) 从本地的JVM中想去获取目标JVM中Agent的Instrumetation实例，肯定会报错。
			 *4) 只能在目标JVM中执行下面代码。
			 *
			 * Class[] clazzes = Agent.getInstrumentation().getAllLoadedClasses();
			 * for (Class class1 : clazzes) {
			 *     System.out.println(class1.getCanonicalName());
			 * }
			 * </pre>
			 */
		} catch (AttachNotSupportedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (AgentLoadException e) {
			e.printStackTrace();
		} catch (AgentInitializationException e) {
			e.printStackTrace();
		}
	}
}
