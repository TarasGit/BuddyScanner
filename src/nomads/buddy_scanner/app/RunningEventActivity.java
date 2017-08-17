package nomads.buddy_scanner.app;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import nomads.bluetooth_scanner.modell.Buddy;
import nomads.buddy_scanner.bluetooth.BluetoothControl;
import nomads.buddy_scanner.bluetooth.ConnectionNotification;
import nomads.db.MyDBHelper;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.Audio;
import android.util.Log;
import android.widget.SimpleAdapter;

public class RunningEventActivity extends ListActivity {
//---- Debug ---------------------------------    
private static final boolean D = true;
private static final String TAG = "REA";
//-------------------------------------------- 

	private class GuiUpdateByIntentsTask extends AsyncTask<Object, Object, Object> {
		private boolean running = true;
		private int x;
		
		final private String mac_ = "mac";
		final private String icon_ = "icon";
		
		
		public GuiUpdateByIntentsTask() {
		
		}
		
		protected synchronized Object doInBackground(Object... o) {
			while (running) {
				synchronized (this) {
					try {
						this.wait();
			
						this.publishProgress();
					} catch (InterruptedException e) {
						e.printStackTrace();
						return null;
					}
				}
			}
			return null;
		}
	
		protected void onProgressUpdate(Object... progress) {
			getListView().invalidateViews();
		}
	
		public void alert(String device) {
			for (Map entry : data)
				if (entry.get(mac_).equals(device))
					entry.put(icon_, R.drawable.rangealert0001);
			
			synchronized (this) {
				notify();
			}
		}
	
		public void positivAlert(String device) {
		for (Map entry : data)
			if (entry.get(mac_).equals(device))
				entry.put(icon_, R.drawable.withinrange0001);
		
			synchronized (this) {
				notify();
			}
		}
	
		public synchronized void setQuit() {
			running = false;
		
			synchronized (this) {
				notify();
			}
		}
	}

	private final BroadcastReceiver mDiscoveryReceiver = new BroadcastReceiver() {
		public List<BluetoothDevice> found = new ArrayList<BluetoothDevice>();

		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(D)Log.i(TAG, action);
			
			if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
				found.clear();
				
			} else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE);
				if(D)Log.i(TAG, "found: "+device.getName());
				found.add(device);
					String mac = device.getAddress();
					if (mBondedDevices.containsKey(mac)) {
						Buddy buddy = mBondedDevices.get(mac);
						buddy.setRSSI(rssi);
						buddy.setTime(System.currentTimeMillis());
						buddy.setDiscovered(true);
						mBondedDevices.replace(mac, buddy);
						if(D)Log.i(TAG, "call positive alert for: "+device.getAddress());
					}

			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				mBlueCtrl.startUpdater();
				
			} else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)){
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				String mac = device.getAddress(); 
				if(D)Log.d(TAG, "disconnected="+device.getName());
				t.alert(mac);
				buddyLost(device);
				if(mBlueCtrl != null)
					mBlueCtrl.disconnected(device);
			} else if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
				final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
				switch (state) {
					case BluetoothAdapter.STATE_OFF:
					
					break;
					case BluetoothAdapter.STATE_TURNING_OFF:
					
					break;
					case BluetoothAdapter.STATE_ON:
						if(D)Log.i(TAG, "BluetoothAdapter.STATE_ON");
						mBluetoothAdapter.startDiscovery();
						// if(D) fillBuddyList();
							
					break;
					case BluetoothAdapter.STATE_TURNING_ON:
					
					break;
				}
			}
		}
	};
	
	
	private final Handler mConnHandler = new Handler() {
		
		@Override
        public void handleMessage(Message msg) {
        	if(D)Log.i(TAG, msg.toString());
        	BluetoothDevice device = (BluetoothDevice) msg.obj;
        	String mac = device.getAddress();
        	switch (msg.what) {
        	case ConnectionNotification.CLIENT_EVENT:
        	case ConnectionNotification.SERVER_EVENT:
        		buddyFound(device);
        		t.positivAlert(mac);
        		break;
            }
        }
	};
	
	private static final int DISCOVERABLE_TIME = 1;
	
	private static final int BUDDY_FOUND = 10;
	
	private BluetoothControl mBlueCtrl;
	private BluetoothAdapter	mBluetoothAdapter;
	private NotificationManager	mNotificationManager;
	private List<Map<String, ?>>	data = new ArrayList<Map<String, ?>>();
	private GuiUpdateByIntentsTask	t;
	private ConcurrentHashMap<String,Buddy>	mBondedDevices;
	
	private final List<String> watching = new ArrayList<String>();
	
	private boolean isAlreadyDiscoverable;
	
	private int mDTime;
	
	@Override 
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.btanchorgui);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		//setContentView(R.layout.btlistlayout);
		String tableID = getIntent().getExtras().getString("event_id");
		
		mNotificationManager = getNotificationManager();
		//String tableID = savedInstanceState.getString("event_id");
		mBondedDevices = getEventBuddies(tableID);
		
		SimpleAdapter mAdapter = provideAdapter(mBondedDevices);
		this.setListAdapter(mAdapter);
		if(savedInstanceState != null) {
			isAlreadyDiscoverable = savedInstanceState.getBoolean("isAlreadyDiscoverable");
			if(D) Log.i(TAG, ""+Boolean.toString(isAlreadyDiscoverable));
		}
		if(!isAlreadyDiscoverable)
			showDialog(DISCOVERABLE_TIME);
		
		t = new GuiUpdateByIntentsTask();
		t.execute();
		
		mBlueCtrl = new BluetoothControl(mBondedDevices, mConnHandler);
		
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		registerReceiver(mDiscoveryReceiver, filter);
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(mDiscoveryReceiver);
		try {
			Thread.sleep(300L);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		t.setQuit();
		if(mBlueCtrl != null)
			mBlueCtrl.cancel();
		mNotificationManager.cancelAll();
		super.onDestroy();
	}
	
	@Override
	protected Dialog onCreateDialog(int id, Bundle intent) {
		switch(id) {
		case  DISCOVERABLE_TIME:
			return createDTimeDialog();
		default:
			return null;
		}
	}
	
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if(isAlreadyDiscoverable)
			outState.putBoolean("isAlreadyDiscoverable", isAlreadyDiscoverable); 
	}
	
	/**
	 * 
	 * @param mBuddy
	 * @return
	 */
	private SimpleAdapter provideAdapter(ConcurrentHashMap<String, Buddy> mBuddy) {
		String[] elementnames = { "name", "mac", "icon" };
		int[] elements = { R.id.btfound_name, R.id.btfound_mac, R.id.inreach };

		Map<String, Object> entry = new HashMap<String, Object>();
		Collection<Buddy> tmp =  mBuddy.values();
		for (Buddy bd : tmp) {
			entry = new HashMap<String, Object>();
			entry.put("name", bd.getName());
			entry.put("mac", bd.getDevice().getAddress());
			entry.put("icon", R.drawable.arbeiten0001);
			data.add(entry);
			watching.add(entry.get("mac").toString());
		}

		return new SimpleAdapter(this, data, R.layout.btlistlayout, elementnames, elements);
	}

	/**
	 * Return the list of buddies preserved for provided event.
	 * @param tableID event id.
	 * @return
	 */
	private ConcurrentHashMap<String,Buddy> getEventBuddies(String tableID) {
		MyDBHelper dbH = new MyDBHelper(this);
		dbH.openDB(false);
		Cursor c = dbH.getAll(tableID);
		int count = c.getCount();
		ConcurrentHashMap<String,Buddy> buddy = new ConcurrentHashMap<String,Buddy>(count);
		while(c.moveToNext()) {
			String mac = c.getString(0);
			String name = c.getString(1);
			if(D)Log.i(TAG, mac+ " "+name);
			Buddy b = new Buddy(mBluetoothAdapter.getRemoteDevice(mac), (short)0);
			b.setName(name);
			buddy.put(mac,b);
		}
		c.close();
		dbH.close();
		return buddy;
		
	}
	
	private NotificationManager getNotificationManager() {
		String ns = Context.NOTIFICATION_SERVICE;
		return (NotificationManager) getSystemService(ns);
	}
	
	private void buddyFound(BluetoothDevice device) {
		//TODO icon ?
		//TODO send to ctr  
		eventOccuresNotify("Found buddy", device);
	}
	
	private void eventOccuresNotify(CharSequence title, BluetoothDevice device) {
		int icon = R.drawable.icon;
		CharSequence tickerText = "Buddy scanner";
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);
		
		Context context = getApplicationContext();
		CharSequence contentTitle = title+": "+device.getName();
		CharSequence contentText = device.getAddress();
		Intent notificationIntent = new Intent(this, RunningEventActivity.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		
		notification.defaults |= Notification.DEFAULT_SOUND;
		
		
		notification.sound = Uri.parse("file:///sdcard/notification/ringer.mp3");
		notification.sound = Uri.withAppendedPath(Audio.Media.INTERNAL_CONTENT_URI, "6");
		
		notification.defaults |= Notification.DEFAULT_VIBRATE;
		
		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

		mNotificationManager.notify(BUDDY_FOUND, notification);
	}
	
	private void buddyLost(BluetoothDevice device) {
		if(device != null)
			eventOccuresNotify("Lost buddy", device);
	}
	/**
	 * Create dialog for picking up discoverability time.
	 * @return 
	 */
	private Dialog createDTimeDialog() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
	    alert.setTitle("Event duration");  
	    CharSequence[] items = {"30 min", "1  h", "2  h", "infinite" };
	    alert.setSingleChoiceItems(items , -1, new DialogInterface.OnClickListener() {

	        public void onClick(DialogInterface dialog, int item){
	          switch(item) {
	          case 0:
	        	  mDTime = 30*60;
	        	  break;
	          case 1:
	        	  mDTime = 60*60;
	        	  break;
	          case 2:
	        	  mDTime = 120*60;
	        	  break;
	          case 5:
	        	  mDTime = 0;
	        	  break;
	          }
	          if(D)Log.i(TAG, "mDTime ="+mDTime);
	        }
	        
	    });

	    alert.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
	    	
	        public void onClick(DialogInterface dialog, int id) {
	        	isAlreadyDiscoverable = true;
	        	//mServer = new BluetoothServer();
	        	mBluetoothAdapter.startDiscovery();
	        	Intent discoverableIntent = new
        			Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, mDTime);
        			startActivity(discoverableIntent);
	        }
	    });

	    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

	        public void onClick(DialogInterface dialog, int id) {
	            dialog.cancel();
	            finish();
	        }
	    });
		return alert.create();
	}

// BLuetoothServierInterface Methods -------------------------------------------------------
	
	
	
}

