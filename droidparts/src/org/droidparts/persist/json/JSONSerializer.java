/**
 * Copyright 2013 Alex Yanchenko
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package org.droidparts.persist.json;

import static org.droidparts.type.FieldSpecBuilder.getJsonKeySpecs;
import static org.droidparts.type.ReflectionUtils.getFieldVal;
import static org.droidparts.type.ReflectionUtils.instantiate;
import static org.droidparts.type.ReflectionUtils.setFieldVal;
import static org.json.JSONObject.NULL;

import java.util.ArrayList;
import java.util.Collection;

import org.droidparts.inject.Injector;
import org.droidparts.model.Model;
import org.droidparts.type.TypeHandlerRegistry;
import org.droidparts.type.ann.FieldSpec;
import org.droidparts.type.ann.json.KeyAnn;
import org.droidparts.type.handler.AbstractTypeHandler;
import org.droidparts.util.L;
import org.droidparts.util.PersistUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

public class JSONSerializer<ModelType extends Model> {

	// ASCII GS (group separator), '->' for readability
	public static final String __ = "->" + (char) 29;

	private final Class<ModelType> cls;
	private Context ctx;

	public JSONSerializer(Class<ModelType> cls, Context ctx) {
		this.cls = cls;
		if (ctx != null) {
			this.ctx = ctx.getApplicationContext();
			Injector.get().inject(ctx, this);
		}
	}

	public Context getContext() {
		return ctx;
	}

	public JSONObject serialize(ModelType item) throws JSONException {
		JSONObject obj = new JSONObject();
		for (FieldSpec<KeyAnn> spec : getJsonKeySpecs(cls)) {
			readFromModelAndPutToJSON(item, spec, obj, spec.ann.name);
		}
		return obj;
	}

	public ModelType deserialize(JSONObject obj) throws JSONException {
		ModelType model = instantiate(cls);
		for (FieldSpec<KeyAnn> spec : getJsonKeySpecs(cls)) {
			readFromJSONAndSetFieldVal(model, spec, obj, spec.ann.name);
		}
		return model;
	}

	public JSONArray serialize(Collection<ModelType> items)
			throws JSONException {
		JSONArray arr = new JSONArray();
		for (ModelType item : items) {
			arr.put(serialize(item));
		}
		return arr;
	}

	public ArrayList<ModelType> deserialize(JSONArray arr) throws JSONException {
		ArrayList<ModelType> list = new ArrayList<ModelType>();
		for (int i = 0; i < arr.length(); i++) {
			list.add(deserialize(arr.getJSONObject(i)));
		}
		return list;
	}

	protected <T> void putToJSONObject(JSONObject obj, String key,
			Class<T> valType, Class<?> arrCollItemType, Object val)
			throws Exception {
		if (val == null) {
			obj.put(key, NULL);
		} else {
			AbstractTypeHandler<T> handler = TypeHandlerRegistry
					.getHandlerOrThrow(valType);
			@SuppressWarnings("unchecked")
			Object jsonVal = handler.convertForJSON(valType, arrCollItemType,
					(T) val);
			obj.put(key, jsonVal);
		}
	}

	protected <T, V> Object readFromJSON(Class<T> valType,
			Class<V> arrCollItemType, JSONObject obj, String key)
			throws Exception {
		Object jsonVal = obj.get(key);
		if (NULL.equals(jsonVal)) {
			return jsonVal;
		} else {
			AbstractTypeHandler<T> handler = TypeHandlerRegistry
					.getHandlerOrThrow(valType);
			return handler.readFromJSON(valType, arrCollItemType, obj, key);
		}
	}

	protected boolean hasNonNull(JSONObject obj, String key)
			throws JSONException {
		return PersistUtils.hasNonNull(obj, key);
	}

	private void readFromModelAndPutToJSON(ModelType item,
			FieldSpec<KeyAnn> spec, JSONObject obj, String key)
			throws JSONException {
		Pair<String, String> keyParts = getNestedKeyParts(key);
		if (keyParts != null) {
			String subKey = keyParts.first;
			JSONObject subObj;
			if (hasNonNull(obj, subKey)) {
				subObj = obj.getJSONObject(subKey);
			} else {
				subObj = new JSONObject();
				obj.put(subKey, subObj);
			}
			readFromModelAndPutToJSON(item, spec, subObj, keyParts.second);
		} else {
			Object columnVal = getFieldVal(item, spec.field);
			try {
				putToJSONObject(obj, key, spec.field.getType(),
						spec.arrCollItemType, columnVal);
			} catch (Exception e) {
				if (spec.ann.optional) {
					L.w("Failded to serialize %s.%s: %s.", cls.getSimpleName(),
							spec.field.getName(), e.getMessage());
				} else {
					throw new JSONException(Log.getStackTraceString(e));
				}
			}
		}
	}

	private void readFromJSONAndSetFieldVal(ModelType model,
			FieldSpec<KeyAnn> spec, JSONObject obj, String key)
			throws JSONException {
		Pair<String, String> keyParts = getNestedKeyParts(key);
		if (keyParts != null) {
			String subKey = keyParts.first;
			if (hasNonNull(obj, subKey)) {
				JSONObject subObj = obj.getJSONObject(subKey);
				readFromJSONAndSetFieldVal(model, spec, subObj, keyParts.second);
			} else {
				throwIfRequired(spec);
			}
		} else if (obj.has(key)) {
			try {
				Object val = readFromJSON(spec.field.getType(),
						spec.arrCollItemType, obj, key);
				if (!NULL.equals(val)) {
					setFieldVal(model, spec.field, val);
				} else {
					L.i("Received NULL '%s', skipping.", spec.ann.name);
				}
			} catch (Exception e) {
				if (spec.ann.optional) {
					L.w("Failed to deserialize '%s': %s.", spec.ann.name,
							e.getMessage());
				} else {
					throw new JSONException(Log.getStackTraceString(e));
				}
			}
		} else {
			throwIfRequired(spec);
		}
	}

	private Pair<String, String> getNestedKeyParts(String key) {
		int firstSep = key.indexOf(__);
		if (firstSep != -1) {
			String subKey = key.substring(0, firstSep);
			String leftKey = key.substring(firstSep + __.length());
			Pair<String, String> pair = Pair.create(subKey, leftKey);
			return pair;
		} else {
			return null;
		}
	}

	private void throwIfRequired(FieldSpec<KeyAnn> spec) throws JSONException {
		if (!spec.ann.optional) {
			throw new JSONException("Required key '" + spec.ann.name
					+ "' not present.");
		}
	}

}