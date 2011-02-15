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
    boolean inContents;

    boolean accumulate;
	int contentsStartPage;
	int contentsEndPage;
	int pageZero;
	int pageRomanZero;
	String contentPattern;
	String ignorePattern;

    int maskSectionLevel;
    int maskLevel;
    List<Bookmark> contents;

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

                    accumulate = "true".equals(attributes.getValue("accumulate"));

                    contentsStartPage = Config.parseInt(attributes.getValue("startPage"),
                                                        "table-of-contents startPage must be an integer");
                    contentsEndPage = Config.parseInt(attributes.getValue("endPage"),
                                                      "table-of-contents endPage must be an integer");
                } else if ("contents".equalsIgnoreCase(qName)) {
                    String maskSection = attributes.getValue("maskSectionLevel");
                    String mask = attributes.getValue("maskLevel");

                    maskSectionLevel = (null == maskSection) ? 0 : Config.parseInt(maskSection, "contents maskSectionLevel must be an int.");
                    maskLevel = (null == mask) ? Integer.MAX_VALUE : Config.parseInt(mask, "contents maskLevel must be an int.");
                    contents = new ArrayList<Bookmark>();

                    inContents = true;
                } else if ("contents-item".equalsIgnoreCase(qName)) {
                    int level;
                    String name = attributes.getValue("name").trim();
                    int page = Config.parseInt(attributes.getValue("page"), "bookmark '" + name + "' page must be an integer.");
                    String section = attributes.getValue("section");
                    String levelAttrib = attributes.getValue("level");


                    if (null != levelAttrib) {
                        level = Config.parseInt(attributes.getValue("level"), "bookmark level must be an integer.");
                    } else if (null != section) {
                        level = AddPDFBookmarks.levelForSection(section);
                    } else {
                        throw new IllegalArgumentException("contents-item must have a section or level.");
                    }

                    if (level < maskLevel) {
                        if (null != section && level < maskSectionLevel) {
                            name = section.trim() + " " + name;
                        }

                        contents.add(new Bookmark(name, level, page));
                    } else {
                        System.out.println(name = section.trim() + " " + name);
                    }
				} else if ("pdf-pages".equalsIgnoreCase(qName)) {
                    pageZero = Config.parseInt(attributes.getValue("zero"),
                                               "pdf-pages zero must be an integer");
                    pageRomanZero= Config.parseInt(attributes.getValue("romanZero"),
                                                   "pdf-pages zero must be an integer");
				} else if ("section-mapping".equalsIgnoreCase(qName)) {
                    inSectionMapping = true;
                    sectionMapping = new HashMap<String, Integer>();
                }  else if ("custom-bookmarks".equalsIgnoreCase(qName)) {
                    inCustomBookmarks = true;
                    customBookmarks = new ArrayList<Bookmark>();
                } else if ("map".equalsIgnoreCase(qName) && inSectionMapping) {
                    sectionMapping.put(attributes.getValue("name").trim(),
                                       Config.parseInt(attributes.getValue("value"),
                                                       "map value must be an integer."));
                } else if ("bookmark".equalsIgnoreCase(qName) && inCustomBookmarks) {
                    customBookmarks.add(new Bookmark(attributes.getValue("name").trim(),
                                                     Config.parseInt(attributes.getValue("level"),
                                                                     "bookmark level must be an integer."),
                                                     Config.parseInt(attributes.getValue("page"),
                                                                     "bookmark page must be an integer.")));
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

        if (null == contents) {
            if (null == contentPattern) {
                throw new IllegalArgumentException("Must define contents or a content pattern.");
            }

            System.out.println("Configuration:");
            System.out.println("  contentPattern: " + contentPattern);
            System.out.println("  ignorePattern: " + ignorePattern);
            System.out.println("  contents start page: " + contentsStartPage);
            System.out.println("  contents end page: " + contentsEndPage);
            System.out.println("  accumulate: " + accumulate);
        }
	}

    public boolean getAccumulate() {
        return accumulate;
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

    public List<Bookmark> getContents() {
        return contents;
    }

    public Map<String, Integer> getSectionMapping() {
        return sectionMapping;
    }

    public List<Bookmark> getCustomBookmarks() {
        return customBookmarks;
    }

    public static int parseInt(String value, String errorMessage) {
        int retval;

		try {
			retval = Integer.parseInt(value.trim());
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException(errorMessage);
		}

        return retval;
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
