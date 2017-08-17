package nomads.buddy_scanner.bluetooth;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import nomads.bluetooth_scanner.modell.Buddy;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

public class BluetoothControl implements ConnectionNotification {
//---- Debug ---------------------------------    
private static final boolean D = true;
private static final String TAG = "BluezCtrl";
//--------------------------------------------	

	private class BuddyUpdater extends Thread {
		private boolean monitore;
		private boolean firstRun;
		
		public BuddyUpdater() {
			monitore = true;
			firstRun = true;
		}
		
		@Override
		public void run() {
			while(monitore) {
				Collection<Buddy> tmp = mBuddies.values();
				for(Buddy buddy: tmp) {
					if(!isCancel) {
						if(D)Log.d(TAG, "current buddy="+buddy.getName());
						if(	mClientConn.size() < MAX_CLIENT_CONNECTION  && !buddy.isConnected() ) {
							String mac = buddy.getDevice().getAddress();
							Buddy b = mBuddies.get(mac);
							mBuddies.replace(mac, b);
							//if(D)Log.d(TAG, "firstRun="+Boolean.toString(firstRun));
							if(firstRun) {
								if(b.isDiscovered())
									mClient = new BluetoothClient(b.getDevice(), BluetoothControl.this);
							} else {
								mClient = new BluetoothClient(b.getDevice(), BluetoothControl.this);
								//if(D)Log.d(TAG,buddy.getName());
							}
						}
						synchronized(this) {
							//if(D)Log.d(TAG, "lock mUpdaterThread");
							try {
								wait(10000L);
							} catch (InterruptedException e) {
								e.printStackTrace();
								monitore = false;
								return;
							}
						}
					}
				}
				firstRun = false;
			}
		}
		
		public void cancel() {
			monitore = false;
			synchronized(this) {
				notify();
			}
			if(D)Log.d(TAG, "BuddyUpdater cancel");
		}
	}

	/** Maximal possible client connections */
	private static final int MAX_CLIENT_CONNECTION = 6;
	
	private BluetoothServer mServer;
	private BuddyUpdater mUpdaterThread;
	private ConcurrentHashMap<String,BluetoothSocket> mClientConn;
	private ConcurrentHashMap<String,BluetoothSocket> mServerConn;
	private ConcurrentHashMap<String,Buddy> mBuddies;
	
	private Handler mGuiHandler;
	private BluetoothClient mClient;
	private boolean isCancel;
		/**
		 * Constructor.
		 * @param buddySet
		 */
		public BluetoothControl (ConcurrentHashMap<?,?> buddySet, Handler handler) {
			if(D)Log.i(TAG, "constructor");
			mBuddies = (ConcurrentHashMap<String, Buddy>) buddySet;
			//String s = mBuddies.toString();
			mGuiHandler = handler;
			
			int count = buddySet.size();
			mServer = new BluetoothServer(this);
			mServerConn = new ConcurrentHashMap<String, BluetoothSocket>(count);
			mClientConn = new ConcurrentHashMap<String, BluetoothSocket>(MAX_CLIENT_CONNECTION);
			
			mUpdaterThread = new BuddyUpdater();
			//mUpdaterThread.start(); 
		}
		
	
	public void cancel() {
		isCancel = true;
		try {
			Thread.sleep(300L);
		} catch (InterruptedException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		mUpdaterThread.cancel();
		mServer.cancelServer();
		if(mClient != null)
			mClient.stop();
		mClient = null;
		
		Collection<BluetoothSocket> tmp = mServerConn.values();
		for(BluetoothSocket b: tmp) {
			try {
				b.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		tmp = mClientConn.values();
		for(BluetoothSocket b: tmp) {
			try {
				b.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if(D)Log.d(TAG, "mClientConn="+ mClientConn.toString()+"\n mServerConn="+mServerConn.toString());
		mClientConn.clear();
		mServerConn.clear();
		
		mClientConn = null;
		mServerConn = null;
	}

	public void setConnected(String mac, boolean status) {
		if(mBuddies.containsKey(mac)) {
			Buddy b = mBuddies.get(mac);
			b.setConnected(status);
			mBuddies.replace(mac, b);
			if(D)Log.d(TAG, mBuddies.get(mac).getName()+"="+Boolean.toString(status));
		}
	}


	@Override
	public void connectionEstablished(BluetoothSocket socket, int who) {
		if(isCancel)
			return;
		if(D)Log.d(TAG,"connected: "+socket.getRemoteDevice().getName()+" who ="+who);
		String mac = socket.getRemoteDevice().getAddress();
		BluetoothDevice device = socket.getRemoteDevice();
		setConnected(mac, true);
		switch(who){
		case CLIENT_EVENT:
			if(!mClientConn.contains(mac) && !mServerConn.contains(mac))
				mClientConn.put(mac, socket);
				mGuiHandler.obtainMessage(CLIENT_EVENT, device).sendToTarget();
				synchronized(mUpdaterThread) {
					mUpdaterThread.notify();
				}
			break;
		case SERVER_EVENT:
			if(!mServerConn.contains(mac) && !mClientConn.contains(mac))
				mServerConn.put(mac, socket);
				mGuiHandler.obtainMessage(SERVER_EVENT, device).sendToTarget();
			break;
		}
		//if(D)Log.d(TAG, "connect\nmClientConn="+ mClientConn.toString()+"\n mServerConn="+mServerConn.toString());
	}


	@Override
	public void disconnected(BluetoothDevice device){
		if(isCancel)
			return;
		if(D)Log.d(TAG,"disconnected: "+device.getName());
		BluetoothSocket clientSocket = null;
		BluetoothSocket serverSocket = null;
		if(mClientConn.containsKey(device.getAddress())) {
			clientSocket = mClientConn.remove(device.getAddress());
			
		} else {	
			serverSocket = mServerConn.remove(device.getAddress());
			
		}
			try {
				if(clientSocket != null)
					clientSocket.close();
				if(serverSocket != null)
					serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(800L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			setConnected(device.getAddress(), false);
			//if(D)Log.d(TAG, "disconnect\nmClientConn="+ mClientConn.toString()+"\n mServerConn="+mServerConn.toString());
	}


	@Override
	public void connectionFailed(BluetoothDevice device) {
		if(isCancel)
			return;
		if(D)Log.d(TAG, "failed: "+device.getName());
		//avoid the case server is connected 
		if(!mServerConn.containsKey(device.getAddress()))
			setConnected(device.getAddress(), false);
		synchronized(mUpdaterThread) {
			
			mUpdaterThread.notify();
		}
		//if(D)Log.d(TAG, " failed \nmClientConn="+ mClientConn.toString()+"\nmServerConn="+mServerConn.toString());
	}


	public void startUpdater() {
		if(mUpdaterThread == null)
			mUpdaterThread = new BuddyUpdater();
		if(!mUpdaterThread.isAlive())
		mUpdaterThread.start();
		
	}
}

