package org.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.MessageFormat;

public class ImportData {

	private final static String url = "jdbc:mysql://192.168.1.14:3306/ematerialdb?useUnicode=true&characterEncoding=UTF-8";
	private final static String username = "activiti";
	private final static String password = "activiti";

	private static Connection conn = null;

	public static Connection getConnection() {
		if (conn == null) {
			try {
				Class.forName("com.mysql.jdbc.Driver");
				conn = DriverManager.getConnection(url, username, password);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return conn;
	}

	private final static String logFilePath = "d:\\c3p0.log";

	private BufferedReader getLogFile() {
		File file = new File(logFilePath);
		try {
			BufferedReader isr = new BufferedReader(new FileReader(file));

			return isr;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return null;
	}

	public void saveLog() {
		BufferedReader fr = getLogFile();

		if (fr == null) {
			return;
		}

		String content = null;
		try {
			int i = 1;
			String title = null;
			String type = null;
			String caller = null;
			String connectionObj = null;
			while ((content = fr.readLine()) != null) {
				if (content.contains("logResourceStatus")
						|| content.contains("NumConnections")
						|| content.contains("logResourceThreadStatus")
						|| content.contains("ThreadCount")) {
					continue;
				}

				if (i == 1) {
					String fragements[] = content.split("INFO");
					title = fragements[0];
					type = fragements[1];
					i++;
				} else if (i == 2) {
					String fragements[] = content
							.split(", Object's hash code:");
					caller = fragements[0];
					connectionObj = fragements[1];

					executeInsertAction(title, type, caller, connectionObj);
					title = null;
					type = null;
					caller = null;
					connectionObj = null;
					i = 1;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			closeConnection();
		}
	}

	/**
	 * <pre>
	 * SELECT * FROM (
	 *      SELECT t.`connectionObj`,
	 *        SUM((CASE WHEN t.type='checkIn' THEN 1 ELSE 0 END)) checkInCount,
	 *        SUM((CASE WHEN t.type='checkOut' THEN 1 ELSE 0 END)) checkOutCount
	 *       FROM c3p0anlysis t GROUP BY t.`connectionObj`) 
	 *  m WHERE m.checkOutCount > m.checkInCount;
	 * </pre>
	 */
	/**
	 * <pre>
	 * SELECT p.`caller` 
	 *     FROM c3p0Anlysis p 
	 *     JOIN (SELECT t.`connectionObj`,MAX(t.logTime) lastCheckOutTime 
	 * 	FROM c3p0Anlysis t WHERE t.`type`='checkOut'
	 * 		AND t.`connectionObj` IN (SELECT m.`connectionObj` FROM (
	 * 		     SELECT t.`connectionObj`,
	 * 		       SUM((CASE WHEN t.type='checkIn' THEN 1 ELSE 0 END)) checkInCount,
	 * 		       SUM((CASE WHEN t.type='checkOut' THEN 1 ELSE 0 END)) checkOutCount
	 * 		      FROM c3p0anlysis t GROUP BY t.`connectionObj`) 
	 * 		 m WHERE m.checkOutCount > m.checkInCount) GROUP BY t.`connectionObj`) q 
	 *     ON p.`connectionObj` = q.connectionObj AND p.`logTime` = q.lastCheckOutTime;
	 * </pre>
	 */
	public static void closeConnection() {
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private void executeInsertAction(String title, String type, String caller,
			String connectionObj) {
		if (isBlank(title) || isBlank(type) || isBlank(caller)
				|| isBlank(connectionObj)) {
			System.out.println("---有数据为空，不执行数据库操作---");
			return;
		}

		if (type.contains("checkOutConnection")) {
			type = "checkOut";
		} else if (type.contains("checkInConnection")) {
			type = "checkIn";
		} else {
			System.err.println("---数据操作类型不正确，不执行数据库操作---");
			return;
		}

		String insertSql = "insert into c3p0anlysis(logTime,title,type,caller,connectionObj) values(''{0}'',''{1}'',''{2}'',''{3}'',''{4}'')";
		insertSql = MessageFormat.format(insertSql,
				new Object[] { title.trim().substring(0, 21),
						title.trim().substring(21), type.trim(), caller.trim(),
						connectionObj.trim() });

		PreparedStatement pstat = null;
		try {
			System.out.println(insertSql);
			pstat = getConnection().prepareStatement(insertSql);
			pstat.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println("---执行SQL错误----");
		} finally {
			if (pstat != null) {
				try {
					pstat.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private boolean isBlank(String value) {
		if (value == null)
			return true;

		if (value.trim().length() == 0)
			return true;

		return false;
	}

	public static void main(String[] args) {
		ImportData dataImport = new ImportData();
		dataImport.saveLog();
	}

}
