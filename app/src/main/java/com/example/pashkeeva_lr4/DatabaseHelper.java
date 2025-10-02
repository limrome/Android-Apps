package com.example.pashkeeva_lr4;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "notes.db";
    private static final int DATABASE_VERSION = 4;
    public static final String TABLE_NAME = "notes";

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_PRIORITY = "priority";

    private static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TITLE + " TEXT, " +
                    COLUMN_CONTENT + " TEXT, " +
                    COLUMN_DATE + " TEXT, " +
                    COLUMN_PRIORITY + " INTEGER);";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
        seedDatabase(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    private void seedDatabase(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, "Заметка 1");
        values.put(COLUMN_CONTENT, "Вчера - мечта");
        values.put(COLUMN_DATE, "2025-03-04");
        values.put(COLUMN_PRIORITY, 1);
        db.insert(TABLE_NAME, null, values);

        values.put(COLUMN_TITLE, "Заметка 2");
        values.put(COLUMN_CONTENT, "Сегодня - цель");
        values.put(COLUMN_DATE, "2025-03-03");
        values.put(COLUMN_PRIORITY, 2);
        db.insert(TABLE_NAME, null, values);

        values.put(COLUMN_TITLE, "Заметка 3");
        values.put(COLUMN_CONTENT, "Завтра - реальность");
        values.put(COLUMN_DATE, "2025-03-02");
        values.put(COLUMN_PRIORITY, 3);
        db.insert(TABLE_NAME, null, values);
    }
}
