package nomads.bluetooth_scanner.modell;

import android.bluetooth.BluetoothDevice;

/** Class holds bluetooth device and his parameters for later use.*/
public class Buddy {

	private BluetoothDevice device;
	private String mName; 
	private long time;
	private short rssi;
	private boolean isConnected;
	private boolean isDiscovered;
	
		/* Constructor */
		public Buddy(BluetoothDevice device, short rssi){
			this.device = device;
			this.setRSSI(rssi);
		}
	
	public BluetoothDevice getDevice(){
		return device;
	}
	
	/**
	 * @return the rssi
	 */
	public short getRSSI() {
		return rssi;
	}

	/**
	 * @param rssi the rssi to set
	 */
	public void setRSSI(short rssi) {
		this.rssi = rssi;
	}

	/**
	 * @return the isConnected
	 */
	public boolean isConnected() {
		return isConnected;
	}

	/**
	 * @param isConnected the isConnected to set
	 */
	public synchronized void setConnected(boolean isConnected) {
		this.isConnected = isConnected;
	}

	/**
	 * @param rssi the rssi to set
	 */
	public void setName(String name) {
		this.mName = name;
	}
	
	/**
	 * @return the rssi
	 */
	public String getName() {
		return this.mName;
	}
	
	@Override
	public String toString(){
		return device.getName();
	}
	
	@Override
	public boolean equals(Object object) {
		return this.device.getAddress().equals(object.toString());
	}

	/**
	 * @return the time
	 */
	public long getTime() {
		return time;
	}

	/**
	 * @param time the time to set
	 */
	public void setTime(long time) {
		this.time = time;
	}

	/**
	 * @return the isDiscovered
	 */
	public boolean isDiscovered() {
		return isDiscovered;
	}

	/**
	 * @param isDiscovered the isDiscovered to set
	 */
	public void setDiscovered(boolean isDiscovered) {
		this.isDiscovered = isDiscovered;
	}
}
