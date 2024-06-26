// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// Rivet Copyright (C) 2011 Ian Wraith
// This program comes with ABSOLUTELY NO WARRANTY

package org.e2k;

import java.awt.Color;

import javax.swing.JOptionPane;

public class FSK200500 extends FSK {
	
	private int baudRate=200;
	private int state=0;
	private double samplesPerSymbol;
	private Rivet theApp;
	public long sampleCount=0;
	private long symbolCounter=0;
	private StringBuilder lineBuffer=new StringBuilder();
	private CircularDataBuffer energyBuffer=new CircularDataBuffer();
	private int characterCount=0;
	private int highBin;
	private int lowBin;
	private boolean inChar[]=new boolean[7];
	private final int MAXCHARLENGTH=80;
	private int bcount;
	private long missingCharCounter=0;
	private long totalCharCounter=0;
	private double adjBuffer[]=new double[2];
	private int adjCounter=0;
	private double errorPercentage;
	
	public FSK200500 (Rivet tapp,int baud)	{
		baudRate=baud;
		theApp=tapp;
	}
	
	public void setBaudRate(int baudRate) {
		this.baudRate = baudRate;
	}

	public int getBaudRate() {
		return baudRate;
	}

	// Set the objects decode state and the status bar
	public void setState(int state) {
		this.state=state;
		if (state==1) theApp.setStatusLabel("Sync Hunt");
		else if (state==2) theApp.setStatusLabel("Decoding Traffic");
	}

	public int getState() {
		return state;
	}
		
	public boolean decode (CircularDataBuffer circBuf,WaveData waveData)	{
		// Just starting
		if (state==0)	{
			// Check the sample rate
			if (waveData.getSampleRate()!=8000.0)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nFSK200/500 recordings must have\nbeen recorded at a sample rate\nof 8 KHz.","Rivet", JOptionPane.INFORMATION_MESSAGE);
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
			lineBuffer.delete(0,lineBuffer.length());
			lettersMode=true;
			return true;
		}
		
		// Hunt for the sync sequence
		if (state==1)	{
			String dout;
			if (sampleCount>0) dout=syncSequenceHunt(circBuf,waveData);
			else dout=null;
			if (dout!=null)	{
				setState(2);
				theApp.writeLine(dout,Color.BLACK,theApp.italicFont);
				theApp.newLineWrite();
				energyBuffer.setBufferCounter(0);
				bcount=0;
				totalCharCounter=0;
				missingCharCounter=0;
			}
		}
				
		// Decode traffic
		if (state==2)	{
			// Only do this at the start of each symbol
			if (symbolCounter>=samplesPerSymbol)	{
				int ibit=fsk200500FreqHalf(circBuf,waveData,0);
				if (theApp.isInvertSignal()==true)	{
					if (ibit==0) ibit=1;
					else if (ibit==1) ibit=0;
				}
				// Is the bit stream being recorded ?
				if (theApp.isBitStreamOut()==true)	{
					if (ibit==1) theApp.bitStreamWrite("1");
					else if (ibit==0) theApp.bitStreamWrite("0");
					else if (ibit==2) theApp.bitStreamWrite("2");
					else if (ibit==3) theApp.bitStreamWrite("3");
				}
				// If this is a full bit add it to the character buffer
				// If it is a half bit it signals the end of a character
				if (ibit==2)	{
					totalCharCounter++;
					symbolCounter=(int)samplesPerSymbol/2;
					// If debugging display the character buffer in binary form + the number of bits since the last character and this baudot character
					if (theApp.isDebug()==true)	{
						String dout=getCharBuffer()+" ("+Integer.toString(bcount)+")  "+getBaudotChar();
						theApp.writeLine(dout,Color.BLACK,theApp.boldMonospaceFont);
					}
					else	{
						// Display the character in the standard way
						String ch=getBaudotChar();
						// LF
						if (ch.equals(getBAUDOT_LETTERS(2))) theApp.newLineWrite();
						// CR
						else if (ch.equals(getBAUDOT_LETTERS(8))) theApp.newLineWrite();
						else	{
							lineBuffer.append(ch);
							theApp.writeChar(ch,Color.BLACK,theApp.boldMonospaceFont);
							characterCount++;
							// Does the line buffer end with "162)5761" if so start a new line
							if (lineBuffer.lastIndexOf("162)5761")!=-1) characterCount=MAXCHARLENGTH;
							// Improve the formatting of messages which contain traffic
							if ((lineBuffer.length()>20)&&(lineBuffer.charAt(lineBuffer.length()-6)=='=')) characterCount=MAXCHARLENGTH;
							if ((lineBuffer.length()>20)&&(lineBuffer.charAt(lineBuffer.length()-6)==')')&&(lineBuffer.charAt(lineBuffer.length()-7)==' ')) characterCount=MAXCHARLENGTH;
						}
					}
					if (bcount!=7)	{
						missingCharCounter++;
				        errorPercentage=((double)missingCharCounter/(double)totalCharCounter)*100.0;
						// If more than 50% of the received characters are bad we have a serious problem
						if (errorPercentage>50)	{
							String dout=theApp.getTimeStamp()+" FSK200/500 Sync Lost";
							theApp.writeLine(dout,Color.BLACK,theApp.italicFont);
							setState(1);
						}
					}
					bcount=0;
				}
				else	{
					addToCharBuffer(ibit);
					symbolCounter=adjAdjust();
				}
				// If the character count has reached MAXCHARLENGTH then display this line
				if (characterCount>=MAXCHARLENGTH)	{
					theApp.newLineWrite();
					lineBuffer.delete(0,lineBuffer.length());
					characterCount=0;
				}
			}
		}
		sampleCount++;
		symbolCounter++;
		return true;				
	}
	
	// Look for a sequence of 4 alternating tones with 500 Hz difference
	private String syncSequenceHunt (CircularDataBuffer circBuf,WaveData waveData)	{
		int difference;
		// Get 4 symbols
		int freq1=fsk200500Freq(circBuf,waveData,0);
		int bin1=getFreqBin();
		// Check this first tone isn't just noise
		if (getPercentageOfTotal()<5.0) return null;
		int freq2=fsk200500Freq(circBuf,waveData,(int)samplesPerSymbol*1);
		int bin2=getFreqBin();
		// Check we have a high low
		if (freq2>freq1) return null;
		// Check there is around 500 Hz of difference between the tones
		difference=freq1-freq2;
		if ((difference<475)||(difference>525) ) return null;
		int freq3=fsk200500Freq(circBuf,waveData,(int)samplesPerSymbol*2);
		// Don't waste time carrying on if freq1 isn't the same as freq3
		if (freq1!=freq3) return null;
		int freq4=fsk200500Freq(circBuf,waveData,(int)samplesPerSymbol*3);
		// Check 2 of the symbol frequencies are different
		if ((freq1!=freq3)||(freq2!=freq4)) return null;
		// Check that 2 of the symbol frequencies are the same
		if ((freq1==freq2)||(freq3==freq4)) return null;
		// Store the bin numbers
		if (freq1>freq2)	{
			highBin=bin1;
			lowBin=bin2;
		}
		else	{
			highBin=bin2;
			lowBin=bin1;
		}
		// If either the low bin or the high bin are zero there is a problem so return false
		if ((lowBin==0)||(highBin==0)) return null;
		String line=theApp.getTimeStamp()+" FSK200/500 Sync Sequence Found";
		return line;
	}
	
	// Find the frequency of a FSK200/500 symbol
	// Currently the program only supports a sampling rate of 8000 KHz
	private int fsk200500Freq (CircularDataBuffer circBuf,WaveData waveData,int pos)	{
		// 8 KHz sampling
		if (waveData.getSampleRate()==8000.0)	{
			int freq=doFSK200500_8000FFT(circBuf,waveData,pos,(int)samplesPerSymbol);
			return freq;
		}
		return -1;
	}
	
	// The "normal" way of determining the frequency of a FSK200/500 symbol
	// is to do two FFTs of the first and last halves of the symbol
	// that allows us to use the data for the early/late gate and to detect a half bit (which is used as a stop bit)
	private int fsk200500FreqHalf (CircularDataBuffer circBuf,WaveData waveData,int pos)	{
		int v;
		int sp=(int)samplesPerSymbol/2;
		// First half
		double early[]=do64FFTHalfSymbolBinRequest (circBuf,pos,sp,lowBin,highBin);
		// Last half
		double late[]=do64FFTHalfSymbolBinRequest (circBuf,(pos+sp),sp,lowBin,highBin);
		// Determine the symbol value
		int high1,high2;
		if (early[0]>early[1]) high1=0;
		else high1=1;
		if (late[0]>late[1]) high2=0;
		else high2=1;
		// Both the same
		if (high1==high2)	{
			if (high1==0) v=1;
			else v=0;
		}
		else	{
			// Test if this really could be a half bit
			if (checkValid()==true)	{
				// Is this a stop bit
				if (high2>high1) v=2;
				// No this must be a normal full bit
				else if ((early[0]+late[0])>(early[1]+late[1])) v=1;
				else v=0;
			}
			else	{
				// If there isn't a vaid baudot character in the buffer this can't be a half bit and must be a full bit
				if ((early[0]+late[0])>(early[1]+late[1])) v=1;
				else v=0;
			}
		}
		
		// Early/Late gate code
		// was <2
		if (v<2)	{
			double lowTotal=early[0]+late[0];
			double highTotal=early[1]+late[1];
			if (lowTotal>highTotal) addToAdjBuffer(getPercentageDifference(early[0],late[0]));
			else addToAdjBuffer(getPercentageDifference(early[1],late[1]));
			
			//theApp.debugDump(Double.toString(early[0])+","+Double.toString(late[0])+","+Double.toString(early[1])+","+Double.toString(late[1]));
			
		}
		
	return v;
	}
	
	// Add incoming data to the character buffer
	private void addToCharBuffer (int in)	{
		int a;
		for (a=1;a<inChar.length;a++)	{
			inChar[a-1]=inChar[a];
		}
		if (in==0) inChar[6]=false;
		else inChar[6]=true;
		// Increment the bit counter
		bcount++;
	}
	
	// Display the inChar buffer in binary when in debug mode
	private String getCharBuffer()	{
		StringBuilder lb=new StringBuilder();
		int a;
		for (a=0;a<7;a++)	{
			if (inChar[a]==true) lb.append("1");
			else lb.append("0");
			if ((a==0)||(a==5)) lb.append(" ");
		}
		return lb.toString();
	}
	
	// Returns the baudot character in the character buffer
	private String getBaudotChar()	{
		int a=0;
		if (inChar[5]==true) a=16;
		if (inChar[4]==true) a=a+8;
		if (inChar[3]==true) a=a+4;
		if (inChar[2]==true) a=a+2;
		if (inChar[1]==true) a++;
		// Look out for figures or letters shift characters
		if (a==0)	{
			return "";
		}
		else if (a==27)	{
			lettersMode=false;
			return "";
		}
		else if (a==31)	{
			lettersMode=true;
			return "";
		}
		// Only return numbers when in FSK200/500 decode mode
		//else if (lettersMode==true) return BAUDOT_LETTERS[a];
		else return getBAUDOT_NUMBERS(a);
	}
	
	// Check if this a valid Baudot character this a start and a stop
	private boolean checkValid()	{
		if ((inChar[0]==false)&&(inChar[6]==true)&&(bcount>=7)) return true;
		else return false;
	}
	
	// Add a comparator output to a circular buffer of values
	private void addToAdjBuffer (double in)	{
		adjBuffer[adjCounter]=in;
		adjCounter++;
		if (adjCounter==adjBuffer.length) adjCounter=0;
	}
	
	// Return the average of the circular buffer
	private double adjAverage()	{
		int a;
		double total=0.0;
		for (a=0;a<adjBuffer.length;a++)	{
			total=total+adjBuffer[a];
		}
		return (total/adjBuffer.length);
	}
	
	// Get the average value and return an adjustment value
	private int adjAdjust()	{
		double av=adjAverage();
		double r=Math.abs(av)/5;
		if (av<0) r=0-r;
		//theApp.debugDump(Double.toString(av)+","+Double.toString(r));
		//r=0;
		return (int)r;
	}	

	// Return a quality indicator
	public String getQuailty()	{
		String line="Missing characters made up "+String.format("%.2f",errorPercentage)+"% of this message ("+Long.toString(missingCharCounter)+" characters missing)";
		return line;
	}

	
	
}
