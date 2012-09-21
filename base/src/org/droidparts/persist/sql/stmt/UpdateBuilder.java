/**
 * Copyright 2012 Alex Yanchenko
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
package org.droidparts.persist.sql.stmt;

import java.util.Arrays;

import org.droidparts.model.Entity;
import org.droidparts.util.L;

import android.content.ContentValues;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Pair;

public class UpdateBuilder<EntityType extends Entity> extends
		StatementBuilder<EntityType> {

	private ContentValues contentValues = null;

	public UpdateBuilder(SQLiteDatabase db, String tableName) {
		super(db, tableName);
	}

	@Override
	public UpdateBuilder<EntityType> whereId(long id, long... moreIds) {
		return (UpdateBuilder<EntityType>) super.whereId(id, moreIds);
	}

	@Override
	public UpdateBuilder<EntityType> where(String columnName, Is operator,
			Object... columnValue) {
		return (UpdateBuilder<EntityType>) super.where(columnName, operator,
				columnValue);
	}

	@Override
	public UpdateBuilder<EntityType> where(String selection,
			Object... selectionArgs) {
		return (UpdateBuilder<EntityType>) super
				.where(selection, selectionArgs);
	}

	public UpdateBuilder<EntityType> setContent(ContentValues contentValues) {
		this.contentValues = contentValues;
		return this;
	}

	public int execute() {
		Pair<String, String[]> selection = getSelection();
		L.d("UPDATE on table '" + tableName + ", contentValues: '"
				+ contentValues + "', selection: '" + selection.first
				+ "', selectionArgs: '" + Arrays.toString(selection.second)
				+ "'.");
		int rowCount = 0;
		try {
			rowCount = db.update(tableName, contentValues, selection.first,
					selection.second);
		} catch (SQLException e) {
			L.e(e.getMessage());
			L.d(e);
		}
		return rowCount;
	}
}