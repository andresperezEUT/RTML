+ RTML {

	*loadSynthDefs {


		//////////////////////////////////////////////////////////////////////////////////////////
		// TEST TONES
		//////////////////////////////////////////////////////////////////////////////////////////

		SynthDef(\click, { |amp=0.5, freq=1000|
			var sig = SinOsc.ar(freq)*amp;
			var env = EnvGen.kr(Env.perc(releaseTime:0.5),doneAction:2);
			Out.ar(0,(sig*env)!2);
		}).add;

		SynthDef(\tone, { |amp=0.2, freq=400|
			var sig = SinOsc.ar(freq+(freq/40*SinOsc.kr(5)))*amp;
			Out.ar(0,(sig)!2);
		}).add;



		//////////////////////////////////////////////////////////////////////////////////////////
		// LOW LEVEL FEATURES
		//////////////////////////////////////////////////////////////////////////////////////////


		////////////////////////////// FFT MAG //////////////////////

		SynthDef(\fftTracker,{ |channel = 0, buffer, winType = 1, replyRate = 20|

			var in = SoundIn.ar(channel);

			var fft = FFT(buffer,in,wintype:winType);
			var mag = PV_MagSmear(fft, 0);
			var pulseCount = PulseCount.kr(fft >= 0);

			SendTrig.kr(Impulse.kr(replyRate),0,pulseCount);

		}).add;



		////////////////////////////// SPECTRAL FEATURES //////////////////////

		SynthDef(\spectralTracker,{ |channel = 0, fftSize = 2048, winType = 1, pcile = 0.9, pcileInterpol = 0, replyRate = 20|

			var in = SoundIn.ar(channel);

			var fft = FFT(LocalBuf(fftSize),in,wintype:winType);

			var centroid = SpecCentroid.kr(fft);
			var flatness = SpecFlatness.kr(fft);
			var specpcile = SpecPcile.kr(fft,pcile,pcileInterpol);

			SendTrig.kr(Impulse.kr(replyRate),0,centroid);
			SendTrig.kr(Impulse.kr(replyRate),1,flatness);
			SendTrig.kr(Impulse.kr(replyRate),2,specpcile);

		}).add;




		////////////////////////////// MFCC //////////////////////


		SynthDef(\mfccTracker,{ |channel = 0, buffer, fftSize = 2048, winType = 1, replyRate = 20|

			var in = SoundIn.ar(channel);

			var fft = FFT(LocalBuf(fftSize),in,wintype:winType);
			var pulseCount = PulseCount.kr(fft >= 0);

			var mfcc = MFCC.kr(fft,RTML.numMfcc);

			BufWr.kr(mfcc, buffer);
			//SendTrig.kr(fft,0,bufFrame);
			SendTrig.kr(Impulse.kr(replyRate),0,pulseCount);


		}).add;




		////////////////////////////// PEAK FOLLOWER //////////////////////

		SynthDef(\peakTracker,{ |channel=0, monitor=0, replyRate=20, lagTime=0.1, decay=0.999, gain=1|

			var in = SoundIn.ar(channel);

			var peak = PeakFollower.ar(in,decay) * gain;
			peak = Lag.ar(peak,lagTime);
			peak = Clip.ar(peak,0,1);

			SendTrig.kr(Impulse.kr(replyRate),0,peak);
			// SendPeakRMS.ar(in,replyRate,peakLag3,'/tr',0);

			Out.ar(0, Pan2.ar(in,level:Clip.kr(monitor,0,1)));

		}).add;



		//////////////////////////////////////////////////////////////////////////////////////////
		// MID-LEVEL FEATURES
		//////////////////////////////////////////////////////////////////////////////////////////



		////////////////////////////// ONSET DETECTOR //////////////////////

		SynthDef(\onsetDetector,{ |id =0, channel=0, fftSize=512, monitor=0, threshold=0.5, odftype='rcomplex', relaxtime=1, floor=0.1, mingap=10, medianspan=11, whtype=1, rawodf=0|

			var in, fft, onsets;
			var value;

			// osc message info
			value=0;

			// onset detection
			in = SoundIn.ar(channel);
			fft= FFT(LocalBuf(fftSize), in);
			onsets = Onsets.kr(fft,threshold,odftype,relaxtime,floor,mingap,medianspan,whtype,rawodf);
			SendTrig.kr(onsets,id,value);

			// monitor signal
			Out.ar(0, Pan2.ar(in,level:Clip.kr(monitor,0,1)));

		}).add;




		////////////////////////////// BEAT TRACKER //////////////////////

		SynthDef(\beatTracker,{ | channel=0, fftSize=1024, monitor=0, krChannel=0, numChannels=5, windowSize=2.5, phaseaccuracy=0.02, lock=0|
			var in, kbus;
			var trackb,trackh,trackq,tempo, phase, period, groove;
			var bsound,hsound,qsound, beep;
			var fft;
			var feature1, feature2, feature3, feature4;

			in = SoundIn.ar(channel);

			//Create some features
			fft= FFT(LocalBuf(fftSize), in);

			feature1= RunningSum.rms(in,64);
			feature2= MFCC.kr(fft,2); //two coefficients
			feature3= A2K.kr(LPF.ar(in,1000));
			feature4= Onsets.kr(fft);

			kbus= Out.kr(krChannel, [feature1, feature3]++feature2++feature4);

			#trackb,trackh,trackq,tempo, phase, period, groove=BeatTrack2.kr(krChannel,numChannels,windowSize, phaseaccuracy, lock, -2.5);

			// beats, subbeats with tempo (1=60bpm)
			SendTrig.kr(trackb,0,tempo);
			SendTrig.kr(trackh,1,tempo);
			SendTrig.kr(trackq,2,tempo);

			Out.ar(0, Pan2.ar(in,level:Clip.kr(monitor,0,1)));

		}).add;





		////////////////////////////// MONOPHONIC PITCH //////////////////////

		SynthDef(\pitchTracker,{ |channel=0, monitor=0, initFreq=440, minFreq=60, maxFreq=4000, execFreq=100, maxBinsPerOctave=16, median=1, ampThreshold=0.01, peakThreshold=0.5, downSample=1, clar=0, replyRate=20|

			var in, freq, hasFreq, out;
			in = SoundIn.ar(channel);

			# freq, hasFreq = Pitch.kr(in, initFreq, minFreq, maxFreq, execFreq, maxBinsPerOctave, median, ampThreshold, peakThreshold, downSample, clar);

			// [freq,hasFreq].poll;

			SendTrig.kr(Impulse.kr(replyRate),0,freq);

			// SendTrig.kr(hasFreq,0,freq);
			// SendTrig.kr(1-hasFreq,0,0);  //this can be used as a kinda noteOff


			// monitor signal
			Out.ar(0, Pan2.ar(in,level:Clip.kr(monitor,0,1)));

		}).add;





		////////////////////////////// CHROMAGRAM //////////////////////


		SynthDef(\chromaTracker,{ |channel = 0, buffer, fftSize = 2048, winType = 1, tuningBase = 32.703195662575, octaves = 8, integrate = 0, integrateCoeff = 0.9,  octaveRatio = 2, normalize = 1, replyRate = 20|

			var numDiv = 12;

			var in = SoundIn.ar(channel);

			var fft = FFT(LocalBuf(fftSize),in,wintype:winType);
			var pulseCount = PulseCount.kr(fft >= 0);

			var chroma = Chromagram.kr(fft, fftsize:fftSize, n:numDiv, tuningbase:tuningBase, octaves:octaves, integrationflag:integrate, coeff:integrateCoeff, octaveratio:octaveRatio, perframenormalize:normalize);

			BufWr.kr(chroma, buffer);
			SendTrig.kr(Impulse.kr(replyRate),0,pulseCount);

		}).add;




		////////////////////////////// KEY TRACKER //////////////////////


		SynthDef(\keyTracker,{ |channel = 0, fftSize = 2048, winType = 1, keyDecay = 2, chromaLeak = 0.5, replyRate = 20|

			var in = SoundIn.ar(channel);

			var fft = FFT(LocalBuf(fftSize),in,wintype:winType);

			var key = KeyTrack.kr(fft,keyDecay,chromaLeak);

			SendTrig.kr(Impulse.kr(replyRate),0,key);

		}).add;




		//////////////////////////////////////////////////////////////////////////////////////////
		// PEAK EQUALIZER
		//////////////////////////////////////////////////////////////////////////////////////////

/*		SynthDef(\peakEQ,{ |channel=0, monitor=0|

			var in = SoundIn.ar(channel);

			var eq = eqSetting;
			var out = eq.ar(in);

			Out.ar(0, Pan2.ar(in,level:Clip.kr(monitor,0,1)));

		}).add;*/

	}
}
