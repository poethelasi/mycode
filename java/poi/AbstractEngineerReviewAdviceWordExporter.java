package com.zzbest.online.review.ui.view.base;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.POIXMLDocumentPart;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlToken;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumData;
import org.openxmlformats.schemas.drawingml.x2006.chart.ChartSpaceDocument;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualDrawingProps;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPositiveSize2D;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTInline;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTNumPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STJc;

import com.vaadin.server.BrowserWindowOpener;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.ui.Button;
import com.zzbest.online.consultation.model.PCBABasicInfo;
import com.zzbest.online.consultation.service.PCBABasicInfoDocumentService;
import com.zzbest.online.consultation.service.impl.PCBABasicInfoDocumentServiceImpl;
import com.zzbest.online.review.model.EngineerReviewAdvice;
import com.zzbest.platform.util.BeanFactoryUtils;

/**
 * Word版评审报告导出工具抽象类.
 * 
 * @author jovi.chou
 * @since 2016年4月1日
 */
public abstract class AbstractEngineerReviewAdviceWordExporter {

	protected static final String XML_CHART_TYPE = "application/vnd.openxmlformats-officedocument.drawingml.chart+xml";

	protected Map<Long, EngineerReviewAdvice> adviceMap;

	protected XWPFDocument wordDoc = null;

	protected PCBABasicInfoDocumentService basicInfoDocService; // 单板信息

	/**
	 * 记录每个维度评审问题表格的插入位置。
	 */
	protected Map<String, XWPFParagraph> insertionPositions = new HashMap<String, XWPFParagraph>();

	/**
	 * 记录每个维度的评审问题总数。
	 */
	protected Map<String, Integer> dimensionCounts = new HashMap<String, Integer>();

	/**
	 * 下载按钮.
	 */
	protected Button btnDownload;

	protected Map<String, Object> properties = new HashMap<String, Object>();

	protected int imageIndex = 0;

	public AbstractEngineerReviewAdviceWordExporter(
			Map<Long, EngineerReviewAdvice> adviceMap, Button btnDownload,
			Map<String, Object> properties) {
		this.adviceMap = adviceMap;
		this.btnDownload = btnDownload;
		this.properties = properties;

		this.basicInfoDocService = BeanFactoryUtils
				.getBean(PCBABasicInfoDocumentServiceImpl.class);
		this.initialize();
	}

	protected abstract String getTemplateFilePath();

	protected void initialize() {
		InputStream is = PCBAReliabilityEngineerReviewAdviceWordExporter.class
				.getResourceAsStream(getTemplateFilePath());

		try {
			wordDoc = new XWPFDocument(OPCPackage.open(is));
			getTableInsertionPositions(wordDoc.getParagraphs());
		} catch (InvalidFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected abstract void getTableInsertionPositions(
			List<XWPFParagraph> paragraphs);

	protected String getVariableFormat(String variableName) {
		return "{$" + variableName + "$}";
	}

	protected void replaceVariableValue(String variableName,
			String variableValue, Style style) {
		if (wordDoc.getParagraphs() != null
				&& wordDoc.getParagraphs().size() > 0) {
			List<XWPFParagraph> paragraphs = wordDoc.getParagraphs();

			for (int i = 0; i < paragraphs.size(); i++) {
				XWPFParagraph paragraph = paragraphs.get(i);
				String value = paragraph.getText();
				if (value != null
						&& value.indexOf(getVariableFormat(variableName)) > -1) {
					List<XWPFRun> runs = paragraph.getRuns();
					int[] variablePos = variablePosition(runs);

					for (int j = variablePos[1]; j >= variablePos[0]; j--) {
						paragraph.removeRun(j);
					}

					if (variablePos[0] > -1) {
						XWPFRun run = paragraph.insertNewRun(variablePos[0]);
						run.setText(variableValue);

						run.setBold(style.isBold());

						if (style.getFontSize() != -1) {
							run.setFontSize(style.getFontSize());
						}
						run.setFontFamily(style.getFontFamily());
					}
				}
			}
		}
	}

	protected int[] variablePosition(List<XWPFRun> runs) {
		if (runs == null) {
			return new int[] { -1, -1 };
		}

		if (runs.size() == 1) {
			if (runs.get(0).text().indexOf("{$") > -1)
				return new int[] { 0, 0 };
			else
				return new int[] { -1, -1 };
		}

		int startPos = -1;
		int endPos = -1;
		boolean isFoundStarter = false;
		for (int i = 0; i < runs.size() - 1; i++) {
			XWPFRun first = runs.get(i);
			XWPFRun second = runs.get(i + 1);

			if (!isFoundStarter) {
				if (second != null && second.text().indexOf("{$") > -1) {
					startPos = i + 1;
					isFoundStarter = true;
				} else if (first != null && first.text().indexOf("{$") > -1) {
					startPos = i;
					isFoundStarter = true;

					if (first.text().indexOf("}") > -1) {
						endPos = i;
						break;
					}
				} else if (first != null && second != null
						&& (first.text() + second.text()).indexOf("{$") > -1) {
					startPos = i;
					isFoundStarter = true;
				}
			}

			if (isFoundStarter) {
				if (second != null && second.text().indexOf("}") > -1) {
					endPos = i + 1;
					break;
				}
			}
		}

		return new int[] { startPos, endPos };
	}

	public void exportPCBABasicInfo(XWPFDocument wordDoc) {
		Long pcbaBasicInfoId = (Long) properties.get("pcbaBasicInfoId");
		if (pcbaBasicInfoId == null) {
			return;
		}

		PCBABasicInfo basicInfo = basicInfoDocService.getById(pcbaBasicInfoId);
		if (basicInfo == null) {
			return;
		}

		List<XWPFTable> tableList = wordDoc.getTables();

		// ------------单板正反面图片-------------------------
		XWPFTable table0 = tableList.get(1);
		XWPFTableRow row1 = table0.getRow(1);
		XWPFTableCell cell_0 = row1.getCell(0);
		XWPFTableCell cell_1 = row1.getCell(1);
		addPCBAImages(cell_0, basicInfo.getImage1());
		addPCBAImages(cell_1, basicInfo.getImage2());

		// ------------单板基础信息---------------------------
		XWPFTable table = tableList.get(2);
		Integer rowIndex = 0;
		XWPFTableRow row = table.getRow(rowIndex++);

		// 第一行 第2列
		// 单板名称_版本
		XWPFTableCell cell1 = row.getCell(1);
		cell1.setText(basicInfo.getPcbaName() + "/"
				+ basicInfo.getPcbaVersion());

		// 行业/产品类型
		XWPFTableCell cell2 = row.getCell(3);
		cell2.setText(basicInfo.getIndustry().getIndustryName() + "/"
				+ basicInfo.getProductType());

		// 当前的工艺路线
		XWPFTableRow row2 = table.getRow(rowIndex++);
		XWPFTableCell cell3 = row2.getCell(1);
		cell3.setText(basicInfo.getDangQianDeGongYiXianLu());

		// PCB基材板材
		XWPFTableRow row3 = table.getRow(rowIndex++);
		XWPFTableCell cell4 = row3.getCell(1);
		cell4.setText(basicInfo.getBanCai());

		// 层数
		XWPFTableCell cell5 = row3.getCell(3);
		cell5.setText(basicInfo.getCengShu());

		// Tg值（℃）
		XWPFTableRow row4 = table.getRow(rowIndex++);
		XWPFTableCell cell6 = row4.getCell(1);
		cell6.setText(basicInfo.getTgZhi());

		// 表面处理方式
		XWPFTableCell cell7 = row4.getCell(3);
		cell7.setText(basicInfo.getBiaoMianChuLiFangShi());

		// 板长（mm）
		XWPFTableRow row5 = table.getRow(rowIndex++);
		XWPFTableCell cell8 = row5.getCell(1);
		cell8.setText(basicInfo.getBanChang());
		// 板宽（mm）
		XWPFTableCell cell9 = row5.getCell(3);
		cell9.setText(basicInfo.getBanKuan());
		// 板厚（mm）
		XWPFTableRow row6 = table.getRow(rowIndex++);
		XWPFTableCell cell10 = row6.getCell(1);
		cell10.setText(basicInfo.getBanHou());

		// 最小过孔（mm）
		XWPFTableCell cell11 = row6.getCell(3);
		cell11.setText(basicInfo.getZuiXiaoGuoKongKongJing());

		// 拼板尺寸（mm*mm）
		XWPFTableRow row7 = table.getRow(rowIndex++);
		XWPFTableCell cell12 = row7.getCell(1);
		cell12.setText(basicInfo.getPinBanHouDeChiChun());
		// 分板工序/方式
		XWPFTableCell cell13 = row7.getCell(3);
		cell13.setText(basicInfo.getFenBanGongXu());

		// 元器件总数
		XWPFTableRow row8 = table.getRow(rowIndex++);
		XWPFTableCell cell14 = row8.getCell(1);
		cell14.setText(basicInfo.getQiJianZongShu());
		// 焊点总数
		XWPFTableCell cell15 = row8.getCell(3);
		cell15.setText(basicInfo.getHanDianZongShu());

		// 表贴率
		XWPFTableRow row9 = table.getRow(rowIndex++);
		XWPFTableCell cell16 = row9.getCell(1);
		cell16.setText(basicInfo.getQiJianBiaoTieLv());
		// 焊接工艺
		XWPFTableCell cell17 = row9.getCell(3);
		cell17.setText(basicInfo.getHanJieGongYi());

		// 片式元件最小封装
		XWPFTableRow row10 = table.getRow(rowIndex++);
		XWPFTableCell cell18 = row10.getCell(1);
		cell18.setText(basicInfo.getPianShiYuanJianZuiXiaoFengZhuang());
		// BGA最小间距（pitch）
		XWPFTableCell cell19 = row10.getCell(3);
		cell19.setText(basicInfo.getBgaZuiXiaoJianJu());

		// SOP/QFP最小间距（pitch）
		XWPFTableRow row11 = table.getRow(rowIndex++);
		XWPFTableCell cell20 = row11.getCell(1);
		cell20.setText(basicInfo.getSopQfpZuiXiaoJianJu());
		// QFN最小间距（pitch）
		XWPFTableCell cell21 = row11.getCell(3);
		cell21.setText(basicInfo.getQfnZuiXiaoJianJu());

		// 压接器件
		XWPFTableRow row12 = table.getRow(rowIndex++);
		XWPFTableCell cell22 = row12.getCell(1);
		cell22.setText(basicInfo.getYaJieQiJian());
		// 插件最小间距（Pitch）
		XWPFTableCell cell23 = row12.getCell(3);
		cell23.setText(basicInfo.getChaJianZuiXiaoJianJu());

		// 物料种类
		XWPFTableRow row13 = table.getRow(rowIndex++);
		XWPFTableCell cell24 = row13.getCell(1);
		cell24.setText(basicInfo.getWuLiaoZhongLei());
		XWPFTableCell cell25 = row13.getCell(3);
		cell25.setText("");

		// 其它工艺难点器件
		XWPFTableRow row14 = table.getRow(rowIndex++);
		XWPFTableCell cell26 = row14.getCell(1);
		cell26.setText(basicInfo.getQiTaGongYiNanDianQiJian());

		// 钢网设计和制作要求
		XWPFTableRow row15 = table.getRow(rowIndex++);
		XWPFTableCell cell27 = row15.getCell(1);
		cell27.setText(basicInfo.getGangWangZhiZuoYaoQiu());

		// 单板是否有特殊工艺
		XWPFTableRow row16 = table.getRow(rowIndex++);
		XWPFTableCell cell28 = row16.getCell(1);
		cell28.setText(basicInfo.getDanBanShiFouYouTeShuGongYi());

		// 散热器的装配方式
		XWPFTableRow row17 = table.getRow(rowIndex++);
		XWPFTableCell cell29 = row17.getCell(1);
		cell29.setText(basicInfo.getSanReQiDeZhuangPeiFangShi());

		// 前期生产或调试发现的主要问题
		XWPFTableRow row18 = table.getRow(rowIndex++);
		XWPFTableCell cell30 = row18.getCell(1);
		cell30.setText(basicInfo.getShiChanTiaoShiZhiZaoWenTi());
	}

	private void addPCBAImages(XWPFTableCell _cell, byte[] content) {
		XWPFParagraph para = _cell.addParagraph();

		if (content == null || content.length < 1) {
			_cell.addParagraph();
			_cell.addParagraph();
			_cell.addParagraph();
			_cell.addParagraph();
		} else {
			_cell.removeParagraph(0);
			para.setAlignment(ParagraphAlignment.CENTER);

			XWPFRun run = para.createRun();
			String rid = null;
			try {
				rid = wordDoc
						.addPictureData(
								content,
								org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_JPEG);
				addPicture(wordDoc, rid, imageIndex++, 90, 90, run);
			} catch (InvalidFormatException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 将评审报告与下载按钮关联.
	 */
	@SuppressWarnings("serial")
	protected void downloadWordReport() {
		String reportTitle = (String) properties.get(ERAConst.REPORT_TITLE);
		reportTitle = reportTitle == null ? "未命名" : reportTitle;
		ByteArrayOutputStream out = null;
		try {

			//wordDoc.createTOC();
			wordDoc.enforceUpdateFields();
			out = new ByteArrayOutputStream();
			wordDoc.write(out);

			final byte[] context = out.toByteArray();
			StreamResource resource = new StreamResource(new StreamSource() {
				public InputStream getStream() {
					return new ByteArrayInputStream(context);
				}
			}, new String(reportTitle.getBytes("gb2312"), "ISO8859-1")
					+ ".docx");

			resource.getStream().setParameter(
					"Content-Disposition",
					"attachment; filename="
							+ new String(reportTitle.getBytes("gb2312"),
									"ISO8859-1") + ".docx");

			resource.getStream()
					.setContentType(
							"application/vnd.openxmlformats-officedocument.wordprocessingml.document");

			BrowserWindowOpener opener = new BrowserWindowOpener(resource);
			opener.extend(btnDownload);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				out.close();
				wordDoc.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	protected void addPicture(XWPFDocument wordDoc, String rid, int id,
			int width, int height, XWPFRun run) {
		final int EMU = 9525;
		width *= EMU;
		height *= EMU;

		CTInline inline = run.getCTR().addNewDrawing().addNewInline();

		String picXml = ""
				+ "<a:graphic xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\">"
				+ "   <a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">"
				+ "      <pic:pic xmlns:pic=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">"
				+ "         <pic:nvPicPr>" + "            <pic:cNvPr id=\""
				+ id
				+ "\" name=\"Generated\"/>"
				+ "            <pic:cNvPicPr/>"
				+ "         </pic:nvPicPr>"
				+ "         <pic:blipFill>"
				+ "            <a:blip r:embed=\""
				+ rid
				+ "\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"/>"
				+ "            <a:stretch>"
				+ "               <a:fillRect/>"
				+ "            </a:stretch>"
				+ "         </pic:blipFill>"
				+ "         <pic:spPr>"
				+ "            <a:xfrm>"
				+ "               <a:off x=\"0\" y=\"0\"/>"
				+ "               <a:ext cx=\""
				+ width
				+ "\" cy=\""
				+ height
				+ "\"/>"
				+ "            </a:xfrm>"
				+ "            <a:prstGeom prst=\"rect\">"
				+ "               <a:avLst/>"
				+ "            </a:prstGeom>"
				+ "         </pic:spPr>"
				+ "      </pic:pic>"
				+ "   </a:graphicData>" + "</a:graphic>";

		inline.addNewGraphic().addNewGraphicData();
		XmlToken xmlToken = null;
		try {
			xmlToken = XmlToken.Factory.parse(picXml);
		} catch (XmlException xe) {
			xe.printStackTrace();
		}
		inline.set(xmlToken);

		inline.setDistT(0);
		inline.setDistB(0);
		inline.setDistL(0);
		inline.setDistR(0);

		CTPositiveSize2D extent = inline.addNewExtent();
		extent.setCx(width);
		extent.setCy(height);

		CTNonVisualDrawingProps docPr = inline.addNewDocPr();
		docPr.setId(id);
		docPr.setName("pic" + id);
		docPr.setDescr("");
	}

	protected void addSpace(XWPFRun run, int number) {
		for (int i = 0; i < number; i++) {
			run.addTab();
		}
	}

	/**
	 * 清除表格位置标志.
	 * 
	 * @param _doc
	 */
	protected void clearPositionFlags(XWPFDocument _doc) {
		if (insertionPositions != null && insertionPositions.size() > 0) {
			Set<String> keySet = insertionPositions.keySet();
			Iterator<String> itr = keySet.iterator();

			while (itr.hasNext()) {
				String key = itr.next();
				XWPFParagraph paragrah = insertionPositions.get(key);

				if (ERAConst.GYNDFX.equals(key)) {
					int pos = _doc.getBodyElements().indexOf(paragrah);
					_doc.removeBodyElement(pos);
				} else {
					int count = dimensionCounts.get(paragrah.getText()) == null ? 0
							: dimensionCounts.get(paragrah.getText());

					List<XWPFRun> runs = paragrah.getRuns();
					for (int i = runs.size() - 1; i >= 0; i--) {
						paragrah.removeRun(i);
					}

					if (count > 0) {
						paragrah.insertNewRun(0).setText("无。");
					}
				}
			}
		}
	}

	/**
	 * 按维度为评审问题编号.
	 */
	protected void organizeAdviceNumber(XWPFDocument _doc) {
		List<XWPFTable> tables = _doc.getTables();

		if (tables == null || tables.size() < 1) {
			return;
		}

		for (XWPFTable xwpfTable : tables) {
			XWPFTableRow row = xwpfTable.getRow(0);

			if (row != null) {
				XWPFTableCell cell = row.getCell(0);
				if (cell != null && cell.getText() != null
						&& cell.getText().indexOf("问题编号：") == 0) {
					if (row.getCell(1) != null) {
						XWPFTableCell numberCell = row.getCell(1);
						XWPFParagraph paragr = numberCell.getParagraphs()
								.get(0);
						CTPPr cttpr = paragr.getCTP().addNewPPr();
						CTNumPr ctnumPr = cttpr.addNewNumPr();
						ctnumPr.addNewIlvl().setVal(new BigInteger("0"));
						ctnumPr.addNewNumId().setVal(
								new BigInteger(String.valueOf("16")));
						cttpr.addNewJc().setVal(STJc.CENTER);
					}
				}
			}
		}
	}

	/**
	*更新图片信息，必须这么做
	*/
	protected void updateChart(XWPFDocument doc) {
		List<POIXMLDocumentPart> parts = doc.getRelations();

		for (POIXMLDocumentPart part : parts) {
			PackagePart packagePart = part.getPackagePart();
			if (XML_CHART_TYPE.equalsIgnoreCase(packagePart.getContentType()))
				try {
					ChartSpaceDocument chartSpaceDoc = ChartSpaceDocument.Factory
							.parse(packagePart.getInputStream());

					CTNumData numData = chartSpaceDoc.getChartSpace()
							.getChart().getPlotArea().getPieChartArray(0)
							.getSerArray(0).getVal().getNumRef().getNumCache();

					updateChartData(numData);

					OutputStream out = packagePart.getOutputStream();
					chartSpaceDoc.save(out,
							POIXMLDocumentPart.DEFAULT_XML_OPTIONS);
					out.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
		}
	}

	protected abstract void updateChartData(CTNumData numData);

	/**
	 * 给相应维度的评审问题总数加1.以便最终获得总数.
	 * 
	 * @param dimension
	 */
	protected void incrementCountOfAdvice(String dimension) {
		if (dimension == null) {
			return;
		}

		if (dimensionCounts.containsKey(dimension)) {
			dimensionCounts.put(dimension, dimensionCounts.get(dimension) + 1);
		} else {
			dimensionCounts.put(dimension, new Integer(1));
		}
	}

	/**
	 * 在指定位置创建指定行数和列数的表格.
	 * 
	 * @param _docomument
	 * @param positionFlag
	 * @param rows
	 * @param cols
	 * @return
	 */
	protected XWPFTable createTable(XWPFDocument _docomument,
			String positionFlag, int rows, int cols) {
		XWPFParagraph paragraph = insertionPositions.get(positionFlag);
		XmlCursor cursor = null;

		if (dimensionCounts.get(positionFlag) != null
				&& ((Integer) dimensionCounts.get(positionFlag)).intValue() > 0) {
			cursor = paragraph.getCTP().newCursor();
			_docomument.insertNewParagraph(cursor).createRun().setText("");
		}

		cursor = paragraph.getCTP().newCursor();
		XWPFTable _table = _docomument.insertNewTbl(cursor);
		_table.getCTTbl().getTblPr().addNewJc().setVal(STJc.CENTER);

		XWPFTableRow _row = _table.getRow(0);
		if (_row != null) {
			for (int j = 0; j < cols - 1; j++) {
				_row.createCell();
			}
		}

		for (int i = 1; i < rows; i++) {
			_row = _table.createRow();
		}

		// 在表格与表格之间增加一个空行
		// cursor = paragraph.getCTP().newCursor();
		// _docomument.insertNewParagraph(cursor).createRun().setText("");

		return _table;
	}

	/**
	 * 在指定位置创建指定行数和列数的表格.
	 * 
	 * @param _docomument
	 * @param positionFlag
	 * @param rows
	 * @param cols
	 * @return
	 */
	protected XWPFTable createTable(XWPFDocument _docomument,
			String positionFlag, boolean isNewLine, int rows, int cols) {
		XWPFParagraph paragraph = insertionPositions.get(positionFlag);
		XmlCursor cursor = null;

		if (isNewLine) {
			cursor = paragraph.getCTP().newCursor();
			_docomument.insertNewParagraph(cursor).createRun().setText("");
		}

		cursor = paragraph.getCTP().newCursor();
		XWPFTable _table = _docomument.insertNewTbl(cursor);
		_table.getCTTbl().getTblPr().addNewJc().setVal(STJc.CENTER);

		XWPFTableRow _row = _table.getRow(0);
		if (_row != null) {
			for (int j = 0; j < cols - 1; j++) {
				_row.createCell();
			}
		}

		for (int i = 1; i < rows; i++) {
			_row = _table.createRow();
		}

		// 在表格与表格之间增加一个空行
		// cursor = paragraph.getCTP().newCursor();
		// _docomument.insertNewParagraph(cursor).createRun().setText("");

		return _table;
	}

	protected String getSeriousLevel(String seriousLevel) {
		String levelText = null;
		if (ERAConst.SERIOUS_LEVEL_ERROR.equals(seriousLevel)) {
			levelText = ERAConst.SERIOUS_LEVEL_FORMAT_1;
		} else if (ERAConst.SERIOUS_LEVEL_IMPORTANT.equals(seriousLevel)) {
			levelText = ERAConst.SERIOUS_LEVEL_FORMAT_2;
		} else if (ERAConst.SERIOUS_LEVEL_COMMON.equals(seriousLevel)) {
			levelText = ERAConst.SERIOUS_LEVEL_FORMAT_3;
		} else if (ERAConst.SERIOUS_LEVEL_PROMPT.equals(seriousLevel)) {
			levelText = ERAConst.SERIOUS_LEVEL_FORMAT_4;
		}

		return levelText;
	}

	protected void formatPrint(XWPFParagraph parag, XWPFTableCell cell,
			String textContent) {
		if (cell == null || textContent == null
				|| textContent.trim().length() < 0) {
			return;
		}

		if (!textContent.contains("\n")) {
			parag.createRun().setText(textContent);
			return;
		}

		String[] values = textContent.split("\n");
		for (int i = 0; i < values.length; i++) {
			if (i == 0) {
				parag.createRun().setText(values[i]);
			} else {
				cell.addParagraph().createRun().setText(values[i]);
			}
		}
	}

	class Style {
		private boolean isBold;
		private int fontSize;
		private String fontFamily;

		public boolean isBold() {
			return isBold;
		}

		public void setBold(boolean isBold) {
			this.isBold = isBold;
		}

		public int getFontSize() {
			return fontSize;
		}

		public void setFontSize(int fontSize) {
			this.fontSize = fontSize;
		}

		public String getFontFamily() {
			return fontFamily;
		}

		public void setFontFamily(String fontFamily) {
			this.fontFamily = fontFamily;
		}
	}
}
