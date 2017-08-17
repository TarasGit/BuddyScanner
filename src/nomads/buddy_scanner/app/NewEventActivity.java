package nomads.buddy_scanner.app;

import nomads.buddy_scanner.controller.Parser;
import nomads.db.MyDBHelper;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class NewEventActivity extends Activity {
//---- Debug ---------------------------------    
private static final boolean D = true;
private static final String TAG = "NEA";
//--------------------------------------------

	private static final int START_OR_RETURN = 1;

	private EditText mEventName;
	private ListView mList;
	private MyDBHelper mHelper;
	private Cursor mCursor;
	
	
	private final OnClickListener mCreateListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Editable e = mEventName.getText();
			String s = e.toString();
			if(s.length() > 0) {
				s = s.replace(Parser.WS, Parser.UL);
				add2EventTable(s);
				String[] columnName  = getApplicationContext().getResources().getStringArray(
						R.array.db_create_buddy_table);
				String variableType = getApplicationContext().getString(R.string.db_varchar);
				columnName = mHelper.addVariableType(columnName, variableType);
				mHelper.createTable(s, columnName);
				//TODO create table
			int count = mList.getAdapter().getCount();
			for(int i=0; i<count; i++) {
				if(mList.isItemChecked(i)){
					mCursor.moveToPosition(i);
					String mac = mCursor.getString(0);
					String name = mCursor.getString(1);
					String descr = mCursor.getString(2);
					ContentValues insertEvent = new ContentValues();
					String[] columns = getResources().getStringArray(R.array.db_create_buddy_table);
					    	insertEvent.put(columns[0], mac);
					        insertEvent.put(columns[1], name);
					        insertEvent.put(columns[2], descr);
					mHelper.insertEntry(s, insertEvent);
				}
			}
			finish();
			//TODO showDialog(START_OR_RETURN);
			} else {
				//TODO if no name 
			}
		}
	};
	
	/**
	 * 
	 */
	private final OnEditorActionListener mNameListener = new OnEditorActionListener() {

		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			CharSequence s = v.getText();
			
			
			return true;
		}
		
	};
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_event_layout);
        mEventName = (EditText)findViewById(R.id.new_event_name);
        //mEventName.setOnEditorActionListener(mNameListener);
        mList = (ListView)findViewById(R.id.new_event_buddy_list);
        CursorAdapter adapter = getBuddyList();
        mList.setAdapter(adapter);
        mList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        Button addBuddy = (Button) findViewById(R.id.create_new_event_button); 
        addBuddy.setOnClickListener(mCreateListener);
	}
	
	
	protected void add2EventTable(String s) {
		String eventTable = getApplicationContext().getResources().getString(
				R.string.db_table_name_event);
		String[] columnName = getApplicationContext().getResources().getStringArray(
				R.array.db_create_event_table);
		ContentValues cv = new ContentValues();
		cv.put(columnName[0],s);
		mHelper.insertEntry(eventTable, cv);
	}


	protected void onDestroy() {
		super.onDestroy();
		mCursor.close();
		mHelper.close();
	}
	
	/**
	 * Return cursor for events.
	 * @return cursor adapter for events.
	 */
	private CursorAdapter getBuddyList() {
		String[] columns = getResources().getStringArray(R.array.db_create_buddy_table);
		String[] c = new String[2];
		System.arraycopy(columns, 1, c, 0, c.length);
		 int[] to = new int[] {android.R.id.text1, android.R.id.text2}; //new int[] { R.id.buddy_name, R.id.mac_address };
		mHelper = new MyDBHelper(this);
        mHelper.openDB(true);
        String tableID = getResources().getString(R.string.db_table_name_buddy);
        
        /*
        BluetoothAdapter blueAdapter = BluetoothAdapter.getDefaultAdapter();
         Set<BluetoothDevice> pairedDevices = blueAdapter.getBondedDevices();
		// If there are paired devices
		if (pairedDevices.size() > 0) {
		    // Loop through paired devices
		    for (BluetoothDevice device : pairedDevices) {
		    	if(!device.getName().contains("Tuber")){ 
		    		ContentValues cv = getPairedDevices(device);
		    		mHelper.insertEntry(tableID, cv);
		    	}
	    	}
		}
		*/
        mCursor = mHelper.getAll(tableID);
        SimpleCursorAdapter ca = new SimpleCursorAdapter(this, 
        		android.R.layout.simple_list_item_multiple_choice, mCursor, c, to);
        return ca;
	}
	
	private ContentValues getPairedDevices(BluetoothDevice device) {
		ContentValues insertEvent = new ContentValues();
		String[] columns = getResources().getStringArray(R.array.db_create_buddy_table);
		    	Log.i(TAG, "paired device="+device.getName());
		    	insertEvent.put(columns[0], device.getAddress());
		        insertEvent.put(columns[1], device.getName());
		        insertEvent.put(columns[2], "from connected list");
		return insertEvent;
	}
}
