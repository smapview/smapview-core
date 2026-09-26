package com.smapview.view;

class JoinValue {
	
	final String value;
	
	final short joinId;
	
	JoinValue(String value, short joinId) {
		this.value = value;
		this.joinId = joinId;
	}

	@Override
	public int hashCode() {
		int result = 17; 
	    result = 31 * result + value.hashCode();
	    result = 31 * result + joinId;
	    return result;
	}
	
	@Override
	public boolean equals(Object object) {
		JoinValue jval = (JoinValue)object;
		return value.equals(jval.value)
				&& joinId == jval.joinId;
	}
	
	@Override
	public String toString() {
		return String.format("[%d,%s]", joinId, value);
	}
	
}