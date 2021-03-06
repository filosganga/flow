package com.github.jodersky.flow.internal;

import com.github.jodersky.flow.internal.NativeLoader;

/** Thin layer on top of native code. */
class NativeSerial {
	
	static {
		NativeLoader.load();
	}
	
	final static int E_IO = -1;
	final static int E_ACCESS_DENIED = -2;
	final static int E_BUSY = -3;
	final static int E_INVALID_SETTINGS = -4;
	final static int E_INTERRUPT = -5;
	final static int E_NO_PORT = -6;
	
	final static int PARITY_NONE = 0;
	final static int PARITY_ODD = 1;
	final static int PARITY_EVEN = 2;

	/**Opens a serial port and allocates memory for storing configuration. Note: if this function fails,
	 * any internally allocated resources will be freed.
	 * @param device name of port
	 * @param baud baud rate
	 * @param characterSize character size of data transmitted through serial device
	 * @param twoStopBits set to use two stop bits instead of one
	 * @param parity kind of parity checking to use
	 * @param serial pointer to memory that will be allocated with a serial structure
	 * @return 0 on success
	 * @return E_NO_PORT if the given port does not exist
	 * @return E_ACCESS_DENIED if permissions are not sufficient to open port
	 * @return E_BUSY if port is already in use
	 * @return E_INVALID_SETTINGS if any of the specified settings are not supported
	 * @return E_IO on other error */
	native static int open(String device, int baud, int characterSize, boolean twoStopBits, int parity, long[] serial);
	
	/**Starts a blocking read from a previously opened serial port. The read is blocking, however it may be
	 * interrupted by calling 'serial_interrupt' on the given serial port.
	 * @param serial pointer to serial configuration from which to read
	 * @param buffer buffer into which data is read
	 * @param size maximum buffer size
	 * @return n>0 the number of bytes read into buffer
	 * @return E_INTERRUPT if the call to this function was interrupted
	 * @return E_IO on IO error */
	native static int read(long serial, byte[] buffer);
	
	/**Writes data to a previously opened serial port.
	 * @param serial pointer to serial configuration to which to write
	 * @param data data to write
	 * @param size number of bytes to write from data
	 * @return n>0 the number of bytes written
	 * @return E_IO on IO error */
	native static int write(long serial, byte[] buffer);
	
	/**Interrupts a blocked read call.
	 * @param serial_config the serial port to interrupt
	 * @return 0 on success
	 * @return E_IO on error */
	native static int interrupt(long serial);
	
	/**Closes a previously opened serial port and frees memory containing the configuration. Note: after a call to
	 * this function, the 'serial' pointer will become invalid, make sure you only call it once. This function is NOT
	 * thread safe, make sure no read or write is in prgress when this function is called (the reason is that per 
	 * close manual page, close should not be called on a file descriptor that is in use by another thread). 
	 * @param serial pointer to serial configuration that is to be closed (and freed)
	 * @return 0 on success
	 * @return E_IO on error */
	native static int close(long serial);
	
	/**Sets debugging option. If debugging is enabled, detailed error message are printed (to stderr) from method calls. */
	native static void debug(boolean value);

}
