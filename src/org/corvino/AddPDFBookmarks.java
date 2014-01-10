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
					parseContents(args[1], args[2], false);
				}
            } else if ("parse-contents-detailed".equals(command)) {
                if (3 != args.length) {
                    System.err.println("Usage: parse-contents-detailed <config-file> <pdf-file>");
                } else {
                    parseContents(args[1], args[2], true);
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

	private static void parseContents(String configFile, String filename, boolean detailed)
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
            String title = (String) bookmark.get("Title");

			for (int i = 0; i < level; i++) {
				System.out.print("  ");
			}

            if (detailed) {
                System.out.println(title + " (" + Config.showCodepoints(title) + ") " + " : " + bookmark.get("Page"));
            } else {
			    System.out.println(title + " : " + bookmark.get("Page"));
            }

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

    private static HashMap<String, Object> getBookmark(String title, int pageNumber) {
		HashMap<String, Object> bookmark = new HashMap<String, Object>();

		bookmark.put("Action", "GoTo");
        bookmark.put("Title", title);
        bookmark.put("Page", pageNumber + " XYZ 0 792 0.0");

        return bookmark;
    }

	private static List<HashMap<String, Object>> getBookmarks(Config config, String pdfFile)
		throws IOException
    {
        List<HashMap<String, Object>> bookmarks;

        bookmarks = getBookmarksFromConfig(config);

        if (null == bookmarks) {
            bookmarks = getBookmarksFromPDF(config, pdfFile);
        }

        return bookmarks;
    }

    private static List<HashMap<String, Object>> getBookmarksFromConfig(Config config)
    {
        List<HashMap<String, Object>> bookmarks = null;
        List<Config.Bookmark> contents = config.getContents();

        if (null != contents) {
            HashMap<String, Object> bookmark;
            HierarchicalBookmarkCollector bookmarkCollector = new HierarchicalBookmarkCollector();

            for (Config.Bookmark custom : config.getContents()) {
                bookmark = getBookmark(custom.getTitle(), custom.getPage());
				bookmarkCollector.addBookmark(bookmark, custom.getLevel());
            }

            bookmarkCollector.unwindBookmarks();
            bookmarks = bookmarkCollector.getBookmarks();
        }

        return bookmarks;
    }

	private static List<HashMap<String, Object>> getBookmarksFromPDF(Config config, String pdfFile)
		throws IOException
	{
		boolean result;
		HashMap<String, Object> bookmark;
		int level;
		int pageNumber;

		String contentsLine;
		String pageValue;
		String section;
		String title;

        StringBuffer accumulator = new StringBuffer();
        boolean accumulate = config.getAccumulate();
		int pageZero = config.getPageZero();
		int pageRomanZero = config.getPageRomanZero();

		String contentRegEx = config.getContentPattern();
		Pattern contentPattern = Pattern.compile(contentRegEx);
		Matcher contentMatcher = contentPattern.matcher("");
		String ignoreRegEx = config.getIgnorePattern();
		Matcher ignoreMatcher = null;

		HierarchicalBookmarkCollector bookmarkCollector = new HierarchicalBookmarkCollector();
		String contents = getContents(config, pdfFile);
		BufferedReader contentsReader = new BufferedReader(new StringReader(contents));

        Map<String, Integer> sectionLevelMapping = config.getSectionLevelMapping();
        Map<String, String> sectionNameMapping = config.getSectionNameMapping();
        List<Config.Bookmark> leadingBookmarks = config.getLeadingBookmarks();
        List<Config.Bookmark> trailingBookmarks = config.getTrailingBookmarks();
        Map<String, List<Config.Bookmark>> titleFollowingBookmarks = config.getTitleFollowingBookmarks();


		System.out.println("Content regex: " + contentRegEx);

        // Add leading custom bookmarks from config.

        if (null != leadingBookmarks) {
            for (Config.Bookmark leadingBookmark : config.getLeadingBookmarks()) {
                bookmark = getBookmark(leadingBookmark.getTitle(), leadingBookmark.getPage());
                bookmarkCollector.addBookmark(bookmark, leadingBookmark.getLevel());
            }
        }

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
                if (accumulate) {
                    if (0 != accumulator.length()) {
                        accumulator.append(" ");
                    }
                    accumulator.append(contentsLine);
                    contentMatcher.reset(accumulator);
                } else {
                    contentMatcher.reset(contentsLine);
                }

                result = contentMatcher.find();

				if (result) {
					section = contentMatcher.group(1);
					title = contentMatcher.group(2).trim();
					pageValue = contentMatcher.group(3).trim();
                    level = config.defaultLevel;
                    accumulator.setLength(0);

                    if (null != sectionLevelMapping && sectionLevelMapping.containsKey(title)) {
                        level = sectionLevelMapping.get(title);
                    } else if (null != section && 0 < section.length()) {
                        level = section.split("\\.").length;
                    }

                    if (null != sectionNameMapping && sectionNameMapping.containsKey(title)) {
                        title = sectionNameMapping.get(title);
                    }

					if (Character.isDigit(pageValue.charAt(0))) {
						pageNumber = Integer.parseInt(pageValue) + pageZero;
					} else {
						pageNumber = RomanNumeral.parseInteger(pageValue) + pageRomanZero;
					}

                    bookmark = getBookmark(title, pageNumber);
					bookmarkCollector.addBookmark(bookmark, level);

                    if (null != titleFollowingBookmarks) {
                        if (titleFollowingBookmarks.containsKey(title)) {
                            for (Config.Bookmark followingBookmark : titleFollowingBookmarks.get(title))
                                bookmarkCollector.addBookmark(getBookmark(followingBookmark.getTitle(), followingBookmark.getPage()), followingBookmark.getLevel());
                        }
                    }
				}
			}
		}

        // Add trailing custom bookmarks from config.

        if (null != trailingBookmarks) {
            for (Config.Bookmark tralingBookmark : config.getTrailingBookmarks()) {
                bookmark = getBookmark(tralingBookmark.getTitle(), tralingBookmark.getPage());
                bookmarkCollector.addBookmark(bookmark, tralingBookmark.getLevel());
            }
        }

		// If the last bookmark was not at the top level, the bookmark stack needs
		// to be unwound.

		bookmarkCollector.unwindBookmarks();

		return bookmarkCollector.getBookmarks();
	}
}
