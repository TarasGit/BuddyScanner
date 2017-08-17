package nomads.buddy_scanner.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

public interface ConnectionNotification {

	public static final int CLIENT_EVENT = 3010;
	public static final int SERVER_EVENT = 3020;
	
	/**
	 * 
	 * @param socket
	 * @param status
	 * @param who
	 */
	abstract void connectionEstablished(BluetoothSocket socket, int who);
	
	/**
	 * 
	 * @param device
	 * @param who
	 */
	abstract void disconnected(BluetoothDevice device);
	
	/**
	 * 
	 * @param mac
	 */
	abstract void connectionFailed(BluetoothDevice device);
}
