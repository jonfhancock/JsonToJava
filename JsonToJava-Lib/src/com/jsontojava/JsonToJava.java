package com.jsontojava;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.modeshape.common.text.Inflector;

public class JsonToJava {
	private static final String DEFAULT_BASE_TYPE = "Root";
	private static final String FILE_EXTENSION_JAVA = ".java";
	private static final String PACKAGE_SEPARATOR = ".";
	private static final String PROPERTY_FILE_SEPARATOR = "file.separator";

	private String mUrl;
	private Inflector mInflector;
	private String mPackage;
	private String mBaseType;
	private Map<String, NewType> mTypes;
	private boolean mParcel;
	private boolean mGsonAnnotations;

	public JsonToJava() {
		mInflector = new Inflector();
		mTypes = new HashMap<String, NewType>();

	}

	public void fetchJson() {
		try {
			Object root = getJsonFromUrl(mUrl);
			if (root instanceof JSONObject) {
				NewType clazz = generateClass((JSONObject) root, mBaseType);
				mTypes.put(mBaseType, clazz);

			} else if (root instanceof JSONArray) {
				NewType clazz = new NewType(mInflector);
				clazz.name = DEFAULT_BASE_TYPE;
				JSONArray rootArray = (JSONArray) root;
				for (int i = 0; i < rootArray.length(); i++) {
					NewType subClazz = generateClass(rootArray.getJSONObject(i), mBaseType);
					mTypes.put(mBaseType, subClazz);
				}
			}
		} catch (IOException e) {

			e.printStackTrace();
		}
	}
	
	public OutputStream outputZipFile(OutputStream outputStream) throws IOException{
//		File zipFile = new File(mPackage + ".zip");
		String[] packageParts = StringUtils.split(mPackage, PACKAGE_SEPARATOR);
		String fileSeparater = System.getProperty(PROPERTY_FILE_SEPARATOR);
		String path = StringUtils.join(packageParts, fileSeparater);
//		File dir = new File(path);
//		dir.mkdirs();
		ZipOutputStream out = new ZipOutputStream(outputStream);
		

		for (Map.Entry<String, NewType> entry : mTypes.entrySet()) {
			String className = entry.getKey();
			NewType type = entry.getValue();
			ZipEntry e = new ZipEntry(path+ fileSeparater + className + FILE_EXTENSION_JAVA);
			out.putNextEntry(e);
//			File classFile = new File(dir, className + FILE_EXTENSION_JAVA);
			IOUtils.write(type.toString(mParcel,mGsonAnnotations,this), out);
			out.closeEntry();
//			IOUtils.write(classFile, );
			System.out.println("Created " + className + FILE_EXTENSION_JAVA);
		}
		out.finish();
		return outputStream;
//		System.out.println("\nFinished creating java classes.  Your files are located in " + zipFile.getAbsolutePath() );

	}

	public String getUrl() {
		return mUrl;
	}

	public void setUrl(String url) {
		mUrl = url;
	}

	public Inflector getInflector() {
		return mInflector;
	}

	public void setInflector(Inflector inflector) {
		mInflector = inflector;
	}

	public String getPackage() {
		return mPackage;
	}

	public void setPackage(String package1) {
		mPackage = package1;
	}

	public String getBaseType() {
		return mBaseType;
	}

	public void setBaseType(String baseType) {
		mBaseType = baseType;
	}

	public Map<String, NewType> getTypes() {
		return mTypes;
	}

	public void setTypes(Map<String, NewType> types) {
		mTypes = types;
	}

	public void useParcelable(boolean useParcelable) {
		mParcel = useParcelable;
	}

	public void useGsonAnnotations(boolean useGson) {
		mGsonAnnotations = useGson;
	}

	private Object getJsonFromUrl(String url) throws IOException {
		Object retVal = new JSONObject();
//		OkHttpClient client = new OkHttpClient();
//		URLConnection connection = client.open(new URL(url));
//		BufferedReader reader = new BufferedReader()
		InputStream in = new URL(url).openStream();

		String jsonString = IOUtils.toString(in);
		try {
			retVal = new JSONObject(jsonString);
		} catch (JSONException e) {
			retVal = new JSONArray(jsonString);

		}
		return retVal;
	}

	private Member generateMember(String key, Object current) {
		Member.Builder memberBuilder = new Member.Builder();

		String singular = mInflector.singularize(key);
		String className = mInflector.camelCase(singular, true, '_', '.', '-');
		memberBuilder.setJsonField(key).setName(className);

		if (current instanceof JSONArray) {
			memberBuilder.setPlural();

			JSONArray array = (JSONArray) current;

			if (array.length() > 0 && TypeUtils.isPrimitiveType(array.get(0))) {
				String pType = TypeUtils.getPrimitiveClassType(array.get(0));
				memberBuilder.setType(pType);

			} else {
				NewType.Builder typeBuilder = new NewType.Builder(mInflector);
				typeBuilder.setName(className).setPackage(mPackage);
				for (int i = 0; i < array.length(); i++) {
					Object element = array.get(i);
					if (element instanceof JSONObject) {
						NewType subClass = generateClass((JSONObject) element, className);
						typeBuilder.addImports(subClass.imports);
						typeBuilder.addMembers(subClass.members);
					}

				}
				memberBuilder.setType(className);
				NewType type = typeBuilder.build();

				if (mTypes.containsKey(className)) {
					mTypes.get(className).imports.addAll(type.imports);
					mTypes.get(className).members.addAll(type.members);
				} else {
					mTypes.put(className, type);

				}
			}

		} else {
			if (current instanceof JSONObject) {
				NewType.Builder typeBuilder = new NewType.Builder(mInflector);
				typeBuilder.setName(className).setPackage(mPackage);

				NewType subClass = generateClass((JSONObject) current, className);

				typeBuilder.addImports(subClass.imports);
				typeBuilder.addMembers(subClass.members);

				NewType type = typeBuilder.build();

				if (mTypes.containsKey(className)) {
					mTypes.get(className).imports.addAll(type.imports);
					mTypes.get(className).members.addAll(type.members);
				} else {
					mTypes.put(className, type);

				}
				memberBuilder.setType(className);
			} else {
				String clazz = TypeUtils.getPrimitiveType(current);

				if (clazz.equals(TypeUtils.TYPE_NULL)) {
					clazz = TypeUtils.TYPE_STRING;
				}
				memberBuilder.setType(clazz);
			}

		}
		memberBuilder.addModifier("private");
		return memberBuilder.build();
	}

	private NewType generateClass(JSONObject obj, String typeName) {
		NewType.Builder typeBuilder = new NewType.Builder(mInflector);
		typeBuilder.setPackage(mPackage).setName(typeName);

		String[] keys = JSONObject.getNames(obj);

		for (String s : keys) {
			Object current = obj.opt(s);
			Member m = generateMember(s, current);
			typeBuilder.addMember(m);
			if (current instanceof JSONArray) {
				typeBuilder.addImport(NewType.IMPORT_JAVA_UTIL_LIST);

			}

		}
		return typeBuilder.build();
	}

}
