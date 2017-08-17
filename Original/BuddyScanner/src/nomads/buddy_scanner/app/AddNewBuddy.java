/**
 * 
 */
package nomads.buddy_scanner.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import nomads.bluetooth_scanner.modell.Buddy;
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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.Toast;

/**
 * @author Argon
 *
 */
public class AddNewBuddy extends ListActivity {
//---- Debug ---------------------------------    
private static final boolean D = true;
private static final String TAG = "ANB";
//--------------------------------------------

	private static final String[]	ELEMENT_KEY	= { "name", "mac"};
	private static final int[]		ELEMENT_VIEW = { R.id.btfound_name, R.id.btfound_mac};

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public List<BluetoothDevice> found = new ArrayList<BluetoothDevice>();

		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if(D)Log.i(TAG, "found: "+device.getName());
				
				Map<String, String> entry = new HashMap<String, String>();
				entry.put(ELEMENT_KEY[0], device.getName());
				entry.put(ELEMENT_KEY[1], device.getAddress());
				mDeviceList.add(entry);
				//mDevListAdapter.notifyDataSetChanged();
				getListView().invalidateViews();
				
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				ProgressBar pb =(ProgressBar) findViewById(R.id.anb_search_bar);
				pb.setVisibility(View.INVISIBLE);
				found.clear();
				synchronized (this) {
					try {
						wait(1000); // /Wichtig: sonst gibts Fehlscans!
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			} else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
				if(D) Log.i(TAG, "ACTION_DISCOVERY_STARTED");
				ProgressBar pb =(ProgressBar) findViewById(R.id.anb_search_bar);
				pb.setVisibility(View.VISIBLE);
				found.clear();
			}
		}
	};
	
	private final OnClickListener mSearchDeviceLister = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if(!mDeviceList.isEmpty())
				mDeviceList.clear();
			if(!mBluetoothAdapter.isDiscovering()) {
				if(mBluetoothAdapter.startDiscovery())
					//TODO wartefröhlich
					if(D)Log.i(TAG, "discovery started");
			}
		}
	}; 
	
	private List<Map<String, ?>>	mDeviceList;		// device list
	private BluetoothAdapter	mBluetoothAdapter;
	private SimpleAdapter	mDevListAdapter;		// list adapter 
	private Vector<Buddy>	mBuddy;
	
	
	@Override 
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_new_buddy_layout);
		
		findViewById(R.id.anb_search_view).setOnClickListener(mSearchDeviceLister);
		mDeviceList = new ArrayList<Map<String, ?>>();
		mDevListAdapter = new SimpleAdapter(this, mDeviceList, R.layout.btlistlayout,
				ELEMENT_KEY, ELEMENT_VIEW);
		setListAdapter(mDevListAdapter);
		
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(mReceiver, filter);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		mBuddy = getBuddies(getResources().getString(R.string.db_table_name_buddy));
	}
	
	@Override
	protected void onListItemClick (ListView l, View v, int position, long id) {
		Map<String, ?> device = mDeviceList.get(position);
		String name = device.get(ELEMENT_KEY[0]).toString();
		String mac = device.get(ELEMENT_KEY[1]).toString();
		if(mBuddy != null) {
			for(Buddy b : mBuddy){
				if(b.equals(mac))
					return;
			}
		}
		addNewBudy(new String[]{mac,name,""});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mReceiver);
	}
	
	/**
	 * Return the list of buddies preserved for provided event.
	 * @param tableID event id.
	 * @return
	 */
	private Vector<Buddy> getBuddies(String tableID) {
		MyDBHelper dbH = new MyDBHelper(this);
		dbH.openDB(false);
		Cursor c = dbH.getAll(tableID);
		int count = c.getCount();
		
		// no entries yet in DB
		if(count == 0) {
			c.close();
			dbH.close();
			return null;
		}
		
		Vector<Buddy> buddy = new Vector<Buddy>(count);
		while(c.moveToNext()) {
			String mac = c.getString(0);
			String name = c.getString(1);
			if(D)Log.i(TAG, mac+ " "+name);
			Buddy b = new Buddy(mBluetoothAdapter.getRemoteDevice(mac), (short)0);
			b.setName(name);
			buddy.add(b);
		}
		c.close();
		dbH.close();
		return buddy;
		
	}
	
	/**
	 * Insert a new buddy to buddy DB.
	 * @param strings MAC-address and name of new buddy.
	 */
	private void addNewBudy(String[] strings) {
		String table = getResources().getString(R.string.db_table_name_buddy);
		String[] columnLabel = getResources().getStringArray(R.array.db_create_buddy_table);
		
		MyDBHelper dbH = new MyDBHelper(this);
		dbH.openDB(true);
		
		ContentValues values = new ContentValues();
		for(int i=0; i<columnLabel.length; i++) {
			values.put(columnLabel[i], strings[i]);
		}
		if(dbH.insertEntry(table, values)!= -1)
			Toast.makeText(getApplicationContext(), strings[1]+" successfuly added to buddy list",
					Toast.LENGTH_SHORT).show();
		dbH.close();
	}
}
