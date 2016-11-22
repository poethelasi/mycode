package com.zzbest.tools.worddata;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGraphicalObject;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTInline;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDrawing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTc;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.zzbest.online.review.model.ReviewAdvice;

public class WordDataImporter {

	private static final String NODE_NAME_PIC_BLIPFILL = "pic:blipFill";

	private static final String NODE_NAME_A_BLIP = "a:blip";

	private static final String ATTR_R_EMBED = "r:embed";

	private Map<String, XWPFPictureData> pictureMap = null;

	private String reviewDimension = null;

	public void importDataFromDirectory(String directoryPath) {
		try {
			Collection<File> files = FileUtils.listFiles(
					new File(directoryPath), new SuffixFileFilter(".docx"),
					null);
			for (File file : files) {
				// System.out.println(file.getCanonicalPath());
				// getWordDocument(file.getCanonicalPath());
				importDataFromFile(file.getCanonicalPath());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void importDataFromFile(String filePath) {
		XWPFDocument document = getWordDocument(filePath);
		if (document == null) {
			System.out.println(">>>>>>>读取文件失败：" + filePath);
			return;
		}

		System.out.println(">>>>>>>开始导入文件内容:" + filePath);

		// 如果有图片将图片数据缓存到Map中
		List<XWPFPictureData> allPictures = document.getAllPictures();
		if (allPictures != null) {
			System.out.println(">>>>图片总数量:" + allPictures.size());
			pictureMap = new HashMap<String, XWPFPictureData>();
			for (XWPFPictureData picData : allPictures) {
				pictureMap.put(picData.getPackageRelationship().getId(),
						picData);
			}
		}

		// 设置维度
		setReviewDimension(filePath);

		List<XWPFTable> tables = document.getTables();
		if (tables != null && tables.size() > 0) {
			System.out.println(">>>>评审意见总数量:" + tables.size());
			readTables(tables);
		}
	}

	private void readTables(List<XWPFTable> tables) {
		for (int i = 0; i < tables.size(); i++) {
			XWPFTable table = tables.get(i);
			if (table != null) {
				readTable((i + 1), tables.get(i));
			} else {
				System.out.println("table index: <" + i + "> is null.");
			}
		}
	}

	private void readTable(int tableIndex, XWPFTable table) {
		if (validateTableFormat(tableIndex, table)) {
			ReviewAdvice ra = new ReviewAdvice();

			// 评审维度
			ra.setReviewDimension(getReviewDimension());

			// 编号和严重程度
			XWPFTableRow firstRow = table.getRow(0);
			String number = String.valueOf(tableIndex);
			String level = readLevelData(firstRow);
			ra.setAdviceNumber(number);
			ra.setLevel(level);

			if (isFiveRowsFormat()) {
				XWPFTableRow thirdRow = table.getRow(1);
				String description = readPrombleDescription(thirdRow);
				ra.setProblemDesc(description);

				XWPFTableRow fourthRow = table.getRow(2);
				String analysis = readPrombleAnalysis(fourthRow);
				ra.setProblemAnalysis(analysis);

				XWPFTableRow fifthRow = table.getRow(3);
				String resolution = readPrombleResolution(fifthRow);
				ra.setPrombleResolution(resolution);

				XWPFTableRow sixthRow = table.getRow(4);
				List<CTGraphicalObject> graphicalObjects = readPictures(sixthRow);
				setPictureData(ra, graphicalObjects);
			} else {
				XWPFTableRow secondRow = table.getRow(1);
				String materialType = readMaterialType(secondRow);
				ra.setMaterialType(materialType);

				XWPFTableRow thirdRow = table.getRow(2);
				String description = readPrombleDescription(thirdRow);
				ra.setProblemDesc(description);

				XWPFTableRow fourthRow = table.getRow(3);
				String analysis = readPrombleAnalysis(fourthRow);
				ra.setProblemAnalysis(analysis);

				XWPFTableRow fifthRow = table.getRow(4);
				String resolution = readPrombleResolution(fifthRow);
				ra.setPrombleResolution(resolution);

				XWPFTableRow sixthRow = table.getRow(5);
				List<CTGraphicalObject> graphicalObjects = readPictures(sixthRow);
				setPictureData(ra, graphicalObjects);
			}
		}
	}

	private void setPictureData(ReviewAdvice ra,
			List<CTGraphicalObject> graphicalObjects) {
		if (graphicalObjects == null) {
			return;
		}

		if (graphicalObjects.size() >= 1) {
			CTGraphicalObject graphicalObject1 = graphicalObjects.get(0);
			if (graphicalObject1 != null) {
				String relId = getRelId(graphicalObject1);
				XWPFPictureData pictureData = getPicture(relId);
				ra.setImage1(pictureData.getData());
				ra.setPicExtension1(parsePictureType(pictureData
						.getPictureType()));
			}
		}

		if (graphicalObjects.size() >= 2) {
			CTGraphicalObject graphicalObject2 = graphicalObjects.get(1);
			if (graphicalObject2 != null) {
				String relId = getRelId(graphicalObject2);
				XWPFPictureData pictureData = getPicture(relId);
				ra.setImage2(pictureData.getData());
				ra.setPicExtension2(parsePictureType(pictureData
						.getPictureType()));
			}
		}

		if (graphicalObjects.size() >= 3) {
			CTGraphicalObject graphicalObject3 = graphicalObjects.get(2);
			if (graphicalObject3 != null) {
				String relId = getRelId(graphicalObject3);
				XWPFPictureData pictureData = getPicture(relId);
				ra.setImage3(pictureData.getData());
				ra.setPicExtension3(parsePictureType(pictureData
						.getPictureType()));
			}
		}

		if (graphicalObjects.size() > 3) {
			// System.err.println(">>>>图片数量超过3张:" + graphicalObjects.size()
			// + " ,详情：" + ra.toString());
		}
	}

	private String parsePictureType(int type) {
		if (5 == type) {
			return "jpeg";
		} else if (6 == type) {
			return "png";
		} else if (8 == type) {
			return "gif";
		} else if (11 == type) {
			return "bmp";
		}

		return "jpeg";
	}

	private String getRelId(CTGraphicalObject graphicalObject) {
		Node picRootNode = graphicalObject.getGraphicData().getDomNode();
		if (picRootNode == null) {
			return null;
		}

		if (picRootNode.getFirstChild() == null
				|| picRootNode.getFirstChild().getChildNodes() == null) {
			return null;
		}

		NodeList nodeList = picRootNode.getFirstChild().getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (NODE_NAME_PIC_BLIPFILL.equals(node.getNodeName())) {
				Node firstNode = node.getFirstChild();

				if (firstNode != null
						&& firstNode.getNodeName().equals(NODE_NAME_A_BLIP)) {
					NamedNodeMap nodeMap = firstNode.getAttributes();
					Node atrr = nodeMap.getNamedItem(ATTR_R_EMBED);
					String relId = atrr.getNodeValue();
					return relId;
				}
			}
		}

		return null;
	}

	/**
	 * 问题编号。
	 * 
	 * @param firstRow
	 * @return
	 */
	protected String readNumber(XWPFTableRow firstRow) {
		return firstRow.getCell(1).getText();
	}

	/**
	 * 读取问题重要等级
	 */
	private String readLevelData(XWPFTableRow firstRow) {
		String level = firstRow.getCell(3).getText();
		if (level == null) {
			return null;
		}

		String values[] = level.split("、");
		if (values != null) {
			for (String value : values) {
				if (value.contains("√")) {
					return value.substring(1, value.length());
				}
			}
		}

		return null;
	}

	/**
	 * 物料类别/电路
	 * 
	 * @param secondRow
	 * @return
	 */
	private String readMaterialType(XWPFTableRow secondRow) {
		return secondRow.getCell(1).getText();
	}

	/**
	 * 问题描述.
	 * 
	 * @param thirdRow
	 * @return
	 */
	private String readPrombleDescription(XWPFTableRow thirdRow) {
		return thirdRow.getCell(1).getText();
	}

	/**
	 * 问题分析.
	 * 
	 * @param fourthRow
	 * @return
	 */
	private String readPrombleAnalysis(XWPFTableRow fourthRow) {
		return fourthRow.getCell(1).getText();
	}

	/**
	 * 问题解决方案.
	 * 
	 * @param fifthRow
	 * @return
	 */
	private String readPrombleResolution(XWPFTableRow fifthRow) {
		return fifthRow.getCell(1).getText();
	}

	/**
	 * 读取图片。
	 * 
	 * @param sixthRow
	 * @return
	 */
	private List<CTGraphicalObject> readPictures(XWPFTableRow sixthRow) {
		List<CTGraphicalObject> pictures = new ArrayList<CTGraphicalObject>();
		XWPFTableCell cell = sixthRow.getCell(1);
		CTTc cTTc = cell.getCTTc();

		if (cTTc != null) {
			List<CTP> ctpList = cTTc.getPList();
			for (CTP ctp : ctpList) {
				List<CTR> ctrList = ctp.getRList();
				if (ctrList == null || ctrList.size() <= 0) {
					continue;
				}
				for (CTR ctr : ctrList) {
					List<CTDrawing> drawingList = ctr.getDrawingList();
					if (drawingList == null || drawingList.size() <= 0) {
						continue;
					}
					for (CTDrawing ctDrawing : drawingList) {
						List<CTInline> inlineList = ctDrawing.getInlineList();
						if (inlineList == null || inlineList.size() <= 0) {
							continue;
						}
						for (CTInline ctInline : inlineList) {
							if (ctInline.getGraphic() != null) {
								pictures.add(ctInline.getGraphic());
							}
						}
					}
				}
			}
		}
		return pictures;
	}

	private boolean validateTableFormat(int tableIndex, XWPFTable table) {
		if (isFiveRowsFormat()) {
			return validateTableFormat(table, tableIndex, 5);
		} else { // others
			return validateTableFormat(table, tableIndex, 6);
		}
	}

	private boolean validateTableFormat(XWPFTable table, int tableIndex,
			int specifiedRowNum) {

		if (table == null) {
			return false;
		}

		// 验证总行数，必须为specifiedRowNum行
		int rowNum = table.getRows() == null ? 0 : table.getRows().size();
		if (rowNum != specifiedRowNum) {
			System.err.println(">>>>第" + tableIndex + "个评审意见格式不正确，行数不为"
					+ specifiedRowNum + "行:" + rowNum);
			return false;
		}

		// 验证第1~specifiedRowNum行单元格数
		for (int i = 0; i < specifiedRowNum; i++) {
			XWPFTableRow row = table.getRow(i);
			int cellNum = row.getTableCells() == null ? 0 : row.getTableCells()
					.size();
			if (i == 0 && cellNum != 4) {// 第一行单元格数，必須為4個
				System.err.println(">>>>第" + tableIndex + "个评审意见格式不正确，第1行不为4列:"
						+ rowNum);
				return false;
			}

			if (i > 0 && cellNum != 2) { // 其他行单元格数，都必須為2個
				System.err.println(">>>>第" + tableIndex + "个评审意见格式不正确，第"
						+ (i + 1) + "行不为2列:" + rowNum);
				return false;
			}
		}

		return true;
	}

	private XWPFDocument getWordDocument(String filePath) {
		File file = FileUtils.getFile(filePath);

		try {
			InputStream is = FileUtils.openInputStream(file);
			XWPFDocument document = new XWPFDocument(is);

			return document;
		} catch (Exception e) {
			// System.out.println(e.getMessage());
		}

		return null;
	}

	private XWPFPictureData getPicture(String relId) {
		return pictureMap != null ? pictureMap.get(relId) : null;
	}

	private String getReviewDimension() {
		return reviewDimension;
	}

	private boolean isFiveRowsFormat() {
		String demonsion = getReviewDimension();
		// 13封装库设计/12PCB工艺设计/11器件工艺应用/10互连可靠性/9工艺线路设计/8热设计/7电源及信号完整性（PI and
		// SI）分析/6EMC安规防护设计
		if ("封装库设计".equals(demonsion) || "PCB工艺设计".equals(demonsion)
				|| "器件工艺应用".equals(demonsion) || "互连可靠性".equals(demonsion)
				|| "工艺线路设计".equals(demonsion) || "热设计".equals(demonsion)
				|| "电源及信号完整性（PI and SI）分析".equals(demonsion)
				|| "EMC安规防护设计".equals(demonsion)) {
			return true;
		}
		return false;
	}

	private void setReviewDimension(String filePath) {
		String dimension = "【未设置维度】";
		if (filePath.contains("物料的厂家选择")) {// 1
			dimension = "物料的厂家选择";
		} else if (filePath.contains("物料的历史质量数据分析")) {// 2
			dimension = "物料的历史质量数据分析";
		} else if (filePath.contains("物料归一化")) {// 3
			dimension = "物料归一化";
		} else if (filePath.contains("环境要求")) {// 4
			dimension = "环境要求";
		} else if (filePath.contains("物料降额")) {// 5
			dimension = "物料降额";
		} else if (filePath.contains("EMC安规防护设计")) {// 6
			dimension = "EMC安规防护设计";
		} else if (filePath.contains("电源及信号完整性（PI and SI）分析")) {// 7
			dimension = "电源及信号完整性（PI and SI）分析";
		} else if (filePath.contains("热设计")) {// 8
			dimension = "热设计";
		} else if (filePath.contains("工艺线路设计")) {// 9
			dimension = "工艺线路设计";
		} else if (filePath.contains("互连可靠性")) {// 10
			dimension = "互连可靠性";
		} else if (filePath.contains("器件工艺应用")) {// 11
			dimension = "器件工艺应用";
		} else if (filePath.contains("PCB工艺设计")) {// 12
			dimension = "PCB工艺设计";
		} else if (filePath.contains("封装库设计")) {// 13
			dimension = "封装库设计";
		}

		this.reviewDimension = dimension;
	}

	public static final String filePath = "D:\\reviewWord\\1物料的厂家选择.docx";

	public static final String directoryPath = "d:\\reviewWord";

	public static void main(String[] args) {
		new WordDataImporter().importDataFromDirectory(directoryPath);
	}
}
