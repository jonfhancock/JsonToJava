package com.jsontojava;

import java.util.Arrays;

public class TypeUtils {

	public static final String PRIMITIVE_LONG = "long";
	public static final String PRIMITIVE_DOUBLE = "double";
	public static final String PRIMITIVE_INT = "int";
	public static final String PRIMITIVE_BOOLEAN = "boolean";
	public static final String TYPE_LONG = "Long";
	public static final String TYPE_STRING = "String";
	public static final String TYPE_DOUBLE = "Double";
	public static final String TYPE_INTEGER = "Integer";
	public static final String TYPE_BOOLEAN = "Boolean";
	public static final String TYPE_NULL = "Null";

	public static final String[] PRIMITIVE_TYPES = new String[]{PRIMITIVE_LONG,PRIMITIVE_DOUBLE,PRIMITIVE_INT,PRIMITIVE_BOOLEAN,TYPE_LONG,TYPE_STRING,TYPE_DOUBLE,TYPE_INTEGER,TYPE_BOOLEAN};
	public static boolean isPrimitiveType(Object current) {
		String clazz = current.getClass().getSimpleName();

		return isPrimitiveType(clazz);
	}

	public static boolean isPrimitiveType(String clazz) {
		return Arrays.asList(PRIMITIVE_TYPES).contains(clazz);
	}

	public static String getPrimitiveClassType(Object current) {
		String clazz = current.getClass().getSimpleName();

		if (clazz.equals(TYPE_BOOLEAN)) {
			clazz = TYPE_BOOLEAN;
		}
		if (clazz.equals(TYPE_INTEGER)) {
			clazz = TYPE_INTEGER;
		}
		if (clazz.equals(TYPE_DOUBLE)) {
			clazz = TYPE_DOUBLE;
		}
		if (clazz.equals(TYPE_STRING)) {
			if(((String) current).matches("^[0-9]+(\\.[0-9]+)?$")){
				try {
					
					long l = Long.parseLong((String) current);
					
					clazz = TYPE_LONG;
	
					if (Math.abs(l) < Integer.MAX_VALUE / 2) {
						clazz = TYPE_INTEGER;
					}
				} catch (NumberFormatException e) {
					try {
						Double.parseDouble((String) current);
						clazz = TYPE_DOUBLE;
					} catch (NumberFormatException e2) {
	
					}
				}
			}

		}
		return clazz;
	}

	public static String getPrimitiveType(Object current) {
		String clazz = current.getClass().getSimpleName();

		if (clazz.equals(TYPE_BOOLEAN)) {
			clazz = PRIMITIVE_BOOLEAN;
		}
		if (clazz.equals(TYPE_INTEGER)) {
			clazz = PRIMITIVE_INT;
		}
		if (clazz.equals(TYPE_DOUBLE)) {
			clazz = PRIMITIVE_DOUBLE;
		}
		if (clazz.equals(TYPE_STRING)) {
			if(((String) current).matches("^[0-9]+(\\.[0-9]+)?$")){

				try {
					long l = Long.parseLong((String) current);
					clazz = PRIMITIVE_LONG;
	
					if (Math.abs(l) < Integer.MAX_VALUE / 2) {
						clazz = PRIMITIVE_INT;
					}
				} catch (NumberFormatException e) {
					try {
						Double.parseDouble((String) current);
						clazz = PRIMITIVE_DOUBLE;
					} catch (NumberFormatException e2) {
	
					}
				}
			}
		}
		return clazz;
	}
}
