package org.corvino;

import com.itextpdf.text.pdf.BadPdfFormatException;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

public class AddPDFBookmarks {

	public static void main(String args[]) throws Exception
	{
		if (1 > args.length) {
			printUsage();
		} else {
			String command = args[0];

			if ("bookmark".equals(command)) {
				if (4 != args.length) {
					System.err.println("Usage: bookmark <config-file> <pdf-file> <out-file>");
				} else {
					String configFile = args[1];
					String pdfFile = args[2];
					String outFile = args[3];

					bookmark(configFile, pdfFile, outFile);
				}
			} else if ("show-contents".equals(command)) {
				if (3 != args.length) {
					System.err.println("Usage: show-contents <config-file> <pdf-file>");
				} else {
					Config config = new Config(args[1]);
					String contents = getContents(config, args[2]);

					System.out.println("\nContents:\n-----------------");
					System.out.println(contents);
				}
			} else if ("parse-contents".equals(command)) {
				if (3 != args.length) {
					System.err.println("Usage: parse-contents <config-file> <pdf-file>");
				} else {
					parseContents(args[1], args[2]);
				}
			} else {
				printUsage();
			}
		}
	}

	private static void printUsage()
	{
		System.err.println("Usage: <config-file> <pdf-file> [<out-file>] <command>");
		System.err.println("Commands:");
		System.err.println("  bookmark");
		System.err.println("  show-contents");
		System.err.println("  parse-contents");
	}

	private static void bookmark(String configFile, String inputFile, String outputFile)
		throws ParserConfigurationException, SAXException,
			   IOException, DocumentException, BadPdfFormatException
	{
		int numOfPages;

		PdfCopy writer;
		Document document;
		PdfImportedPage page;

		Config config = new Config(configFile);
		PdfReader reader = new PdfReader(inputFile);
		List<HashMap<String, Object>> bookmarks = getBookmarks(config, inputFile);

		reader.consolidateNamedDestinations();
		numOfPages = reader.getNumberOfPages();

		document = new Document(reader.getPageSizeWithRotation(1));
		writer = new PdfCopy(document, new FileOutputStream(outputFile));

		document.open();

		for (int i = 1; i <= numOfPages; i++) {
			page = writer.getImportedPage(reader, i);
			writer.addPage(page);
		}

		writer.setOutlines(bookmarks);
		reader.close();
		writer.close();
		document.close();
	}

	private static void parseContents(String configFile, String filename)
		throws ParserConfigurationException, SAXException, IOException
	{
		Config config = new Config(configFile);
		List<HashMap<String, Object>> bookmarks = getBookmarks(config, filename);

		System.out.println("\nBookmarks:\n-----------------");

		int level = 0;
		Stack<Iterator<HashMap<String, Object>>> above =
			new Stack<Iterator<HashMap<String, Object>>>();
		Iterator<HashMap<String, Object>> iterator = bookmarks.iterator();

		while (iterator.hasNext()) {
			HashMap<String, Object> bookmark = iterator.next();

			for (int i = 0; i < level; i++) {
				System.out.print("	");
			}

			System.out.println(bookmark.get("Title") + " : " + bookmark.get("Page"));

			if (bookmark.containsKey("Kids")) {
				level += 1;
				@SuppressWarnings("unchecked")
				List<HashMap<String, Object>> kids =
						(List<HashMap<String, Object>>) bookmark.get("Kids");
				above.push(iterator);
				iterator = kids.iterator();
			}

			while (!iterator.hasNext() && above.size() > 0) {
				iterator = above.pop();
				level -= 1;
			}
		}
	}


	private static String getContents(Config config, String filename)
		throws IOException
	{
		PdfReader pdfReader = new PdfReader(filename);
		StringBuffer contents = new StringBuffer();

		for (int i = config.getContentsStartPage(); i <= config.getContentsEndPage(); i++) {
			contents.append(PdfTextExtractor.getTextFromPage(pdfReader, i));
		}

		return contents.toString();
	}

	private static List<HashMap<String, Object>> getBookmarks(Config config, String pdfFile)
		throws IOException
	{
		boolean result;
		HashMap<String, Object> bookmark;
		int numOfPages;
		int pageNumber;
		int	level;
		PdfReader reader;

		String contentsLine;
		String pageValue;
		String section;
		String title;

		int pageZero = config.getPageZero();
		int pageRomanZero = config.getPageRomanZero();

		String contentRegEx = config.getContentPattern();
		Pattern contentPattern = Pattern.compile(contentRegEx);
		Matcher contentMatcher = contentPattern.matcher("");
		String ignoreRegEx = config.getIgnorePattern();
		Matcher ignoreMatcher = null;

		String accumulatedContent = "";
		HierarchicalBookmarkCollector		bookmarkCollector = new HierarchicalBookmarkCollector();
		String contents = getContents(config, pdfFile);
		BufferedReader contentsReader = new BufferedReader(new StringReader(contents));

		System.out.println("Content regex: " + contentRegEx);

		if (null != ignoreRegEx) {
			Pattern ignorePattern = Pattern.compile(ignoreRegEx);
			ignoreMatcher = ignorePattern.matcher("");
		}

		for (contentsLine = contentsReader.readLine();
								 null != contentsLine;
								 contentsLine = contentsReader.readLine()) {
            if (null != ignoreMatcher) {
                ignoreMatcher.reset(contentsLine);
            }

			if (null == ignoreMatcher || !ignoreMatcher.find()) {
				contentMatcher.reset(contentsLine);
				result = contentMatcher.find();

				if (result) {
					section = contentMatcher.group(1);
					title = contentMatcher.group(2);
					pageValue = contentMatcher.group(3);

					bookmark = new HashMap<String, Object>();
					bookmark.put("Action", "GoTo");
					bookmark.put("Title", title);

					if (Character.isDigit(pageValue.charAt(0))) {
						pageNumber = Integer.parseInt(pageValue) + pageZero;
					} else {
						pageNumber = RomanNumeral.parseInteger(pageValue) + pageRomanZero;
					}

					// Hmm? Legacy comment whose meaning is missing.
					// Page=108 FitH 794
					//
					// The page number format is weird.
					//
					// TODO: Investigate this and document it better.
					// Also, can we link inside a page, and would we
					// want to?

					bookmark.put("Page", Integer.toString(pageNumber) + " XYZ 0 792 0.0");
					level = null == section ? 1 : 0 == section.length() ? 1 : (section.length() + 1) / 2;

					bookmarkCollector.addBookmark(bookmark, level);
				}
			}
		}

		// If the last bookmark was not at the top level, the bookmark stack needs
		// to be unwound.

		bookmarkCollector.unwindBookmarks();

		return bookmarkCollector.getBookmarks();
	}
}
