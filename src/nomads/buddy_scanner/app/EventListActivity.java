package nomads.buddy_scanner.app;

import java.util.HashSet;
import java.util.Set;

import nomads.db.MyDBHelper;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class EventListActivity extends ListActivity {
//---- Debug ---------------------------------    
private static final boolean D = true;
private static final String TAG = "ELA";
//-------------------------------------------- 

	private static final int ADD_BUDDY_TO_DB = 1;
	
	/** Listen for Add new event click. */
	private final OnClickListener addEventListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(getApplicationContext(), NewEventActivity.class);
			startActivity(intent);
		}
	};

	/** Listen for long click to delete event. */
	private final OnItemLongClickListener deleteListener = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			// TODO Auto-generated method stub
			MyDBHelper dbH = new MyDBHelper(getApplicationContext());
			dbH.openDB(true);
			String event = getEventID(id);
			String table = getResources().getString(R.string.db_table_name_event);
			String[] columnName = getResources().getStringArray(R.array.db_create_event_table);
			dbH.deleteEntryFromTable(table, columnName[0]+"='"+event+"'");
			dbH.dropTable(event);
			dbH.close();
			CursorAdapter ca = getEvents(); 
			setListAdapter(ca);
			if(D)Log.i(TAG, "onLongClick");
			return true;
		}
	};

	/** BLUETOOTH adapter state change listener. */

	/** Receive the status change of bluetooth adapter. */
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
				final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
				switch (state) {
					case BluetoothAdapter.STATE_OFF:
					
					break;
					case BluetoothAdapter.STATE_TURNING_OFF:
					
					break;
					case BluetoothAdapter.STATE_ON:
						if(D)Log.i(TAG, "BluetoothAdapter.STATE_ON");
						// if(D) fillBuddyList();
							
					break;
					case BluetoothAdapter.STATE_TURNING_ON:
					
					break;
				}
			}
		}
	};
	
	
	
	private BluetoothAdapter mBlueAdapter;
	private MyDBHelper mHelper;
	private Cursor mCursor;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    //if(D) deleteDatabase(MyDBHelper.DATABASE_NAME);
	   
	    setContentView(R.layout.events_layout);
	    Button addEvent = (Button) findViewById(R.id.event_add_new);
	    addEvent.setOnClickListener(addEventListener);
	    ListView lv = getListView();
	    lv.setOnItemLongClickListener(deleteListener);
	    
	    IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		//filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		//filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(mReceiver, filter);
	    
	    mBlueAdapter = BluetoothAdapter.getDefaultAdapter();
	    if(!mBlueAdapter.isEnabled())
	    	mBlueAdapter.enable();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		CursorAdapter adapter = getEvents();
	    setListAdapter(adapter);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mCursor.close();
		mHelper.close();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mBlueAdapter.disable();
		unregisterReceiver(mReceiver);
	}
	
	@Override
	protected void onListItemClick (ListView l, View v, int position, long id) {
		String s = getEventID(id);
		Intent intent = new Intent(getApplicationContext(), RunningEventActivity.class);
		intent.putExtra("event_id", s);
		startActivity(intent);
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(0, ADD_BUDDY_TO_DB, 0, "Add new buddy").setShortcut('3', 'a');
    	return true;
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()){
			case ADD_BUDDY_TO_DB:
				Intent i = new Intent(getApplicationContext(), AddNewBuddy.class);
				startActivity(i);
				break;				
    	}
    	return super.onOptionsItemSelected(item);
    }
	
	/**
	 * Return cursor for events.
	 * @return cursor adapter for events.
	 */
	private CursorAdapter getEvents() {
		String[] columns = getResources().getStringArray(R.array.db_create_event_table);
		 int[] to = new int[] { R.id.name_entry};//, R.id.time_entry };
		mHelper = new MyDBHelper(this);
        mHelper.openDB(false);
        String tableID = getResources().getString(R.string.db_table_name_event);
        mCursor = mHelper.getAll(tableID);
        SimpleCursorAdapter ca = new SimpleCursorAdapter(this, 
        		R.layout.event_list_entry_layout, mCursor, columns, to);
        
        return ca;
	}
	
	private String getEventID(long id) {
		return mCursor.getString(0);
	}
	
	public void fillBuddyList() {
		Set<BluetoothDevice> bonded = new HashSet<BluetoothDevice>();
    	bonded.addAll(BluetoothAdapter.getDefaultAdapter().getBondedDevices());
    	MyDBHelper dbH = new MyDBHelper(getApplicationContext());
		dbH.openDB(true);

		String tableName = getResources().getString(R.string.db_table_name_buddy);
		String[] column = getResources().getStringArray(R.array.db_create_buddy_table);
		
		for(BluetoothDevice device : bonded){
			ContentValues cv = new ContentValues();
			if(D)Log.i(TAG, device.getAddress());
			cv.put(column[0], device.getAddress());
			cv.put(column[1], device.getName());
			dbH.insertEntry(tableName, cv);
		}
		dbH.close();
	}
	
}
