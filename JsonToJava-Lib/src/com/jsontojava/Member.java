package com.jsontojava;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.modeshape.common.text.Inflector;

public class Member {
	private Set<String> mModifiers;
	private String mFieldName;
	private String mJsonField;
	private String mType;
	private String mName;

	public static class Builder {
		private static final Inflector mInflector = new Inflector();
		private Set<String> mModifiers;
		private String mFieldConstantName;
		private String mJsonField;
		private String mType;
		private String mName;
		private boolean mPlural;

		public Builder() {
			mPlural = false;
			mModifiers = new HashSet<String>();
		}

		public Member.Builder setPlural(){
			mPlural = true;
			if(mType != null){
				mType = "List<"+mType+">";
			}
			if(mName != null){
				mName = mInflector.pluralize(mName);
			}
			return this;
		}
		public Member.Builder setName(String name) {
			name = "m" + name;
			if(mPlural){
				mName = mInflector.pluralize(name);
			}else{
				mName = name;
			}
			return this;
		}

		public Member.Builder setType(String type) {
			if(mPlural){
				mType = "List<" + type + ">";

			}else{
				mType = type;
			}
			return this;
		}

		public Member.Builder setJsonField(String jsonField) {
			mJsonField = jsonField;
			mFieldConstantName = "FIELD_" + mInflector.underscore(jsonField).toUpperCase();
			return this;
		}

		public Member.Builder addModifier(String modifier) {
			mModifiers.add(modifier);
			return this;
		}
		
		public Member build(){
			Member member = new Member();
			member.setName(mName);
			member.setType(mType);
			member.setFieldName(mFieldConstantName);
			member.setJsonField(mJsonField);
			member.mModifiers = mModifiers;
			return member;
			
		}


	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Member) {

			return ((Member) obj).getName().equals(getName());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	public String getGetterSignature() {
		StringBuilder sBuilder = new StringBuilder();
		String methodName = StringUtils.removeStart(getName(), "m");

		String setPrefix = "get";
		try{
		if (getType().equals("boolean")) {
			setPrefix = "is";
		}
		}catch (NullPointerException e){
			e.printStackTrace();
		}

		sBuilder.append(setPrefix).append(methodName).append("()");
		return sBuilder.toString();
	}

	public String getSetter(Inflector inflector) {
		StringBuilder sBuilder = new StringBuilder();
		String methodName = StringUtils.removeStart(getName(), "m");
		String nameNoPrefix = inflector.camelCase(methodName, false);
		sBuilder.append("    public void set").append(methodName).append("(").append(getType()).append(" ")
				.append(nameNoPrefix).append(") {\n        ").append(getName()).append(" = ").append(nameNoPrefix)
				.append(";").append("\n    }\n\n");
		return sBuilder.toString();
	}

	public String getGetter() {
		StringBuilder sBuilder = new StringBuilder();

		sBuilder.append("    public ").append(getType()).append(" ").append(getGetterSignature())
				.append(" {\n        return ").append(getName()).append(";\n    }\n\n");
		return sBuilder.toString();
	}

	public String getFieldName() {
		return mFieldName;
	}

	public void setFieldName(String fieldName) {
		this.mFieldName = fieldName;
	}

	public String getJsonField() {
		return mJsonField;
	}

	public void setJsonField(String jsonField) {
		this.mJsonField = jsonField;
	}

	public String getType() {
		return mType;
	}

	public void setType(String type) {
		this.mType = type;
	}

	public String getName() {
		return mName;
	}

	public void setName(String name) {
		this.mName = name;
	}
}