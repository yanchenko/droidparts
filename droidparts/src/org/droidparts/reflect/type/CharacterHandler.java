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
package org.droidparts.reflect.type;

import static org.droidparts.util.Arrays2.toPrimitive;

import java.util.ArrayList;

import org.droidparts.reflect.util.TypeHelper;

import android.content.ContentValues;
import android.database.Cursor;

public class CharacterHandler extends TypeHandler<Character> {

	@Override
	public boolean canHandle(Class<?> cls) {
		return TypeHelper.isCharacter(cls);
	}

	@Override
	public String getDBColumnType() {
		return INTEGER;
	}

	@Override
	protected Character parseFromString(Class<Character> cls, String str) {
		return Character.valueOf((str.length() == 0) ? ' ' : str.charAt(0));
	}

	@Override
	public void putToContentValues(ContentValues cv, String key, Object val) {
		cv.put(key, String.valueOf((Character) val));
	}

	@Override
	public Character readFromCursor(Class<Character> cls, Cursor cursor,
			int columnIndex) {
		return parseFromString(cls, cursor.getString(columnIndex));
	}

	@Override
	public Object parseTypeArr(Class<?> arrValType, String[] arr) {
		ArrayList<Character> list = toTypeColl(Character.class, arr);
		Character[] tArr = list.toArray(new Character[list.size()]);
		return (arrValType == char.class) ? toPrimitive(tArr) : tArr;
	}

}