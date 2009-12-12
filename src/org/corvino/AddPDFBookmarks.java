package org.corvino;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;

public class AddPDFBookmarks {

	private static Pattern fContentPattern = Pattern.compile("^(?:((?:[0-9]+\\.)*[0-9]+)\\s*)?(.*?)\\.++\\s?((?:[0-9]+?)|(?:[vixc]+?))$");
	private static Pattern fIgnorePattern = Pattern.compile("(?:AppDevMozilla-000Book Page [cxvi]+ Thursday, December 4, 2003 6:21 PM)|(?:[cxvi]+ Contents)|(?:Contents [cxvi]+)");
	private static Matcher fContentMatcher = fContentPattern.matcher("");
	private static Matcher fIgnoreMatcher = fIgnorePattern.matcher("");

	public static void main(String args[]) {
		File								contentsFile;
		File								inputFile;
		File								outputFile;
		int									pageI;
		int									page0;
		
		
		try {
			if (5==args.length) {
				inputFile = new File(args[0]);
				outputFile = new File(args[1]);
				contentsFile = new File(args[2]);
				
				if (!inputFile.exists()) {
					throw new IllegalArgumentException("Input file must exist.");
				}

				if (!outputFile.getParentFile().exists()) {
					throw new IllegalArgumentException("Directory of output file must exist.");
				}
				
				if (!contentsFile.exists()) {
					throw new IllegalArgumentException("Contents file must exist.");
				}
				
				try {
					pageI = Integer.parseInt(args[3]);
				} catch (NumberFormatException nfe) {
					throw new IllegalArgumentException("Page i must parse to an integer.");
				}
				
				try {
					page0 = Integer.parseInt(args[4]);
				} catch (NumberFormatException nfe) {
					throw new IllegalArgumentException("Page 1 must parse to an integer.");
				}

				addBookmarks(inputFile.getPath(), outputFile.getPath(), contentsFile.getPath(), pageI, page0);

			}
		} catch(IOException ioe) {
			
		} catch(DocumentException de) {

		} catch(NumberFormatException nfe) {
			
		}
	}

	private static void addBookmarks(String inputFilePath, String outputFilePath, String contentsFilePath, int pageI, int page0) throws IOException, DocumentException {
		boolean								result;
		Document							document;
		Map<String,Object>					bookmark;
		int									numOfPages;
		PdfCopy								writer;
		PdfImportedPage						page;
		PdfReader							reader;
		BufferedReader						contentsReader;
		String								contentsLine;
		String								accumulatedContent = "";
		String								pageValue;
		int									pageNumber;
		String								title;
		String								section;
		int									level;

		List<Map<String, Object>>			poppedBookmarks;
		List<Map<String, Object>>			bookmarks = new ArrayList<Map<String, Object>>();
		Stack<List<Map<String, Object>>>	parents = new Stack<List<Map<String, Object>>>();
		int									currentLevel = 1;
		File								contentsFile = new File(contentsFilePath);
		File								inputFile = new File(inputFilePath);
		File								outputFile = new File(outputFilePath);


		contentsReader = new BufferedReader(new FileReader(contentsFile));

		for (contentsLine = contentsReader.readLine(); null != contentsLine; contentsLine = contentsReader.readLine()) {
			contentsLine = contentsLine.trim();

			fIgnoreMatcher.reset(contentsLine);

			if (!fIgnoreMatcher.find()) {

				// Accumulate lines content to handle multi-line items, and then match to see
				// if we have completed a contents item yet.

				accumulatedContent = (accumulatedContent.trim() + " " + contentsLine.trim()).trim();
				fContentMatcher.reset(accumulatedContent);
				result = fContentMatcher.find();

				if (result && (fContentMatcher.group(2).length() > 0)) {

					// This is a real match.  So far the regular expression has
					// resisted only matching when there is a title., so we check
					// for title length as well as a match.

					section = fContentMatcher.group(1);
					title = fContentMatcher.group(2);
					pageValue = fContentMatcher.group(3);

					bookmark = new HashMap<String, Object>();
					bookmark.put("Action", "GoTo");
					bookmark.put("Title", title);

					if (Character.isDigit(pageValue.charAt(0))) {
						pageNumber = Integer.parseInt(pageValue) + page0;
					} else {
						pageNumber = RomanNumeral.parseInteger(pageValue) + pageI;
					}

					bookmark.put("Page", Integer.toString(pageNumber) + " XYZ 0 792 0.0"); // Page=108 FitH 794

					level = null == section ? 1 : 0 == section.length() ? 1 : (section.length() + 1) / 2;

					if (level == currentLevel) {
						bookmarks.add(bookmark);
					} else if (level > currentLevel) {
						parents.push(bookmarks);
						bookmarks = new ArrayList<Map<String, Object>>();
						bookmarks.add(bookmark);

						if (level - currentLevel > 1) {

							// This is a weird--it means that the bookmark level is
							// descending multiple levels at once.  This doesn't
							// make sense, and also isn't really feasible; it also
							// causes things to go haywire.  Force it to only descend
							// one level.

							level = currentLevel + 1;
						}
					} else if (level < currentLevel) {
						for (int i = 0; i < currentLevel - level; i++) {
							poppedBookmarks = parents.pop();
							poppedBookmarks.get(poppedBookmarks.size() - 1).put("Kids", bookmarks);
							bookmarks = poppedBookmarks;
						}

						bookmarks.add(bookmark);
					}

					currentLevel = level;
					accumulatedContent = "";
				}
			}
		}

		System.out.println("Level: " + currentLevel);

		// If the last bookmark is not at the top level, the bookmark stack needs
		// to be unwound.

		// This is code duplication, really, and shows a need for an object to
		// encapsulate the bookmark collection logic.  But that is for another
		// day...

		while (1 < currentLevel) {
			poppedBookmarks = parents.pop();

			if (poppedBookmarks.size() > 0) {
				poppedBookmarks.get(poppedBookmarks.size() -1).put("Kids", bookmarks);
				bookmarks = poppedBookmarks;
			}

			currentLevel--;
		}

		System.out.println("Level: " + currentLevel);

		reader = new PdfReader(inputFile.getPath());
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
}
