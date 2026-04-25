package com.smapview.core.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegularPattern {

	final Pattern pattern;
	
	public RegularPattern(String regex) {
		this.pattern = Pattern.compile(regex);
	}
	
	public void check(String str) {
		if (str == null || !pattern.matcher(str).matches()) {
			throw new IllegalArgumentException();
		}
	}

	public String[] getGroups(String str) {
		Matcher m = pattern.matcher(str);
		if (!m.matches()) throw new IllegalArgumentException();
		String[] groups = new String[m.groupCount()];
		for (int i=0; i<m.groupCount(); i++) groups[i] = m.group(i+1);
		return groups;
	}

	public String getGroup(String str, int group) {
		Matcher m = pattern.matcher(str);
		if (!m.matches()) throw new IllegalArgumentException();
		return m.group(group);
	}

}
