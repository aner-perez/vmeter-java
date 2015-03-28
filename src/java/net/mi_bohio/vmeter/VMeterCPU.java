package net.mi_bohio.vmeter;

import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.io.BufferedReader;
import java.util.Scanner;

public class VMeterCPU implements Runnable
{
	private static final int LED_COUNT = 38;
	// cpu  user nice system idle iowait irq sofirq
	private static final String CPU_STAT_PATH = "/proc/stat";
	private boolean firstStat = true;
	private long user;
	private long nice;
	private long system;
	private long idle;
	private long iowait;
	private long irq;
	private long softirq;

	private String midiDevice;
	private int refresh;
	private FileOutputStream vmeter;

	public boolean verbose = false;

	// Free = MemFree + Cached
	// Used = MemTotal - Free
	private static final String MEM_STAT_PATH = "/proc/meminfo";
	private static final String TOTAL_MEM_STAT = "MemTotal";
	private static final String FREE_MEM_STAT = "MemFree";
	private static final String CACHED_MEM_STAT = "Cache";

	public static void main( String[] argv ) throws Exception {
		String midiDevice = "/dev/midi1";
		int millis = 250;
		int argPos;
		boolean verbose = false;

		for(argPos = 0; argPos < argv.length; argPos++) {
			if(!argv[argPos].startsWith("--")) {
				break;
			}
			if(argv[argPos].equals("--midi")) {
				midiDevice = "/dev/midi"+argv[++argPos];
			} else if (argv[argPos].equals("--dev")) {
				midiDevice = argv[++argPos];
			} else if (argv[argPos].equals("--interval")) {
				millis = Integer.parseInt(argv[++argPos]);
			} else if (argv[argPos].equals("--verbose")) {
				verbose = true;
			} else {
				System.err.println("unrecongnized option: "+argv[argPos]);
			}
		}
		VMeterCPU vmeter = new VMeterCPU(midiDevice, millis);
		vmeter.verbose = verbose;
		// Constantly read midi data to keep vmeter happy
		new Thread(vmeter).start();
		vmeter.start();
	}

	public void run() {
		FileInputStream midiIn = null;
		try {
			String[] volumeCommand = { "amixer", "set", "Master", "" };
			byte[] buffer = new byte[512];
			midiIn = new FileInputStream(midiDevice);
			int count = midiIn.read(buffer);
			while(count != -1) {
//				for(int i=0;i<count;i++) {
//					System.out.print(String.format("%02X ", buffer[i]));
//				}
//				System.out.println();
				if(count==3 && buffer[0] == (byte)0xB0 && buffer[1] == (byte)0x14) {
					// buffer[2] is touch position: 0-127
					volumeCommand[3] = (buffer[2]*100/127) + "%";
					Runtime.getRuntime().exec(volumeCommand);
//					System.out.println("Volume set to "+volumeCommand[3]);
				}
				count = midiIn.read(buffer);
			}
			System.err.println("MIDI drain terminated");
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			if(midiIn != null) {
				try { midiIn.close(); } catch (IOException ioe2) { }
			}
		}
	}

	public VMeterCPU(String midiDevice, int refresh) throws IOException {
		this.midiDevice = midiDevice;
		this.refresh = refresh;
		this.vmeter = new FileOutputStream(midiDevice);
	}

	public void start() throws IOException {
		while(true) {
			int cpu = getCPU();
			int mem = getMem();
			if(verbose)
				System.out.println("cpu = "+cpu+"%, mem="+mem+"%");
			try {
				update(cpu, mem);
				Thread.sleep(refresh);
			} catch(InterruptedException ex) {
				
			}
		}
	}

	private int getCPU() throws IOException {
	// cpu  user nice system idle iowait irq sofirq
	//private static final String CPU_STAT_PATH = "/proc/stat";
		int cpuPercent = 0;
		File procStat = new File(CPU_STAT_PATH);
		Scanner cpuScanner = new Scanner(procStat);
		cpuScanner.next(); // skip the word "cpu"
		long newUser = cpuScanner.nextLong();
		long newNice = cpuScanner.nextLong();
		long newSystem = cpuScanner.nextLong();
		long newIdle = cpuScanner.nextLong();
		long newIowait = cpuScanner.nextLong();
		long newIrq = cpuScanner.nextLong();
		long newSoftirq = cpuScanner.nextLong();
		cpuScanner.close();
		if(firstStat) {
			firstStat = false;
		} else {
			long newUsedCPU = newUser + newNice + newSystem + newIowait + newIrq + newSoftirq;
			long usedCPU = user + nice + system + iowait + irq + softirq;
			long intervalCycles = (newUsedCPU-usedCPU) + (newIdle - idle);
//	System.out.println("new cpu = "+newUsedCPU+", cpu="+usedCPU);
//	System.out.println("new idle = "+newIdle+", idle="+idle);
//	System.out.println("cycles = "+intervalCycles);
			cpuPercent = (int)((newUsedCPU-usedCPU)*100/intervalCycles);
		}
		user = newUser;
		nice = newNice;
		system = newSystem;
		idle = newIdle;
		iowait = newIowait;
		irq = newIrq;
		softirq = newSoftirq;
		return cpuPercent;
	}

	private int getMem() throws IOException {
		int totalMem = 0;
		int freeMem = 0;
		int cachedMem = 0;
		BufferedReader memReader = new BufferedReader(new FileReader(MEM_STAT_PATH));
		String line = memReader.readLine();
		while(line != null) {
			if(line.startsWith(TOTAL_MEM_STAT)) {
				Scanner scanner = new Scanner(line);
				scanner.next();
				totalMem = scanner.nextInt();
			} else if(line.startsWith(FREE_MEM_STAT)) {
				Scanner scanner = new Scanner(line);
				scanner.next();
				freeMem = scanner.nextInt();
			} else if(line.startsWith(CACHED_MEM_STAT)) {
				Scanner scanner = new Scanner(line);
				scanner.next();
				cachedMem = scanner.nextInt();
			}
			line = memReader.readLine();
		}
		memReader.close();
		return (totalMem-freeMem-cachedMem)*100/totalMem;
	}

	private void update(int cpuPercent, int memPercent) throws IOException {
		byte[] midiData = lightUpTo(percentToLEDIndex(cpuPercent));
		xorSingleLED(midiData, percentToLEDIndex(memPercent));
		if(verbose) {
			for(int i=0;i<midiData.length;i++) {
				System.out.print(String.format("%02X ", midiData[i]));
			}
			System.out.println();
		}
		vmeter.write(midiData);
	}

	private void xorSingleLED(byte[] midiData, int led) {
//	System.out.println("xor led = "+led);
		// none lit
		if(led == 0)
			return;
		led = validLED(led);
		int bytePos = (led-1)/7;
		int bit = (led-1)%7;
//	System.out.println("byte = "+bytePos);
//	System.out.println("bit = "+bit);
		// adjust bytePos based on midi data format
		// <cmd> <7 bits> <7 bits> <cmd> <7 bits> <7 bits> <cmd> <7 bits> <3 bits>
		if(bytePos > 3)
			bytePos+=3;
		else if(bytePos > 1)
			bytePos += 2;
		else
			bytePos++;
//	System.out.println("adjusted byte = "+bytePos);
//	System.out.println("data = "+midiData[bytePos]);
		midiData[bytePos] ^= 1 << bit;
//	System.out.println("data' = "+midiData[bytePos]);
	}

	private byte[] lightUpTo(int led) {
//	System.out.println("light up to = "+led);
		led = validLED(led);
		byte[] midiData = new byte[] { (byte)0xAD, 0x0, 0x0, (byte)0xAE, 0x0, 0x0, (byte)0xAF, 0x0, 0x0 };
		int fullBytes = led/7;
//	System.out.println("full byes = "+fullBytes);
		int byteOffset = 1;
		for(int i=0;i<fullBytes;i++) {
			// skip a command byte every 2 data bytes
//	System.out.println("mididData["+byteOffset+"] = 127");
			midiData[byteOffset] = (byte)127;
			byteOffset++;
			if(byteOffset%3 == 0)
				byteOffset++;
		}
		// write last byte with partial bits (leds) set
		midiData[byteOffset] = (byte)(1 << (led - fullBytes*7 + 1));
		midiData[byteOffset]--;
//	System.out.println("mididData["+byteOffset+"] = "+midiData[byteOffset]);

		return midiData;
	}

	private int percentToLEDIndex(int percent) {
		if(percent < 0)
			percent = 0;
		if(percent > 100)
			percent = 100;
		return (int)Math.round(LED_COUNT * (percent/100.0));
	}

	private int validLED(int led) {
		if(led<0)
			led = 0;
		else if(led>38)
			led = 38;
		return led;
	}
}
