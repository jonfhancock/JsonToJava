package com.jsontojava;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.modeshape.common.text.Inflector;

public class NewType {
	private static final String PUBLIC_STATIC_FINAL = "public static final ";
	private static final String ONE_TAB = "    ";
	private static final String TWO_TABS = ONE_TAB+ONE_TAB;
	public static final String IMPORT_JAVA_UTIL_LIST = "java.util.List";
	public static final String IMPORT_JAVA_UIIL_ARRAYLIST = "java.util.ArrayList";
	public static final String IMPORT_ANDROID_OS_PARCELABLE = "android.os.Parcelable";
	public static final String IMPORT_ANDROID_OS_PARCEL = "android.os.Parcel";
	public static final String IMPORT_GSON_SERIALIZED_NAME = "com.google.gson.annotations.SerializedName";
	public String name;
	public String pack;
	public Set<String> imports;
	public Set<Member> members;
	
	private Inflector mInflector;

	public NewType(Inflector inflector) {
		mInflector = inflector;
		members = new HashSet<Member>();
	}

	public static class Builder {
		private String mName;
		private String mPackage;
		private Set<String> mImports;
		private Set<Member> mMembers;
		private Inflector mInflector;

		public Builder(Inflector inflector) {
			mImports = new HashSet<String>();
			mMembers = new HashSet<Member>();
			mInflector = inflector;
		}

		public NewType.Builder setName(String name) {
			this.mName = name;
			return this;
		}

		public NewType.Builder setPackage(String pack) {
			this.mPackage = pack;
			return this;
		}

		public NewType.Builder addImport(String imp) {
			mImports.add(imp);
			return this;
		}

		public NewType.Builder addImports(Set<String> imports) {
			this.mImports.addAll(imports);
			return this;
		}

		public NewType.Builder addMember(Member member) {
			mMembers.add(member);
			return this;
		}

		public NewType.Builder addMembers(Set<Member> members) {
			this.mMembers.addAll(members);
			return this;
		}

		public NewType build() {
			NewType type = new NewType(mInflector);
			type.name = mName;
			type.pack = mPackage;
			type.imports = mImports;
			type.members = mMembers;
			return type;
		}
	}

	public String toString() {
		return toPojoString(EnumSet.noneOf(OutputOption.class),null);
	}

	public String getColumns(){
		String pluralizedName = mInflector.pluralize(name);
		String underscorePlural = mInflector.underscore(pluralizedName);
		String underscore = mInflector.underscore(name);

		StringBuilder sb = new StringBuilder();
		sb.append("public static class ").append(name).append(" implements BaseColumns {\n");
		
		// Content mime types
		sb.append(ONE_TAB).append(PUBLIC_STATIC_FINAL).append("String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + /vnd.contentrovider." );
		sb.append(mInflector.pluralize(underscore.toLowerCase())).append(";\n\n");
		sb.append(ONE_TAB).append(PUBLIC_STATIC_FINAL).append("String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + /vnd.contentrovider." );
		sb.append(underscore.toLowerCase()).append(";\n\n");
		
		// Content URIs
	
		sb.append(ONE_TAB).append(PUBLIC_STATIC_FINAL).append("Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_").append(underscorePlural.toUpperCase()).append(").build();\n\n");
		if(members.contains("mId") || members.contains("mUniqueId")){
			sb.append(ONE_TAB).append(PUBLIC_STATIC_FINAL).append("Uri CONTENT_BY_ID = BASE_CONTENT_URI.buildUpon().appendPath(PATH_").append(underscorePlural.toUpperCase()).append("_BY_ID).build();\n\n");
		}
		
		// Table name
		sb.append(ONE_TAB).append(PUBLIC_STATIC_FINAL).append("String TABLE_NAME = \"").append(underscore.toLowerCase()).append("\";\n\n");
		
		// column names
		for(Member member:members){
			String simpleName = StringUtils.removeStart(member.getName(), "m");
			String underscoreMember = mInflector.underscore(simpleName);
			sb.append(ONE_TAB).append(PUBLIC_STATIC_FINAL).append("String COLUMN_NAME_").append(underscoreMember.toUpperCase()).append(" = \"").append(underscoreMember.toLowerCase()).append("\";\n\n");
		}
		
		sb.append("}\n");
		return sb.toString();
	}
	
	public String toPojoString(EnumSet<OutputOption> options,JsonToJava jsonToJava) {
		if (options.contains(OutputOption.PARCELABLE)) {
			imports.add(IMPORT_ANDROID_OS_PARCEL);
			imports.add(IMPORT_ANDROID_OS_PARCELABLE);
			if (imports.contains(IMPORT_JAVA_UTIL_LIST)) {
				imports.add(IMPORT_JAVA_UIIL_ARRAYLIST);
			}
		}
		if (options.contains(OutputOption.GSON)) {
			imports.add(IMPORT_GSON_SERIALIZED_NAME);
		}
		StringBuilder sBuilder = new StringBuilder();

		sBuilder.append("package ").append(pack).append(";\n\n");
		for (String s : imports) {
			sBuilder.append("import ").append(s).append(";\n");
		}
		sBuilder.append("\n\n");
		sBuilder.append("public class ").append(name);
		if (options.contains(OutputOption.PARCELABLE)) {
			sBuilder.append(" implements Parcelable");
		}
		sBuilder.append("{\n\n");


			// Insert the static fields to define the json names
			// eg. private static final String FIELD_FIRST_NAME = "first_name";
			for (Member member : members) {
				sBuilder.append(
						ONE_TAB+"private static final String " + member.getFieldName() + " = \"" + member.getJsonField() + "\";")
						.append("\n");
			}
			sBuilder.append("\n\n");
		
		// Insert the actual member names including the SerializedName
		// annotation for Gson
		for (Member member : members) {
			if (options.contains(OutputOption.GSON)) {
				sBuilder.append(ONE_TAB+"@SerializedName(" + member.getFieldName() + ")\n");
			}
			sBuilder.append(ONE_TAB+"private " + member.getType() + " " + member.getName() + ";").append("\n");
		}
		sBuilder.append("\n\n");

		sBuilder.append(ONE_TAB+"public ").append(name).append("(){\n\n").append(ONE_TAB+"}\n\n");

		// Insert the accessor methods for the members;
		for (Member member : members) {
			sBuilder.append(member.getSetter(mInflector));
			sBuilder.append(member.getGetter());

		}

		sBuilder.append(generateExtraMethods());

		if (options.contains(OutputOption.PARCELABLE)) {
			sBuilder.append(generateParcelableCode(jsonToJava.getTypes()));
		}
		
		if(options.contains(OutputOption.TO_STRING)){
			sBuilder.append(generateToString());
		}
		sBuilder.append("\n}");
//		System.out.println(getColumns());
		return sBuilder.toString();
	}

	private String generateExtraMethods() {
		String type = StringUtils.removeEnd(StringUtils.removeStart(name, "List<"), ">");
		for (Member member : members) {
			if (member.getName().equalsIgnoreCase("mId") || member.getName().equalsIgnoreCase("mUniqueId")) {
				StringBuilder sb = new StringBuilder();

				sb.append(ONE_TAB+"@Override\n");
				sb.append(ONE_TAB+"public boolean equals(Object obj){\n");
				sb.append(TWO_TABS+"if(obj instanceof ").append(type).append("){\n");
				if(TypeUtils.isPrimitiveType(member.getType())){
					sb.append(ONE_TAB+TWO_TABS+"return ((").append(type).append(") obj).").append(member.getGetterSignature())
					.append(" == ").append(member.getName()).append(";\n");
				}else{
					sb.append(ONE_TAB+TWO_TABS+"return ((").append(type).append(") obj).").append(member.getGetterSignature())
							.append(".equals(").append(member.getName()).append(");\n");
				}
				sb.append(TWO_TABS+"}\n");
				sb.append(TWO_TABS+"return false;\n");
				sb.append(ONE_TAB+"}\n\n");
				sb.append(ONE_TAB+"@Override\n");
				sb.append(ONE_TAB+"public int hashCode(){\n");
				sb.append(TWO_TABS+"return ");
				if(member.getType().equals("long")){
					sb.append("((Long)");
				}
				sb.append(member.getName());
				if(member.getType().equals("long")){
					sb.append(")");
				}
				sb.append(".hashCode();\n");
				sb.append(ONE_TAB+"}\n\n");
				return sb.toString();
			}
		}
		return "";
	}
	
	private String generateToString(){
		StringBuilder sb = new StringBuilder();
		sb.append(ONE_TAB+"@Override\n");
		sb.append(ONE_TAB+"public String toString(){\n");
		sb.append(TWO_TABS).append("return ");
		int i = 0;
		for(Member member: members){
			if(i == 0){
				sb.append("\"");
			}else{
				sb.append(" + \", ");
			}
			i++;
			sb.append(member.getDisplayName()).append(" = \" + ").append(member.getName());
		}
		sb.append(";\n");
		sb.append(ONE_TAB+"}\n\n");
		return sb.toString();
	}
	private String generateParcelableCode(Map<String,NewType> types) {
		StringBuilder sb = new StringBuilder();

		sb.append(ONE_TAB+"public ").append(name).append("(Parcel in) {\n");
		for (Member member : members) {

			if (member.getType().startsWith("List")) {
				String type = StringUtils.removeEnd(StringUtils.removeStart(member.getType(), "List<"), ">");
				if (TypeUtils.isPrimitiveType(type)) {
					sb.append(TWO_TABS+"in.readArrayList(").append(type).append(".class.getClassLoader());");

				} else {
					sb.append(TWO_TABS).append(member.getName());
					sb.append(" = new ArrayList<").append(type).append(">();\n");

					sb.append(TWO_TABS+"in.readTypedList(").append(member.getName()).append(", ").append(type);
					sb.append(".CREATOR);");

				}
			} else {
				sb.append(ONE_TAB+ONE_TAB);
				sb.append(member.getName()).append(" = ");
				if (member.getType().equals("boolean")) {

					sb.append("in.readInt() == 1 ? true: false;");
				} else if (types.containsKey(member.getType())) {
					sb.append("in.readParcelable(").append(member.getType()).append(".class.getClassLoader());");
				} else {
					sb.append("in.read").append(StringUtils.capitalize(member.getType())).append("();");

				}
			}
			sb.append("\n");
		}
		sb.append(ONE_TAB+"}\n\n");

		sb.append(ONE_TAB+"@Override\n"+ONE_TAB+"public int describeContents() {\n"+ONE_TAB+ONE_TAB+"return 0;\n"+ONE_TAB+"}\n\n");

		sb.append(ONE_TAB+"public static final Parcelable.Creator<").append(name);
		sb.append("> CREATOR = new Parcelable.Creator<").append(name);
		sb.append(">() {\n"+ONE_TAB+ONE_TAB+"public ").append(name);
		sb.append(" createFromParcel(Parcel in) {\n            return new ").append(name);
		sb.append("(in);\n"+ONE_TAB+ONE_TAB+"}\n\n");

		sb.append(ONE_TAB+ONE_TAB+"public ").append(name);
		sb.append("[] newArray(int size) {\n"+ONE_TAB+ONE_TAB+"return new ").append(name);
		sb.append("[size];\n"+ONE_TAB+ONE_TAB+"}\n"+ONE_TAB+"};\n\n");

		sb.append(ONE_TAB+"@Override\n");
		sb.append(ONE_TAB+"public void writeToParcel(Parcel dest, int flags) {\n");
		for (Member member : members) {
			sb.append(ONE_TAB+ONE_TAB);
			if (member.getType().startsWith("List")) {
				String type = StringUtils.removeEnd(StringUtils.removeStart(member.getType(), "List<"), ">");
				if (TypeUtils.isPrimitiveType(type)) {
					sb.append("dest.writeList(").append(member.getName()).append(");");

				} else {
					sb.append("dest.writeTypedList(").append(member.getName()).append(");");
				}
			} else if (member.getType().equals("boolean")) {
				sb.append("dest.writeInt(").append(member.getName()).append(" ? 1 : 0);");
			} else if (types.containsKey(member.getType())) {
				sb.append("dest.writeParcelable(").append(member.getName()).append(", flags);");
			} else {
				sb.append("dest.write").append(StringUtils.capitalize(member.getType())).append("(").append(member.getName())
						.append(");");

			}
			sb.append("\n");
		}
		sb.append(ONE_TAB+"}\n\n");

		return sb.toString();
	}
}