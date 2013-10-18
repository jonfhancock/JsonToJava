package com.jsontojava;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.modeshape.common.text.Inflector;

public class Member {
	private Set<String> mModifiers;
	public String fieldName;
	public String jsonField;
	public String type;
	public String name;

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
			member.name = mName;
			member.type = mType;
			member.fieldName = mFieldConstantName;
			member.jsonField = mJsonField;
			member.mModifiers = mModifiers;
			return member;
			
		}


	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Member) {

			return ((Member) obj).name.equals(name);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	public String getGetterSignature() {
		StringBuilder sBuilder = new StringBuilder();
		String methodName = StringUtils.removeStart(name, "m");

		String setPrefix = "get";
		try{
		if (type.equals("boolean")) {
			setPrefix = "is";
		}
		}catch (NullPointerException e){
			e.printStackTrace();
		}

		sBuilder.append(setPrefix).append(methodName).append("()");
		return sBuilder.toString();
	}

	public String getSetter() {
		StringBuilder sBuilder = new StringBuilder();
		String methodName = StringUtils.removeStart(name, "m");
		String nameNoPrefix = Main.mInflector.camelCase(methodName, false);
		sBuilder.append("    public void set").append(methodName).append("(").append(type).append(" ")
				.append(nameNoPrefix).append(") {\n        ").append(name).append(" = ").append(nameNoPrefix)
				.append(";").append("\n    }\n\n");
		return sBuilder.toString();
	}

	public String getGetter() {
		StringBuilder sBuilder = new StringBuilder();

		sBuilder.append("    public ").append(type).append(" ").append(getGetterSignature())
				.append(" {\n        return ").append(name).append(";\n    }\n\n");
		return sBuilder.toString();
	}
}