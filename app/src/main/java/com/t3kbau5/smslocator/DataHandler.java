package com.t3kbau5.smslocator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DataHandler extends SQLiteOpenHelper{

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "interactions.db";
	public static final String TABLE_INTERACTIONS = "interactions";
	
	public static final String COLUMN_ID = "id";
	public static final String COLUMN_NUMBER = "number";
	public static final String COLUMN_COMMAND = "command";
	public static final String COLUMN_RESPONSE = "response";
	public static final String COLUMN_DATE = "date";
	
	public DataHandler(Context context, String name, CursorFactory factory,
			int version) {
		super(context, DATABASE_NAME, factory, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String CREATE_PRODUCTS_TABLE = "CREATE TABLE " +
				TABLE_INTERACTIONS + "("
	             + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + COLUMN_NUMBER 
	             + " TEXT," + COLUMN_COMMAND + " TEXT," + COLUMN_RESPONSE + " TEXT, " + COLUMN_DATE + " INTEGER)";
	      db.execSQL(CREATE_PRODUCTS_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_INTERACTIONS);
	    onCreate(db);
	}

	public void addInteraction(Interaction interaction) {

        ContentValues values = new ContentValues();
        
        
        values.put(COLUMN_NUMBER, interaction.getNumber());
        values.put(COLUMN_COMMAND, interaction.getCommand());
        values.put(COLUMN_RESPONSE, interaction.getResponse());
        values.put(COLUMN_DATE, interaction.getDate());
 
        SQLiteDatabase db = this.getWritableDatabase();
        
        db.insert(TABLE_INTERACTIONS, null, values);
        db.close();
	}
	
	public List<Interaction> getInteractions(){
		List<Interaction> interactions = new ArrayList<Interaction>();
		
		SQLiteDatabase db = this.getWritableDatabase();
    	
    	Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_INTERACTIONS , null);
    	
    	Interaction iaction;
    	int i = 0;
    	while(cursor.moveToNext()){
    		iaction = new Interaction(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getLong(4));
    		interactions.add(iaction);
    		
    		i++;
    	}
    	
    	cursor.close();
		
		return interactions;
	}
	
	public void deleteInteraction(int id){
		SQLiteDatabase db = this.getWritableDatabase();
		
		db.delete(TABLE_INTERACTIONS, COLUMN_ID + " = ?", new String[] {String.valueOf(id)});
		
		db.close();
	}
	
	public void clearInteractions(){
SQLiteDatabase db = this.getWritableDatabase();
		
		db.execSQL("delete from " + TABLE_INTERACTIONS);
		
		db.close();
	}
	
}
