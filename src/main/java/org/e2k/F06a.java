// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// Rivet Copyright (C) 2011 Ian Wraith
// Copyright (C) 2017 Daniel Ekmann
// Copyright (C) 2023 priyom.org
// This program comes with ABSOLUTELY NO WARRANTY

package org.e2k;

import java.awt.Color;
import javax.swing.JOptionPane;

public class F06a extends FSK2001000 {
	private int msgStartPos[]=new int[8];
    private int encodingType; //0: blocks will be parsed as raw binary; 1: blocks will be parsed as ASCII

    public F06a (Rivet tapp,int baud)	{
		super(tapp, baud);
		encodingType = 0;
	}

	public void setState(int state) {
		this.state=state;
		if (state==1) theApp.setStatusLabel("Sync Hunt");
		else if (state==2 && encodingType==0) theApp.setStatusLabel("Msg Hunt Binary");
        else if (state==2 && encodingType==1) theApp.setStatusLabel("Msg Hunt ASCII");
		else if (state==3) theApp.setStatusLabel("Debug Only");
	}

    public void setEncoding(int encoding){
        this.encodingType=encoding;
		if (state==2 && encodingType==0) theApp.setStatusLabel("Msg Hunt Binary");
        else if (state==2 && encodingType==1) theApp.setStatusLabel("Msg Hunt ASCII");
    }

    public int getEncoding(){
        return encodingType;
    }

    public boolean decode (CircularDataBuffer circBuf,WaveData waveData)	{
		// Just starting
		if (state==0)	{
			// Check the sample rate
			if (waveData.getSampleRate()!=8000.0 && waveData.getSampleRate()!=12000.0)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nF06a recordings must have\nbeen recorded at a sample rate\nof 8 KHz or 12 Khz.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return false;
			}
			// Check this is a mono recording
			if (waveData.getChannels()!=1)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"Rivet can only process\nmono WAV files.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return false;
			}
			// Check this is a 16 bit WAV file
			if (waveData.getSampleSizeInBits()!=16)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"Rivet can only process\n16 bit WAV files.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return false;
			}
			samplesPerSymbol=samplesPerSymbol(baudRate,waveData.getSampleRate());
			setState(1);
			// sampleCount must start negative to account for the buffer gradually filling
			sampleCount=0-circBuf.retMax();
			symbolCounter=0;
			// Clear the energy buffer
			energyBuffer.setBufferCounter(0);
			// Clear the display side of things
			characterCount=0;
			lettersMode=true;
            txType=0;
			return true;
		}

		// Hunt for the sync sequence
		else if (state==1)	{
			String dout;
			if (sampleCount>0) dout=syncSequenceHunt(circBuf,waveData);
			else dout=null;
			if (dout!=null)	{
				theApp.writeLine(dout,Color.BLACK,theApp.boldFont);
				if (theApp.isDebug()==true) setState(3);
				else setState(2);
				energyBuffer.setBufferCounter(0);
				bitsSinceLastBlockHeader=0;
			}
		}

		// Main message decode section
		else if (state==2)	{
			// Only do this at the start of each symbol
			if (symbolCounter>=samplesPerSymbol)	{
				symbolCounter=0;
				boolean ibit=fsk2001000FreqHalf(circBuf,waveData,0);
				circularBitSet.add(ibit);
				bitCount++;
				bitsSinceLastBlockHeader++;
				// Compare the first 32 bits of the circular buffer to the known FSK200/1000 header
				int difSync=compareSync(circularBitSet.extractSection(0,32));
				// If there are no or just 1 differences this is a valid block so process it
				if (difSync<2) processBlock();
				// If there have been more than 2880 bits with a header (i.e 10 blocks) we have a serious problem
				if (bitsSinceLastBlockHeader>2880) setState(1);
			}
		}
		// Debug only
		else if (state==3)	{
			if (symbolCounter>=samplesPerSymbol)	{
				symbolCounter=0;
				boolean ibit=fsk2001000FreqHalf(circBuf,waveData,0);
				if (ibit==true) theApp.writeChar("1",Color.BLACK,theApp.boldFont);
				else theApp.writeChar("0",Color.BLACK,theApp.boldFont);
				characterCount++;
				// Display MAXCHARLENGTH characters on a line
				if (characterCount==MAXCHARLENGTH)	{
					theApp.newLineWrite();
					characterCount=0;
				}
			}
		}
		sampleCount++;
		symbolCounter++;
		return true;
	}

    // Process a FSK200/1000 block
	private void processBlock() {
		if (bitCount > 288) missingBlockCount = missingBlockCount + (bitCount / 288);
		int[] frame = circularBitSet.returnInts();

		// Check whether this block is the in-TX repeat divider.
		if (checkDividerBlock(frame)) {
			theApp.writeLine("----------------------------------------------------------", Color.BLUE, theApp.boldFont);
			return;
		}

		// First 11 bits after the sync sequence are the frame index.
		int frameIndex = (frame[4] << 3) | ((frame[5] & 224) >> 5);

		// Extract the 128 bits of data contained in the frame.
		int[] data = new int[16];
		data[0] = (frame[6] & 0xf0) | ((frame[8] >> 4) & 0x0f);
		data[1] = (frame[10] & 0xf0) | ((frame[12] >> 4) & 0x0f);
		data[2] = (frame[14] & 0xf0) | ((frame[16] >> 4) & 0x0f);
		data[3] = (frame[18] & 0xf0) | ((frame[20] >> 4) & 0x0f);
		data[4] = (frame[22] & 0xf0) | (frame[6] & 0x0f);
		data[5] = ((frame[8] << 4) & 0xf0) | (frame[10] & 0x0f);
		data[6] = ((frame[12] << 4) & 0xf0) | (frame[14] & 0x0f);
		data[7] = ((frame[16] << 4) & 0xf0) | (frame[18] & 0x0f);
		data[8] = ((frame[20] << 4) & 0xf0) | (frame[22] & 0x0f);
		data[9] = (frame[7] & 0xf0) | ((frame[9] >> 4) & 0x0f);
		data[10] = (frame[11] & 0xf0) | ((frame[13] >> 4) & 0x0f);
		data[11] = (frame[15] & 0xf0) | ((frame[17] >> 4) & 0x0f);
		data[12] = (frame[19] & 0xf0) | ((frame[21] >> 4) & 0x0f);
		data[13] = (frame[23] & 0xf0) | (frame[7] & 0x0f);
		data[14] = ((frame[9] << 4) & 0xf0) | (frame[11] & 0x0f);
		data[15] = ((frame[13] << 4) & 0xf0) | (frame[15] & 0x0f);

		// Extract the 16-bit CRC and verify it.
		int crc = ((frame[17] << 12) & 0xffff) | ((frame[19] << 8) & 0xfff) | ((frame[21] << 4) & 0xff) | (frame[23] & 0xf);
		boolean isValid = crc == crcCalculator.compute(data);

		// Info frame, sent as frames #0, #16, #32, #48, ...
		if (frameIndex % 16 == 0) {
			int frameCount = (data[0] << 3) | (data[1] >> 5);
			int messageCount = data[1] & 0x1f;
			if (messageCount > 7) isValid = false; //There cannot be more than 7 messages in F06
			theApp.writeLine(String.format("[#%d] [INFO] %d blocks, %d message(s) | CRC %s", frameIndex, frameCount, messageCount, isValid ? "OK" : "ERROR"), isValid ? Color.BLUE : Color.RED, theApp.boldFont);
			if (isValid){
				theApp.writeLine(String.format("[INFO] %s", processMetadataBlock(data)), Color.BLUE, theApp.boldFont);
			} 
			return;
		}

		if (frameIndex == 1 && data[0] == 0x34 && data[1] == 0x36) {
			// Check whether this is a pre-transmission test.
			txType = 1;
		}  else if (frameIndex == 1 && data[0] == 0x1b) {
            // Check this is standard F06
            theApp.writeLine(String.format("[INFO] Standard F06 header block detected. Switching to F06 decoding..."), Color.BLUE, theApp.boldFont);
            theApp.setSystem(8);
			theApp.setModeLabel(theApp.MODENAMES[8]);
			transferSyncData(theApp.fsk2001000Handler);
			txType=2;
		}

		if (txType == 0) {
			//Print header blocks
            if (isHeaderBlock(frameIndex)){
				theApp.writeLine(String.format("[#%d] [INFO] File named %s, size %d bytes | CRC %s", frameIndex, getF06aFilename(data), getFileSize(data), isValid ? "OK" : "ERROR"), isValid ? Color.BLUE : Color.RED, theApp.boldFont);
				return;
			}
			else if (isHeaderBlock(frameIndex - 1)){
                theApp.writeLine(String.format("[#%d] [INFO] CRC of file contents: 0x%02x%02x%02x%02x. File contents now follow... | CRC %s", frameIndex, data[0], data[1], data[2], data[3], isValid ? "OK" : "ERROR"), isValid ? Color.BLUE : Color.RED, theApp.boldFont);
            }
				
			String contents = "";
			if (encodingType == 1){
                //Parse ASCII
				contents=processF06aASCII(data, frameIndex, isValid);
            }
            if (encodingType == 0){
                //Parse raw binary
				contents = processF06aBinary(data, frameIndex, isValid);
			}
			//Print normal block
			theApp.writeLine(String.format("[#%d] %s | CRC %s", frameIndex, contents, isValid ? "OK" : "ERROR"), isValid ? Color.BLACK : Color.RED, theApp.boldMonospaceFont);
		}
			
		else if (txType == 1) {
			theApp.writeLine(String.format("[#%d] %c%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c | CRC %s", frameIndex, data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7], data[8], data[9], data[10], data[11], data[12], data[13], data[14], data[15], isValid ? "OK" : "ERROR"), isValid ? Color.BLACK : Color.RED, theApp.boldMonospaceFont);
		} 

		bitCount=0;
		bitsSinceLastBlockHeader=0;
		blockCount++;
	}

	//Convert big endian file size from an F06a block 1 into an integer
	private int getFileSize(int[] dataBlock){
        int offset = 0;
		int value = dataBlock[offset];
        while (++offset < 4) {
            value <<= 8;
            value |= dataBlock[offset] & 0xFF;
        }
        return value;
	}

	//Convert the hex from F06a block 1 into a readable ASCII filename and return non-printable characters (except NUL) as hex
	private String getF06aFilename(int[] dataBlock){
		String filename = "";
		for(int i=4;i<16;i++){
			if ((dataBlock [i] < 32 || dataBlock [i] >= 127) && dataBlock[i] != 0){
				filename += String.format("[0x%02x]", dataBlock[i]);
			}
			else if (dataBlock [i] != 0){
				filename += (char)dataBlock [i];
			}
		}
		return filename;
	}

	private String processF06aBinary(int[] dataBlock, int frameIndex, boolean isValid){
		String contents = "";
		for (int i=0;i<16;i++){
			if (isHeaderBlock(frameIndex-1) && i<4) {
                //First 4 bytes of second block of a message are CRC of file contents so they can be omitted
                i=4;
            }
			contents += String.format("%02x", dataBlock[i]);
		}
		return contents;
	}

    //Convert the hex from a regular F06a block into readable ASCII, detect EOF and new lines automatically.
    private String processF06aASCII(int[] dataBlock, int frameIndex, boolean isValid){
        String contents = "";
		int nonPrintableChars = 0;
		for (int i=0;i<16;i++){
            if (isHeaderBlock(frameIndex-1) && i<4) {
                //First 4 bytes of second block of a message are CRC of file contents so they can be omitted
                i=4;
            }
            if (dataBlock [i] == 0 && isValid){
                //Detect EOF NUL terminator. Reject if CRC is not valid to avoid data loss
                contents += "<EOF>";
				return contents;
            }
            else if (dataBlock [i] == 13 && isValid){
                //Detect a carrier return
                contents += "<CR>";
            }
			else if (dataBlock[i] == 10 && isValid){
				//Detect a line feed
				contents += "<LF>";
			}
            else if ((dataBlock [i] < 32 || dataBlock [i] >= 127)){
                //Detect non-printable characters and print them as hex
                contents += String.format("[0x%02x]", dataBlock[i]);
				nonPrintableChars++;
            }
            else{
                contents += String.format("%c", dataBlock[i]);
             }
        }
		if (nonPrintableChars > 10 && isValid){
			//If we have a lot of non-printable characters with a valid CRC the file might not be encoded as ASCII
			theApp.setF06aASCII(false);
			theApp.writeLine("[INFO] This F06a file appears to be formatted in a non-ASCII format! Decoding will continue in raw binary.", Color.BLUE, theApp.boldFont);
		}
		return contents;
    }

	//Returns a list of blocks where a message start is. Also updates the global message table
	private String processMetadataBlock(int da[]){
		int positions[]= new int[8];
		positions[0] = (da[2] << 3) | (da[3] >> 5);
		positions[1] = (da[4] << 3) | (da[5] >> 5);
		positions[2] = (da[6] << 3) | (da[7] >> 5);
		positions[3] = (da[8] << 3) | (da[9] >> 5);
		positions[4] = (da[10] << 3) | (da[11] >> 5);
		positions[5] = (da[12] << 3) | (da[13] >> 5);
		positions[6] = (da[14] << 3) | (da[15] >> 5);
		
		String msgList ="";
		int index=0;
		while (positions[index] != 0 && index < 8){
			if (positions[index] != 0){
				if (index != 0) msgList += ", ";
				msgList += String.format("Message %d at block #%d", index + 1, positions[index]);
			}
			index++;
		}

		msgStartPos = positions;
		return msgList;
	} 

	//Checks if the given block index contains the start of a message
	private boolean isHeaderBlock(int index){
		for (int i=0;i<8;i++){
			if (msgStartPos[i] == index && msgStartPos[i] != 0) return true;
		}
		return false;
	}
}
