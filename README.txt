Rivet is a free open source decoder of various HF data modes which interest members 
of the Enigma 2000 group.

Webpage: http://www.signalshed.com
E-mail: enigma2000@gaggle.email

and the UDXF group ..

https://groups.io/g/UDXF

A binary version of this program which will run without being compiled is available from ..

http://www.signalshed.com/rivet/

Currently the program decodes the modes ..

Baudot (various speeds) : Used by amateurs , weather stations and other users.
CCIR493-4 : A HF selective calling mode
CIS36-50 (50 baud only currently) : Used by the Russian Navy.
CROWD36 : Used for Russian diplomatic and intelligence messages
FSK200/500 : Used for Russian diplomatic and intelligence messages
FSK200/1000 : Used for Russian diplomatic and intelligence messages
FSK (raw) : For advanced users to investigate unknown FSK modes
GW FSK (100 baud) : A commercial ship to shore data system.
XPA (10 and 20 baud) : Believed used for Russian intelligence messages.
XPA2 : Believed used for Russian intelligence messages.

but more modes will be added soon. The program can decode directly from your PCs soundcard or from a WAV file.

Rivet is written in Java so it should run on any PC which has Java installed.
If you haven't got Java then you can download it for free from ..

https://www.java.com

This program uses the excellent JTransforms library ..

https://github.com/wendykierp/JTransforms

The main problem users have reported when trying to run Rivet under MS Windows is that a program other than Java has taken
ownership of the .JAR extension. If you have this problem try running this program ..

https://johann.loefflmann.net/en/software/jarfix/index.html

(Thanks to Mario for that)

Change Log
----------

Build 04 adds an online help facility and XPA2 decoding.
Build 05 adds the foundations of CROWD36 decoding and hopefully fixes the logging bug.
Build 06 added basic CROWD36 decoding
Build 07 allows direct sound card decoding.
Build 08 fixed a bug which caused the program to read from the sound card after a WAV file had been loaded.
         also improved XPA and XPA2 symbol timing and support for 20 baud XPA.
Build 09 improves CROWD36 decoding and adds the copy to clipboard feature.
Build 10 adds basic binary CIS 36-50 decoding and the option to define the CROWD36 high sync tone in use.
Build 11 improves the display of CIS 36-50 messages.
Build 12 adds basic FSK200/500 decoding and improved decoding of CIS 36-50
Build 13 allows settings to be saved and then reloaded at startup
Build 14 auto detects if the signal needs to be inverted when decoding XPA2.
Build 15 detects the start and end of CIS36-50 messages.
Build 16 displays CIS36-50 traffic in 7 bit blocks.
Build 17 incorporates information received on the make up of CIS36-50 messages
Build 18 adds an early/late gate to improve CIS36-50 symbol sync
Build 19 further improves the CIS36-50 early/late gate and improves FSK200/500 decoding
Build 20 much better decoding of CIS36-50 and FSK200/500.
Build 21 added experimental CCIR493-4 decoding
Build 22 yet more experimental CCIR493-4 decoding features
Build 23 adds CCIR493-4 decoding
Build 24 fixes a bug in CCIR493-4 decoding putting leading zeros in front of station identity sections
Build 25 adds basic error correction to the CCIR493-4 mode 
Build 26 adds a input level slider plus other improvements to the look of the status bar also improve the FSK200/500 code. 
Build 27 adds further CCIR493-4 debugging data to try and find out why weak signals are being lost
Build 28 hopefully further improve the detection of weak CCIR493-4 signals
Build 29 improved FSK200/1000 decoding
Build 30 adds proportional early/late gate control to the FSK modes.
Build 31 Improved input level control which can attenuate,improved CIS36-50 decoding,FSK200/500 status bar updating,
		 improved FSK200/1000 binary decoding
Build 32 adds basic FSK200/1000 frame decoding
Build 33 fixes an ITA3 character set bug , checks for 8 bit WAV files and enables bitstream outputs to be saved
Build 34 Inverts the FSK200/1000 bit demodulation and extracts the block number from a block. Also add very basic
         GW 100 baud FSK support.
Build 35 Fixes a bug which calculated the total number of FSK200/1000 blocks in a message.

         From build 36 onwards Java 7 is required to run Rivet.

Build 36 Adds baudot decoding and limits bitstream out lines to 80 characters.
Build 37 Improves the display so that as characters are received they are displayed rather than waiting until
         an entire line was received then displaying it. Also now display received messages in bold and display
         other information in italic. 
Build 38 Fix a null pointer exception in the log file code.    
Build 39 Improve the detection of loss of baudot signals. Use the same code in the GW object to hopefully size
         packets correctly.    
Build 40 Add the FSK (Raw) decoding mode.
Build 41 Add further FSK/Baudot shifts and baud rates plus the new trigger feature
Build 42 Change some of the GW identifiers , add date and addressee identifiers to the FSK200/1000 decode module
Build 43 Display CIS36-50 messages as hexadecimal bytes , display the date in block 1 FSK200/1000 msgs of any size
Build 44 Fix a spelling mistake in the GW identifiers and add a menu item to link to the new upload page. 
Build 45 Add RDFT detection , a 625 Hz FSK shift and very basic ASCII decoding of GW FSK traffic.
Build 46 Improve the decoding and display of GW ship side packets.
Build 47 Add partial decoding of the GW character set.
Build 48 Fix a bug in the GW character set.
Build 49 Package GW position reports together and display them on a single line.
Build 50 Display GW short packets on a single line and add a clear screen option.
Build 51 Display the GW 8 bit packets in raw binary form.
Build 53 Various improvements to the GW FSK decoding side of things.
Build 54 Allow Triggers to be added , deleted and edited within the program
Build 55 Fix a bug in the trigger code which allowed a STOP to activate before a START.
Build 56 Increase the number of display lines , add a audio selection source menu item and a GW position report time out
Build 57 Fix a bug in the screen display which meant certain lines weren't being displayed.
Build 58 Fix yet another bug in the screen display code. Hope this is it !
Build 59 Display GW 2/101 contents as hex. Always display the contents of 5/86 packets.
Build 60 Improve the detection and decoding of GW FSK packets.
Build 61 Add a newline after the RTTY sync info line.
         Decode and display much more information from FSK200/1000 data.
         Improve the speed and generally accuracy of the XPA and XPA2 decoders.
         Display ships MMSIs in GW FSK 2/101 packets
Build 62 Improve the decoding of MMSI data in GW 2/101 packets  
Build 63 Fix a bug handling GW channel free markers
         Improve the usability of the volume bar
         Hopefully fix the GW MMSI decoding problems
Build 64 Add a clear display button
         Add a pause display button
         Add support for the ships.xml file
Build 65 Ensure the display is set wide enough for the entire status bar
         Add the auto scrolling feature
Build 66 Display the Clear and Pause display buttons on the status bar in their own pane on top of each other to make more room.
         Remove the code which made sure the Rivet window was of a certain size
Build 67 Display error messages from parsing the ships.xml file
Build 68 Display system information for diagnostics
Build 69 Add a small screen mode which removes the buttons from the status bar to make space
Build 70 Return a "Clear Display" item to the menu
         Handle shore side 2/101 packets differently to ship side ones
Build 71 Make a change to the GW byte identifiers
         Add the "Display Bad Packets" menu item. If this isn't enabled then only known GW packet types will be displayed
Build 72 Add an option to show time stamps in UTC
Build 73 Credit UDXF group,Add further decoding of GW especially the shore side (once again big thanks to Alan W)
Build 74 Record all MMSIs received and display them at the end of GW logs
Build 75 Assorted small changes to the Raw FSK module and trigger handling                
         Fix the bug where if the user cleared the screen it wouldn't scroll back to the top. 
         Fix assorted spelling mistakes and bad idents in the GW base ident identifiers
         Write all audio mixer stage errors to a file to enable latter analysis      
Build 76 Add further audio source debugging information
Build 77 Add yet more audio source debugging information and the getMixer() method in the AudioMixer class.
Build 78 Add further audio source debugging information
Build 79 Ensure that the only audio sources added to the menu are capture ones
Build 80 Modify the setupAudio() method in the InputThread class
Build 81 Remove the debugging information put in for builds 76 to 80
Build 82 Allow the mouse wheel to be used for scrolling
         Improve the performance of the FSK(Raw) module
         Add support for 300 baud FSK
         Fix a bug where the program would keep trying to process a WAV file that was in the incorrect format
Build 83 Make the baud rate,shift and stop boxes selections in the Baudot/FSK options box into combo boxes
         Remove references to the RDFT and AT3x01 code in the main Rivet class
Build 84 Fix a bug in the RTTY/FSK shift selection dialog box
         Add CRs to the end of each line in rivet_settings.xml to improve readability
Build 85 Fix various problems in the FSK (Raw) mode
Build 86 Support both 200 Hz and 250 Hz CIS36-50 mode shifts
Build 87 Add support for a 500 Hz CIS36-50 mode shift
Build 88 Add support for 75 Hz and 400 Hz CIS36-50 mode shifts
Build 89 Improve the decoded CROWD36 display and add a debug output
         Display the CROWD36 sync tone number at the start of a message
         Add 800 Hz shift support for RTTY and raw FSK
         Add 145 baud support for RTTY and raw FSK
         Add 150 baud support for RTTY and raw FSK
Build 90 Rewrite the FSK200/1000 block parser to use the newest findings
         Fix the XPA2 decoder being unreliable after a while
Build 91 Add selectable F06a decoding in two different formats (ASCII and binary)
         Improve F06a detection when using FSK200/1000
         Improve FSK200/1000 metadata block displays
         Minor fixes and display changes in FSK200/500, XPA, XPA2, Baudot
         Add and update some links
         Gradle wrapper update to 7.5
                
Reported Bugs
-------------
Still problems with ..

CWOWD36 - Lack a fundamental understanding of this mode. Suspect a unknown tone to alphabet map is in use.

CIS36-50 - Messages are OK from stations that start transmitting idle briefly but Rivet has problems with 
messages from stations which idle constantly.
