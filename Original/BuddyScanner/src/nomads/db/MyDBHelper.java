package nomads.db;

import nomads.buddy_scanner.app.R;
import nomads.buddy_scanner.controller.Parser;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MyDBHelper extends SQLiteOpenHelper {
//---- Debug ---------------------------------    
private static final boolean D = true;
private static final String TAG = "BTC";
//--------------------------------------------

    public static final String DATABASE_NAME = "BUDDY_SCANNER.DB";
    private static final int DATABASE_VERSION = 1;
    	
    private final Context mContext; 
    private SQLiteDatabase mDB;
    
    	/**
    	 * Constructor
    	 * @param context
    	 */
		public MyDBHelper(Context context)
		{
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			mContext = context;
		}

		
	/**
	 * Creates a new table if not already exists. 
	 * @param tableName a name of table to be created.
	 * @param args array of column definitions with types and constraints[xxx type(int)]. 
	 * @return true if created or already exists;	
	 * 			false if failure in parameters.
	 */
	public boolean createTable(String tableName, String[] args)
	{
		boolean ok = false;
		StringBuilder execString = new StringBuilder(mContext.getString(
				R.string.db_create_table)).append(Parser.WS);
		execString.append(tableName).append(Parser.WS).append(Parser.RBL);
		for(int i=0; i<args.length-1; i++){
			execString.append(args[i]).append(Parser.COMMA);
		}
		execString.append(args[args.length-1]).append(Parser.RBR).append(Parser.SC);
		try {
			mDB.execSQL(execString.toString());
			ok = true;
			//Log.d(TAG, "created: "+execString.toString());
		} 
		catch(SQLException sql_e) { 
			Log.e(TAG, "create table("+ tableName+")", sql_e);
		}
		return ok;
	}
	
	/**
	 * Deletes single entry from a mDB.
	 * @param table 
	 * @param key
	 */
	public void deleteEntryFromTable(String table, String key)
	{
		mDB.delete(table, key, null);
	}
	 
	
	/**
	 * Deletes table from mDB.
	 * @param table table to delete.
	 */
	public void dropTable(String table)
	{
		try {
			mDB.execSQL(mContext.getString(R.string.db_drop_table)+Parser.WS+table);
		}
		catch(SQLException sql_e) {
			Log.e("mDB", "drop table", sql_e);
		}
	}
	 

	/**
	 * Fetches database.
	 * @return opened DB.
	 */
	public SQLiteDatabase getDB()
	{
		return mDB;
	}
	
	/**
	 * Get all entries from given table.
	 * @param tableID	table to read out.
	 * @return cursor.
	 */
	public Cursor getAll(String tableID) 
	{
		Cursor cursor = mDB.query(tableID, null, null, null, null, null, null);
		return cursor;
		
	}
	/*
	 *  Gets the stored mDB-table name
	 * @param context
	 * @return table name
	 
	public String getTableID(Context context)
	{
		  SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		  return pref.getString(context.getString(R.string.current_db_table), null);
		  
	}
	*/
	
	/**
	 * Inserting single row into mDB. 
	 * @param table 
	 * @param values
	 * @return true if no exceptions. 
	 */
	public long insertEntry(String table, ContentValues values)
	{
		return mDB.insert(table, null, values);
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		mDB = db;
		String[] columnName = mContext.getResources().getStringArray(R.array.db_create_event_table);
		String variableType = mContext.getString(R.string.db_varchar);
		columnName = addVariableType(columnName, variableType);
		String tableName = mContext.getResources().getString(R.string.db_table_name_event);
		createTable(tableName, columnName);
		
		columnName  = mContext.getResources().getStringArray(R.array.db_create_buddy_table);
		columnName = addVariableType(columnName, variableType);
		tableName= mContext.getResources().getString(R.string.db_table_name_buddy);
		createTable(tableName, columnName);		
	}
	
	public String[] addVariableType(String[] columns, String type){
		for(int i=0; i<columns.length; i++) {
			columns[i] = columns[i]+Parser.WS+type; 
		}
		return columns;
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2)
	{
		
	}

	/**
	 * Opens DB-connection.
	 * @param writeable DB modus.
	 */
	public void openDB(boolean writable) throws SQLiteException
	{
		if(writable) 
			mDB = this.getWritableDatabase();
		else 
			mDB = this.getReadableDatabase();
	}

}
