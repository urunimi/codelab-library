package com.hovans.android.uuid;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.UUID;

/**
 * Time based UUID를 위한 Helper Class
 * 기본 UUID는 시간 기반이 아니다.
 * @author Hovan
 */
public class UUIDHelper {
	
	/**
	 * 현재시간을 기준으로 UUID생성 후 리턴
	 * @return 현재 시간 기준의 UUID
	 */
	public static UUID newTimeUUID(){
		return new UUID(UUIDHelper.createTime(System.currentTimeMillis()), UUIDHelper.getClockSeqAndNode());		
	}
	
	/**
	 * 1970-01-01을 기준으로 UUID생성 후 리턴
	 * @return 1970-01-01기준 UUID
	 */
	public static UUID oldTimeUUID(){
		return new UUID(UUIDHelper.createTime(0L), UUIDHelper.getClockSeqAndNode());
	}
	
	/**
	 * time 기준으로 Timed UUID생성 후 리턴
	 * @param time 기준시간
	 * @return UUID
	 */
	public static UUID newSetTimeUUID(long time){
		return new UUID(UUIDHelper.createTime(time), UUIDHelper.getClockSeqAndNode());
	}
	
	/**
	 * Magic number obtained from #cassandra's thobbs, who claims to have stolen it from a Python library.
	 * @param d
	 * @return 인자를 해석한 시간 기준의 UUID
	 */
	public static UUID newTimeUUIDSimple(Date d) {
		
		final long NUM_100NS_INTERVALS_SINCE_UUID_EPOCH = 0x01b21dd213814000L;

		long origTime = d.getTime();
		long time = origTime * 10000 + NUM_100NS_INTERVALS_SINCE_UUID_EPOCH;
		long timeLow = time & 0xffffffffL;
		long timeMid = time & 0xffff00000000L;
		long timeHi = time & 0xfff000000000000L;
		long upperLong = (timeLow << 32) | (timeMid >> 16) | (1 << 12) | (timeHi >> 48);
		return new UUID(upperLong, 0xC000000000000000L);
	}
	
	/**
	 * 간단히 사용하기 위한 UUID <br/>
	 * 실제 사용은 위의 {@link #newTimeUUID()}를 권장
	 * @return 현재 시간 기준의 UUID
	 */
	public static java.util.UUID newTimeUUIDSimple() {
		return newTimeUUIDSimple(new Date());
	}
	
    /**
     * No instances needed.
     */
    private UUIDHelper() {
        super();
    }

    /**
     * The last time value. Used to remove duplicate UUIDs.
     */
    private static long lastTime = Long.MIN_VALUE;
    
    /**
     * The cached MAC address.
     */
    private static String macAddress = null;

    /**
     * The current clock and node value.
     */
    private static long clockSeqAndNode = 0x8000000000000000L;

    static {

        try {
            Class.forName("java.net.InterfaceAddress");
            macAddress = Class.forName(
                    "com.codelab.library.uuid.UUIDGen$HardwareAddressLookup").newInstance().toString();
        }
        catch (ExceptionInInitializerError err) {
            // Ignored.
        }
        catch (ClassNotFoundException ex) {
            // Ignored.
        }
        catch (LinkageError err) {
            // Ignored.
        }
        catch (IllegalAccessException ex) {
            // Ignored.
        }
        catch (InstantiationException ex) {
            // Ignored.
        }
        catch (SecurityException ex) {
            // Ignored.
        }

        if (macAddress == null) {

            Process p = null;
            BufferedReader in = null;

            try {
                String osname = System.getProperty("os.name", "");

                if (osname.startsWith("Windows")) {
                    p = Runtime.getRuntime().exec(
                            new String[] { "ipconfig", "/all" }, null);
                }
                // Solaris code must appear before the generic code
                else if (osname.startsWith("Solaris")
                        || osname.startsWith("SunOS")) {
                    String hostName = getFirstLineOfCommand(
                            "uname", "-n" );
                    if (hostName != null) {
                        p = Runtime.getRuntime().exec(
                                new String[] { "/usr/sbin/arp", hostName },
                                null);
                    }
                }
                else if (new File("/usr/sbin/lanscan").exists()) {
                    p = Runtime.getRuntime().exec(
                            new String[] { "/usr/sbin/lanscan" }, null);
                }
                else if (new File("/sbin/ifconfig").exists()) {
                    p = Runtime.getRuntime().exec(
                            new String[] { "/sbin/ifconfig", "-a" }, null);
                }

                if (p != null) {
                    in = new BufferedReader(new InputStreamReader(
                            p.getInputStream()), 128);
                    String l = null;
                    while ((l = in.readLine()) != null) {
                        macAddress = MACAddressParser.parse(l);
                        if (macAddress != null
                                && Hex.parseShort(macAddress) != 0xff) {
                            break;
                        }
                    }
                }

            }
            catch (SecurityException ex) {
                // Ignore it.
            }
            catch (IOException ex) {
                // Ignore it.
            }
            finally {
                if (p != null) {
                    if (in != null) {
                        try {
                            in.close();
                        }
                        catch (IOException ex) {
                            // Ignore it.
                        }
                    }
                    try {
                        p.getErrorStream().close();
                    }
                    catch (IOException ex) {
                        // Ignore it.
                    }
                    try {
                        p.getOutputStream().close();
                    }
                    catch (IOException ex) {
                        // Ignore it.
                    }
                    p.destroy();
                }
            }

        }

        if (macAddress != null) {
            clockSeqAndNode |= Hex.parseLong(macAddress);
        }
        else {
            try {
                byte[] local = InetAddress.getLocalHost().getAddress();
                clockSeqAndNode |= (local[0] << 24) & 0xFF000000L;
                clockSeqAndNode |= (local[1] << 16) & 0xFF0000;
                clockSeqAndNode |= (local[2] << 8) & 0xFF00;
                clockSeqAndNode |= local[3] & 0xFF;
            }
            catch (UnknownHostException ex) {
                clockSeqAndNode |= (long) (Math.random() * 0x7FFFFFFF);
            }
        }

        // Skip the clock sequence generation process and use random instead.

        clockSeqAndNode |= (long) (Math.random() * 0x3FFF) << 48;

    }

    /**
     * Returns the current clockSeqAndNode value.
     *
     * @return the clockSeqAndNode value
     */
    public static long getClockSeqAndNode() {
        return clockSeqAndNode;
    }

    /**
     * Generates a new time field. Each time field is unique and larger than the
     * previously generated time field.
     *
     * @return a new time value
     */
    public static long newTime() {
        return createTime(System.currentTimeMillis());
    }
    
    /**
     * Creates a new time field from the given timestamp. Note that even identical
     * values of <code>currentTimeMillis</code> will produce different time fields.
     * 
     * @param currentTimeMillis the timestamp
     * @return a new time value
     */
    public static synchronized long createTime(long currentTimeMillis) {

        long time;

        // UTC time

        long timeMillis = (currentTimeMillis * 10000) + 0x01B21DD213814000L;

        if (timeMillis > lastTime) {
            lastTime = timeMillis;
        }
        else {
            timeMillis = ++lastTime;
        }

        // time low

        time = timeMillis << 32;

        // time mid

        time |= (timeMillis & 0xFFFF00000000L) >> 16;

        // time hi and version

        time |= 0x1000 | ((timeMillis >> 48) & 0x0FFF); // version 1

        return time;

    }
    
    /**
     * Returns the MAC address. Not guaranteed to return anything.
     * 
     * @return the MAC address, may be <code>null</code>
     */
    public static String getMACAddress() {
        return macAddress;
    }

    /**
     * Returns the first line of the shell command.
     *
     * @param commands the commands to run
     * @return the first line of the command
     * @throws IOException
     */
    static String getFirstLineOfCommand(String... commands) throws IOException {

        Process p = null;
        BufferedReader reader = null;

        try {
            p = Runtime.getRuntime().exec(commands);
            reader = new BufferedReader(new InputStreamReader(
                    p.getInputStream()), 128);

            return reader.readLine();
        }
        finally {
            if (p != null) {
                if (reader != null) {
                    try {
                        reader.close();
                    }
                    catch (IOException ex) {
                        // Ignore it.
                    }
                }
                try {
                    p.getErrorStream().close();
                }
                catch (IOException ex) {
                    // Ignore it.
                }
                try {
                    p.getOutputStream().close();
                }
                catch (IOException ex) {
                    // Ignore it.
                }
                p.destroy();
            }
        }

    }

//    /**
//     * Scans MAC addresses for good ones.
//     */
//    static class HardwareAddressLookup {
//
//        /**
//         * @see java.lang.Object#toString()
//         */
//        @Override
//        public String toString() {
//            String out = null;
//            try {
//                Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
//                if (ifs != null) {
//                    while (ifs.hasMoreElements()) {
//                        NetworkInterface iface = ifs.nextElement();
//                        byte[] hardware = iface.getHardwareAddress();
//                        if (hardware != null && hardware.length == 6
//                                && hardware[1] != (byte) 0xff) {
//                            out = Hex.append(new StringBuilder(36), hardware).toString();
//                            break;
//                        }
//                    }
//                }
//            }
//            catch (SocketException ex) {
//                // Ignore it.
//            }
//            return out;
//        }
//    }
    
    /**
	 * Returns an instance of uuid.
	 * 
	 * @param uuid byte[16]
	 * @return uuid
	 */
	public static java.util.UUID toUUID(byte[] uuid) {
		long msb = 0;
		long lsb = 0;
		assert uuid.length == 16;
		for (int i = 0; i < 8; i++) {
			msb = (msb << 8) | (uuid[i] & 0xff);
		}
		for (int i = 8; i < 16; i++) {
			lsb = (lsb << 8) | (uuid[i] & 0xff);
		}
		UUID u = new UUID(msb, lsb);
		return java.util.UUID.fromString(u.toString());
	}

	/**
	 * As byte array.
	 * 
	 * @param uuid
	 * @return byte[16]
	 */
	public static byte[] asByteArray(java.util.UUID uuid) {
		long msb = uuid.getMostSignificantBits();
		long lsb = uuid.getLeastSignificantBits();
		byte[] buffer = new byte[16];
		for (int i = 0; i < 8; i++) {
			buffer[i] = (byte) (msb >>> 8 * (7 - i));
		}
		for (int i = 8; i < 16; i++) {
			buffer[i] = (byte) (lsb >>> 8 * (7 - i));
		}
		return buffer;
	}
}
