package com.jsontojava;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class NewType {
	public static final String IMPORT_JAVA_UTIL_LIST = "java.util.List";
	public static final String IMPORT_JAVA_UIIL_ARRAYLIST = "java.util.ArrayList";
	public static final String IMPORT_ANDROID_OS_PARCELABLE = "android.os.Parcelable";
	public static final String IMPORT_ANDROID_OS_PARCEL = "android.os.Parcel";
	public static final String IMPORT_GSON_SERIALIZED_NAME = "com.google.gson.annotations.SerializedName";
	public String name;
	public String pack;
	public Set<String> imports;
	public Set<Member> members;

	public NewType() {

		members = new HashSet<Member>();
	}

	public static class Builder {
		private String mName;
		private String mPackage;
		private Set<String> mImports;
		private Set<Member> mMembers;

		public Builder() {
			mImports = new HashSet<String>();
			mMembers = new HashSet<Member>();
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
			NewType type = new NewType();
			type.name = mName;
			type.pack = mPackage;
			type.imports = mImports;
			type.members = mMembers;
			return type;
		}
	}

	public String toString() {
		return toString(false, false);
	}

	public String toString(boolean parcelable, boolean gson) {
		if (parcelable) {
			imports.add(IMPORT_ANDROID_OS_PARCEL);
			imports.add(IMPORT_ANDROID_OS_PARCELABLE);
			if (imports.contains(IMPORT_JAVA_UTIL_LIST)) {
				imports.add(IMPORT_JAVA_UIIL_ARRAYLIST);
			}
		}
		if (gson) {
			imports.add(IMPORT_GSON_SERIALIZED_NAME);
		}
		StringBuilder sBuilder = new StringBuilder();

		sBuilder.append("package ").append(pack).append(";\n\n");
		for (String s : imports) {
			sBuilder.append("import ").append(s).append(";\n");
		}
		sBuilder.append("\n\n");
		sBuilder.append("public class ").append(name);
		if (parcelable) {
			sBuilder.append(" implements Parcelable");
		}
		sBuilder.append("{\n\n");

		if (gson) {
			// Insert the static fields to define the json names
			// eg. private static final String FIELD_FIRST_NAME = "first_name";
			for (Member member : members) {
				sBuilder.append(
						"    private static final String " + member.fieldName + " = \"" + member.jsonField + "\";")
						.append("\n");
			}
			sBuilder.append("\n\n");
		}
		// Insert the actual member names including the SerializedName
		// annotation for Gson
		for (Member member : members) {
			if (gson) {
				sBuilder.append("    @SerializedName(" + member.fieldName + ")\n");
			}
			sBuilder.append("    private " + member.type + " " + member.name + ";").append("\n");
		}
		sBuilder.append("\n\n");

		sBuilder.append("    public ").append(name).append("(){\n\n").append("    }\n\n");

		// Insert the accessor methods for the members;
		for (Member member : members) {
			sBuilder.append(member.getSetter());
			sBuilder.append(member.getGetter());

		}

		sBuilder.append(generateExtraMethods());

		if (parcelable) {
			sBuilder.append(generateParcelableCode());
		}

		sBuilder.append("\n}");
		return sBuilder.toString();
	}

	private String generateExtraMethods() {
		String type = StringUtils.removeEnd(StringUtils.removeStart(name, "List<"), ">");
		for (Member member : members) {
			if (member.name.equalsIgnoreCase("mId") || member.name.equalsIgnoreCase("mUniqueId")) {
				StringBuilder sb = new StringBuilder();

				sb.append("    @Override\n");
				sb.append("    public boolean equals(Object obj){\n");
				sb.append("        if(obj instanceof ").append(type).append("){\n");
				sb.append("        		return ((").append(type).append(") obj).").append(member.getGetterSignature())
						.append(".equals(").append(member.name).append(");\n");
				sb.append("        }\n");
				sb.append("        return false;\n");
				sb.append("    }\n\n");
				sb.append("    @Override\n");
				sb.append("    public int hashCode(){\n");
				sb.append("        return ").append(member.name).append(".hashCode();\n");
				sb.append("    }\n\n");
				return sb.toString();
			}
		}
		return "";
	}

	private String generateParcelableCode() {
		StringBuilder sb = new StringBuilder();

		sb.append("    public ").append(name).append("(Parcel in) {\n");
		for (Member member : members) {

			if (member.type.startsWith("List")) {
				String type = StringUtils.removeEnd(StringUtils.removeStart(member.type, "List<"), ">");
				if (TypeUtils.isPrimitiveType(type)) {
					sb.append("        in.readArrayList(").append(type).append(".class.getClassLoader());");

				} else {

					sb.append("new ArrayList<").append(type).append(">();\n");

					sb.append("        in.readTypedList(").append(member.name).append(", ").append(type);
					sb.append(".CREATOR);");

				}
			} else {
				sb.append("        ");
				sb.append(member.name).append(" = ");
				if (member.type.equals("boolean")) {

					sb.append("in.readInt() == 1 ? true: false;");
				} else if (Main.mTypes.containsKey(member.type)) {
					sb.append("in.readParcelable(").append(member.type).append(".class.getClassLoader());");
				} else {
					sb.append("in.read").append(StringUtils.capitalize(member.type)).append("();");

				}
			}
			sb.append("\n");
		}
		sb.append("    }\n\n");

		sb.append("    @Override\n    public int describeContents() {\n        return 0;\n    }\n\n");

		sb.append("    public static final Parcelable.Creator<").append(name);
		sb.append("> CREATOR = new Parcelable.Creator<").append(name);
		sb.append(">() {\n        public ").append(name);
		sb.append(" createFromParcel(Parcel in) {\n            return new ").append(name);
		sb.append("(in);\n        }\n\n");

		sb.append("        public ").append(name);
		sb.append("[] newArray(int size) {\n            return new ").append(name);
		sb.append("[size];\n        }\n    };\n\n");

		sb.append("    @Override\n");
		sb.append("    public void writeToParcel(Parcel dest, int flags) {\n");
		for (Member member : members) {
			sb.append("        ");
			if (member.type.startsWith("List")) {
				String type = StringUtils.removeEnd(StringUtils.removeStart(member.type, "List<"), ">");
				if (TypeUtils.isPrimitiveType(type)) {
					sb.append("dest.writeList(").append(member.name).append(");");

				} else {
					sb.append("dest.writeTypedList(").append(member.name).append(");");
				}
			} else if (member.type.equals("boolean")) {
				sb.append("dest.writeInt(").append(member.name).append(" ? 1 : 0);");
			} else if (Main.mTypes.containsKey(member.type)) {
				sb.append("		dest.writeParcelable(").append(member.name).append(", flags);");
			} else {
				sb.append("dest.write").append(StringUtils.capitalize(member.type)).append("(").append(member.name)
						.append(");");

			}
			sb.append("\n");
		}
		sb.append("    }\n\n");

		return sb.toString();
	}
}