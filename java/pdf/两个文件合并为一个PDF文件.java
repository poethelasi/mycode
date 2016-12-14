package com.zzbest.tools.worddata;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.multipdf.Overlay;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;

public class PDFMergeUtil {

	public static final String file1 = "C:\\Temp\\PDF(1).pdf";
	public static final String file2 = "C:\\Temp\\PDF(2).pdf";

	public static void main(String[] args) throws IOException {
		twoFilesIntoOneFile();
	}

	public static void twoFilesIntoOneFile() {
		try {
			PDDocument _document1 = PDDocument.load(new File(file1));
			PDPage _page1 = _document1.getPage(0);
			PDRectangle _cropBox1 = _page1.getCropBox();

			float originalHeight = _cropBox1.getHeight();

			_cropBox1.setLowerLeftY(-(PDRectangle.A4.getHeight() - _cropBox1.getHeight()));
			_page1.setMediaBox(_cropBox1);
			_page1.setCropBox(_cropBox1);

			ByteArrayOutputStream _baos1 = new ByteArrayOutputStream();
			_document1.save(_baos1);
			_document1.close();

			PDDocument _document2 = PDDocument.load(new File(file2));
			PDPage _page2 = _document2.getPage(0);
			PDRectangle _cropBox2 = _page2.getCropBox();

			_cropBox2.setLowerLeftY(-(PDRectangle.A4.getHeight() - _cropBox2.getHeight() + originalHeight + 20));
			_page2.setMediaBox(_cropBox2);
			_page2.setCropBox(_cropBox2);

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

			PDPageXYZDestination dest2 = new PDPageXYZDestination();
			dest2.setPage(_doc.getPage(0));
			dest2.setZoom(1f);
			dest2.setTop(new Float(PDRectangle.A4.getHeight()).intValue());
			PDActionGoTo action2 = new PDActionGoTo();
			action2.setDestination(dest2);
			_doc.getDocumentCatalog().setOpenAction(action2);

			_doc.save("c:\\Temp\\PDF.pdf");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
