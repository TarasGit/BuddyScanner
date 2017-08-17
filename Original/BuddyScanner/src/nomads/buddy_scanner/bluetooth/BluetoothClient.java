package nomads.buddy_scanner.bluetooth;

import java.io.IOException;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class BluetoothClient {
//---- Debug ---------------------------------    
private static final boolean D = true;
private static final String TAG = "Blue Client";
//--------------------------------------------
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

        public synchronized void cancel() throws IOException {
            mRun = false;
            try {
				Thread.sleep(300L);
			} catch (InterruptedException e) {
				Log.e(TAG, "cancel connectedThread", e);
			}
            //mmInStream.close();
            mmOutStream.close();
            mmSocket.close();
        }

        @Override
		public void run() {
        	if(D)Log.i(TAG, "ConnectedThread run()");
            int bytes;
            while (mRun) {
            	try { 
            		//mmOutStream.write("Hello Buddy! Client".getBytes());
            		//mmOutStream.flush();
            		if((bytes = mmInStream.available()) > 0 && !_mBuffer.bufferFull(bytes)) {
            			byte[] buffer = new byte[bytes];
            			bytes = mmInStream.read(buffer);
            			if(D)Log.i(TAG, new String(buffer));
        					_mBuffer.put(buffer, bytes);
            		}
            		Thread.yield();
                } catch (IOException e) {
                    Log.e(TAG, "ConnectedThread.run()", e);
                    if(mRun)
                    	connectionLost();
						
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
                try {
					cancel();
				} catch (IOException e1) {
					
					e1.printStackTrace();
				}
                connectionLost();
            }
        }
    }
	*/
	
	/**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private boolean isCreated;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            if(D)Log.i(TAG, "ConnectThread constructor");
            UUID uuid = UUID.fromString(BluetoothServer.UUID_STRING);
            try {
                tmp = device.createInsecureRfcommSocketToServiceRecord(uuid);
                isCreated = true;
                if(D)Log.i(TAG, "socket="+tmp.toString());
            } catch (IOException e) {
            	isCreated = false;
            	 mmSocket = null;
                Log.e(TAG, "socket=null ", e);
                return;
            }
            mmSocket = tmp;
            
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }

        @Override
		public void run() {
           if(isCreated){
	        	//if(D)Log.i(TAG, "mConnectThread run");
	            setName("ConnectThread");
	            // Make a connection to the BluetoothSocket
	            try {
	                // This is a blocking call and will only return on a
	                // successful connection or an exception
	                mmSocket.connect();
	                if(D)Log.i(TAG, "client connect to: "+mmSocket.getRemoteDevice().getName());
	            } catch (IOException e) {
	                if(D)Log.e(TAG, "failed connected to: "+mmSocket.getRemoteDevice().getName(), e);
	                try {
	                    mmSocket.close();
	                } catch (IOException e2) {
	                    Log.e(TAG, "unable to close() socket during connection failure", e2);
	                }
	                //connectionFailed();
	                synchronized (BluetoothClient.this) {
		                mConnectThread = null;
		            }
	                return;
	            }
	            // Reset the ConnectThread because we're done
	            synchronized (BluetoothClient.this) {
	                mConnectThread = null;
	            }
	            // Start the connected thread
	            connected(mmSocket, mmDevice);
           }
       }
    }
		
    // Constants that indicate the current connection state
    public static final int CONNECTION_FAILED	= -1;
    public static final int CONNECTION_LOST 	= -2;
    public static final int STATE_CONNECTING 	= 2;	// now initiating an outgoing connection
    public static final int STATE_CONNECTED 	= 3;  // now connected to a remote device
    public static final int STATE_NONE 			= 0;       // destroy all threads
    
    private static boolean connected;
    
    private final BluetoothAdapter mAdapter;
	private ConnectThread mConnectThread;
	private ConnectionNotification mNotifire;
	private BluetoothDevice mDevice;
	private int mState;
	private int mMode;
	
    
    /**
	 * @return the connected
	 */
	public static boolean isConnected() {
		return connected;
	}
	/**
	 * @param connected the connected to set
	 */
	public static void setConnected(boolean connected) {
		BluetoothClient.connected = connected;
	}
	
    /**
	 * Constructor
	 * @param device bluetooth device.
	 * @param handler message handler.
	 * @param buffer  read buffer.
	 */
	public BluetoothClient(BluetoothDevice device, ConnectionNotification notifire){
			this.mDevice = device;
			this.mNotifire = notifire;
			mAdapter = BluetoothAdapter.getDefaultAdapter();
			connect(device);
	}

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);
        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
            	mConnectThread.cancel(); 
            	mConnectThread = null;
            }
        }
        
        /* Cancel any thread currently running a connection
        if (mConnectedThread != null) {
        	try {
				mConnectedThread.cancel();
			} catch (IOException e) {
				e.printStackTrace();
			}
        	mConnectedThread = null;
        }
        */
        
        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }
    
    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (D) Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
        	mConnectThread.cancel();
        	mConnectThread = null;
        }
        mNotifire.connectionEstablished(socket, ConnectionNotification.CLIENT_EVENT);
        setConnected(true);
        setState(STATE_CONNECTED);
        
    /* Cancel any thread currently running a connection
        if (mConnectedThread != null) {
        	try {
        		mConnectedThread.cancel();
        	} catch (IOException e) {
        		e.printStackTrace();
        	}
        	mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        */
    }
    
    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        setState(CONNECTION_FAILED);
        mNotifire.connectionFailed(mDevice);
    }
    
    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        setState(CONNECTION_LOST);
        setConnected(false);
        
    }
	
    /**
     * Return the current connection state. */
    public synchronized int getMode() {
        return mMode;
    }
    
    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }
    
    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    public synchronized void setMode(int mode) {
        if (D) Log.d(TAG, "setState() " + mMode + " -> " + mode);
        mMode = mode;
    }

	/**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if(D) Log.d(TAG, "setState() " + mState + " -> " + state);
        	mState = state;
    }

	/**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        if (mConnectThread != null) {
        	mConnectThread.cancel();
        	mConnectThread = null;
    	}
        setConnected(false);
        setState(STATE_NONE);
    }

}
