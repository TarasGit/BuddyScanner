/**
 * 
 */
package nomads.buddy_scanner.bluetooth;

import java.io.UnsupportedEncodingException;

import org.xml.sax.Parser;

import android.util.Log;

/**
 * @author Max Tag.
 *
 */
public class BytesRingBuffer {
//---- Debug ---------------------------------    
	private static final boolean D = true;
	private static final String TAG = "BRB";
//--------------------------------------------	
		
	public static final int BUFF_SIZE = 5048;
	
// --- Byte buffer values --------------------
	
	private byte[] bytesBuffer;
	private int buffSize;
	private int putPosition;
	private int startPosition;
	private int elementCount;
	private int lineCounter;

	
		/**
		 * Default constructor.
		 */
		public BytesRingBuffer(){
			buffSize = BUFF_SIZE;
			bytesBuffer = new byte[buffSize];
		}
	
		/**
		 * Constructor
		 * @param buffSize current buffer size.
		 */
		public BytesRingBuffer(int buffSize){
			this.buffSize = buffSize;
			bytesBuffer = new byte[buffSize];
		}
		
	/* Ensures the pointer jumps to the first element if end achieved. */
	private int addRing(int i) {
	  return (i+1) == buffSize ? 0 : i+1;
	}

	/**
	 * Return true if the provided array don't fit the free space in buffer. 
	 * @param lenght
	 * @return
	 */
	public synchronized boolean bufferFull(int lenght){
		return (elementCount +lenght < buffSize);
	}
	
	/**
	 * Clears buffer.
	 */
	public synchronized void clearBuffer() {
		elementCount = 0;
		startPosition = putPosition;
	}
	
	/** 
	 * Gets the buffer size.
	 */
	public int getBuferSize(){
		return buffSize;
	}

	/**
	 *  Checks if there elements to read.
	 */
	public synchronized boolean hasMoreElements() {
		return (startPosition < putPosition-1);
	}

	/**
	 * Check if byte is a end of line char.
	 * @param mByte 
	 * @return 
	 */
	private boolean isEOL(byte mByte) {
		return (mByte == (byte)'\n' || mByte == (byte)'\r');
	}
	
	/**
	 * Return true if unblocking reading is possible.
	 * @return 
	 */
	public boolean isReady() {
		return (lineCounter > 0);
	}
	
	/**
	 * Adds new Element to buffer.
	 * @param str element to add.
	 */
	public synchronized void put(byte[] array, int size){
		elementCount +=size;
		for(int i=0; i<size; i++) {
			bytesBuffer[putPosition] = array[i];
			putPosition = addRing(putPosition);
			if(isEOL(array[i])) {
				lineCounter++;
				notify();
			}
		}
	}

	/** 
	 * Return line without end-of-line-chars.
	 */
	public synchronized String readLine(){
		int i = 0;
		switch(lineCounter) {
		case 0:
			if(D)Log.i(TAG, "readLine() wait()");
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		default:
			if(D)Log.i(TAG, "readLine()");
			byte[] buff = new byte[(elementCount)];
			while(!isEOL(bytesBuffer[startPosition])) {
				buff[i] = bytesBuffer[startPosition];
				startPosition = addRing(startPosition);
				i++;
				elementCount--;
			}
			/* insure that new line char goes away  
			if(bytesBuffer[startPosition] == (byte) Parser.NL){
				startPosition = addRing(startPosition);
				elementCount--;
			}*/
			lineCounter--;
			String s = "";
			try {
				s = new String(buff, 0, i, "windows-1252");
				if(D)Log.i(TAG,s);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			startPosition = addRing(startPosition);
			elementCount--;
			return s;
		}
	}
}
