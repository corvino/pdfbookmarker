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
	
	private static Pattern fContentLinePattern = Pattern.compile("^(?:((?:[0-9]+\\.)*[0-9]+)\\s*)?(.*?)\\.++((?:[0-9]+?)|(?:[vixc]+?))$");
	private static Pattern fIgnoreLines = Pattern.compile("(?:AppDevMozilla-000Book  Page [cxvi]+  Thursday, December 4, 2003  6:21 PM)|(?:[cxvi]+ Contents)");
	private static Matcher fLineContentMatcher = fContentLinePattern.matcher("");
	private static Matcher fAcuumulatedContentMatcher = fContentLinePattern.matcher("");
	private static Matcher fIgnoreMatcher = fIgnoreLines.matcher("");
	
	public static void main(String args[]) {
		boolean								lineResult;
		boolean								accumulatedResult;
		Document							document;
		Map<String,Object>					bookmark;
		int									numOfPages;
		PdfCopy								writer;
		PdfImportedPage						page;
		PdfReader							reader;
		File								contentsFile;
		File								inputFile;
		File								outputFile;
		BufferedReader						contentsReader;
		String								contentsLine;
		String								accumulatedContent = "";
		String								pageValue;
		int									pageNumber;
		String								title;
		String								section;
		int									pageI;
		int									page0;
		
		List<Map<String, Object>>			poppedBookmarks;
		List<Map<String, Object>>			bookmarks = new ArrayList<Map<String, Object>>();
		Stack<List<Map<String, Object>>>	parents = new Stack<List<Map<String, Object>>>();
		int									level;
		int									currentLevel = 1;
		
		
		try {
			if (5==args.length) {
				inputFile = new File(args[0]);
				outputFile = new File(args[1]);
				contentsFile = new File(args[2]);
				
				if (!inputFile.exists()) {
					throw new IllegalArgumentException("Input file must exist.");
				}
				
				if (!outputFile.getParentFile().exists())
				{
					throw new IllegalArgumentException("Directory of output file must exist.");
				}
				
				if (!contentsFile.exists()) {
					throw new IllegalArgumentException("Contents file must exist.");
				}
				
				try {
					pageI = Integer.parseInt(args[3]);
				}
				catch (NumberFormatException nfe) {
					throw new IllegalArgumentException("Page i must parse to an integer.");
				}
				
				try {
					page0 = Integer.parseInt(args[4]);
				}
				catch (NumberFormatException nfe) {
					throw new IllegalArgumentException("Page 1 must parse to an integer.");
				}
				
				contentsReader = new BufferedReader(new FileReader(contentsFile));
				
				for (contentsLine = contentsReader.readLine(); null != contentsLine; contentsLine = contentsReader.readLine()) {
					contentsLine = contentsLine.trim();
					
					fIgnoreMatcher.reset(contentsLine);
					
					if (!fIgnoreMatcher.find()) {
					
						fLineContentMatcher.reset(contentsLine);
						lineResult = fLineContentMatcher.find();
						
						// So, if we don't match, we accumulate content.  This is problematic,
						// since there are some lines we want to ignore.  Therefore, we only use
						// accumulated content if 1) the line did not macth (we don't expect this
						// to happen), or 2) The line did not define a section, but the accumulated
						// content did.
						
						accumulatedContent = (accumulatedContent.trim() + " " + contentsLine.trim()).trim();
						fAcuumulatedContentMatcher.reset(accumulatedContent);
						accumulatedResult = fAcuumulatedContentMatcher.find();
						
						//if (lineResult && (fLineContentMatcher.group(2).length() > 0)) {
						if ((lineResult && (fLineContentMatcher.group(2).length() > 0)) ||
								(accumulatedResult && (fAcuumulatedContentMatcher.group(2).length() > 0)))
						{
							
							// This is a real match.  So far the regular expression has
							// resisted only matching when there is a title., so we check
							// for title length as well as a match.
							
							accumulatedContent = "";
							
							if (null != fAcuumulatedContentMatcher.group(1) && null == fLineContentMatcher.group(1))
							{
								section = fAcuumulatedContentMatcher.group(1);
								title = fAcuumulatedContentMatcher.group(2);
								pageValue = fAcuumulatedContentMatcher.group(3);					
							} else {
								section = fLineContentMatcher.group(1);
								title = fLineContentMatcher.group(2);
								pageValue = fLineContentMatcher.group(3);
							}
							
							bookmark = new HashMap<String, Object>();
							bookmark.put("Action", "GoTo");
							bookmark.put("Title", title);
							
							if (Character.isDigit(pageValue.charAt(0))) {
								pageNumber = Integer.parseInt(pageValue) + page0;
							}
							else {
								pageNumber = RomanNumeral.parseInteger(pageValue) + pageI;
							}
							
							bookmark.put("Page", Integer.toString(pageNumber) + " XYZ 0 792 0.0"); // Page=108 FitH 794
							
							
							level = null == section ? 1 : 0 == section.length() ? 1 : (section.length() + 1) / 2;
							
							if (level == currentLevel) {
								bookmarks.add(bookmark);
							}
							else if (level > currentLevel) {
								parents.push(bookmarks);
								bookmarks = new ArrayList<Map<String, Object>>();
								bookmarks.add(bookmark);
								
								if (level-currentLevel > 1) {
									
									// This is a weird and dangerous situation.  The bookmark
									// heirachy is trying to pop down more than one level at
									// once, without intervening parents.  This will break the
									// world!  So, don't allow it--force it to only step down
									// one level.
									
									level = currentLevel + 1;
								}
							}
							else if (level < currentLevel) {
								for (int i = 0; i < currentLevel - level; i++) {
									poppedBookmarks = parents.pop();
									poppedBookmarks.get(poppedBookmarks.size() - 1).put("Kids", bookmarks);
									bookmarks = poppedBookmarks;
								}
								
								bookmarks.add(bookmark);
							}
							
							currentLevel = level;
						}
					}
					else
					{
						lineResult = false;
					}
				}
				
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
		catch(IOException ioe) {
			
		}
		catch(DocumentException de) {
			
		}
		catch(NumberFormatException nfe) {
			
		}
	}
}
