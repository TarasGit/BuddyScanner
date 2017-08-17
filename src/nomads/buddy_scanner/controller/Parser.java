package nomads.buddy_scanner.controller;

import nomads.buddy_scanner.bluetooth.BytesRingBuffer;
import android.content.Context;
import android.os.Handler;
import android.util.Log;


public abstract class Parser extends Thread {
//---- Debug ---------------------------------    
	private static final boolean D = false;
	private static final String TAG = "Parser";
//--------------------------------------------	

	/*
	public static final char EQ		='=';
	public static final char NL		='\n';
	public static final char CR		='\r';
	public static final char TAB 	='\t';
	
	
	public static final char COL 	=':';
	public static final char ABL 	= '<';
	public static final char ABR 	= '>';
	
	
	public static final char DOT 	='.';
	
	public static final char PLUS 	='+';
	public static final char SLSH 	= '/';
	public static final char APOST 	= '"';
	
	public static final char MINUS 	= '-';
	public static final char PERCENT='%';
	
	public static final char ESC = (char)27;
	
		
	public static final int WRONG_PWD		= -6;
	public static final int ERROR 			= -5;
	public static final int CANCEL_MEASURE 	= -4;
	public static final int CANCELED 		= -3;		// cancel operation is complete
	public static final int TABLE_EXIST		= -2;	// table is already in the database stored
	public static final int CANCEL_READ 	= -1;	// forced stop reading measure
	public static final int READ_CLEAR 		= 0;
	public static final int START_READ 		= 1;	
	public static final int END_READ 		= 2;	// normal stop
	public static final int GET_DAMAGED_LINES = 4;
	public static final int BATTERY_VALUE	= 5;    
	public static final int MEASURE_LIST 	= 6;	// list of measures 
	public static final int MEASUREMENT_SIZE= 7;	// size of a measurement
	public static final int MEASURE_ACTIVE 	= 8;    // measure in operation
	public static final int UPDATE_PROGRESS = 9;	// download progress reading measure
	public static final int OPERATING_MODE  = 10; 	// logger operating mode
	public static final int MEASURE_STARTED = 11;	// measure is started
	public static final int ERASE_DATA 		= 12;
	public static final int NO_DATA_STORED 	= 13;	// initial state of logger or after clearing the storage
	public static final int ONLINE_OUTPUT 	= 14;
	
	
	*/
	public static final char SC 	=';';
	public static final char COMMA 	=',';
	public static final char RBR 	=')';
	public static final char UL 	='_';
	public static final char WS 	=' ';
	public static final char RBL 	='(';
	
	/**
	  * Finds the next position of a digit within passed string.
	  * @param string passed string.
	  * @return index of next digit.
	  */
	 public static int getNextDigitIndex(String string) {
		 int i = 0;
		 while(string.charAt(i)<48 || string.charAt(i)>57) i++;
		 return i;
	 }
	
	protected int mState;
	private boolean run;
	protected int _counter;
	protected Context _mContext;
	protected Handler _mHandler;
	
	
		protected BytesRingBuffer _mBuff;
	
	
	public Parser() {
			// TODO Auto-generated constructor stub
		}


		
	 /**
	 * Cancel thread. 
	 */
 	public void cancel(int mode){
		setMode(mode);
		setRunning(false);
		_mBuff.clearBuffer();
		_mBuff = null;
		/*if(D){
			Log.d(TAG, "abbruch");
			Log.d(TAG, "geparst "+_counter);
		}*/
	}

    /**
	 * Finish thread in proper way. 
	 */
	public void finish(){
		setRunning(false);
		_mBuff.clearBuffer();
		/*if(D){
			Log.d(TAG, "beendet");
			Log.d(TAG, "geparst "+_counter);
		}*/
	}
 
    public int getCount() {
    	return _counter;
    }

    /**
     * Return the current connection state. */
    public synchronized int getMode() {
        return mState;
    }
	 
    /**
     * Return the current connection state. */
    public synchronized boolean getRunning() {
        return run;
    }
    
	protected abstract void  parseString(final String element);
	
	@Override
	public void run() {
		if(D) Log.d(TAG, "parser starts");
		while(run) {
			String s = _mBuff.readLine();
				if(s != null) {
					parseString(s);
					if(D)Log.i(TAG, s);
				} else {
					yield();
				}
		}
	}
 
	/**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    protected synchronized void setMode(int state) {
        //if (D) Log.d(TAG, "setMode() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        //_mHandler.obtainMessage(state,0, -1).sendToTarget();
    }
	
	/**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    public synchronized void setRunning(boolean state) {
        //if (D) Log.d(TAG, "setMode() " + run + " -> " + state);
        run = state;

        // Give the new state to the Handler so the UI Activity can update
        //_mHandler.obtainMessage(state,0, -1).sendToTarget();
    }
}
