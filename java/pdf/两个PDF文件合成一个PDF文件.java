package com.zzbest.tools.worddata;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.multipdf.Overlay;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.PDFMergerUtility1;
import org.apache.pdfbox.pdfwriter.COSWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;

public class PDFMergeUtil {

	public static final String file1 = "C:\\Temp\\PDF(1).pdf";
	public static final String file2 = "C:\\Temp\\PDF(2).pdf";

	public static void main(String[] args) throws IOException {
		combine2();
	}

	public static void combine1() throws IOException {
		PDDocument document = PDDocument.load(new File(file2));
		RandomAccessRead inputData = new RandomAccessBufferedFileInputStream(file1);
		COSWriter writer = new COSWriter(new BufferedOutputStream(new FileOutputStream(new File("c:\\Temp\\ddd.pdf"))),
				inputData);
		writer.write(document);
		writer.close();
	}

	public static void createNewPDF() throws IOException {
		PDDocument document = new PDDocument();
		PDPage page = new PDPage(PDRectangle.A4);// 设置页面大小,坐标(0,0)的位置在左下角
		// page.setRotation(180); //旋转角度0/90/180/270
		document.addPage(page);

		PDFont font = PDType1Font.HELVETICA_BOLD;

		PDPageContentStream contentStream = new PDPageContentStream(document, page);

		String message = "Hello.World------";
		contentStream.beginText();
		contentStream.setFont(font, 12);
		contentStream.newLineAtOffset(0, 800);
		for (int i = 0; i < 10000; i++) {
			contentStream.showText(message + " " + Integer.toString(i));
			System.out.println(message + " " + Integer.toString(i));
			contentStream.newLineAtOffset(0, -15); // (x,y)坐标，当前鼠标的位置为(0.0) -15
													// 表示鼠标沿Y方向（向下）移动15
		}
		contentStream.endText();

		contentStream.close();

		// 设置缩放比例:100% 75% 等等
		PDPageXYZDestination dest = new PDPageXYZDestination();
		dest.setPage(page);
		dest.setZoom(1f);
		dest.setTop(new Float(PDRectangle.A4.getHeight()).intValue());
		PDActionGoTo action = new PDActionGoTo();
		action.setDestination(dest);
		document.getDocumentCatalog().setOpenAction(action);

		document.save("c:\\Temp\\Hello World.pdf");
		document.close();
	}

	public static void combine() {
		try {
			PDFMergerUtility1 mergePdf = new PDFMergerUtility1();
			mergePdf.addSource(new File(file1));
			mergePdf.addSource(new File(file2));

			mergePdf.setDestinationFileName("c:\\Temp\\PDF.pdf");
			mergePdf.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void combine2() {
		try {
			PDDocument _document1 = PDDocument.load(new File(file1));
			PDPage _page1 = _document1.getPage(0);
			PDRectangle _cropBox1 = _page1.getCropBox();
			System.out.println("before:LowerLeftX:" + _cropBox1.getLowerLeftX() + "LowerLeftY:"
					+ _cropBox1.getLowerLeftY() + "UpperRightX:" + _cropBox1.getUpperRightX() + "UpperRightY:"
					+ _cropBox1.getUpperRightY());

			float originalHeight = _cropBox1.getHeight();

			_cropBox1.setLowerLeftY(-(PDRectangle.A4.getHeight() - _cropBox1.getHeight()));
			_page1.setMediaBox(_cropBox1);
			_page1.setCropBox(_cropBox1);

			System.out.println("after:LowerLeftX:" + _page1.getCropBox().getLowerLeftX() + "LowerLeftY:"
					+ _page1.getCropBox().getLowerLeftY() + "UpperRightX:" + _page1.getCropBox().getUpperRightX()
					+ "UpperRightY:" + _page1.getCropBox().getUpperRightY());

			// 设置缩放比例:100% 75% 等等
			PDPageXYZDestination dest1 = new PDPageXYZDestination();
			dest1.setPage(_page1);
			dest1.setZoom(1f);
			dest1.setTop(new Float(PDRectangle.A4.getHeight()).intValue());
			PDActionGoTo action1 = new PDActionGoTo();
			action1.setDestination(dest1);
			_document1.getDocumentCatalog().setOpenAction(action1);

			ByteArrayOutputStream _baos1 = new ByteArrayOutputStream();
			_document1.save(_baos1);
			_document1.close();

			PDDocument _document2 = PDDocument.load(new File(file2));
			PDPage _page2 = _document2.getPage(0);
			PDRectangle _cropBox2 = _page2.getCropBox();
			System.out.println("before:LowerLeftX:" + _cropBox2.getLowerLeftX() + "LowerLeftY:"
					+ _cropBox2.getLowerLeftY() + "UpperRightX:" + _cropBox2.getUpperRightX() + "UpperRightY:"
					+ _cropBox2.getUpperRightY());

			_cropBox2.setLowerLeftY(-(PDRectangle.A4.getHeight() - _cropBox2.getHeight() + originalHeight + 20));
			_page2.setMediaBox(_cropBox2);
			_page2.setCropBox(_cropBox2);

			System.out.println("after:LowerLeftX:" + _page2.getCropBox().getLowerLeftX() + "LowerLeftY:"
					+ _page2.getCropBox().getLowerLeftY() + "UpperRightX:" + _page2.getCropBox().getUpperRightX()
					+ "UpperRightY:" + _page2.getCropBox().getUpperRightY());

			// 设置缩放比例:100% 75% 等等
			PDPageXYZDestination dest2 = new PDPageXYZDestination();
			dest2.setPage(_page2);
			dest2.setZoom(1f);
			dest2.setTop(new Float(PDRectangle.A4.getHeight()).intValue());
			PDActionGoTo action2 = new PDActionGoTo();
			action2.setDestination(dest2);
			_document2.getDocumentCatalog().setOpenAction(action2);

			ByteArrayOutputStream _baos2 = new ByteArrayOutputStream();
			_document2.save(_baos2);
			_document2.close();

			PDFMergerUtility mergePdf = new PDFMergerUtility();
			mergePdf.addSource(new ByteArrayInputStream(_baos1.toByteArray()));
			mergePdf.addSource(new ByteArrayInputStream(_baos2.toByteArray()));

			Overlay overLay = new Overlay();
			overLay.setInputPDF(PDDocument.load(_baos1.toByteArray()));
			overLay.setDefaultOverlayPDF(PDDocument.load(_baos2.toByteArray()));
			Map<Integer, String> specificPageOverlayFile = new HashMap<Integer, String>();
			PDDocument _doc = overLay.overlay(specificPageOverlayFile);
			_doc.save("c:\\Temp\\PDF.pdf");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
