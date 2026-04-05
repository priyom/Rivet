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

import javax.swing.JOptionPane;

public class FSK extends FFT {
	private final String BAUDOT_LETTERS[]={"N/A","E","<LF>","A"," ","S","I","U","<CR>","D","R","J","N","F","C","K","T","Z","L","W","H","Y","P","Q","O","B","G","<FIG>","M","X","V","<LET>"};
	private final String BAUDOT_NUMBERS[]={"N/A","3","<LF>","-"," ","<BELL>","8","7","<CR>","$","4","'",",","!",":","(","5","+",")","2","#","6","0","1","9","?","&","<FIG>",".","/","=","<LET>"};
	public final int ITA3VALS[]={13,37,56,100,69,21,50,112,70,74,26,42,28,19,97,82,35,11,98,49,22,76,73,25,84,81,67,88,38,14,41,44,52,104,7};
	public final String ITA3LETS[]={"Q","W","E","R","T","Y","U","I","O","P","A","S","D","F","G","H","J","K","L","Z","X","C","V","B","N","M","<cr>","<lf>","<fig>","<let>","<alpha>","<beta>","<rep>","<0x68>","<0x7>"};
	public final int CCIR476VALS[]={106,92,46,39,86,85,116,43,78,77,113,45,71,75,83,27,53,105,23,30,101,99,58,29,60,114,89,57,120,108,54,90,15};
	public final String CCIR476LETS[]={"<32>"," ","Q","W","E","R","T","Y","U","I","O","P","A","S","D","F","G","H","J","K","L","Z","X","C","V","B","N","M","<cr>","<lf>","<fig>","<let>","<alpha>"};
	public final String CCIR476NUMS[]={"<32>"," ","1","2","3","4","5","6","7","8","9","0","-","'"," ","%","@","#","*","(",")","+","/",":","=","?",",",".","<cr>","<lf>","<fig>","<let>","<alpha>"};
	public boolean lettersMode=true;
	public double kalmanNew=0.0;
	public double kalmanOld=0.0; 
			
	///////////////////////////
	// GENERAL FFT FUNCTIONS //
	///////////////////////////

	// Determines a frequency for the FSK classes
	 public int doRTTY_FFT (CircularDataBuffer circBuf,WaveData waveData,int start,int ss,double baud)	{
		// 45.45 baud
	    if (baud==45.45) return do45d45baudFFT(circBuf, waveData, start,ss);
		switch((int)baud){
	    	// 50 baud
	    	case 50: return do50baudFFT(circBuf,waveData,start,ss);
	    	// 75 baud
	    	case 75: return do75baudFFT(circBuf, waveData, start,ss);
	    	// 100 baud
	    	case 100: return do100baudFFT(circBuf,waveData,start,ss); 
	    	// 145 baud
			case 145: return do145baudFFT(circBuf,waveData,start,ss);
	    	// 150 baud
	    	case 150: return do150baudFFT(circBuf,waveData,start,ss);
	    	// 200 baud
	    	case 200: return do200baudFFT (circBuf,waveData,start,ss);
	    	// 300 baud
	    	case 300: return do300baudFFT(circBuf,waveData,start,ss);
	    	// 600 baud
	    	case 600: return do600baudFFT(circBuf,waveData,start,ss);

	    	default: return 0;
		}
	}	

	// Calculates the half symbol bin values for the FSK classes
	public double[] doRTTYHalfSymbolBinRequest (double baud,CircularDataBuffer circBuf,int start,int samples,int bin0,int bin1)	{
		// 45.45 baud
		if (baud==45.45) return do45d45baudFSKHalfSymbolBinRequest(circBuf, start, bin0, bin1, samples);
		
		switch((int)baud){
			// 50 baud
			case 50: return do50baudFSKHalfSymbolBinRequest(circBuf, start, bin0, bin1, samples);
			// 75 baud
			case 75: return do75baudFSKHalfSymbolBinRequest(circBuf, start, bin0, bin1, samples);
			// 100 baud
			case 100: return do100baudFSKHalfSymbolBinRequest (circBuf,start,bin0,bin1,samples);
			// 145 baud
			case 145: return do145baudFSKHalfSymbolBinRequest (circBuf,start,bin0,bin1,samples);
			// 150 baud
			case 150: return do150baudFSKHalfSymbolBinRequest (circBuf,start,bin0,bin1, samples);
			// 200 baud 
			case 200: return do200baudFSKHalfSymbolBinRequest (circBuf,start,bin0,bin1,samples); 
			// 300 baud 
			case 300: return do300baudFSKHalfSymbolBinRequest (circBuf,start,bin0,bin1,samples);
			// 600 baud 
			case 600: return do600baudFSKHalfSymbolBinRequest (circBuf,start,bin0,bin1,samples);
			default:
				// We have a problem here !
				JOptionPane.showMessageDialog(null,"Unsupported Baud Rate","Rivet", JOptionPane.ERROR_MESSAGE);
				return null;
		}
	}

	///////////////////////////////
	// HALF SYMBOL FFT FUNCTIONS //
	///////////////////////////////
	
	// Does an FFT on a 45.45 baud half symbol then returns the values of two specific bins
	private double[] do45d45baudFSKHalfSymbolBinRequest (CircularDataBuffer circBuf,int start,int bin0,int bin1,int ss) {
		double vals[]=new double[2];
		double samData[],datar[],spec[];
		// Get the data from the circular buffer
		samData=circBuf.extractDataDouble(start,ss);

		switch (ss){
			//8 khz
			case 88:
				datar=new double[FFT_176_SIZE];
				// Run the data through a Blackman window
				for (int a=0;a<datar.length;a++)	{
					if ((a>=44)&&(a<132)) datar[a]=samData[a-44];
					else datar[a]=0.0;
					datar[a]=windowBlackman(datar[a],a,datar.length);
					}
				fft176.realForward(datar);
				spec=getSpectrum(datar);
				vals[0]=spec[bin0];
				vals[1]=spec[bin1];
				return vals;
			
			//12 khz
			case 132:
				datar=new double[FFT_176_SIZE];
				// Run the data through a Blackman window
				for (int a=0;a<datar.length;a++)	{
					if ((a>=27)&&(a<149)) datar[a]=samData[a-27];
					else datar[a]=0.0;
					datar[a]=windowBlackman(datar[a],a,datar.length);
					}
				fft176.realForward(datar);
				spec=getSpectrum(datar);
				vals[0]=spec[bin0];
				vals[1]=spec[bin1];
				return vals;
			}
		return vals;
	}

	// Does an FFT on a 50 baud half symbol then returns the values of two specific bins
	private double[] do50baudFSKHalfSymbolBinRequest (CircularDataBuffer circBuf,int start,int bin0,int bin1,int ss) {
		double vals[]=new double[2];
		double samData[],datar[],spec[];
		// Get the data from the circular buffer
		samData=circBuf.extractDataDouble(start,ss);

		switch(ss){
			//8 khz
			case 80:
				datar=new double[FFT_160_SIZE];
				// Run the data through a Blackman window
				for (int a=0;a<datar.length;a++)	{
					if ((a>=40)&&(a<120)) datar[a]=samData[a-40];
					else datar[a]=0.0;
					datar[a]=windowBlackman(datar[a],a,datar.length);
					}
				fft160.realForward(datar);
				spec=getSpectrum(datar);
				vals[0]=spec[bin0];
				vals[1]=spec[bin1];
				return vals;

			//12 khz
			case 120:
				datar=new double[FFT_240_SIZE];
				// Run the data through a Blackman window
				for (int a=0;a<datar.length;a++)	{
					if ((a>=60)&&(a<180)) datar[a]=samData[a-60];
					else datar[a]=0.0;
					datar[a]=windowBlackman(datar[a],a,datar.length);
					}
				fft240.realForward(datar);
				spec=getSpectrum(datar);
				vals[0]=spec[bin0];
				vals[1]=spec[bin1];
				return vals;
		}
		return vals;
	}

	// Does an FFT on a 75 baud half symbol then returns the values of two specific bins
	private double[] do75baudFSKHalfSymbolBinRequest (CircularDataBuffer circBuf,int start,int bin0,int bin1,int ss) {
		double vals[]=new double[2];
		double samData[],datar[],spec[];
		// Get the data from the circular buffer
		samData=circBuf.extractDataDouble(start,ss);

		switch(ss){
			//8 khz
			case 53:
				datar=new double[FFT_106_SIZE];
				// Run the data through a Blackman window
				for (int a=0;a<datar.length;a++)	{
					if ((a>=26)&&(a<79)) datar[a]=samData[a-26];
					else datar[a]=0.0;
					datar[a]=windowBlackman(datar[a],a,datar.length);
					}
				fft106.realForward(datar);
				spec=getSpectrum(datar);
				vals[0]=spec[bin0];
				vals[1]=spec[bin1];
				return vals;

			//12 khz
			case 80:
				datar=new double[FFT_160_SIZE];
				// Run the data through a Blackman window
				for (int a=0;a<datar.length;a++)	{
					if ((a>=40)&&(a<120)) datar[a]=samData[a-40];
					else datar[a]=0.0;
					datar[a]=windowBlackman(datar[a],a,datar.length);
					}
				fft160.realForward(datar);
				spec=getSpectrum(datar);
				vals[0]=spec[bin0];
				vals[1]=spec[bin1];
				return vals;
		}
		return vals;
	}
	
	// Does an FFT on a 100 baud half symbol then returns the values of two specific bins
	private double[] do100baudFSKHalfSymbolBinRequest (CircularDataBuffer circBuf,int start,int bin0,int bin1,int ss)	{
		double vals[]=new double[2];
		double samData[],datar[],spec[];
		// Get the data from the circular buffer
		samData=circBuf.extractDataDouble(start,ss);

		switch(ss){
			//8 khz
			case 40:
				datar=new double[FFT_160_SIZE];
				// Run the data through a Blackman window
				for (int a=0;a<datar.length;a++)	{
					if ((a>=60)&&(a<100)) datar[a]=samData[a-60];
					else datar[a]=0.0;
					datar[a]=windowBlackman(datar[a],a,datar.length);
					}
				fft160.realForward(datar);
				spec=getSpectrum(datar);
				vals[0]=spec[bin0];
				vals[1]=spec[bin1];
				return vals;

			//12 khz
			case 60:
				datar=new double[FFT_240_SIZE];
				// Run the data through a Blackman window
				for (int a=0;a<datar.length;a++)	{
					if ((a>=90)&&(a<150)) datar[a]=samData[a-90];
					else datar[a]=0.0;
					datar[a]=windowBlackman(datar[a],a,datar.length);
					}
				fft240.realForward(datar);
				spec=getSpectrum(datar);
				vals[0]=spec[bin0];
				vals[1]=spec[bin1];
				return vals;
			}
			return vals;
		}
	
	
	// Does an FFT on a 145 baud half symbol then returns the values of two specific bins
	private double[] do145baudFSKHalfSymbolBinRequest (CircularDataBuffer circBuf,int start,int bin0,int bin1,int ss)	{
		double vals[]=new double[2];
		double samData[],datar[],spec[];
		// Get the data from the circular buffer
		samData=circBuf.extractDataDouble(start,ss);

		switch(ss){
			//8 khz
			case 27:
				datar=new double[FFT_160_SIZE];
				// Run the data through a Blackman window
				for (int a=0;a<datar.length;a++)	{
					if ((a>=66)&&(a<93)) datar[a]=samData[a-66];
					else datar[a]=0.0;
					datar[a]=windowBlackman(datar[a],a,datar.length);
					}
				fft160.realForward(datar);
				spec=getSpectrum(datar);
				vals[0]=spec[bin0];
				vals[1]=spec[bin1];
				return vals;

			//12 khz
			case 41:
				datar=new double[FFT_240_SIZE];
				// Run the data through a Blackman window
				for (int a=0;a<datar.length;a++)	{
					if ((a>=100)&&(a<141)) datar[a]=samData[a-100];
					else datar[a]=0.0;
					datar[a]=windowBlackman(datar[a],a,datar.length);
					}
				fft240.realForward(datar);
				spec=getSpectrum(datar);
				vals[0]=spec[bin0];
				vals[1]=spec[bin1];
				return vals;
			}
			return vals;
		}
	
	
	// Does an FFT on a 150 baud half symbol then returns the values of two specific bins
	private double[] do150baudFSKHalfSymbolBinRequest (CircularDataBuffer circBuf,int start,int bin0,int bin1,int ss)	{
		double vals[]=new double[2];
		double samData[],datar[],spec[];
		// Get the data from the circular buffer
		samData=circBuf.extractDataDouble(start,ss);

		switch(ss){
			//8 khz
			case 26:
				datar=new double[FFT_160_SIZE];
				// Run the data through a Blackman window
				for (int a=0;a<datar.length;a++)	{
					if ((a>=67)&&(a<93)) datar[a]=samData[a-67];
					else datar[a]=0.0;
					datar[a]=windowBlackman(datar[a],a,datar.length);
					}
				fft160.realForward(datar);
				spec=getSpectrum(datar);
				vals[0]=spec[bin0];
				vals[1]=spec[bin1];
				return vals;

			//12 khz
			case 40:
				datar=new double[FFT_240_SIZE];
				// Run the data through a Blackman window
				for (int a=0;a<datar.length;a++)	{
					if ((a>=100)&&(a<140)) datar[a]=samData[a-100];
					else datar[a]=0.0;
					datar[a]=windowBlackman(datar[a],a,datar.length);
					}
				fft240.realForward(datar);
				spec=getSpectrum(datar);
				vals[0]=spec[bin0];
				vals[1]=spec[bin1];
				return vals;
			}
		return vals;
		}
	
	// Does an FFT on a 200baud half symbol then returns the values of two specific bins
	private double[] do200baudFSKHalfSymbolBinRequest (CircularDataBuffer circBuf,int start,int bin0,int bin1,int ss)	{
		double vals[]=new double[2];
		double samData[],datar[],spec[];
		// Get the data from the circular buffer
		samData=circBuf.extractDataDouble(start,ss);

		switch(ss){
			//8 khz
			case 20:
				datar=new double[FFT_64_SIZE];
				// Run the data through a Blackman window
				for (int a=0;a<datar.length;a++)	{
					if ((a>=22)&&(a<42)) datar[a]=samData[a-22];
					else datar[a]=0.0;
					datar[a]=windowBlackman(datar[a],a,datar.length);
					}
				fft64.realForward(datar);
				spec=getSpectrum(datar);
				vals[0]=spec[bin0];
				vals[1]=spec[bin1];
				return vals;

			//12 khz
			case 30:
				datar=new double[FFT_96_SIZE];
				// Run the data through a Blackman window
				for (int a=0;a<datar.length;a++)	{
					if ((a>=33)&&(a<63)) datar[a]=samData[a-33];
					else datar[a]=0.0;
					datar[a]=windowBlackman(datar[a],a,datar.length);
					}
				fft96.realForward(datar);
				spec=getSpectrum(datar);
				vals[0]=spec[bin0];
				vals[1]=spec[bin1];
				return vals;
			}
			return vals;
		}	
	
	// Does an FFT on a 300baud half symbol then returns the values of two specific bins
	private double[] do300baudFSKHalfSymbolBinRequest (CircularDataBuffer circBuf,int start,int bin0,int bin1,int ss)	{
		double vals[]=new double[2];
		double samData[],datar[],spec[];
		// Get the data from the circular buffer
		samData=circBuf.extractDataDouble(start,ss);

		switch(ss){
			//8 khz
			case 13:
				datar=new double[FFT_64_SIZE];
				// Run the data through a Blackman window
				for (int a=0;a<datar.length;a++)	{
					if ((a>=25)&&(a<38)) datar[a]=samData[a-25];
					else datar[a]=0.0;
					datar[a]=windowBlackman(datar[a],a,datar.length);
					}
				fft64.realForward(datar);
				spec=getSpectrum(datar);
				vals[0]=spec[bin0];
				vals[1]=spec[bin1];
				return vals;

			//12 khz
			case 20:
				datar=new double[FFT_96_SIZE];
				// Run the data through a Blackman window
				for (int a=0;a<datar.length;a++)	{
					if ((a>=38)&&(a<58)) datar[a]=samData[a-38];
					else datar[a]=0.0;
					datar[a]=windowBlackman(datar[a],a,datar.length);
					}
				fft96.realForward(datar);
				spec=getSpectrum(datar);
				vals[0]=spec[bin0];
				vals[1]=spec[bin1];
				return vals;
			}
		return vals;
		}		
	
	
	// Does an FFT on a 600 baud half symbol then returns the values of two specific bins
	private double[] do600baudFSKHalfSymbolBinRequest (CircularDataBuffer circBuf,int start,int bin0,int bin1,int ss)	{
		double vals[]=new double[2];
		double samData[],datar[],spec[];
		// Get the data from the circular buffer
		samData=circBuf.extractDataDouble(start,ss);

		switch(ss){
			//8 khz
			case 6:
				datar=new double[FFT_64_SIZE];
				// Run the data through a Blackman window
				for (int a=0;a<datar.length;a++)	{
					if ((a>=29)&&(a<35)) datar[a]=samData[a-29];
					else datar[a]=0.0;
					datar[a]=windowBlackman(datar[a],a,datar.length);
					}
				fft64.realForward(datar);
				spec=getSpectrum(datar);
				vals[0]=spec[bin0];
				vals[1]=spec[bin1];
				return vals;

			//12 khz
			case 10:
				datar=new double[FFT_96_SIZE];
				// Run the data through a Blackman window
				for (int a=0;a<datar.length;a++)	{
					if ((a>=43)&&(a<53)) datar[a]=samData[a-43];
					else datar[a]=0.0;
					datar[a]=windowBlackman(datar[a],a,datar.length);
					}
				fft96.realForward(datar);
				spec=getSpectrum(datar);
				vals[0]=spec[bin0];
				vals[1]=spec[bin1];
				return vals;
			}
		return vals;
		}	

	///////////////////////////////
	// FULL SYMBOL FFT FUNCTIONS //
	///////////////////////////////

	// Return the frequency on a 45.45 baud sample
	private int do45d45baudFFT (CircularDataBuffer circBuf,WaveData waveData,int start,int ss)	{
		double datar[],spec[];
		// Get the data from the circular buffer
	    datar=circBuf.extractDataDouble(start,ss);

		switch((int)waveData.getSampleRate()){
			//8khz sample rate
			case 8000:
	    		fft176.realForward(datar);
				spec=getSpectrum(datar);
				return getFFTFreq (spec,waveData.getSampleRate());
				
			//12khz sample rate
			case 12000:
	    		fft264.realForward(datar);
	    		spec=getSpectrum(datar);
	    		return getFFTFreq (spec,waveData.getSampleRate());
			}  
		return 0;
		}	

	// Return the frequency on a 50 baud sample
	private int do50baudFFT (CircularDataBuffer circBuf,WaveData waveData,int start,int ss)	{
		double datar[],spec[];
		// Get the data from the circular buffer
	    datar=circBuf.extractDataDouble(start,ss);

		switch((int)waveData.getSampleRate()){
			//8khz sample rate
			case 8000:
	    		fft160.realForward(datar);
				spec=getSpectrum(datar);
				return getFFTFreq (spec,waveData.getSampleRate());
				
			//12khz sample rate
			case 12000:
	    		fft240.realForward(datar);
	    		spec=getSpectrum(datar);
	    		return getFFTFreq (spec,waveData.getSampleRate());
			}  
		return 0;
		}
	
	// Return the frequency on a 75 baud sample
	private int do75baudFFT (CircularDataBuffer circBuf,WaveData waveData,int start,int ss)	{
		double datar[],spec[];
		// Get the data from the circular buffer
	    datar=circBuf.extractDataDouble(start,ss);

		switch((int)waveData.getSampleRate()){
			//8khz sample rate
			case 8000:
	    		fft106.realForward(datar);
				spec=getSpectrum(datar);
				return getFFTFreq (spec,waveData.getSampleRate());
				
			//12khz sample rate
			case 12000:
	    		fft160.realForward(datar);
	    		spec=getSpectrum(datar);
	    		return getFFTFreq (spec,waveData.getSampleRate());
			}  
		return 0;
		}

	// Return the frequency on a 100 baud sample
	private int do100baudFFT (CircularDataBuffer circBuf,WaveData waveData,int start,int ss)	{
		double datar[],spec[];
		// Get the data from the circular buffer
		double samData[]=circBuf.extractDataDouble(start,ss);

		switch((int)waveData.getSampleRate()){
			//8khz
			case 8000:
				datar=new double[FFT_160_SIZE];
				// Run the data through a Blackman window
				for (int a=0;a<datar.length;a++)	{
					if ((a>=40)&&(a<120)) datar[a]=samData[a-40];
					else datar[a]=0.0;
					datar[a]=windowBlackman(datar[a],a,datar.length);
				}		
				fft160.realForward(datar);
				spec=getSpectrum(datar);
				return getFFTFreq (spec,waveData.getSampleRate());
				
			//12khz
			case 12000:
				datar=new double[FFT_240_SIZE];
				// Run the data through a Blackman window
				for (int a=0;a<datar.length;a++)	{
					if ((a>=60)&&(a<180)) datar[a]=samData[a-60];
					else datar[a]=0.0;
					datar[a]=windowBlackman(datar[a],a,datar.length);
				}		
				fft240.realForward(datar);
				spec=getSpectrum(datar);
				return getFFTFreq (spec,waveData.getSampleRate());
		}
		return 0;
		}	
	
	// Return the frequency on a 145 baud sample
	private int do145baudFFT (CircularDataBuffer circBuf,WaveData waveData,int start,int ss)	{
		double datar[],spec[],samData[];

		switch((int)waveData.getSampleRate()){
			//8khz
			case 8000:
			// Get the data from the circular buffer
				samData=circBuf.extractDataDouble(start,80);
				datar=new double[FFT_160_SIZE];
				// Run the data through a Blackman window
				for (int a=0;a<datar.length;a++)	{
					if ((a>=40)&&(a<120)) datar[a]=samData[a-40];
					else datar[a]=0.0;
					datar[a]=windowBlackman(datar[a],a,datar.length);
				}		
				fft160.realForward(datar);
				spec=getSpectrum(datar);
				return getFFTFreq (spec,waveData.getSampleRate());
				
			//12khz
			case 12000:
				samData=circBuf.extractDataDouble(start,120);
				datar=new double[FFT_240_SIZE];
				// Run the data through a Blackman window
				for (int a=0;a<datar.length;a++)	{
					if ((a>=20)&&(a<140)) datar[a]=samData[a-20];
					else datar[a]=0.0;
					datar[a]=windowBlackman(datar[a],a,datar.length);
				}		
				fft240.realForward(datar);
				spec=getSpectrum(datar);
				return getFFTFreq (spec,waveData.getSampleRate());
			}
		return 0;
		}		
	
	// Return the frequency on a 150 baud sample
	private int do150baudFFT (CircularDataBuffer circBuf,WaveData waveData,int start,int ss)	{
		double datar[],spec[],samData[];

		switch((int)waveData.getSampleRate()){
			//8khz
			case 8000:
			// Get the data from the circular buffer
				samData=circBuf.extractDataDouble(start,55);
				datar=new double[FFT_160_SIZE];
				// Run the data through a Blackman window
				for (int a=0;a<datar.length;a++)	{
					if ((a>=52)&&(a<107)) datar[a]=samData[a-52];
					else datar[a]=0.0;
					datar[a]=windowBlackman(datar[a],a,datar.length);
				}		
				fft160.realForward(datar);
				spec=getSpectrum(datar);
				return getFFTFreq (spec,waveData.getSampleRate());
				
			//12khz
			case 12000:
				samData=circBuf.extractDataDouble(start,ss);
				datar=new double[FFT_240_SIZE];
				// Run the data through a Blackman window
				for (int a=0;a<datar.length;a++)	{
					if ((a>=80)&&(a<160)) datar[a]=samData[a-80];
					else datar[a]=0.0;
					datar[a]=windowBlackman(datar[a],a,datar.length);
				}		
				fft240.realForward(datar);
				spec=getSpectrum(datar);
				return getFFTFreq (spec,waveData.getSampleRate());
			}
		return 0;
		}		

	// Return the frequency on a 200 baud sample 
	private int do200baudFFT (CircularDataBuffer circBuf,WaveData waveData,int start,int ss)	{
		double datao[],datar[],spec[];
		// Get the data from the circular buffer
		datao=circBuf.extractDataDouble(start,ss);

		switch((int)waveData.getSampleRate()){
			//8khz sample rate
			case 8000:
	    	datar=new double[FFT_64_SIZE];
	    	for (int a=0;a<datar.length;a++)	{
	    		if ((a>=12)&&(a<52)) datar[a]=datao[a-12];
				else datar[a]=0.0;
				datar[a]=windowBlackman(datar[a],a,datar.length);
	    	}
			fft64.realForward(datar);
			spec=getSpectrum(datar);
			return getFFTFreq (spec,waveData.getSampleRate());
				
		//12khz sample rate
		case 12000:
	    	datar=new double[FFT_96_SIZE];
	    	for (int a=0;a<datar.length;a++)	{
	    		if ((a>=18)&&(a<78)) datar[a]=datao[a-18];
				else datar[a]=0.0;
				datar[a]=windowBlackman(datar[a],a,datar.length);
	    	}
			fft96.realForward(datar);
			spec=getSpectrum(datar);
			return getFFTFreq (spec,waveData.getSampleRate());
		}  
		return 0;
	}

	// Return the frequency on a 300 baud sample
	private int do300baudFFT (CircularDataBuffer circBuf,WaveData waveData,int start,int ss)	{
		double samData[],datar[],spec[];

		switch((int)waveData.getSampleRate()){
			//8khz
			case 8000:
			// Get the data from the circular buffer
				samData=circBuf.extractDataDouble(start,26);
				datar=new double[FFT_64_SIZE];
				// Run the data through a Blackman window
				for (int a=0;a<datar.length;a++)	{
					if ((a>=19)&&(a<45)) datar[a]=samData[a-19];
					else datar[a]=0.0;
					datar[a]=windowBlackman(datar[a],a,datar.length);
				}		
				fft64.realForward(datar);
				spec=getSpectrum(datar);
				return getFFTFreq (spec,waveData.getSampleRate());
				
			//12khz
			case 12000:
				samData=circBuf.extractDataDouble(start,ss);
				datar=new double[FFT_96_SIZE];
				// Run the data through a Blackman window
				for (int a=0;a<datar.length;a++)	{
					if ((a>=28)&&(a<68)) datar[a]=samData[a-28];
					else datar[a]=0.0;
					datar[a]=windowBlackman(datar[a],a,datar.length);
				}		
				fft96.realForward(datar);
				spec=getSpectrum(datar);
				return getFFTFreq (spec,waveData.getSampleRate());
			}
		return 0;
		}	
	
	// Return the frequency on a 600 baud sample
	private int do600baudFFT (CircularDataBuffer circBuf,WaveData waveData,int start,int ss)	{
		double samData[],datar[],spec[];

		switch((int)waveData.getSampleRate()){
			//8khz
			case 8000:
			// Get the data from the circular buffer
				samData=circBuf.extractDataDouble(start,13);
				datar=new double[FFT_64_SIZE];
				// Run the data through a Blackman window
				for (int a=0;a<datar.length;a++)	{
					if ((a>=25)&&(a<38)) datar[a]=samData[a-25];
					else datar[a]=0.0;
					datar[a]=windowBlackman(datar[a],a,datar.length);
				}		
				fft64.realForward(datar);
				spec=getSpectrum(datar);
				return getFFTFreq (spec,waveData.getSampleRate());
				
			//12khz
			case 12000:
				samData=circBuf.extractDataDouble(start,ss);
				datar=new double[FFT_96_SIZE];
				// Run the data through a Blackman window
				for (int a=0;a<datar.length;a++)	{
					if ((a>=38)&&(a<58)) datar[a]=samData[a-38];
					else datar[a]=0.0;
					datar[a]=windowBlackman(datar[a],a,datar.length);
				}		
				fft96.realForward(datar);
				spec=getSpectrum(datar);
				return getFFTFreq (spec,waveData.getSampleRate());
			}
		return 0;
		}	

	/////////////////////////
	// OTHER FSK FUNCTIONS //
	/////////////////////////
	
	// Test for a specific tone
	public boolean toneTest (int freq,int tone,int errorAllow)	{
		if ((freq>(tone-errorAllow))&&(freq<(tone+errorAllow))) return true;
		else return false;
		}
	
	// Given a frequency decide the bit value
	public boolean freqDecision (int freq,int centreFreq,boolean inv)	{
		if (inv==false)	{
			if (freq>centreFreq) return true;
			else return false;
			}
		else	{
			if (freq>centreFreq) return false;
			else return true;
			}
		}
	
	// Get a Baudot letter
	public String getBAUDOT_LETTERS(int i) {
		return BAUDOT_LETTERS[i];
	}

	// Get a Baudot number
	public String getBAUDOT_NUMBERS(int i) {
		return BAUDOT_NUMBERS[i];
	}

	// This returns the percentage difference between x and y
	public double getPercentageDifference (double x,double y)	{
		return (((x-y)/(x+y))*100.0);
	}
		
	// A Kalman filter for use by the FSK early/late gate
	public double kalmanFilter (double in,double cof1,double cof2)	{
		double newo=(cof1*kalmanOld)+(cof2*in);
		kalmanOld=kalmanNew;
		kalmanNew=newo;
		return newo;
	}
	
	// Return a ITA-3 character
	public int retITA3Val (int c)	{
		int a;
		for (a=0;a<ITA3VALS.length;a++)	{
			if (c==ITA3VALS[a]) return a;
		}
		return 0;
	}
	
	// Check if a number if a valid ITA-3 character
	public boolean checkITA3Char (int c)	{
		int a;
		for (a=0;a<ITA3VALS.length;a++)	{
			if (c==ITA3VALS[a]) return true;
		}
		return false;
	}
	
	// Return a CCIR476 character
	public int retCCIR476Val (int c)	{
		int a;
		for (a=0;a<CCIR476VALS.length;a++)	{
			if (c==CCIR476VALS[a]) return a;
		}
		return 0;
	}
	
	// Check if a number if a valid CCIR476 character
	public boolean checkCCIR476Char (int c)	{
		int a;
		for (a=0;a<CCIR476VALS.length;a++)	{
			if (c==CCIR476VALS[a]) return true;
		}
		return false;
	}	

	
}
