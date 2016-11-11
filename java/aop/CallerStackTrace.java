package com.mchange.v2.resourcepool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallerStackTrace {

	private final static Logger log = LoggerFactory
			.getLogger(CallerStackTrace.class);

	private final static int MAX_DEPTH = 256;

	private String[] ignoreCallerClasses = null;

	public CallerStackTrace() {
		this.ignoreCallerClasses = new String[] { CallerStackTrace.class
				.getSimpleName() };
	}

	public CallerStackTrace(String[] ignoreCallerClasses) {
		if (ignoreCallerClasses == null || ignoreCallerClasses.length == 0) {
			this.ignoreCallerClasses = new String[] { CallerStackTrace.class
					.getSimpleName() };
		} else {
			this.ignoreCallerClasses = new String[ignoreCallerClasses.length + 1];
			this.ignoreCallerClasses[0] = CallerStackTrace.class
					.getSimpleName();
			System.arraycopy(ignoreCallerClasses, 0, this.ignoreCallerClasses,
					1, ignoreCallerClasses.length);
		}
	}

	public List<StackTraceElement> getStackTrace() {
		StackTraceElement[] steArray = Thread.currentThread().getStackTrace();
		List<StackTraceElement> steList = new ArrayList<StackTraceElement>(
				Arrays.asList(steArray));

		steList.remove(0);

		List<StackTraceElement> newSteList = new ArrayList<StackTraceElement>();
		for (StackTraceElement ste : steList) {
			String callerClassName = ste.getFileName();

			if (callerClassName != null && callerClassName.trim().length() > 0) {
				boolean founded = false;
				for (String className : ignoreCallerClasses) {
					if (className == null) {
						continue;
					}

					if (callerClassName.startsWith(className)) {
						founded = true;
						break;
					}
				}

				if (!founded) {
					newSteList.add(ste);
				}
			}
		}

		return newSteList;
	}

	public String getStackTraceInfo() {
		return getStackTraceInfo(MAX_DEPTH);
	}

	public String getStackTraceInfo(int depth) {
		if (depth < 0 || depth > MAX_DEPTH) {
			depth = MAX_DEPTH;
		}

		List<StackTraceElement> steList = getStackTrace();
		depth = Math.min(steList.size(), depth);

		StringBuilder strBuilder = new StringBuilder();
		for (int i = 0; i < depth; i++) {
			strBuilder.append("\tat " + steList.get(i) + "\n");
		}

		return strBuilder.toString();
	}

	public void logStackTraceInfo(int depth) {
		log.info(getStackTraceInfo(depth));
	}

	public void logStackTraceInfo() {
		log.info(getStackTraceInfo(MAX_DEPTH));
	}

	public void logOneTraceInfo(String[] packagePrefixs,
			String[] classNameFragments) {
		String content = getOneTraceInfo(packagePrefixs, classNameFragments);

		if (content != null) {
			log.info(content);
		}
	}

	public String getOneTraceInfo(String[] packagePrefixs,
			String[] classNameFragments) {
		List<StackTraceElement> steList = getStackTrace();

		String content = null;
		for (int i = 0; i < steList.size(); i++) {
			StackTraceElement ste = steList.get(i);

			String fullClassName = ste.getClassName();
			String simpleClassName = ste.getFileName();

			if (assertPackagePrefix(fullClassName, packagePrefixs)
					&& assertClassName(simpleClassName, classNameFragments)) {
				content = ste.getClassName() + "." + ste.getMethodName();
				break;
			}
		}

		return content;
	}

	private boolean assertPackagePrefix(String fullClassName,
			String[] packagePrefixs) {
		if (packagePrefixs == null || packagePrefixs.length == 0) {
			return true;
		}

		if (fullClassName == null || fullClassName.trim().length() == 0) {
			return false;
		}

		for (String prefix : packagePrefixs) {
			if (fullClassName.startsWith(prefix)) {
				return true;
			}
		}

		return false;
	}

	private boolean assertClassName(String simpleClassName,
			String[] classNameFragments) {
		if (classNameFragments == null || classNameFragments.length == 0) {
			return true;
		}

		if (simpleClassName == null || simpleClassName.trim().length() == 0) {
			return false;
		}

		for (String fragment : classNameFragments) {
			if (simpleClassName.equalsIgnoreCase(fragment)) {
				return true;
			}
		}

		return false;
	}

}
