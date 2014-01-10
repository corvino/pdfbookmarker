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
	int defaultLevel;
	int pageZero;
	int pageRomanZero;
	String contentPattern;
	String ignorePattern;

    int maskSectionLevel;
    int maskLevel;
    List<Bookmark> contents;

    Map<String, Integer> sectionLevelMapping;
    Map<String, String> sectionNameMapping;
    List<Bookmark> leadingBookmarks;
    List<Bookmark> trailingBookmarks;
    Map<String, List<Bookmark>> titleFollowingBookmarks;

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

                    String defaultLevelString = attributes.getValue("default-level");
                    defaultLevel = (null == defaultLevelString) ? 1 : Config.parseInt(defaultLevelString, "default-level specified must be an integer");
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
                        level = section.split("\\.").length;
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
                    sectionLevelMapping = new HashMap<String, Integer>();
                    sectionNameMapping = new HashMap<String, String>();
                }  else if ("custom-bookmarks".equalsIgnoreCase(qName)) {
                    inCustomBookmarks = true;
                    leadingBookmarks = new ArrayList<Bookmark>();
                    trailingBookmarks = new ArrayList<Bookmark>();
                    titleFollowingBookmarks = new HashMap<String, List<Bookmark>>();
                } else if ("map".equalsIgnoreCase(qName) && inSectionMapping) {
                    String name = attributes.getValue("name").trim();

                    String levelText = attributes.getValue("level");
                    if (null != levelText) {
                        sectionLevelMapping.put(name,
                                Config.parseInt(levelText,
                                        "map value must be an integer."));
                    }

                    String renameText = attributes.getValue("rename");
                    if (null != renameText) {
                        sectionNameMapping.put(name, renameText);
                    }
                } else if ("bookmark".equalsIgnoreCase(qName) && inCustomBookmarks) {
                    String followingTitle = attributes.getValue("following-title");
                    String position = attributes.getValue("position");

                    if (null != followingTitle) {
                        Bookmark followingBookmark = new Bookmark(attributes.getValue("name").trim(),
                                Config.parseInt(attributes.getValue("level"),
                                        "bookmark level must be an integer."),
                                Config.parseInt(attributes.getValue("page"),
                                        "bookmark page must be an integer."));
                        List<Bookmark> followingBookmarks = titleFollowingBookmarks.get(followingTitle);

                        if (null == followingBookmarks) {
                            followingBookmarks = new ArrayList<Bookmark>();
                            titleFollowingBookmarks.put(followingTitle, followingBookmarks);
                        }
                        followingBookmarks.add(followingBookmark);
                    } else if ("trailing".equals(position)) {
                        trailingBookmarks.add(new Bookmark(attributes.getValue("name").trim(),
                                Config.parseInt(attributes.getValue("level"),
                                        "bookmark level must be an integer."),
                                Config.parseInt(attributes.getValue("page"),
                                        "bookmark page must be an integer.")));
                    } else if ("leading".equals(position) || null == position) {
                        leadingBookmarks.add(new Bookmark(attributes.getValue("name").trim(),
                                Config.parseInt(attributes.getValue("level"),
                                        "bookmark level must be an integer."),
                                Config.parseInt(attributes.getValue("page"),
                                        "bookmark page must be an integer.")));
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

    public boolean getAccumulate() { return accumulate; }
    public int getContentsStartPage() { return contentsStartPage; }
    public int getContentsEndPage() { return contentsEndPage; }
    public int getDefaultLevel () { return defaultLevel; }
    public int getPageZero() { return pageZero; }
    public int getPageRomanZero() { return pageRomanZero; }
    public String getContentPattern() { return contentPattern; }
    public String getIgnorePattern() { return ignorePattern; }
    public List<Bookmark> getContents() { return contents; }
    public Map<String, Integer> getSectionLevelMapping() { return sectionLevelMapping; }
    public Map<String, String> getSectionNameMapping() { return sectionNameMapping; }
    public List<Bookmark> getLeadingBookmarks() { return leadingBookmarks; }
    public List<Bookmark> getTrailingBookmarks() {return trailingBookmarks; }
    public Map<String, List<Bookmark>> getTitleFollowingBookmarks() { return titleFollowingBookmarks; }

    public static String showCodepoints(String string) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < string.length(); i++) {
            buffer.append(string.codePointAt(i));
            if (i +1 < string.length()) {
                buffer.append(" / ");
            }
        }

        return buffer.toString();
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
