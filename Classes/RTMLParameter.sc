RTMLparameter {

	classvar name;
	classvar defaultValue;
	classvar minValue;
	classvar maxValue;
	classvar guiType;

	// todo: add slider \lin or \exp type

	classvar parameters;
	classvar parameterKeys;

	*init {

		parameterKeys = [\defaultVal, \valueType, \minVal, \maxVal, \guiType, \valueList, \valueExpl, \warp, \step];

		parameters = Dictionary[
			// [\defaultVal, \valueType, \minVal, \maxVal, \guiType, \valueList, \valueExpl, \valueScale, \step];
			\winType -> this.createDict([1, Array, nil, nil, PopUpMenu, [-1,0,1], ["rect","sine","hann"],nil,nil]),
			\replyRate -> this.createDict([20, Integer, 1, 20, Slider, nil, nil,\lin,1]),
			\numMfcc -> this.createDict([20, Integer, 1, 42, Slider, nil, nil,\lin,1]),
			\fftSize -> this.createDict([1024, Array, nil, nil, PopUpMenu, [128,256,512,1024,2048,4096], nil,nil,nil]),
			\tuningBase -> this.createDict([32.703195662575, Float, 1, 100, Slider, nil, nil,\lin,0.01]),
			\octaves -> this.createDict([8, Integer, 1, 10, Slider, nil, nil,\lin,1]),
			\integrate -> this.createDict([0, Array, nil, nil, PopUpMenu, [0,1], ["off","on"],nil,nil]),
			\integrateCoeff -> this.createDict([0.9, Float, 0, 1, Slider, nil, nil,\lin,0,01]),
			\octaveRatio -> this.createDict([2, Float, 1, 5, Slider, nil, nil,\lin,0.1]),
			\normalize -> this.createDict([0, Array, nil, nil, PopUpMenu, [0,1], ["off","on"],nil]),
			\pcile -> this.createDict([0.9, Float, 0, 1, Slider, nil, nil,\lin,0.01]),
			\pcileInterpol -> this.createDict([0, Array, nil, nil, PopUpMenu, [0,1], ["off","on"],nil,nil]),
			\keyDecay -> this.createDict([2, Float, 0.01, 10, Slider, nil, nil,\exponential,0.01]),
			\chromaLeak -> this.createDict([0.5, Float, 0, 1, Slider, nil, nil,\lin,0.01]),
			\threshold -> this.createDict([0.5, Float, 0, 1.5, Slider, nil, nil,\lin,0.01]),
			\odftype -> this.createDict([\rcomplex,Array,nil,nil,PopUpMenu,[\power,\magsum,\complex,\rcomplex,\phase,\wphase,\mkl],nil,nil]),
			\relaxtime -> this.createDict([1,Float,0.1, 10, Slider, nil, nil,\exponential,0.1]),
			\floor -> this.createDict([0.1, Float, 0.0001, 0.1, Slider, nil, nil,\exponential,0.0001]),
			\mingap -> this.createDict([10, Integer, 0, 20, Slider, nil,nil,\lin,1]),
			\medianspan -> this.createDict([11, Integer, 0, 20, Slider, nil, nil,\lin,1]),
			\whtype -> this.createDict([1, Array, nil, nil, PopUpMenu, [0,1], nil,nil,nil]), //?
			\rawodf -> this.createDict([0, Array, nil, nil, PopUpMenu, [0,1], nil,nil,nil]), //?
			\krChannel -> this.createDict([0, Array, nil, nil, PopUpMenu, [0,1], nil,nil,nil]), //?
			\numChannels -> this.createDict([5, Integer, 1,5, Slider,nil,nil,\lin,1]),
			\windowSize -> this.createDict([2.5,Float,1,5,Slider,nil,nil,\lin,0.01]),
			\phaseaccuracy -> this.createDict([0.02,Float,0.01,0.1,Slider,nil,nil,\lin,0.01]),
			\lock -> this.createDict([0, Array, nil, nil, PopUpMenu, [0,1], ["off","on"],nil,nil]),
			\initFreq -> this.createDict([440,Float,220,880,Slider,nil,nil,\exponential,0.01]),
			\minFreq -> this.createDict([60,Float,15,120,Slider,nil,nil,\exponential,0.01]),
			\maxFreq -> this.createDict([4000,Float,1000,8000,Slider,nil,nil,\exponential,0.01]),
			\execFreq -> this.createDict([100,Float,50,200,Slider,nil,nil,\exponential,0.01]),
			\maxBinsPerOctave -> this.createDict([16,Integer,4,64,Slider,nil,nil,\exponential,1]),
			\median -> this.createDict([1,Integer,1,31,Slider,nil,nil,\lin,1]),
			\ampThreshold -> this.createDict([0.01,Float,0.001,0.1,Slider,nil,nil,\exponential,0.001]),
			\peakThreshold -> this.createDict([0.5,Float,0,1,Slider,nil,nil,\lin,0.01]),
			\downSample -> this.createDict([1,Array,1,64,PopUpMenu,[1,2,4,8,16,32,64],nil,nil,1]),
			\clar -> this.createDict([0,Array,nil,nil,PopUpMenu,[0,1],["off","on"],nil,1]),
			\lagTime -> this.createDict([0.1,Float,0.001,0.1,Slider,nil,nil,\exponential,0.001]),
			\decay -> this.createDict([0.999,Float,0.9,0.9999,Slider,nil,nil,\exponential,0.0001]),
			\gain -> this.createDict([1,Float,0,1,Slider,nil,nil,\lin,0.01])
		];
	}

	*createDict { | args |
		var dict = Dictionary.new;

		parameterKeys.do{ |key, i|
			dict.add( key -> args[i] );
		};

		^dict;
	}


	// obtain parameter info
	// if it does not exists, outputs \notExist; nil is a valid parameter value

	*get { |par,attribute|

		var p = parameters.at(par);
		var ans;

		if (p.isNil.not) {
			ans = p.at(attribute);
		} {
			("parameter " ++ par ++ " does not exist").warn;
			ans = \notExist;
		};

		^ans;
	}

}
/*
FFTTracker
[\channel,channel, \buffer,buffer, \winType,1, \replyRate,20]

MFCCTracker
\channel,channel, \buffer,buffer, \numMfcc,numMfcc, \fftSize, 1024, \winType,1, \replyRate,20]

ChromaTracker
[\channel,channel, \buffer,buffer, \fftSize, 2048, \winType,1, \tuningBase,32.703195662575, \octaves,8, \integrate,0, \integrateCoeff,0.9,  \octaveRatio,2, \normalize,1,\replyRate,20]

SpectralTracker
[\channel,channel, \fftSize,2048, \winType,1, \pcile,0.9, \pcileInterpol,0, \replyRate,20]

KeyTracker
[\channel,channel, \fftSize,2048, \winType,1, \keyDecay,2, \chromaLeak,0.5, \replyRate,20]

OnsetDetector
[\channel,channel, \monitor,monitor, \fftSize,512,  \threshold,0.5, \odftype,'rcomplex', \relaxtime,1, \floor,0.1, \mingap,10, \medianspan,11, \whtype,1, \rawodf,0]

BeatTracker
[\channel,channel, \monitor,monitor, \fftSize,1024, \krChannel,0, \numChannels,5, \windowSize,5, \phaseaccuracy,0.02, \lock,0]

PitchFollower
[\channel,channel, \monitor,monitor, \initFreq,440, \minFreq,60, \maxFreq,4000, \execFreq,100, \maxBinsPerOctave,16, \median,1, \ampThreshold,0.01, \peakThreshold,0.5, \downSample,1, \clar,0, \replayRate,20]

PeakTracker
[\channel,channel, \monitor,monitor, \replyRate,20, \lagTime,0.1, \decay,0.999, \gain, 1]
*/
