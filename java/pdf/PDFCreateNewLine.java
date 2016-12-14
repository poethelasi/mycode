package com.zzbest.tools.worddata;

import java.io.IOException;

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
		PDDocument document = new PDDocument();
		PDPage page = new PDPage(PDRectangle.A4);// 设置页面大小,坐标(0,0)的位置在左下角
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

}
