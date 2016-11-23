package com.zzbest.review.util;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.zzbest.review.dao.ReviewAdviceDAO;
import com.zzbest.review.dao.impl.ReviewAdviceDAOImpl;
import com.zzbest.review.model.ReviewAdvice;

public class ReviewAdivceImportUtil {

	private static String SOURCE_FILE_PATH = "d:\\metadata.xlsx";

	private ReviewAdviceDAOImpl dao = null;

	private InputStream getFileInputStream(String _sourceFilePath) throws FileNotFoundException {
		File targetFile = new File(_sourceFilePath);
		if (!targetFile.exists()) {
			throw new FileNotFoundException(_sourceFilePath + "不存在");
		}

		if (targetFile.isDirectory()) {
			throw new FileNotFoundException(_sourceFilePath + "不是文件");
		}

		FileInputStream fis = new FileInputStream(targetFile);

		return fis;
	}

	private Workbook getWorkbook(InputStream is) {
		try {
			Workbook workBook = new XSSFWorkbook(is);
			return workBook;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public void importData(String _sourceFilePath) throws FileNotFoundException {
		Workbook workBook = getWorkbook(getFileInputStream(_sourceFilePath));

		if (workBook != null) {
			importFromWorkbook(workBook);
		}
	}

	private void importFromWorkbook(Workbook workBook) {
		int numberOfSheets = workBook.getNumberOfSheets();
		for (int i = 0; i < numberOfSheets; i++) {
			Sheet sheet = workBook.getSheetAt(i);

			if (sheet == null) {
				continue;
			}

			if (workBook.isSheetHidden(i)) {
				System.err.println(
						"---------------------页签：<" + sheet.getSheetName() + ">是隐藏Sheet，不导入数据。---------------------");
				continue;
			}

			if (existsHiddenColumns(sheet)) {
				System.err.println(
						"---------------------页签：<" + sheet.getSheetName() + ">前七列存在隐藏列，暂时不导入。---------------------");
				continue;
			}

			importFromSheet(sheet);
		}
	}

	private void importFromSheet(Sheet sheet) {
		int rows = sheet.getPhysicalNumberOfRows();
		if (rows <= 2) {
			return;
		}

		printSheetColumnHeaderInfo(sheet);

		for (int i = 2; i < rows; i++) {
			Row row = sheet.getRow(i);
			Cell materialType = row.getCell(0);

			Cell problemDesc = row.getCell(6);
			if (problemDesc == null) {
				problemDesc = row.getCell(5);
			}

			if ((materialType == null || materialType.toString() == null
					|| materialType.toString().trim().length() == 0)
					|| (problemDesc == null || problemDesc.toString() == null
							|| problemDesc.toString().trim().length() == 0)) {
				System.err.println("页签：<" + sheet.getSheetName() + ">数据有误，剩下的列暂时不导入。已导入：( " + (i - 2) + " )列。");
				break;
			}

			ReviewAdvice reviewAdvice = new ReviewAdvice();
			reviewAdvice.setMaterialType(materialType.toString());
			reviewAdvice.setProblemDesc(problemDesc.toString());
			reviewAdvice.setSource("外部导入");

			try {
				getReviewAdviceDAO().createNewReviewAdvice(reviewAdvice);
			} catch (PropertyVetoException e) {
				e.printStackTrace();
			}
		}
	}

	private void printSheetColumnHeaderInfo(Sheet sheet) {
		Row row = sheet.getRow(0);
		Row row1 = sheet.getRow(1);

		if (row != null && row != null) {
			System.out.println("---------------------页签<" + sheet.getSheetName() + ">列头信息-----------------------");
			int cellNum = row.getPhysicalNumberOfCells();
			String info = null;
			for (int i = 0; i < cellNum; i++) {
				info += row.getCell(i).toString();
			}
			System.out.println(info);

			cellNum = row1.getPhysicalNumberOfCells();
			info = null;
			for (int i = 0; i < cellNum; i++) {
				info += row1.getCell(i).toString();
			}
			System.out.println(info);
		} else {

		}
	}

	private boolean existsHiddenColumns(Sheet sheet) {
		for (int i = 0; i < 7; i++) {
			if (!sheet.isColumnHidden(i)) {
				continue;
			} else {
				return true;
			}
		}

		return false;
	}

	protected ReviewAdviceDAO getReviewAdviceDAO() throws PropertyVetoException {
		if (dao == null) {
			dao = new ReviewAdviceDAOImpl();
			dao.setCheckWriteOperations(false);

			com.mchange.v2.c3p0.ComboPooledDataSource dataSource = new ComboPooledDataSource();
			dataSource.setDriverClass("com.mysql.jdbc.Driver");
			dataSource.setUser("activiti");
			dataSource.setPassword("activiti");
			dataSource.setJdbcUrl("jdbc:mysql://192.168.1.14:3306/ematerialdb?useUnicode=true&characterEncoding=UTF-8");

			Configuration cfg = new Configuration();
			cfg.addAnnotatedClass(ReviewAdvice.class);
			cfg.getProperties().put(Environment.DATASOURCE, dataSource);
			@SuppressWarnings("deprecation")
			SessionFactory sf = cfg.buildSessionFactory();

			dao.setSessionFactory(sf);
		}

		return dao;
	}

	public static void main(String[] args) {
		try {
			new ReviewAdivceImportUtil().importData(SOURCE_FILE_PATH);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
