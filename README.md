# vmeter-java

## Java classes

**net.mi_bohio.vmeter.VMeterLED** - Lights the specified LED pattern(s) on the VMeter device

java net.mi_bohio.vmeter.VMeterLED [--midi \<number>] [--dev \<device file>] [--delay \<millis>] [--loop \<count>] \<pattern> [\<pattern>...]

> **--midi \<number>** - Which midi device to use (/dev/midi0, /dev/midi1 etc.)

> **--dev \<device file>** - Alternate way of specifying the device file for the VMeter

> **--delay \<millis>** - Milliseconds to wait between displaying the patterns specified on the command line

> **--loop \<count>** - Number of times to display the patterns specified on the command line

> **\<pattern>** - The pattern of leds to light on the VMeter.  There are 38 LEDs in total, the pattern is a string of 38 digits with 0 being *OFF* and 1 being *ON* 

**net.mi_bohio.vmeter.VMeterCPU** - Displays CPU and memory usage on VMeter LEDs and allows volume controller by touching VMeter.  Memory usage is displayed by a single LED superimposed (using an XOR) on the CPU display. Volume control is achieved by calling "amixer" command.

> **--midi \<number>** - Which midi device to use (/dev/midi0, /dev/midi1 etc.)

> **--dev \<device file>** - Alternate way of specifying the device file for the VMeter

> **--interval \<millis>** - Milliseconds to delay between CPU/memory display updates
