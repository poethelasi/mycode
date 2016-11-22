package com.zzbest.online.review.ui.view.base;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.POIXMLDocumentPart;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumData;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumVal;
import org.openxmlformats.schemas.drawingml.x2006.chart.ChartSpaceDocument;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGridCol;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblLayoutType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblLayoutType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;

import com.vaadin.ui.Button;
import com.zzbest.online.review.model.EngineerReviewAdvice;
import com.zzbest.online.review.model.TechnicalDifficulty;
import com.zzbest.platform.util.StringUtils;

/**
 * 将工程师评审意见导出成Word文档.
 * 
 * 使用该类的功能模块：<br/>
 * 1）单板可靠性评审;<br/>
 * 
 * @author jovi.chou
 * @since 2015年10月23日
 */
public class PCBAReliabilityEngineerReviewAdviceWordExporter extends
		AbstractEngineerReviewAdviceWordExporter {

	/**
	 * <pre>
	 *                 致命   严重  一般   提示<br/>
	 * 厂家优选 .....3..........3............3.........5....... <br/>
	 * 物料优选 .....3..........4............4.........8....<br/>
	 * </pre>
	 * 
	 */
	private Map<String, Map<String, Integer>> barCharData = new HashMap<String, Map<String, Integer>>();

	/**
	 * <pre>
	 * ..................数量.....................
	 * 物料选型............3..............................
	 * 可制造性设计............3..............................
	 * 可靠性工程............4.......................
	 * 电路应用..........5.........................
	 * </pre>
	 */
	private Map<String, Integer> pieCharData = new HashMap<String, Integer>();

	private static final String pie_char_pt1 = "物料选型";
	private static final String pie_char_pt2 = "可制造性设计";
	private static final String pie_char_pt3 = "可靠性工程";
	private static final String pie_char_pt4 = "电路应用";

	public PCBAReliabilityEngineerReviewAdviceWordExporter(
			Map<Long, EngineerReviewAdvice> adviceMap, Button btnDownload,
			Map<String, Object> properties) {
		super(adviceMap, btnDownload, properties);
	}

	/**
	 * 导出Word格式的评审报告.
	 */
	public void exportWordReport() {

		// 输出单板基础信息
		exportPCBABasicInfo(wordDoc);

		// 以表格形式导出评审问题
		exportReviewAdviceTable(wordDoc);

		// 导出工艺难点
		exportTechnicalDifficultity(wordDoc);

		// 给评审问题编号
		organizeAdviceNumber(wordDoc);

		// 清除评审问题表格插入位置标志
		clearPositionFlags(wordDoc);

		// 更新按维度统计的评审问题图片数据
		updateChart(wordDoc);

		// 替换变量
		replaceVariableValues(wordDoc);

		// 将报告绑定到下载按钮上
		downloadWordReport();
	}

	/**
	 * 以表格的形式导出评审意见.
	 */
	private void exportReviewAdviceTable(XWPFDocument _docomument) {
		if (adviceMap == null || adviceMap.size() == 0) {
			return;
		}

		Iterator<Long> iterator = adviceMap.keySet().iterator();
		while (iterator.hasNext()) {
			Long key = iterator.next();

			EngineerReviewAdvice advice = adviceMap.get(key);

			String dimension = advice.getReviewDimension();

			XWPFTable _table = null;
			if (ERAConst.DIMENION_REV_1.equals(dimension)
					|| ERAConst.DIMENION_REV_2.equals(dimension)
					|| ERAConst.DIMENION_REV_3.equals(dimension)
					|| ERAConst.DIMENION_REV_4.equals(dimension)
					|| ERAConst.DIMENION_REV_5.equals(dimension)
					|| ERAConst.DIMENION_REV_9.equals(dimension)) {
				_table = createTable(_docomument, dimension, 6, 4);
			} else {
				_table = createTable(_docomument, dimension, 5, 4);
			}

			try {
				fillReviewAdviceData(_table, advice);
			} catch (Exception e) {
				e.printStackTrace();
			}

			incrementCountOfAdvice(dimension);

			countPieCharAndPieCharData(dimension, advice.getLevel());
		}
	}

	private void countPieCharAndPieCharData(String dimension, String level) {
		if (StringUtils.isEmptyOrNullObject(dimension)
				|| StringUtils.isEmptyOrNullObject(level)) {
			return;
		}

		// pieChart
		if (ERAConst.DIMENION_REV_1.equals(dimension)
				|| ERAConst.DIMENION_REV_2.equals(dimension)
				|| ERAConst.DIMENION_REV_3.equals(dimension)) { // 物料选型：厂家优选、物料优选、物料归一化
			Integer value = pieCharData.get(pie_char_pt1);
			if (value == null) {
				pieCharData.put(pie_char_pt1, new Integer(1));
			} else {
				pieCharData.put(pie_char_pt1, value + 1);
			}
		} else if (ERAConst.DIMENION_REV_14.equals(dimension)
				|| ERAConst.DIMENION_REV_13.equals(dimension)
				|| ERAConst.DIMENION_REV_12.equals(dimension)
				|| ERAConst.DIMENION_REV_11.equals(dimension)
				|| ERAConst.DIMENION_REV_10.equals(dimension)) { // 可制造性设计：封装库设计、PCB工艺设计、器件工艺应用、互连可靠性、工艺路线设计；
			Integer value = pieCharData.get(pie_char_pt2);
			if (value == null) {
				pieCharData.put(pie_char_pt2, new Integer(1));
			} else {
				pieCharData.put(pie_char_pt2, value + 1);
			}
		} else if (ERAConst.DIMENION_REV_6.equals(dimension)
				|| ERAConst.DIMENION_REV_7.equals(dimension)
				|| ERAConst.DIMENION_REV_8.equals(dimension)) { // 可靠性工程：安规/EMC/防护设计、热设计、SI/PI设计；
			Integer value = pieCharData.get(pie_char_pt3);
			if (value == null) {
				pieCharData.put(pie_char_pt3, new Integer(1));
			} else {
				pieCharData.put(pie_char_pt3, value + 1);
			}
		} else if (ERAConst.DIMENION_REV_4.equals(dimension)
				|| ERAConst.DIMENION_REV_5.equals(dimension)
				|| ERAConst.DIMENION_REV_9.equals(dimension)) {// 电路应用：器件电路应用,环境适应性,
																// 降额设计；
			Integer value = pieCharData.get(pie_char_pt4);
			if (value == null) {
				pieCharData.put(pie_char_pt4, new Integer(1));
			} else {
				pieCharData.put(pie_char_pt4, value + 1);
			}
		}

		// BarChar
		Map<String, Integer> dimensionNum = barCharData.get(level);
		if (dimensionNum == null) {
			dimensionNum = new HashMap<String, Integer>();
			dimensionNum.put(dimension, new Integer(1));
		} else {
			Integer value = dimensionNum.get(dimension);
			if (value == null) {
				dimensionNum.put(dimension, new Integer(1));
			} else {
				dimensionNum.put(dimension, value + 1);
			}
		}

		barCharData.put(level, dimensionNum);
	}

	/**
	 * 导出工艺难点.
	 * 
	 * @param _docomument
	 */
	@SuppressWarnings("unchecked")
	public void exportTechnicalDifficultity(XWPFDocument _docomument) {
		if (properties.get("techinicaList") != null) {
			List<TechnicalDifficulty> list = (List<TechnicalDifficulty>) properties
					.get("techinicaList");
			XWPFTable _table = createTable(_docomument, ERAConst.GYNDFX,
					list.size() + 1, 3);
			try {
				fillTableData(_table, list);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void fillTableData(XWPFTable table, List<TechnicalDifficulty> list) {
		CTTblWidth cTTblWidth = table.getCTTbl().addNewTblPr().addNewTblW();
		cTTblWidth.setType(STTblWidth.DXA);
		cTTblWidth.setW(new BigInteger("9505"));

		// 1）第一行
		XWPFTableRow row = table.getRow(0);
		row.setHeight(60);

		// 第一行 第一列
		XWPFTableCell cell = row.getCell(0);
		CTTcPr cTTcPr = cell.getCTTc().addNewTcPr();
		CTTblWidth tblWidth = cTTcPr.addNewTcW();
		tblWidth.setType(STTblWidth.DXA);
		tblWidth.setW(new BigInteger("1659"));

		CTShd cTShd = cTTcPr.addNewShd();
		cTShd.setFill("FFFF00");
		cTShd.setColor("auto");

		XWPFParagraph para = cell.addParagraph();
		XWPFRun run = para.createRun();
		run.setText("工艺难点：");
		cell.removeParagraph(0);

		// 第一行 第二列
		cell = row.getCell(1);
		tblWidth = cell.getCTTc().addNewTcPr().addNewTcW();
		tblWidth.setType(STTblWidth.DXA);
		tblWidth.setW(new BigInteger("2489"));

		CTShd cTShd1 = cell.getCTTc().addNewTcPr().addNewShd();
		cTShd1.setFill("FFFF00");
		cTShd1.setColor("auto");

		para = cell.addParagraph();
		run = para.createRun();
		run.setText("潜在风险描述：");
		cell.removeParagraph(0);

		// 第一行 第三列
		cell = row.getCell(2);
		tblWidth = cell.getCTTc().addNewTcPr().addNewTcW();
		tblWidth.setType(STTblWidth.DXA);
		tblWidth.setW(new BigInteger("4148"));

		CTShd cTShd2 = cell.getCTTc().addNewTcPr().addNewShd();
		cTShd2.setFill("FFFF00");
		cTShd1.setColor("auto");

		para = cell.addParagraph();
		run = para.createRun();
		cell.removeParagraph(0);
		run.setText("改进建议：");
		addSpace(run, 2);

		for (int i = 0; i < list.size(); i++) {
			XWPFTableRow row1 = table.getRow(i + 1);
			XWPFTableCell cell1 = row1.getCell(0);
			para = cell1.addParagraph();
			cell1.removeParagraph(0);
			formatPrint(para, cell1, list.get(i).getDifficultityName());

			XWPFTableCell cell2 = row1.getCell(1);
			para = cell2.addParagraph();
			cell2.removeParagraph(0);
			formatPrint(para, cell2, list.get(i).getDifficultyDescription());

			XWPFTableCell cell3 = row1.getCell(2);
			para = cell3.addParagraph();
			cell3.removeParagraph(0);
			formatPrint(para, cell3, list.get(i).getImprovementAdvice());
		}

	}

	private void fillReviewAdviceData(XWPFTable table,
			EngineerReviewAdvice reviewAdvice) throws Exception {
		CTTblPr cTTblPr = table.getCTTbl().getTblPr();
		if (cTTblPr == null) {
			cTTblPr = table.getCTTbl().addNewTblPr();
		}
		CTTblWidth cTTblWidth = cTTblPr.getTblW();
		if (cTTblWidth == null) {
			cTTblWidth = cTTblPr.addNewTblW();
		}
		cTTblWidth.setType(STTblWidth.DXA);
		cTTblWidth.setW(new BigInteger("8505"));

		CTTblGrid cTTblGrid = table.getCTTbl().getTblGrid();
		if (cTTblGrid == null) {
			cTTblGrid = table.getCTTbl().addNewTblGrid();
		}
		CTTblGridCol cTTblGridCol1 = cTTblGrid.addNewGridCol();
		CTTblGridCol cTTblGridCol2 = cTTblGrid.addNewGridCol();
		CTTblGridCol cTTblGridCol3 = cTTblGrid.addNewGridCol();
		CTTblGridCol cTTblGridCol4 = cTTblGrid.addNewGridCol();

		cTTblGridCol1.setW(new BigInteger("1319"));
		cTTblGridCol2.setW(new BigInteger("1830"));
		cTTblGridCol3.setW(new BigInteger("2412"));
		cTTblGridCol4.setW(new BigInteger("2944"));

		// 固定表格大小，不能根据文字内容自动调整表格宽带
		CTTblLayoutType cTTblLayoutType = cTTblPr.getTblLayout();
		if (cTTblLayoutType == null) {
			cTTblLayoutType = cTTblPr.addNewTblLayout();
		}

		cTTblLayoutType.setType(STTblLayoutType.FIXED);

		int rowIndex = 0;

		// 1）第一行
		XWPFTableRow row = table.getRow(rowIndex++);

		// 第一行 --> 第一列
		XWPFTableCell cell = row.getCell(0);
		CTTblWidth tblWidth = cell.getCTTc().addNewTcPr().addNewTcW();
		tblWidth.setType(STTblWidth.DXA);
		tblWidth.setW(new BigInteger("1244"));

		XWPFParagraph para = cell.addParagraph();
		XWPFRun run = para.createRun();
		run.setText("问题编号：");
		cell.removeParagraph(0);

		// 第一行 --> 第二列
		cell = row.getCell(1);
		tblWidth = cell.getCTTc().addNewTcPr().addNewTcW();
		tblWidth.setType(STTblWidth.DXA);
		tblWidth.setW(new BigInteger("1843"));

		para = cell.addParagraph();
		cell.removeParagraph(0);

		// 第一行 --> 第三列
		cell = row.getCell(2);
		tblWidth = cell.getCTTc().addNewTcPr().addNewTcW();
		tblWidth.setType(STTblWidth.DXA);
		tblWidth.setW(new BigInteger("2426"));

		para = cell.addParagraph();
		run = para.createRun();
		cell.removeParagraph(0);
		run.setText("问题重要等级：");
		addSpace(run, 1);

		// 第一行 --> 第四列
		cell = row.getCell(3);
		tblWidth = cell.getCTTc().addNewTcPr().addNewTcW();
		tblWidth.setType(STTblWidth.DXA);
		tblWidth.setW(new BigInteger("2963"));

		cell.removeParagraph(0);
		para = cell.addParagraph();
		run = para.createRun();
		run.setFontFamily("宋体");
		run.setText(getSeriousLevel(reviewAdvice.getLevel()));

		// 2）如果有物料类别，则新增一行
		String dimension = reviewAdvice.getReviewDimension();
		if (ERAConst.DIMENION_REV_1.equals(dimension)
				|| ERAConst.DIMENION_REV_2.equals(dimension)
				|| ERAConst.DIMENION_REV_3.equals(dimension)
				|| ERAConst.DIMENION_REV_4.equals(dimension)
				|| ERAConst.DIMENION_REV_5.equals(dimension)
				|| ERAConst.DIMENION_REV_9.equals(dimension)) {
			row = table.getRow(rowIndex++);

			cell = row.getCell(0);
			para = cell.addParagraph();
			cell.removeParagraph(0);
			run = para.createRun();

			if (reviewAdvice.getReviewDimension().equals(
					ERAConst.DIMENION_REV_9)) {
				run.setText("物料/电路：");
			} else {
				run.setText("物料小类：");
			}

			cell = row.getCell(1);
			para = cell.addParagraph();
			cell.removeParagraph(0);
			String categoryName = reviewAdvice.getMaterialType() == null ? "【未选择物料类别】"
					: reviewAdvice.getMaterialType().getCategoryName();
			para.createRun().setText(categoryName);

			CTTcPr cTTcPr = cell.getCTTc().addNewTcPr();
			cTTcPr.addNewGridSpan();
			cTTcPr.getGridSpan().setVal(BigInteger.valueOf(3L));

			XWPFTableCell removedCell = row.getCell(2);
			removedCell.getCTTc().newCursor().removeXml();
			row.removeCell(2);
			removedCell = row.getCell(2);
			removedCell.getCTTc().newCursor().removeXml();
			row.removeCell(2);
		}

		// 3）新增一行
		row = table.getRow(rowIndex++);

		cell = row.getCell(0);
		tblWidth = cell.getCTTc().addNewTcPr().addNewTcW();
		tblWidth.setType(STTblWidth.DXA);
		tblWidth.setW(new BigInteger("1244"));

		para = cell.addParagraph();
		cell.removeParagraph(0);
		run = para.createRun();
		run.setText("问题描述：");

		cell = row.getCell(1);
		para = cell.addParagraph();
		cell.removeParagraph(0);
		formatPrint(para, cell, reviewAdvice.getProblemDesc());
		CTTcPr cTTcPr = cell.getCTTc().addNewTcPr();
		cTTcPr.addNewGridSpan();
		cTTcPr.getGridSpan().setVal(BigInteger.valueOf(3L));

		XWPFTableCell removedCell = row.getCell(2);
		removedCell.getCTTc().newCursor().removeXml();
		row.removeCell(2);
		removedCell = row.getCell(2);
		removedCell.getCTTc().newCursor().removeXml();
		row.removeCell(2);

		// 4）新增一行
		row = table.getRow(rowIndex++);

		cell = row.getCell(0);
		tblWidth = cell.getCTTc().addNewTcPr().addNewTcW();
		tblWidth.setType(STTblWidth.DXA);
		tblWidth.setW(new BigInteger("1244"));

		para = cell.addParagraph();
		cell.removeParagraph(0);
		run = para.createRun();
		run.setText("问题分析：");

		cell = row.getCell(1);
		para = cell.addParagraph();
		cell.removeParagraph(0);
		formatPrint(para, cell, reviewAdvice.getProblemAnalysis());
		cTTcPr = cell.getCTTc().addNewTcPr();
		cTTcPr.addNewGridSpan();
		cTTcPr.getGridSpan().setVal(BigInteger.valueOf(3L));

		removedCell = row.getCell(2);
		removedCell.getCTTc().newCursor().removeXml();
		row.removeCell(2);
		removedCell = row.getCell(2);
		removedCell.getCTTc().newCursor().removeXml();
		row.removeCell(2);

		// 5）新增一行
		row = table.getRow(rowIndex++);

		cell = row.getCell(0);
		tblWidth = cell.getCTTc().addNewTcPr().addNewTcW();
		tblWidth.setType(STTblWidth.DXA);
		tblWidth.setW(new BigInteger("1244"));

		para = cell.addParagraph();
		cell.removeParagraph(0);
		run = para.createRun();
		run.setText("解决措施：");

		cell = row.getCell(1);
		para = cell.addParagraph();
		cell.removeParagraph(0);
		formatPrint(para, cell, reviewAdvice.getProblemAnswer());
		cTTcPr = cell.getCTTc().addNewTcPr();
		cTTcPr.addNewGridSpan();
		cTTcPr.getGridSpan().setVal(BigInteger.valueOf(3L));

		removedCell = row.getCell(2);
		removedCell.getCTTc().newCursor().removeXml();
		row.removeCell(2);
		removedCell = row.getCell(2);
		removedCell.getCTTc().newCursor().removeXml();
		row.removeCell(2);

		// 6）新增一行
		row = table.getRow(rowIndex++);

		cell = row.getCell(0);
		para = cell.addParagraph();
		run = para.createRun();
		cell.removeParagraph(0);
		run.setText("问题标识：");

		cell = row.getCell(1);
		para = cell.addParagraph();
		cell.removeParagraph(0);
		para.setAlignment(ParagraphAlignment.CENTER);

		cTTcPr = cell.getCTTc().addNewTcPr();
		cTTcPr.addNewGridSpan();
		cTTcPr.getGridSpan().setVal(BigInteger.valueOf(3L));

		removedCell = row.getCell(2);
		removedCell.getCTTc().newCursor().removeXml();
		row.removeCell(2);
		removedCell = row.getCell(2);
		removedCell.getCTTc().newCursor().removeXml();
		row.removeCell(2);

		byte[] imageContent1 = reviewAdvice.getImage1();
		byte[] imageContent2 = reviewAdvice.getImage2();
		byte[] imageContent3 = reviewAdvice.getImage3();

		if (imageContent1 != null && imageContent1.length > 0) {
			run = para.createRun();

			String rid = wordDoc.addPictureData(imageContent1,
					org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_JPEG);
			addPicture(wordDoc, rid, imageIndex++, 280, 160, run);
			// resizePicture(rid, run, imageContent1);
		}

		if (imageContent2 != null && imageContent2.length > 0) {
			para = cell.addParagraph();
			para.setAlignment(ParagraphAlignment.CENTER);
			run = para.createRun();

			String rid = wordDoc.addPictureData(imageContent2,
					org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_JPEG);
			addPicture(wordDoc, rid, imageIndex++, 280, 160, run);
			// resizePicture(rid, run, imageContent2);
		}

		if (imageContent3 != null && imageContent3.length > 0) {
			para = cell.addParagraph();
			para.setAlignment(ParagraphAlignment.CENTER);
			run = para.createRun();

			String rid = wordDoc.addPictureData(imageContent3,
					org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_JPEG);
			addPicture(wordDoc, rid, imageIndex++, 280, 160, run);
			// resizePicture(rid, run, imageContent3);
		}
	}

	/**
	 * 获取各维度评审问题的插入位置点.
	 * 
	 * @param paragraphs
	 */
	protected void getTableInsertionPositions(List<XWPFParagraph> paragraphs) {
		if (paragraphs == null || paragraphs.size() <= 0) {
			return;
		}

		for (XWPFParagraph parag : paragraphs) {
			if (parag == null) {
				continue;
			}

			if (parag.getText().indexOf(
					getVariableFormat(ERAConst.DIMENION_REV_1)) > -1) {
				insertionPositions.put(ERAConst.DIMENION_REV_1, parag);
			} else if (parag.getText().indexOf(
					getVariableFormat(ERAConst.DIMENION_REV_2)) > -1) {
				insertionPositions.put(ERAConst.DIMENION_REV_2, parag);
			} else if (parag.getText().indexOf(
					getVariableFormat(ERAConst.DIMENION_REV_3)) > -1) {
				insertionPositions.put(ERAConst.DIMENION_REV_3, parag);
			} else if (parag.getText().indexOf(
					getVariableFormat(ERAConst.DIMENION_REV_4)) > -1) {
				insertionPositions.put(ERAConst.DIMENION_REV_4, parag);
			} else if (parag.getText().indexOf(
					getVariableFormat(ERAConst.DIMENION_REV_5)) > -1) {
				insertionPositions.put(ERAConst.DIMENION_REV_5, parag);
			} else if (parag.getText().indexOf(
					getVariableFormat(ERAConst.DIMENION_REV_6)) > -1) {
				insertionPositions.put(ERAConst.DIMENION_REV_6, parag);
			} else if (parag.getText().indexOf(
					getVariableFormat(ERAConst.DIMENION_REV_7)) > -1) {
				insertionPositions.put(ERAConst.DIMENION_REV_7, parag);
			} else if (parag.getText().indexOf(
					getVariableFormat(ERAConst.DIMENION_REV_8)) > -1) {
				insertionPositions.put(ERAConst.DIMENION_REV_8, parag);
			} else if (parag.getText().indexOf(
					getVariableFormat(ERAConst.DIMENION_REV_9)) > -1) {
				insertionPositions.put(ERAConst.DIMENION_REV_9, parag);
			} else if (parag.getText().indexOf(
					getVariableFormat(ERAConst.DIMENION_REV_10)) > -1) {
				insertionPositions.put(ERAConst.DIMENION_REV_10, parag);
			} else if (parag.getText().indexOf(
					getVariableFormat(ERAConst.DIMENION_REV_11)) > -1) {
				insertionPositions.put(ERAConst.DIMENION_REV_11, parag);
			} else if (parag.getText().indexOf(
					getVariableFormat(ERAConst.DIMENION_REV_12)) > -1) {
				insertionPositions.put(ERAConst.DIMENION_REV_12, parag);
			} else if (parag.getText().indexOf(
					getVariableFormat(ERAConst.DIMENION_REV_13)) > -1) {
				insertionPositions.put(ERAConst.DIMENION_REV_13, parag);
			} else if (parag.getText().indexOf(
					getVariableFormat(ERAConst.DIMENION_REV_14)) > -1) {
				insertionPositions.put(ERAConst.DIMENION_REV_14, parag);
			} else if (parag.getText().indexOf(
					getVariableFormat(ERAConst.GYNDFX)) > -1) {
				insertionPositions.put(ERAConst.GYNDFX, parag);
			}
		}
	}

	@SuppressWarnings("deprecation")
	protected void updateChartData(CTNumData numData) {
		CTNumVal[] numVal = numData.getPtArray();

		for (int i = 0; i < 14; i++) {
			numVal[i].setV("0");
		}

		if (dimensionCounts.size() < 1) {
			return;
		}

		Iterator<String> itr = dimensionCounts.keySet().iterator();
		while (itr.hasNext()) {
			String dimension = itr.next();

			if (ERAConst.DIMENION_REV_1.equals(dimension)) {
				numVal[0].setV(String.valueOf(dimensionCounts.get(dimension)));

			} else if (ERAConst.DIMENION_REV_2.equals(dimension)) {
				numVal[1].setV(String.valueOf(dimensionCounts.get(dimension)));

			} else if (ERAConst.DIMENION_REV_3.equals(dimension)) {
				numVal[2].setV(String.valueOf(dimensionCounts.get(dimension)));

			} else if (ERAConst.DIMENION_REV_4.equals(dimension)) {
				numVal[3].setV(String.valueOf(dimensionCounts.get(dimension)));

			} else if (ERAConst.DIMENION_REV_5.equals(dimension)) {
				numVal[5].setV(String.valueOf(dimensionCounts.get(dimension)));

			} else if (ERAConst.DIMENION_REV_6.equals(dimension)) {
				numVal[5].setV(String.valueOf(dimensionCounts.get(dimension)));

			} else if (ERAConst.DIMENION_REV_7.equals(dimension)) {
				numVal[6].setV(String.valueOf(dimensionCounts.get(dimension)));

			} else if (ERAConst.DIMENION_REV_8.equals(dimension)) {
				numVal[7].setV(String.valueOf(dimensionCounts.get(dimension)));

			} else if (ERAConst.DIMENION_REV_9.equals(dimension)) {
				numVal[8].setV(String.valueOf(dimensionCounts.get(dimension)));

			} else if (ERAConst.DIMENION_REV_10.equals(dimension)) {
				numVal[9].setV(String.valueOf(dimensionCounts.get(dimension)));

			} else if (ERAConst.DIMENION_REV_11.equals(dimension)) {
				numVal[10].setV(String.valueOf(dimensionCounts.get(dimension)));

			} else if (ERAConst.DIMENION_REV_12.equals(dimension)) {
				numVal[11].setV(String.valueOf(dimensionCounts.get(dimension)));

			} else if (ERAConst.DIMENION_REV_13.equals(dimension)) {

				numVal[12].setV(String.valueOf(dimensionCounts.get(dimension)));
			} else if (ERAConst.DIMENION_REV_14.equals(dimension)) {

				numVal[12].setV(String.valueOf(dimensionCounts.get(dimension)));
			}
		}
	}

	private void replaceVariableValues(XWPFDocument wordDoc) {
		String pcbName = (String) properties.get(ERAConst.PCBA_NAME);
		pcbName = pcbName == null ? "【请补充名称】" : pcbName.trim();

		Style style = new Style();
		style.setBold(true);
		style.setFontFamily("楷体");
		style.setFontSize(36);
		replaceVariableValue(ERAConst.PCBA_NAME + 1, pcbName, style);

		style.setBold(true);
		style.setFontFamily("宋体");
		style.setFontSize(22);
		replaceVariableValue(ERAConst.PCBA_NAME + 2, pcbName, style);

		style.setBold(false);
		style.setFontFamily("宋体");
		style.setFontSize(-1);
		replaceVariableValue(ERAConst.PCBA_NAME + 3, pcbName, style);

		replaceVariableValue(ERAConst.ADVICE_NUMBER + 1,
				String.valueOf(adviceMap.size()), style);
	}

	protected String getTemplateFilePath() {
		return "/docs/template/Template_PCBAReliabilityReview.docx";
	}

	@SuppressWarnings("deprecation")
	protected void updateChart(XWPFDocument doc) {
		List<POIXMLDocumentPart> parts = doc.getRelations();

		for (POIXMLDocumentPart part : parts) {
			PackagePart packagePart = part.getPackagePart();
			if (XML_CHART_TYPE.equalsIgnoreCase(packagePart.getContentType())) {
				try {
					ChartSpaceDocument chartSpaceDoc = ChartSpaceDocument.Factory
							.parse(packagePart.getInputStream());

					// BarChar
					if (chartSpaceDoc.getChartSpace().getChart().getPlotArea()
							.getBarChartArray() != null
							&& chartSpaceDoc.getChartSpace().getChart()
									.getPlotArea().getBarChartArray().length > 0) {
						for (int i = 0; i < 4; i++) {
							CTNumData numData = chartSpaceDoc.getChartSpace()
									.getChart().getPlotArea()
									.getBarChartArray(0).getSerArray(i)
									.getVal().getNumRef().getNumCache();
							updateBarChartData(i, numData);
						}
					} else if (chartSpaceDoc.getChartSpace().getChart()
							.getPlotArea().getPieChartArray() != null
							&& chartSpaceDoc.getChartSpace().getChart()
									.getPlotArea().getPieChartArray().length > 0) {// PieChar
						CTNumData numData = chartSpaceDoc.getChartSpace()
								.getChart().getPlotArea().getPieChartArray(0)
								.getSerArray(0).getVal().getNumRef()
								.getNumCache();
						updatePieChartData(numData);
					}

					OutputStream out = packagePart.getOutputStream();
					chartSpaceDoc.save(out,
							POIXMLDocumentPart.DEFAULT_XML_OPTIONS);
					out.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		ArrayList<PackagePart> packageParts = doc
				.getPackage()
				.getPartsByContentType(
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		if (packageParts != null && packageParts.size() > 0) {
			for (PackagePart packagePart : packageParts) {
				updateEmbeddedExcel(packagePart);
			}
		}
	}

	private void updateEmbeddedExcel(PackagePart packagePart) {
		XSSFWorkbook embeddedWorkbook = null;
		try {
			embeddedWorkbook = new XSSFWorkbook(packagePart.getInputStream());

			XSSFSheet sheet = embeddedWorkbook.getSheetAt(0);
			XSSFRow row = sheet.getRow(0);
			if (row != null && row.getCell(1) != null
					&& row.getCell(1).getStringCellValue().equals("比重")) {
				sheet.getRow(1)
						.getCell(1)
						.setCellValue(
								pieCharData.get(pie_char_pt1) == null ? 0
										: pieCharData.get(pie_char_pt1));
				sheet.getRow(2)
						.getCell(1)
						.setCellValue(
								pieCharData.get(pie_char_pt2) == null ? 0
										: pieCharData.get(pie_char_pt2));
				sheet.getRow(3)
						.getCell(1)
						.setCellValue(
								pieCharData.get(pie_char_pt3) == null ? 0
										: pieCharData.get(pie_char_pt3));
				sheet.getRow(4)
						.getCell(1)
						.setCellValue(
								pieCharData.get(pie_char_pt4) == null ? 0
										: pieCharData.get(pie_char_pt4));
			} else if (row != null && row.getCell(1) != null
					&& row.getCell(1).getStringCellValue().equals("致命")) {
				Map<String, Integer> _col2_values = barCharData
						.get(ERAConst.SERIOUS_LEVEL_ERROR);
				Map<String, Integer> _col3_values = barCharData
						.get(ERAConst.SERIOUS_LEVEL_IMPORTANT);
				Map<String, Integer> _col4_values = barCharData
						.get(ERAConst.SERIOUS_LEVEL_COMMON);
				Map<String, Integer> _col5_values = barCharData
						.get(ERAConst.SERIOUS_LEVEL_PROMPT);

				sheet.getRow(1)
						.getCell(1)
						.setCellValue(
								getIntValue(_col2_values,
										ERAConst.DIMENION_REV_1));
				sheet.getRow(1)
						.getCell(2)
						.setCellValue(
								getIntValue(_col3_values,
										ERAConst.DIMENION_REV_1));
				sheet.getRow(1)
						.getCell(3)
						.setCellValue(
								getIntValue(_col4_values,
										ERAConst.DIMENION_REV_1));
				sheet.getRow(1)
						.getCell(4)
						.setCellValue(
								getIntValue(_col5_values,
										ERAConst.DIMENION_REV_1));

				sheet.getRow(2)
						.getCell(1)
						.setCellValue(
								getIntValue(_col2_values,
										ERAConst.DIMENION_REV_2));
				sheet.getRow(2)
						.getCell(2)
						.setCellValue(
								getIntValue(_col3_values,
										ERAConst.DIMENION_REV_2));
				sheet.getRow(2)
						.getCell(3)
						.setCellValue(
								getIntValue(_col4_values,
										ERAConst.DIMENION_REV_2));
				sheet.getRow(2)
						.getCell(4)
						.setCellValue(
								getIntValue(_col5_values,
										ERAConst.DIMENION_REV_2));

				sheet.getRow(3)
						.getCell(1)
						.setCellValue(
								getIntValue(_col2_values,
										ERAConst.DIMENION_REV_3));
				sheet.getRow(3)
						.getCell(2)
						.setCellValue(
								getIntValue(_col3_values,
										ERAConst.DIMENION_REV_3));
				sheet.getRow(3)
						.getCell(3)
						.setCellValue(
								getIntValue(_col4_values,
										ERAConst.DIMENION_REV_3));
				sheet.getRow(3)
						.getCell(4)
						.setCellValue(
								getIntValue(_col5_values,
										ERAConst.DIMENION_REV_3));

				sheet.getRow(4)
						.getCell(1)
						.setCellValue(
								getIntValue(_col2_values,
										ERAConst.DIMENION_REV_4));
				sheet.getRow(4)
						.getCell(2)
						.setCellValue(
								getIntValue(_col3_values,
										ERAConst.DIMENION_REV_4));
				sheet.getRow(4)
						.getCell(3)
						.setCellValue(
								getIntValue(_col4_values,
										ERAConst.DIMENION_REV_4));
				sheet.getRow(4)
						.getCell(4)
						.setCellValue(
								getIntValue(_col5_values,
										ERAConst.DIMENION_REV_4));

				sheet.getRow(5)
						.getCell(1)
						.setCellValue(
								getIntValue(_col2_values,
										ERAConst.DIMENION_REV_5));
				sheet.getRow(5)
						.getCell(2)
						.setCellValue(
								getIntValue(_col3_values,
										ERAConst.DIMENION_REV_5));
				sheet.getRow(5)
						.getCell(3)
						.setCellValue(
								getIntValue(_col4_values,
										ERAConst.DIMENION_REV_5));
				sheet.getRow(5)
						.getCell(4)
						.setCellValue(
								getIntValue(_col5_values,
										ERAConst.DIMENION_REV_5));

				row = sheet.getRow(6);
				row.getCell(1).setCellValue(
						getIntValue(_col2_values, ERAConst.DIMENION_REV_6));
				row.getCell(2).setCellValue(
						getIntValue(_col3_values, ERAConst.DIMENION_REV_6));
				row.getCell(3).setCellValue(
						getIntValue(_col4_values, ERAConst.DIMENION_REV_6));
				row.getCell(4).setCellValue(
						getIntValue(_col5_values, ERAConst.DIMENION_REV_6));

				row = sheet.getRow(7);
				row.getCell(1).setCellValue(
						getIntValue(_col2_values, ERAConst.DIMENION_REV_7));
				row.getCell(2).setCellValue(
						getIntValue(_col3_values, ERAConst.DIMENION_REV_7));
				row.getCell(3).setCellValue(
						getIntValue(_col4_values, ERAConst.DIMENION_REV_7));
				row.getCell(4).setCellValue(
						getIntValue(_col5_values, ERAConst.DIMENION_REV_7));

				row = sheet.getRow(8);
				row.getCell(1).setCellValue(
						getIntValue(_col2_values, ERAConst.DIMENION_REV_8));
				row.getCell(2).setCellValue(
						getIntValue(_col3_values, ERAConst.DIMENION_REV_8));
				row.getCell(3).setCellValue(
						getIntValue(_col4_values, ERAConst.DIMENION_REV_8));
				row.getCell(4).setCellValue(
						getIntValue(_col5_values, ERAConst.DIMENION_REV_8));

				row = sheet.getRow(9);
				row.getCell(1).setCellValue(
						getIntValue(_col2_values, ERAConst.DIMENION_REV_9));
				row.getCell(2).setCellValue(
						getIntValue(_col3_values, ERAConst.DIMENION_REV_9));
				row.getCell(3).setCellValue(
						getIntValue(_col4_values, ERAConst.DIMENION_REV_9));
				row.getCell(4).setCellValue(
						getIntValue(_col5_values, ERAConst.DIMENION_REV_9));

				row = sheet.getRow(10);
				row.getCell(1).setCellValue(
						getIntValue(_col2_values, ERAConst.DIMENION_REV_10));
				row.getCell(2).setCellValue(
						getIntValue(_col3_values, ERAConst.DIMENION_REV_10));
				row.getCell(3).setCellValue(
						getIntValue(_col4_values, ERAConst.DIMENION_REV_10));
				row.getCell(4).setCellValue(
						getIntValue(_col5_values, ERAConst.DIMENION_REV_10));

				row = sheet.getRow(11);
				row.getCell(1).setCellValue(
						getIntValue(_col2_values, ERAConst.DIMENION_REV_11));
				row.getCell(2).setCellValue(
						getIntValue(_col3_values, ERAConst.DIMENION_REV_11));
				row.getCell(3).setCellValue(
						getIntValue(_col4_values, ERAConst.DIMENION_REV_11));
				row.getCell(4).setCellValue(
						getIntValue(_col5_values, ERAConst.DIMENION_REV_11));

				row = sheet.getRow(12);
				row.getCell(1).setCellValue(
						getIntValue(_col2_values, ERAConst.DIMENION_REV_12));
				row.getCell(2).setCellValue(
						getIntValue(_col3_values, ERAConst.DIMENION_REV_12));
				row.getCell(3).setCellValue(
						getIntValue(_col4_values, ERAConst.DIMENION_REV_12));
				row.getCell(4).setCellValue(
						getIntValue(_col5_values, ERAConst.DIMENION_REV_12));

				sheet.getRow(13)
						.getCell(1)
						.setCellValue(
								getIntValue(_col2_values,
										ERAConst.DIMENION_REV_13));
				sheet.getRow(13)
						.getCell(2)
						.setCellValue(
								getIntValue(_col3_values,
										ERAConst.DIMENION_REV_13));
				sheet.getRow(13)
						.getCell(3)
						.setCellValue(
								getIntValue(_col4_values,
										ERAConst.DIMENION_REV_13));
				sheet.getRow(13)
						.getCell(4)
						.setCellValue(
								getIntValue(_col5_values,
										ERAConst.DIMENION_REV_13));

				sheet.getRow(14)
						.getCell(1)
						.setCellValue(
								getIntValue(_col2_values,
										ERAConst.DIMENION_REV_14));
				sheet.getRow(14)
						.getCell(2)
						.setCellValue(
								getIntValue(_col3_values,
										ERAConst.DIMENION_REV_14));
				sheet.getRow(14)
						.getCell(3)
						.setCellValue(
								getIntValue(_col4_values,
										ERAConst.DIMENION_REV_14));
				sheet.getRow(14)
						.getCell(4)
						.setCellValue(
								getIntValue(_col5_values,
										ERAConst.DIMENION_REV_14));
			}

			OutputStream out = packagePart.getOutputStream();
			embeddedWorkbook.write(out);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (embeddedWorkbook != null) {
				try {
					embeddedWorkbook.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private int getIntValue(Map<String, Integer> valueMap, String dimension) {
		if (valueMap == null) {
			return 0;
		}

		return valueMap.get(dimension) == null ? 0 : valueMap.get(dimension);
	}

	@SuppressWarnings("deprecation")
	protected void updateBarChartData(int levelIndex, CTNumData numData) {
		CTNumVal[] numVal = numData.getPtArray();

		Map<String, Integer> dimensionCount = null;
		if (levelIndex == 0) {
			dimensionCount = barCharData.get(ERAConst.SERIOUS_LEVEL_ERROR);
		} else if (levelIndex == 1) {
			dimensionCount = barCharData.get(ERAConst.SERIOUS_LEVEL_IMPORTANT);
		} else if (levelIndex == 2) {
			dimensionCount = barCharData.get(ERAConst.SERIOUS_LEVEL_COMMON);
		} else if (levelIndex == 3) {
			dimensionCount = barCharData.get(ERAConst.SERIOUS_LEVEL_PROMPT);
		}

		for (int i = 0; i < 14; i++) {
			numVal[i].setV("0");
		}

		if (dimensionCount == null || dimensionCount.size() < 1) {
			return;
		}

		Iterator<String> itr = dimensionCount.keySet().iterator();
		while (itr.hasNext()) {
			String dimension = itr.next();

			if (ERAConst.DIMENION_REV_1.equals(dimension)) {
				numVal[0].setV(String.valueOf(dimensionCount.get(dimension)));

			} else if (ERAConst.DIMENION_REV_2.equals(dimension)) {
				numVal[1].setV(String.valueOf(dimensionCount.get(dimension)));

			} else if (ERAConst.DIMENION_REV_3.equals(dimension)) {
				numVal[2].setV(String.valueOf(dimensionCount.get(dimension)));

			} else if (ERAConst.DIMENION_REV_4.equals(dimension)) {
				numVal[3].setV(String.valueOf(dimensionCount.get(dimension)));

			} else if (ERAConst.DIMENION_REV_5.equals(dimension)) {
				numVal[5].setV(String.valueOf(dimensionCount.get(dimension)));

			} else if (ERAConst.DIMENION_REV_6.equals(dimension)) {
				numVal[5].setV(String.valueOf(dimensionCount.get(dimension)));

			} else if (ERAConst.DIMENION_REV_7.equals(dimension)) {
				numVal[6].setV(String.valueOf(dimensionCount.get(dimension)));

			} else if (ERAConst.DIMENION_REV_8.equals(dimension)) {
				numVal[7].setV(String.valueOf(dimensionCount.get(dimension)));

			} else if (ERAConst.DIMENION_REV_9.equals(dimension)) {
				numVal[8].setV(String.valueOf(dimensionCount.get(dimension)));

			} else if (ERAConst.DIMENION_REV_10.equals(dimension)) {
				numVal[9].setV(String.valueOf(dimensionCount.get(dimension)));

			} else if (ERAConst.DIMENION_REV_11.equals(dimension)) {
				numVal[10].setV(String.valueOf(dimensionCount.get(dimension)));

			} else if (ERAConst.DIMENION_REV_12.equals(dimension)) {
				numVal[11].setV(String.valueOf(dimensionCount.get(dimension)));

			} else if (ERAConst.DIMENION_REV_13.equals(dimension)) {

				numVal[12].setV(String.valueOf(dimensionCount.get(dimension)));
			} else if (ERAConst.DIMENION_REV_14.equals(dimension)) {

				numVal[12].setV(String.valueOf(dimensionCount.get(dimension)));
			}
		}
	}

	@SuppressWarnings("deprecation")
	protected void updatePieChartData(CTNumData numData) {
		CTNumVal[] numVal = numData.getPtArray();

		for (int i = 0; i < 4; i++) {
			numVal[i].setV("0");
		}

		if (pieCharData.size() < 1) {
			return;
		}

		Iterator<String> itr = pieCharData.keySet().iterator();
		while (itr.hasNext()) {
			String charPt = itr.next();

			if (pie_char_pt1.equals(charPt)) {
				numVal[0].setV(String.valueOf(pieCharData.get(charPt)));
			} else if (pie_char_pt2.equals(charPt)) {
				numVal[1].setV(String.valueOf(pieCharData.get(charPt)));
			} else if (pie_char_pt3.equals(charPt)) {
				numVal[2].setV(String.valueOf(pieCharData.get(charPt)));
			} else if (pie_char_pt4.equals(charPt)) {
				numVal[3].setV(String.valueOf(pieCharData.get(charPt)));
			}
		}
	}
}
