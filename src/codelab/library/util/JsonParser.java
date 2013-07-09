package codelab.library.util;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import codelab.library.log.LogByCodeLab;

/**
 * JSON 객체를 가지고 Object를 생성하는 static method 모음.
 * @author Hovan Yoo
 */
public class JsonParser {
	public static<T extends Object> T getNewInstance(Class<T> type, String jsonString) throws Exception {
		if(jsonString.length() == 0) return null;

		LogByCodeLab.d(type.getSimpleName() + " instance creating... ");
		JSONObject result = new JSONObject(jsonString);
		T object = type.newInstance();
		Class<?> fieldType;
		String fieldName;

		int arrayTypeIndex = 0;

		for(Field f : type.getFields()) {
			fieldType = f.getType();
			fieldName = f.getName();
			try {
				if(result.has(fieldName)) {
					if(int.class.equals(fieldType)) {
						f.set(object, result.get(fieldName));
					} else if(ArrayList.class.equals(fieldType)) {
//						Type gType = ((ParameterizedType)f.getGenericType()).getActualTypeArguments()[0];
//						LogByCodeLab.d(gType.toString() + " list creating... ");
//						f.set(object, getNewList(arrayTypes[arrayTypeIndex++], result.get(fieldName).toString()));
					} else if(String.class.equals(fieldType) == false && result.get(fieldName) instanceof JSONObject){
						f.set(object, getNewInstance(fieldType, result.get(fieldName).toString()));
					} else {
						String value = result.get(fieldName).toString();
						if(value != null && "null".equals(value) == false && "".equals(value) == false) {
							f.set(object, value);
						}
					}
				}
			} catch(Exception e) {
				LogByCodeLab.e(e, f.getType() + " instance failed. " + result.get(f.getName()).toString());
			}
		}
		LogByCodeLab.d(type.getSimpleName() + " instance created. " + object.toString());
		
		return object;
	}
	
	public static<T extends Object> List<T> getNewList(Class<T> type, String jsonString) throws Exception {
		if(jsonString != null) {
			JSONArray jsonArray = new JSONArray(jsonString);
			ArrayList<T> list = new ArrayList<T>(jsonArray.length());
			LogByCodeLab.d(type.getSimpleName() + " list created. size is " + list.size());
			for(int i=0 ; i<jsonArray.length() ; ++i) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				T item = getNewInstance(type, jsonObject.toString());
				if(item != null) {
					list.add(item);	
				}
			}
			
			return list;
		}
		
		return null;
	}
}