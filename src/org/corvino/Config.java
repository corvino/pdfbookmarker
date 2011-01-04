package org.corvino;


import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Config {
	int contentsStartPage;
	int contentsEndPage;
	int pageZero;
	int pageRomanZero;
	String contentPattern;
	String ignorePattern;

	public Config(String filename) throws ParserConfigurationException, SAXException, IOException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();

		DefaultHandler handler = new DefaultHandler() {

			StringBuffer accumulator = new StringBuffer();

			public void startElement(String uri, String localName,
									 String qName, Attributes attributes)
			throws SAXException {
				if (qName.equalsIgnoreCase("table-of-contents")) {
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
				} else if (qName.equalsIgnoreCase("pdf-pages")) {
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
				}
			}

			public void endElement(String uri, String localName,
								   String qName)
			throws SAXException {
				if (qName.equals("content-pattern")) {
					contentPattern = accumulator.toString().trim();
				} else if (qName.equals("ignore-pattern")) {
					ignorePattern = accumulator.toString().trim();
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
 }
