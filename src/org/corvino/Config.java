package org.corvino;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Config {
    boolean inSectionMapping;
    boolean inCustomBookmarks;

	int contentsStartPage;
	int contentsEndPage;
	int pageZero;
	int pageRomanZero;
	String contentPattern;
	String ignorePattern;
    Map<String, Integer> sectionMapping;
    List<Bookmark> customBookmarks;

	public Config(String filename) throws ParserConfigurationException, SAXException, IOException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();

		DefaultHandler handler = new DefaultHandler() {

			StringBuffer accumulator = new StringBuffer();

			public void startElement(String uri, String localName,
									 String qName, Attributes attributes)
			throws SAXException {
				if ("table-of-contents".equalsIgnoreCase(qName)) {

					try {
						contentsStartPage = Integer.parseInt(attributes.getValue("startPage").trim());
					} catch (NumberFormatException nfe) {
						throw new IllegalArgumentException("table-of-contents startPage must be an integer");
					}

					try {
						contentsEndPage = Integer.parseInt(attributes.getValue("endPage").trim());
					} catch (NumberFormatException nfe) {
						throw new IllegalArgumentException("table-of-contents endPage must be an integer");
					}

				} else if ("pdf-pages".equalsIgnoreCase(qName)) {

					try {
						pageZero = Integer.parseInt(attributes.getValue("zero").trim());
					} catch (NumberFormatException nfe) {
						throw new IllegalArgumentException("pdf-pages zero must be an integer");
					}

					try {
						pageRomanZero = Integer.parseInt(attributes.getValue("romanZero").trim());
					} catch (NumberFormatException nfe) {
						throw new IllegalArgumentException("pdf-pages zero must be an integer");
					}

				} else if ("section-mapping".equalsIgnoreCase(qName)) {

                    inSectionMapping = true;
                    sectionMapping = new HashMap<String, Integer>();

                }  else if ("custom-bookmarks".equalsIgnoreCase(qName)) {

                    inCustomBookmarks = true;
                    customBookmarks = new ArrayList<Bookmark>();

                } else if ("map".equalsIgnoreCase(qName) && inSectionMapping) {

                    try {
                        sectionMapping.put(attributes.getValue("name").trim(),
                                           Integer.parseInt(attributes.getValue("value").trim()));
                    } catch (NumberFormatException nfe) {
                        throw new IllegalArgumentException("map value must be an integer.");
                    }

                } else if ("bookmark".equalsIgnoreCase(qName) && inCustomBookmarks) {

                    try {
                        customBookmarks.add(new Bookmark(attributes.getValue("name").trim(),
                                                Integer.parseInt(attributes.getValue("level").trim()),
                                                Integer.parseInt(attributes.getValue("page"))));
                    } catch (NumberFormatException nfe) {
                        throw new IllegalArgumentException("bookmark level and page must be an integers.");
                    }

                }
			}

			public void endElement(String uri, String localName,
								   String qName)
			throws SAXException {
				if (qName.equals("content-pattern")) {
					contentPattern = accumulator.toString().trim();
				} else if (qName.equals("ignore-pattern")) {
					ignorePattern = accumulator.toString().trim();
				} else if ("section-mapping".equalsIgnoreCase(qName)) {
                    inSectionMapping = false;
                }  else if ("custom-bookmarks".equalsIgnoreCase(qName)) {
                    inCustomBookmarks = false;
                }

				accumulator.setLength(0);
			}

			public void characters(char ch[], int start, int length)
			throws SAXException {
				accumulator.append(ch, start, length);
			}
        };

		saxParser.parse(filename, handler);

		if (null == contentPattern) {
			throw new IllegalArgumentException("Must define a content pattern.");
		}

		System.out.println("Configuration:");
		System.out.println("  contentPattern: " + contentPattern);
		System.out.println("  ignorePattern: " + ignorePattern);
		System.out.println("  contents start page: " + contentsStartPage);
		System.out.println("  contents end page: " + contentsEndPage);
	}

	public int getContentsStartPage() {
		return contentsStartPage;
	}

	public int getContentsEndPage() {
		return contentsEndPage;
	}

	public int getPageZero() {
		return pageZero;
	}

	public int getPageRomanZero() {
		return pageRomanZero;
	}

	public String getContentPattern() {
		return contentPattern;
	}

	public String getIgnorePattern() {
		return ignorePattern;
	}

    public Map<String, Integer> getSectionMapping() {
        return sectionMapping;
    }

    public List<Bookmark> getCustomBookmarks() {
        return customBookmarks;
    }

    public static class Bookmark {
        private String title;
        private int level;
        private int page;

        Bookmark(String theTitle, int theLevel, int thePage) {
            title = theTitle;
            level = theLevel;
            page = thePage;
        }

        public String getTitle() {
            return title;
        }

        public int getLevel() {
            return level;
        }

        public int getPage() {
            return page;
        }
    }
}
