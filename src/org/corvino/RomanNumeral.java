package org.corvino;

public class RomanNumeral {

	/**
	 * Parses the specified roman numeral string and returns its
	 * value as an integer.
	 */
	public static int parseInteger(String romanNumeral) {
		int		currentDigit;
		
		int		total = 0;
		int		nextDigit = getDigitValue(romanNumeral.charAt(0));
		
		
		for (int i = 0; i < romanNumeral.length() - 1; i++) {
			currentDigit = nextDigit;
			nextDigit = getDigitValue(romanNumeral.charAt(i + 1));
			
			if (currentDigit < nextDigit) {
				total-=currentDigit;
			}
			else {
				total+=currentDigit;
			}
		}
		
		total+=nextDigit;
		
		return total;
	}
	
	private static int getDigitValue(char romanDigit) {
	
		switch (romanDigit) {
			case 'm' :
			case 'M' :
				romanDigit = 1000;
				break;
			case 'd' :
			case 'D' :
				romanDigit = 500;
				break;
			case 'c' :
			case 'C' :
				romanDigit = 100;
				break;
			case 'l' :
			case 'L' :
				romanDigit = 50;
				break;
			case 'x' :
			case 'X' :
				romanDigit = 10;
				break;
			case 'v' :
			case 'V' :
				romanDigit = 5;
				break;
			case 'i' :
			case 'I' :
				romanDigit = 1;
				break;
		}
		
		return romanDigit;
	}
}