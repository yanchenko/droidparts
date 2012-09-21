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
package org.droidparts.persist.sql;

import java.util.ArrayList;

import org.droidparts.contract.SQL;
import org.droidparts.model.Entity;
import org.droidparts.reflect.processor.EntityAnnotationProcessor;
import org.droidparts.util.PersistUtils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public abstract class AbstractDBOpenHelper extends SQLiteOpenHelper implements
		SQL.DDL {

	public AbstractDBOpenHelper(Context ctx, String name, int version) {
		super(ctx.getApplicationContext(), name, null, version);
	}

	protected abstract Class<? extends Entity>[] getEntityClasses();

	protected void onCreateExtra(SQLiteDatabase db) {
	}

	protected void onOpenExtra(SQLiteDatabase db) {
	}

	// helpers

	protected void execQueries(SQLiteDatabase db, ArrayList<String> queries) {
		PersistUtils.execQueries(db, queries);
	}

	protected void createIndex(SQLiteDatabase db, String table, boolean unique,
			String firstColumn, String... otherColumns) {
		PersistUtils
				.createIndex(db, table, unique, firstColumn, otherColumns);
	}

	protected void dropTables(SQLiteDatabase db, String... optionalTableNames) {
		PersistUtils.dropTables(db, optionalTableNames);
	}

	//

	@Override
	public final void onCreate(SQLiteDatabase db) {
		ArrayList<String> queries = new ArrayList<String>();
		for (Class<? extends Entity> cls : getEntityClasses()) {
			EntityAnnotationProcessor proc = new EntityAnnotationProcessor(cls);
			String query = PersistUtils.getSQLCreate(
					proc.getModelClassName(), proc.getModelClassFields());
			queries.add(query);
		}
		onOpen(db);
		execQueries(db, queries);
		onCreateExtra(db);
	}

	@Override
	public final void onOpen(SQLiteDatabase db) {
		if (!db.isReadOnly()) {
			db.execSQL("PRAGMA foreign_keys = ON;");
		}
		onOpenExtra(db);
	}

	//
	@Deprecated
	protected void dropAll(SQLiteDatabase db, boolean tables, boolean indexes) {
		dropTables(db);
	}

}