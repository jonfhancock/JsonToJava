package com.jsontojava;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.modeshape.common.text.Inflector;

import com.squareup.okhttp.OkHttpClient;

public class Main {
	private static final String OPTION_PACKAGE = "package";
	private static final String OPTION_URL = "url";
	private static final String OPTION_ROOT = "class";
	private static final String OPTION_GSON = "g";
	private static final String OPTION_PARCELABLE = "p";
	private static final String DEFAULT_BASE_TYPE = "Root";
	private static final String FILE_EXTENSION_JAVA = ".java";
	private static final String PACKAGE_SEPARATOR = ".";
	private static final String PROPERTY_FILE_SEPARATOR = "file.separator";
	static String mUrl;
	static Inflector mInflector;
	static String mPackage;
	static String mBaseType;
	static Map<String, NewType> mTypes;

	/**
	 * @param args
	 * @throws IOException
	 * @throws ParseException 
	 */
	public static void main(String[] args) throws IOException, ParseException {
		Options options = createOptions();
		
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse( options, args);
		
		mUrl = cmd.getOptionValue(OPTION_URL);
		
		mPackage = cmd.getOptionValue(OPTION_PACKAGE);
		mBaseType = cmd.getOptionValue(OPTION_ROOT);
		mTypes = new HashMap<String, NewType>();
		mInflector = new Inflector();

		try {
			Object root = getJsonFromUrl(mUrl);
			if (root instanceof JSONObject) {
				NewType clazz = generateClass((JSONObject) root, mBaseType);
				mTypes.put(mBaseType, clazz);

			} else if (root instanceof JSONArray) {
				NewType clazz = new NewType();
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
		String[] packageParts = StringUtils.split(mPackage, PACKAGE_SEPARATOR);
		String path = StringUtils.join(packageParts, System.getProperty(PROPERTY_FILE_SEPARATOR));
		File dir = new File(path);
		dir.mkdirs();

		for (Map.Entry<String, NewType> entry : mTypes.entrySet()) {
			String className = entry.getKey();
			NewType type = entry.getValue();
			File classFile = new File(dir, className + FILE_EXTENSION_JAVA);
			FileUtils.write(classFile, type.toString(cmd.hasOption(OPTION_PARCELABLE),cmd.hasOption(OPTION_GSON)));
			System.out.println("Created " + classFile.getName());
		}
		System.out.println("\nFinished creating java classes.  Your files are located in " + dir.getAbsolutePath() );

	}

	private static NewType generateClass(JSONObject obj, String typeName) {
		NewType.Builder typeBuilder = new NewType.Builder();
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

	private static Options createOptions(){
		Options options = new Options();
		options.addOption(OPTION_PARCELABLE, false, "Enabled implementation of Parcelable for all classes generated");
		options.addOption(OPTION_GSON,false,"Enables Gson annotations");
		Option rootClass = OptionBuilder.hasArg().isRequired().withDescription("The name of the root class of the feed you are parsing").create(OPTION_ROOT);
		options.addOption(rootClass);
		Option url = OptionBuilder.hasArg().isRequired().withDescription("The url of the json feed you want to parse").create(OPTION_URL);
		options.addOption(url);
		Option pack = OptionBuilder.hasArg().isRequired().withDescription("The package name for the generated classes").create(OPTION_PACKAGE);
		options.addOption(pack);
		
		return options;
		
		
	}

	private static Member generateMember(String key, Object current) {
		Member.Builder memberBuilder = new Member.Builder();

		String singular = mInflector.singularize(key);
		String className = mInflector.camelCase(singular, true, '_','.','-');
		memberBuilder.setJsonField(key).setName(className);
		if(key.equals("seals")){
			boolean breakPoint = true;
		}

		if (current instanceof JSONArray) {
			memberBuilder.setPlural();

			JSONArray array = (JSONArray) current;
			
				if (array.length() > 0 && TypeUtils.isPrimitiveType(array.get(0))) {
					String pType = TypeUtils.getPrimitiveClassType(array.get(0));
					memberBuilder.setType(pType);

				} else {
					NewType.Builder typeBuilder = new NewType.Builder();
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
				NewType.Builder typeBuilder = new NewType.Builder();
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

	private static Object getJsonFromUrl(String url) throws IOException {
		Object retVal = new JSONObject();
		OkHttpClient client = new OkHttpClient();
		URLConnection connection = client.open(new URL(url));

		InputStream in = connection.getInputStream();

		String jsonString = IOUtils.toString(in);
		try {
			retVal = new JSONObject(jsonString);
		} catch (JSONException e) {
			retVal = new JSONArray(jsonString);

		}
		return retVal;
	}

}
