package info.zuyfun.bot.utils;

import java.util.regex.Pattern;

public class Validation {
	public boolean checkPattern(String pattern, String text) {
		return Pattern.matches(pattern, text);
	}
}
