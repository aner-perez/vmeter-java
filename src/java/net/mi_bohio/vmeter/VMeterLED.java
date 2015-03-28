package net.mi_bohio.vmeter;

import java.io.FileOutputStream;
import java.io.FileInputStream;

public class VMeterLED
{
	public static void main( String[] argv ) throws Exception {
		String midiDevice = "/dev/midi1";
		int millis = 250;
		int loopCount = 1;
		int argPos;
		for(argPos = 0; argPos < argv.length; argPos++) {
			if(!argv[argPos].startsWith("--")) {
				break;
			}
			if(argv[argPos].equals("--midi")) {
				midiDevice = "/dev/midi"+argv[++argPos];
			} else if (argv[argPos].equals("--dev")) {
				midiDevice = argv[++argPos];
			} else if (argv[argPos].equals("--delay")) {
				millis = Integer.parseInt(argv[++argPos]);
			} else if (argv[argPos].equals("--loop")) {
				loopCount = Integer.parseInt(argv[++argPos]);
			} else {
				System.err.println("unrecongnized option: "+argv[argPos]);
			}
		}
		int firstPattern = argPos;
		String pattern;
		for(int l = 0 ; l < loopCount; l++) {
			argPos = firstPattern;
			while(argPos < argv.length) {
				pattern = argv[argPos];

				// Make sure pattern covers all 38 LEDS
				if(pattern.length() < 38) {
					pattern = String.format("%-38s", pattern);
				} else {
					pattern = pattern.substring(0,38);
				}

	//			FileInputStream seq = new FileInputStream("/dev/snd/seq");
				FileOutputStream vmeter = new FileOutputStream(midiDevice);
				byte[] midiData = { (byte)0xAD, 0x0, 0x0, (byte)0xAE, 0x0, 0x0, (byte)0xAF, 0x0, 0x0 };
				String remainingPattern = pattern;
				for(int i = 0; i < 6;i++) {
					if(i<2) {
						midiData[i+1] = parseBits(remainingPattern);
					} else if(i<4) {
						midiData[i+2] = parseBits(remainingPattern);
					} else {
						midiData[i+3] = parseBits(remainingPattern);
					}
					if(remainingPattern.length() >= 7)
						remainingPattern = remainingPattern.substring(7);
				}
				vmeter.write(midiData);
				Thread.sleep(millis);
				argPos++;
			}
		}
	}
	private static byte parseBits(String pattern) {
		int bits = pattern.length() >= 7 ? 7 : 3;
		byte result = 0;
		for(int i = 0; i < bits; i++) {
			if(pattern.charAt(i) == '1') {
				result |= (byte)(1 << i);
			}
		}
		return result;
	}
}
/*
   //unsigned char rgbData[3] = {0x90, 60, 127};
	unsigned char rgbData[] = { 0xB0, 20, 0x31 }; // change control

	for(int iA=0; iA<cArg; iA++)
		{
		char *pszArg = rgpszArg[ iA ];

		if( !strcmp( pszArg, "--val" ) )
			{
			rgbData[ 2 ] = atoi( rgpszArg[ ++iA ] );
			}
		else if( !strcmp( pszArg, "--dev1" ) )
			{
            device = "/dev/midi1";
            }
		else if( !strcmp( pszArg, "--dev2" ) )
			{
            device = "/dev/midi2";
            }
		else if( !strcmp( pszArg, "--led1" ) )
			{
            rgbData[ 0 ] = 0xAD;
			unsigned int nVal = atoi( rgpszArg[ ++iA ] );
			rgbData[ 1 ] = 0x7F & nVal; // data bits 0:6
			rgbData[ 2 ] = 0x7F & (nVal>>7);
            }
		else if( !strcmp( pszArg, "--led2" ) )
			{
            rgbData[ 0 ] = 0xAE;
			unsigned int nVal = atoi( rgpszArg[ ++iA ] );
			rgbData[ 1 ] = 0x7F & nVal; // data bits 0:6
			rgbData[ 2 ] = 0x7F & (nVal>>7);
            }
		else if( !strcmp( pszArg, "--led3" ) )
			{
            rgbData[ 0 ] = 0xAF;
			unsigned int nVal = atoi( rgpszArg[ ++iA ] );
			rgbData[ 1 ] = 0x7F & nVal; // data bits 0:6
			rgbData[ 2 ] = 0x7F & (nVal>>7);
            }
		}


    //
   	// step 1: open the OSS device for writing
    //
   	int fd = open( device, O_WRONLY, 0 );
   	if ( fd < 0 )
		{
		printf("Error: cannot open %s\n", device);
		exit(1);
		}

    //
   	// step 2: write the MIDI information to the OSS device
    //
   	int cWrite = write( fd, rgbData, sizeof(rgbData) );
	printf( "<debug cWrite='%d'/>\n", cWrite );

    //
   	// step 3: (optional) close the OSS device
    //
   	close( fd );

   	return 0;
}
*/
// ////////////////////////////////////////////////////////////
// ////////////////////////////////////////////////////////////
// ////////////////////////////////////////////////////////////

