package com.jsontojava;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.modeshape.common.text.Inflector;

import com.squareup.javawriter.JavaWriter;
import com.squareup.okhttp.OkHttpClient;

public class Main {
	static String mUrl;
	static Inflector mInflector;
	static String mPackage;
	static String mBaseType;
	static Map<String, NewType> mTypes;
	static JavaWriter mWriter;
	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		mUrl = args[0];
		mPackage = args[1];
		mBaseType = args[2];
		mTypes = new HashMap<String, NewType>();
		mInflector = new Inflector();
		

		try {
			NewType clazz = generateClass(getJsonFromUrl(mUrl), mBaseType);
			mTypes.put(mBaseType, clazz);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] packageParts = StringUtils.split(mPackage, ".");
		String path = StringUtils.join(packageParts,
				System.getProperty("file.separator"));
		File dir = new File(path);
		dir.mkdirs();
		System.out.println(dir.getAbsolutePath() + "\n\n");

		for (Map.Entry<String, NewType> entry : mTypes.entrySet()) {
			String className = entry.getKey();
			NewType type = entry.getValue();
			File classFile = new File(dir, className + ".java");
			FileUtils.write(classFile, type.toString());
			System.out.println(type);
			// sBuilder.append("private " + type + " " + memberName + ";")
			// .append("\n");
		}
	}

	private static NewType generateClass(JSONObject obj, String typeName) {
		NewType currentType = new NewType();
		currentType.pack = mPackage;
		currentType.name = typeName;

		String[] keys = JSONObject.getNames(obj);

		for (String s : keys) {
			Object current = obj.opt(s);
			Member m = generateMember(s, current);
			currentType.members.add(m);
			if (current instanceof JSONArray) {
				currentType.imports.add("java.util.List");

			}

		}
		return currentType;
	}

	private static Member generateMember(String key, Object current) {
		Member currentMember = new Member();

		currentMember.fieldName = "FIELD_" + key.toUpperCase();
		currentMember.jsonField = key;

		String singular = mInflector.singularize(key);
		String className = mInflector.camelCase(singular, true, '_');
		String memberName = "m" + className;
		String pluralMemberName = mInflector.pluralize(memberName);

		if (current instanceof JSONArray) {
			JSONArray array = (JSONArray) current;
			NewType type = new NewType();
			type.name = className;
			type.pack = mPackage;
			for (int i = 0; i < array.length(); i++) {
				Object element = array.get(i);
				if (element instanceof JSONObject) {
					NewType subClass = generateClass((JSONObject) element,
							className);
					type.imports.addAll(subClass.imports);
					type.members.addAll(subClass.members);
				}

			}
			currentMember.name = pluralMemberName;
			currentMember.type = "List<" + className + ">";

			if (mTypes.containsKey(className)) {
				mTypes.get(className).imports.addAll(type.imports);
				mTypes.get(className).members.addAll(type.members);
			} else {
				mTypes.put(className, type);

			}
		} else {
			if (current instanceof JSONObject) {
				NewType type = new NewType();
				type.name = className;
				type.pack = mPackage;
				NewType subClass = generateClass((JSONObject) current,
						className);
				type.imports.addAll(subClass.imports);
				type.members.addAll(subClass.members);
				if (mTypes.containsKey(className)) {
					mTypes.get(className).imports.addAll(type.imports);
					mTypes.get(className).members.addAll(type.members);
				} else {
					mTypes.put(className, type);

				}
				currentMember.name = memberName;
				currentMember.type = className;
			} else {
				String clazz = current.getClass().getSimpleName();

				if (clazz.equals("Boolean")) {
					clazz = "boolean";
				}
				if (clazz.equals("Integer")) {
					clazz = "int";
				}
				if (clazz.equals("Double")) {
					clazz = "double";
				}
				if (clazz.equals("String")) {

					try {
						long l = Long.parseLong((String) current);
						clazz = "long";

						if(Math.abs(l) < Integer.MAX_VALUE/2){
							clazz = "int";
						}
					} catch (NumberFormatException e) {
						try {
							Double.parseDouble((String) current);
							clazz = "double";
						} catch (NumberFormatException e2) {

						}
					}

				}

				if (clazz.equals("Null")) {
					clazz = "String";
				}
				currentMember.name = memberName;
				currentMember.type = clazz;
			}

		}
		return currentMember;
	}

	private static JSONObject getJsonFromUrl(String url) throws IOException {
		OkHttpClient client = new OkHttpClient();
		URLConnection connection = client.open(new URL(url));

		InputStream in = connection.getInputStream();

		String jsonString = IOUtils.toString(in);

		JSONObject obj = new JSONObject(jsonString);
		return obj;
	}

	private static class NewType {
		public String name;
		public String pack;
		public Set<String> imports;
		public Set<Member> members;

		public NewType() {
			imports = new HashSet<String>();
			imports.add("com.google.gson.annotations.SerializedName");
			imports.add("android.os.Parcel");
			imports.add("android.os.Parcelable");
			imports.add("java.util.ArrayList");
			members = new HashSet<Member>();
		}

		public String toString() {
			StringBuilder sBuilder = new StringBuilder();
			sBuilder.append("package ").append(pack).append(";\n\n");
			for (String s : imports) {
				sBuilder.append("import ").append(s).append(";\n");
			}
			sBuilder.append("\n\n");
			sBuilder.append("public class ").append(name)
					.append(" implements Parcelable{\n\n");

			// Insert the static fields to define the json names
			// eg. private static final String FIELD_FIRST_NAME = "first_name";
			for (Member member : members) {
				sBuilder.append(
						"    private static final String " + member.fieldName
								+ " = \"" + member.jsonField + "\";").append(
						"\n");
			}
			sBuilder.append("\n\n");

			// Insert the actual member names including the SerializedName
			// annotation for Gson
			for (Member member : members) {
				sBuilder.append("    @SerializedName(" + member.fieldName
						+ ")\n");
				sBuilder.append(
						"    private " + member.type + " " + member.name + ";")
						.append("\n");
			}
			sBuilder.append("\n\n");

			sBuilder.append("    public ").append(name).append("(){\n\n")
					.append("    }\n\n");

			// Insert the accessor methods for the members;
			for (Member member : members) {
				sBuilder.append(member.getSetter());
				sBuilder.append(member.getGetter());

			}

			sBuilder.append(generateExtraMethods());

			//sBuilder.append(generateParcelableCode());

			sBuilder.append("\n}");
			return sBuilder.toString();
		}

		private String generateExtraMethods() {
			String type = StringUtils.removeEnd(
					StringUtils.removeStart(name, "List<"), ">");
			for (Member member : members) {
				if (member.name.equalsIgnoreCase("mId")
						|| member.name.equalsIgnoreCase("mUniqueId")) {
					StringBuilder sb = new StringBuilder();

					sb.append("    @Override\n");
					sb.append("    public boolean equals(Object obj){\n");
					sb.append("        if(obj instanceof ").append(type)
							.append("){\n");
					sb.append("        		return ((").append(type)
							.append(") obj).")
							.append(member.getGetterSignature())
							.append(".equals(").append(member.name)
							.append(");\n");
					sb.append("        }\n");
					sb.append("        return false;\n");
					sb.append("    }\n\n");
					sb.append("    @Override\n");
					sb.append("    public int hashCode(){\n");
					sb.append("        return ").append(member.name)
							.append(".hashCode();\n");
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

				sb.append("        ");
				sb.append(member.name).append(" = ");
				if (member.type.startsWith("List")) {
					String type = StringUtils.removeEnd(
							StringUtils.removeStart(member.type, "List<"), ">");

					sb.append("new ArrayList<").append(type).append(">();\n");

					sb.append("        in.readTypedList(").append(member.name)
							.append(", ").append(type).append(".CREATOR);");
				} else if (member.type.equals("boolean")) {
					sb.append("in.readInt() == 1 ? true: false;");
				} else if (mTypes.containsKey(member.type)) {
					sb.append("in.readParcelable(").append(member.type)
							.append(".class.getClassLoader());");
				} else {
					sb.append("in.read")
							.append(StringUtils.capitalize(member.type))
							.append("();");

				}
				sb.append("\n");
			}
			sb.append("    }\n\n");

			sb.append("    @Override\n    public int describeContents() {\n        return 0;\n    }\n\n");

			sb.append("    public static final Parcelable.Creator<").append(
					name);
			sb.append("> CREATOR = new Parcelable.Creator<").append(name);
			sb.append(">() {\n        public ").append(name);
			sb.append(" createFromParcel(Parcel in) {\n            return new ")
					.append(name);
			sb.append("(in);\n        }\n\n");

			sb.append("        public ").append(name);
			sb.append("[] newArray(int size) {\n            return new ")
					.append(name);
			sb.append("[size];\n        }\n    };\n\n");

			sb.append("    @Override\n");
			sb.append("    public void writeToParcel(Parcel dest, int flags) {\n");
			for (Member member : members) {
				sb.append("        ");
				if (member.type.startsWith("List")) {
					sb.append("dest.writeTypedList(").append(member.name)
							.append(");");
				} else if (member.type.equals("boolean")) {
					sb.append("dest.writeInt(").append(member.name)
							.append(" ? 1 : 0);");
				} else if (mTypes.containsKey(member.type)) {
					sb.append("		dest.writeParcelable(").append(member.name)
							.append(", flags);");
				} else {
					sb.append("dest.write")
							.append(StringUtils.capitalize(member.type))
							.append("(").append(member.name).append(");");

				}
				sb.append("\n");
			}
			sb.append("    }\n\n");

			return sb.toString();
		}
	}

	private static class Member {
		public String fieldName;
		public String jsonField;
		public String type;
		public String name;

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
			if (type.equals("boolean")) {
				setPrefix = "is";
			}

			sBuilder.append(setPrefix).append(methodName).append("()");
			return sBuilder.toString();
		}

		public String getSetter() {
			StringBuilder sBuilder = new StringBuilder();
			String methodName = StringUtils.removeStart(name, "m");
			String nameNoPrefix = mInflector.camelCase(methodName, false);
			sBuilder.append("    public void set").append(methodName)
					.append("(").append(type).append(" ").append(nameNoPrefix)
					.append(") {\n        ").append(name).append(" = ")
					.append(nameNoPrefix).append(";").append("\n    }\n\n");
			return sBuilder.toString();
		}

		public String getGetter() {
			StringBuilder sBuilder = new StringBuilder();

			sBuilder.append("    public ").append(type).append(" ")
					.append(getGetterSignature()).append(" {\n        return ")
					.append(name).append(";\n    }\n\n");
			return sBuilder.toString();
		}
	}

}
