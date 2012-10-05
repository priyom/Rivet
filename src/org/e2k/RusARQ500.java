package org.e2k;

import javax.swing.JOptionPane;

public class RusARQ500 extends FSK {
	
	private int baudRate=50;
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
	
	public RusARQ500 (Rivet tapp,int baud)	{
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
	
	public String[] decode (CircularDataBuffer circBuf,WaveData waveData)	{
		String outLines[]=new String[2];
		
		// Just starting
		if (state==0)	{
			// Check the sample rate
			if (waveData.getSampleRate()!=8000.0)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nRus-ARQ recordings must have\nbeen recorded at a sample rate\nof 8 KHz.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return null;
			}
			// Check this is a mono recording
			if (waveData.getChannels()!=1)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"Rivet can only process\nmono WAV files.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return null;
			}
			// Check this is a 16 bit WAV file
			if (waveData.getSampleSizeInBits()!=16)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"Rivet can only process\n16 bit WAV files.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return null;
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
			lineBuffer.delete(0,lineBuffer.length());
			return null;
		}
		
		// Hunt for the sync sequence
		if (state==1)	{
			if (sampleCount>0) outLines[0]=syncSequenceHunt(circBuf,waveData);
			if (outLines[0]!=null)	{
				setState(2);
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
				int ibit=rusARQFreqHalf(circBuf,waveData,0);
				if (theApp.isInvertSignal()==true)	{
					if (ibit==0) ibit=1;
					else ibit=1;
				}
				// If this is a full bit add it to the character buffer
				// If it is a half bit it signals the end of a character
				if (ibit==2)	{
					totalCharCounter++;
					symbolCounter=(int)samplesPerSymbol/2;
					// If debugging display the character buffer in binary form + the number of bits since the last character and this baudot character
					if (theApp.isDebug()==true)	{
						lineBuffer.append(getCharBuffer()+" ("+Integer.toString(bcount)+")  "+getBaudotChar());
						characterCount=MAXCHARLENGTH;
					}
					else	{
						// Display the character in the standard way
						String ch=getBaudotChar();
						// LF
						if (ch.equals(getBAUDOT_LETTERS(2))) characterCount=MAXCHARLENGTH;
						// CR
						else if (ch.equals(getBAUDOT_LETTERS(8))) characterCount=MAXCHARLENGTH;
						else	{
							lineBuffer.append(ch);
							characterCount++;
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
					outLines[0]=lineBuffer.toString();
					lineBuffer.delete(0,lineBuffer.length());
					characterCount=0;
				}
			}
		}
		sampleCount++;
		symbolCounter++;
		return outLines;				
	}
	
	// Look for a sequence of 4 alternating tones with 500 Hz difference
	private String syncSequenceHunt (CircularDataBuffer circBuf,WaveData waveData)	{
		int difference;
		// Get 4 symbols
		int freq1=rusARQFreq(circBuf,waveData,0);
		int bin1=getFreqBin();
		// Check this first tone isn't just noise
		if (getPercentageOfTotal()<5.0) return null;
		int freq2=rusARQFreq(circBuf,waveData,(int)samplesPerSymbol*1);
		int bin2=getFreqBin();
		// Check we have a high low
		if (freq2>freq1) return null;
		// Check there is around 500 Hz of difference between the tones
		difference=freq1-freq2;
		if ((difference<475)||(difference>525) ) return null;
		int freq3=rusARQFreq(circBuf,waveData,(int)samplesPerSymbol*2);
		// Don't waste time carrying on if freq1 isn't the same as freq3
		if (freq1!=freq3) return null;
		int freq4=rusARQFreq(circBuf,waveData,(int)samplesPerSymbol*3);
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
		String line=theApp.getTimeStamp()+" Rus-ARQ Sync Sequence Found";
		return line;
	}
	
	// Find the frequency of a Rus-ARQ symbol
	// Currently the program only supports a sampling rate of 8000 KHz
	private int rusARQFreq (CircularDataBuffer circBuf,WaveData waveData,int pos)	{
		// 8 KHz sampling
		if (waveData.getSampleRate()==8000.0)	{
			int freq=do160FFT(circBuf,waveData,pos);
			return freq;
		}
		return -1;
	}
	
	// The "normal" way of determining the frequency of a Rus-ARQ symbol
	// is to do two FFTs of the first and last halves of the symbol
	// that allows us to use the data for the early/late gate and to detect a half bit (which is used as a stop bit)
	private int rusARQFreqHalf (CircularDataBuffer circBuf,WaveData waveData,int pos)	{
		int v;
		int sp=(int)samplesPerSymbol/2;
		// First half
		double early[]=doRusARQ160HalfSymbolBinRequest (circBuf,pos,lowBin,highBin);
		// Last half
		double late[]=doRusARQ160HalfSymbolBinRequest (circBuf,(pos+sp),lowBin,highBin);
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
		else if (lettersMode==true) return getBAUDOT_LETTERS(a);
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
	
}