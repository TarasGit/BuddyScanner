package nomads.buddy_scanner.bluetooth;

import java.io.IOException;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class BluetoothServer {
//---- Debug ---------------------------------    
private static final boolean D = true;
private static final String TAG = "Blue Server";
//--------------------------------------------
	
	public static final String UUID_STRING="c2953118-9fc9-4f5e-9334-0873b3a846c6";
	private static final String SERVICE_ID ="buddy_service";
	
	public static final int DEVICE_CONNECTED = 101;
	public static final int NOT_CONNECTED = 102;
	
	private ConnectionNotification mNotifier;
	//private ConnectedThread mConnectedThread;
	private BluetoothAdapter mBluetoothAdapter;
	private AcceptThread mAcceptThread; 
	
		/**
		 * Constructor.
		 * @param Interface 
		 */
		public BluetoothServer(ConnectionNotification notifier) {
			this.mNotifier = notifier;
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			mAcceptThread = new AcceptThread();
			mAcceptThread.start();
		}
	
	/**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
    		mNotifier.connectionEstablished(socket, ConnectionNotification.SERVER_EVENT);
    	
    	/*
        if (mConnectedThread != null) {
        	mConnectedThread.cancel();
        	mConnectedThread = null;
        }
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        */
    }
    
    /*
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     
    private class ConnectedThread extends Thread {
    	private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private boolean mRun;

        public ConnectedThread(BluetoothSocket socket) {
        	if(D)Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            mRun = true;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
            	 tmpIn = socket.getInputStream();
            	 tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "ConnectedThread init sockets", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public synchronized void cancel() {
            mRun = false;
            try {
				Thread.sleep(300L);
			} catch (InterruptedException e) {
				Log.e(TAG, "cancel connectedThread", e);
			}
            //mmInStream.close();
            try {
				mmOutStream.close();
				 mmSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
           
        }

        @Override
		public void run() {
        	if(D)Log.i(TAG, "ConnectedThread run()");
            int bytes;
            while (mRun) {
            	try { 
            		//mmOutStream.write("Hello Buddy! Client".getBytes());
            		//mmOutStream.flush();
            		if((bytes = mmInStream.available()) > 0){// && !_mBuffer.bufferFull(bytes)) {
            			byte[] buffer = new byte[bytes];
            			bytes = mmInStream.read(buffer);
            			if(D)Log.i(TAG, new String(buffer));
        					//_mBuffer.put(buffer, bytes);
            		}
            		Thread.yield();
                } catch (IOException e) {
                    Log.e(TAG, "ConnectedThread.run()", e);
                    if(mRun)
                    	//connectionLost();
						
                    try {
						mmInStream.close();
						mmSocket.close();
					} catch (IOException e1) {
					
					}
                    break;
                }
            }
        }
	
        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                mmOutStream.flush();
                Log.d("Bluetooth written", new String(buffer));
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
                cancel();
            }
        }
    }
	*/
    
    
    private class AcceptThread extends Thread {
	    private BluetoothServerSocket mmServerSocket;
	    private boolean accept;
	 
	    public AcceptThread() {
	        if(D)Log.i(TAG, "AcceptThread constructor");
	        BluetoothServerSocket tmp = null;
	        try {
	            // MY_UUID is the app's UUID string, also used by the client code
	        	UUID myUUID = UUID.fromString(UUID_STRING);
	            tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(SERVICE_ID, myUUID);
	            accept = true;
	        } catch (IOException e) {
	        	Log.e(TAG, "AcceptThread", e);
	        }
	        mmServerSocket = tmp;
	    }
	 
	    public void run() {
	        BluetoothSocket socket = null;
	        // Keep listening until exception occurs or a socket is returned
	        while (accept) {
	            try {
	            	if(D)Log.i(TAG, "AcceptThread run");
	                socket = mmServerSocket.accept();
	            } catch (Exception e) {
                	Log.e(TAG, "accept socket", e);
                	socket = null;
                	break;
	            }
	            if (socket != null) {
	            	if(D)Log.i(TAG, "server connected to: " + socket.getRemoteDevice().getName());
		            	 connected(socket, socket.getRemoteDevice());
	            }
	        }
	    }

		/** Will cancel the listening socket, and cause the thread to finish */
	    public void cancel() {
	    	try {
	    		if(mmServerSocket!= null)
	    			mmServerSocket.close();
	        } catch (Exception e) {
	        	if(D)Log.e(TAG, "cancel()", e);
	        }
	    	mmServerSocket = null;
	    }
	}
	
	public synchronized void cancelServer() {
		if (D) Log.d(TAG, "cancel Server");
	    if (mAcceptThread != null) {
	        mAcceptThread.cancel();
	        mAcceptThread = null;
	    }
	}
}
