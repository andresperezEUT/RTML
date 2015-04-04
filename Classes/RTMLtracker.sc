RTMLelement {

	classvar <abstract = true;

	var <nodeID;
	var <synth;

	var <name;
	var <oscMsgName;

	var <parameters;
	var <defaultParameters;



	/*	*new {
	^super.new;
	}*/

	/*	*elements {
	^[this.class.dsps,this.class.trackers].flat;
	}

	*dsps {
	^[PeakEQ];
	}

	*trackers{
	^[OnsetDetector,PitchFollower,BeatTracker,PeakTracker];
	}*/


	runSynth {
		synth = Synth(this.class.synthName,parameters.getPairs);
	}


	// COMPATIBILITY: instanciate by children
	oscReceiverFunction {
	}

	makeGui {
	}

}

RTMLdsp : RTMLelement {

	classvar <abstract = true;

	*new {
		^super.new;
	}
}

RTMLtracker : RTMLelement {

	classvar <abstract = true;

	var <>testSound;

	var <>delta = 0.2;

	var <>send=true;


	/*	*new { |channel, monitor|
	^super.new.initSynth(channel, monitor).initRTMLtracker;
	}*/

	initRTMLtracker { |channel|

		nodeID = synth.nodeID;
		name = RTML.addElement(this,channel); //given by the RTML number of instances of this type
		oscMsgName = "/rtml" +/+ name;

		testSound = false;
		nodeID = synth.nodeID;

		^name;
	}

	get { |parameter|
		^parameters.at(parameter);
	}

	getAllParameters {
		^parameters.getPairs;
	}

	set { |parameter, value|
		// if parameter exists
		if (parameters.keys.asArray.indexOf(parameter).isNil.not) {
			//change value
			parameters.put(parameter,value);
			//set synth
			synth.set(parameter,value);
		} {
			("Parameter " ++ parameter ++ " not defined").warn;
		}
	}

	start {
		synth.run(true);
	}

	stop {
		synth.run(false);
	}

	//public
	testMsg {
		this.sendMsg;
	}

}

// special case for fft and mfcc trackers,
// when a write buffer must be created for accessing data from sclang
RTMLtrackerFFT : RTMLtracker {

	classvar <abstract = true;

	var <>buffer;

	/*	*new{ |channel = 0, monitor = 0, bufferSize = 1024|

	^super.new.initFFT(channel,bufferSize);
	}*/

	initFFT { |channel, bufferSize = 1024|
		{
			buffer = Buffer.alloc(RTML.server,1,bufferSize);
			0.1.wait; // give time to alloc the buffer;
			this.initSynth(channel,bufferSize);
			this.initRTMLtracker(channel);

		}.fork;
	}

}

//////////////////////////////////////////////////////

FFTTracker : RTMLtrackerFFT  {

	classvar <abstract = false;

	classvar <synthName = \fftTracker;

	classvar <sendType =  \continuous;

	var <>sendMode = \continuous;
	var <>registerTrigger = nil; // instance.name of a \trigger tracker
	var <>triggerOn = false;

	*new { |channel = 0, bufferSize = 1024|
		^super.new.initFFT(channel,bufferSize);
	}


	initSynth { |channel|
		parameters = Dictionary.newFrom([\channel,channel, \buffer,buffer, \winType,1, \replyRate,20]);
		defaultParameters = parameters.deepCopy;

		this.runSynth;
		//synth = Synth(\fftTracker,parameters.getPairs);
	}

	oscReceiverFunction {

		this.buffer.getToFloatArray(wait:0.01,action:{ |array|
			if (send) {this.sendMsg(array)};
		});

		// gui
		// --------TODO!
	}

	sendMsg { |array|
		switch (sendMode)
		{\continuous} {
			RTML.destAddr.sendBundle(0,[oscMsgName]++array);
		}
		{\trigger} {
			if (delta==0) { //one value
				if (triggerOn) {
					RTML.destAddr.sendBundle(0,[oscMsgName]++array);
				};
				triggerOn = false;
			} { // delta seconds after
				if (triggerOn) {
					RTML.destAddr.sendBundle(0,[oscMsgName]++array);
				};
				// triggerOn will be disabled from SystemClock.sched in RTML.oscReceiver
			}
		}
	}
}


MFCCTracker : RTMLtrackerFFT {

	classvar <abstract = false;
	classvar <synthName = \mfccTracker;
	classvar <sendType =  \continuous;

	var <>sendMode = \continuous;
	var <>registerTrigger = nil; // instance.name of a \trigger tracker
	var <>triggerOn = false;
	var sliders;
	var values;

	*new { |channel = 0, numMfcc = 20|
		^super.new.initFFT(channel,numMfcc);
	}

	initSynth { |channel,numMfcc|

		parameters = Dictionary.newFrom([\channel,channel, \buffer,buffer, \numMfcc,numMfcc, \fftSize, 1024, \winType,1, \replyRate,20]);
		defaultParameters = parameters.deepCopy;

		this.runSynth;

		sliders = Array.newClear(20);
	}

	oscReceiverFunction {

		this.buffer.getToFloatArray(wait:0.01,action:{ |array|
			values = array;
			if (send) {this.sendMsg(values)};
		});

		// gui
		{
			if (sliders[0].isNil.not) {
				sliders.do{ |slider,i|
					slider.value = values[i];
				}
			}
		}.defer;
	}

	sendMsg { |array|
		switch (sendMode)
		{\continuous} {
			RTML.destAddr.sendBundle(0,[oscMsgName]++array);
		}
		{\trigger} {
			if (delta==0) { //one value
				if (triggerOn) {
					RTML.destAddr.sendBundle(0,[oscMsgName]++array);
				};
				triggerOn = false;
			} { // delta seconds after
				if (triggerOn) {
					RTML.destAddr.sendBundle(0,[oscMsgName]++array);
				};
				// triggerOn will be disabled from SystemClock.sched in RTML.oscReceiver
			}
		}
	}

	makeGui { |view|
		var slider;
		view.addFlowLayout(0@0,0@0);

		20.do{ |i|
			slider = EZSlider(view,Rect(0,0,view.bounds.width/20-0.5,view.bounds.height),layout:\vert);
			slider.controlSpec = ControlSpec(0,1);
			sliders.put(i,slider);
		}
	}
}

ChromaTracker : RTMLtrackerFFT {

	classvar <abstract = false;
	classvar <synthName = \chromaTracker;
	classvar <sendType =  \continuous;

	var <>sendMode = \continuous;
	var <>registerTrigger = nil; // instance.name of a \trigger tracker
	var <>triggerOn = false;

	var sliders;
	var values;

	*new { |channel = 0, numDiv = 12|
		^super.new.initFFT(channel,numDiv);
	}

	initSynth { |channel|

		parameters = Dictionary.newFrom([\channel,channel, \buffer,buffer, \fftSize, 2048, \winType,1, \tuningBase,32.703195662575, \octaves,8, \integrate,0, \integrateCoeff,0.9,  \octaveRatio,2, \normalize,1,\replyRate,20]);
		defaultParameters = parameters.deepCopy;

		this.runSynth;

		sliders = Array.newClear(12);
	}

	oscReceiverFunction {

		this.buffer.getToFloatArray(wait:0.01,action:{ |array|
			values = array / array.sum ; // normalize
			if (send) {this.sendMsg(values)};
		});

		// gui
		{
			if (sliders[0].isNil.not) {
				sliders.do{ |slider,i|
					slider.value = values[i];
				}
			}
		}.defer;
	}

	sendMsg { |array|
		switch (sendMode)
		{\continuous} {
			RTML.destAddr.sendBundle(0,[oscMsgName]++array);
		}
		{\trigger} {
			if (delta==0) { //one value
				if (triggerOn) {
					RTML.destAddr.sendBundle(0,[oscMsgName]++array);
				};
				triggerOn = false;
			} { // delta seconds after
				if (triggerOn) {
					RTML.destAddr.sendBundle(0,[oscMsgName]++array);
				};
				// triggerOn will be disabled from SystemClock.sched in RTML.oscReceiver
			}
		}
	}

	makeGui { |view|
		var slider;
		view.addFlowLayout(0@0,0@0);

		12.do{ |i|
			slider = EZSlider(view,Rect(0,0,view.bounds.width/12-1,view.bounds.height),layout:\vert);
			slider.controlSpec = ControlSpec(0,1);
			sliders.put(i,slider);
		}
	}
}



SpectralTracker : RTMLtracker {

	classvar <abstract = false;
	classvar <synthName = \spectralTracker;
	classvar <sendType =  \continuous;

	var <>sendMode = \continuous;
	var <>registerTrigger = nil; // instance.name of a \trigger tracker
	var <>triggerOn = false;
	var sliders;

	*new{ |channel = 0|
		^super.new.initSynth(channel).initRTMLtracker(channel);
	}

	initSynth { |channel|

		parameters = Dictionary.newFrom([\channel,channel, \fftSize,2048, \winType,1, \pcile,0.9, \pcileInterpol,0, \replyRate,20]);
		defaultParameters = parameters.deepCopy;

		this.runSynth;

		sliders = Array.newClear(3);
	}


	oscReceiverFunction { |msg|
		var type = msg[2]; // 0,1,2
		var value = msg[3];
		var slider;

		var typeSymbol = switch (type)
		{0} { // centroid
			\centroid;
		}
		{1} { // flatness
			\flatness;
		}
		{2} { // pcile
			\pcile;
		};
		this.sendMsg(typeSymbol,value);

		{
			if (sliders[0].isNil.not) {
				slider = sliders[type];
				slider.value_(value);
				//value.postln;
				//slider.value_(value);
			}
		}.defer;
	}

	sendMsg { |type,value = 0|
		switch (sendMode)
		{\continuous} {
			RTML.destAddr.sendMsg(oscMsgName,type,value)
		}
		{\trigger} {
			if (delta==0) { //one value: HERE IS 3 VALUES!!
				if (triggerOn) {
					RTML.destAddr.sendMsg(oscMsgName,type,value)
				};
				// allow for message types 0 and 1 before finishing oneshot trigger
				if (type == \pcile) {
					triggerOn = false;
				};
			} { // delta seconds after
				if (triggerOn) {
					RTML.destAddr.sendMsg(oscMsgName,type,value)
				};
				// triggerOn will be disabled from SystemClock.sched in RTML.oscReceiver
			}
		}
	}

	// TODO: no va!!!!!!
	makeGui { |view|
		var slider;
		view.addFlowLayout(0@0,0@0);

		// centroid
		slider = EZSlider(view,Rect(0,0,view.bounds.width,view.bounds.height/4));
		slider.controlSpec = ControlSpec(100,20000,\exponential,10);
		sliders.put(0,slider);

		// flatness
		slider = EZSlider(view,Rect(0,0,view.bounds.width,view.bounds.height/4));
		slider.controlSpec = ControlSpec(0,1,\lin,0.01);
		sliders.put(1,slider);

		// pcile
		slider = EZSlider(view,Rect(0,0,view.bounds.width,view.bounds.height/4));
		slider.controlSpec = ControlSpec(100,20000,\exponential,10);
		sliders.put(2,slider);
	}
}

KeyTracker : RTMLtracker {

	classvar <abstract = false;
	classvar <synthName = \keyTracker;
	classvar <sendType =  \continuous;

	var <>sendMode = \continuous;
	var <>registerTrigger = nil; // instance.name of a \trigger tracker
	var <>triggerOn = false;

	classvar keyDict;
	var textField;


	*new{ |channel = 0|
		^super.new.initSynth(channel).initRTMLtracker(channel);
	}

	initSynth { |channel|

		parameters = Dictionary.newFrom([\channel,channel, \fftSize,2048, \winType,1, \keyDecay,2, \chromaLeak,0.5, \replyRate,20]);
		defaultParameters = parameters.deepCopy;

		this.runSynth;

		keyDict = Dictionary.newFrom([0,'C',1,'C#',2,'D',3,'D#',4,'E',5,'F',6,'F#',7,'G',8,'G#',9,'A',10,'A#',11,'B',12,'c',13,'c#',14,'d',15,'d#',16,'e',17,'f',18,'f#',19,'g',20,'g#',21,'a',22,'a#',23,'b']);
	}

	oscReceiverFunction { |msg|
		var key = msg[3];
		if (send) {this.sendMsg(key)};

		// gui
		{
			if (textField.isNil.not) {
				textField.value_(keyDict.at(key));
			}
		}.defer
	}

	sendMsg { |value=0|
		switch (sendMode)
		{\continuous} {
			RTML.destAddr.sendMsg(oscMsgName,value);
		}
		{\trigger} {
			if (delta==0) { //one value
				if (triggerOn) {
					RTML.destAddr.sendMsg(oscMsgName,value);
				};
				triggerOn = false;
			} { // delta seconds after
				if (triggerOn) {
					RTML.destAddr.sendMsg(oscMsgName,value);
				};
				// triggerOn will be disabled from SystemClock.sched in RTML.oscReceiver
			}
		}
	}


	makeGui { |view|
		textField = TextField(view,Rect(0,0,view.bounds.width,view.bounds.height));
		textField.font_(Font(size:60));
		textField.align_(\center);
	}

}




OnsetDetector : RTMLtracker {

	classvar <abstract = false;
	classvar <synthName = \onsetDetector;
	classvar <sendType =  \trigger;

	var <>sendMode = \oneshot;
	var onsetButton;

	*new{ |channel = 0, monitor = 0|
		^super.new.initSynth(channel).initRTMLtracker(channel);
	}

	initSynth { |channel, monitor|

		parameters = Dictionary.newFrom([\channel,channel, \monitor,monitor, \fftSize,512,  \threshold,0.5, \odftype,'rcomplex', \relaxtime,1, \floor,0.1, \mingap,10, \medianspan,11, \whtype,1, \rawodf,0]);
		defaultParameters = parameters.deepCopy;

		this.runSynth;
		//synth = Synth(\onsetDetector,parameters.getPairs);
	}

	oscReceiverFunction { |msg|
		if (testSound) {Synth(\click)};
		if (send) {this.sendMsg};

		// gui
		Task({
			if (onsetButton.isNil.not) {
				onsetButton.value_(1);
				0.05.wait;
				onsetButton.value_(0);
			}
		}).play(AppClock);
	}

	// private
	sendMsg {
		switch (sendMode)
		{\oneshot} {
			RTML.destAddr.sendMsg(oscMsgName,1);
		}
		{\onoff} {
			// sending a 1, alternatively open and close button
			RTML.destAddr.sendMsg(oscMsgName,1);
			{RTML.destAddr.sendMsg(oscMsgName,1)}.defer(delta); // TODO: creo que no va!
		}
		{\button} {
			// sending a 1 open, sending a 0 close
			RTML.destAddr.sendMsg(oscMsgName,1);
			{RTML.destAddr.sendMsg(oscMsgName,0)}.defer(delta);
		}
		/*		{\random} {
		RTML.destAddr.sendMsg("/rtml/onset_"++4.rand,1);
		}*/

	}


	makeGui { |view|
		onsetButton = Button(view,Rect(0,0,view.bounds.width,view.bounds.height));
		onsetButton.states_([["",Color.grey,Color.grey],["",Color.red,Color.red]]);
		onsetButton.canFocus_(false);
	}
}


// BeatTracker : RTMLtracker {
//
// 	classvar <abstract = false;
// 	classvar <synthName = \beatTracker;
// 	classvar <sendType =  \trigger;
//
// 	var <>sendMode = \oneshot;
// 	var buttons; // array: black, half, quarter
// 	var numberField;
//
// 	*new{ |channel = 0, monitor = 0|
// 		^super.new.initSynth(channel).initRTMLtracker(channel);
// 	}
//
// 	initSynth { |channel, monitor|
//
// 		parameters = Dictionary.newFrom([\channel,channel, \monitor,monitor, \fftSize,1024, \krChannel,0, \numChannels,5, \windowSize,5, \phaseaccuracy,0.02, \lock,0]);
// 		defaultParameters = parameters.deepCopy;
//
// 		this.runSynth;
// 		//synth = Synth(\beatTracker,parameters.getPairs);
//
// 		buttons = Array.newClear(3);
//
//
// 	}
//
// 	reset {
// 		synth.free;
// 		synth = Synth(\beatTracker,parameters.getPairs);
// 		nodeID = synth.nodeID;
// 		RTML.nodeIDs.add(name.asSymbol -> nodeID);
// 	}
//
// 	oscReceiverFunction { |msg|
// 		var button;
//
// 		var type = msg[2]; // 0,1,2
// 		var tempo = msg[3] * 60; // tempo is in beats per second
//
// 		var typeSymbol = switch (type)
// 		{0} { // black
// 			\black;
// 		}
// 		{1} { // half
// 			\half;
// 		}
// 		{2} { // quarter
// 			\quarter;
// 		};
//
// 		this.sendMsg(type, tempo);
//
// 		// gui
// 		{
// 			if (numberField.isNil.not){
// 				button = buttons[type];
// 				Task({
// 					button.value_(1);
// 					0.05.wait;
// 					button.value_(0);
// 				}).play(AppClock);
//
// 				numberField.value = tempo;
// 			}
// 		}.defer;
//
// 	}
//
// 	// private
// 	sendMsg { |onsetType, tempo|
// 		switch (sendMode)
// 		{\oneshot} {
// 			RTML.destAddr.sendMsg(oscMsgName,sendType,tempo,1);
// 		}
// 		{\onoff} {
// 			// sending a 1, alternatively open and close button
// 			RTML.destAddr.sendMsg(oscMsgName,sendType,tempo,1);
// 			{RTML.destAddr.sendMsg(oscMsgName,sendType,tempo,1);}.defer(delta); // TODO: creo que no va!
// 		}
// 		{\button} {
// 			// sending a 1 open, sending a 0 close
// 			RTML.destAddr.sendMsg(oscMsgName,sendType,tempo,1);
// 			{RTML.destAddr.sendMsg(oscMsgName,sendType,tempo,0);}.defer(delta);
// 		}
//
//
//
// 		/*		switch (sendMode)
// 		{\oneshot} {
// 		// sending a 1, alternatively open and close button
// 		RTML.destAddr.sendMsg(oscMsgName,1);
// 		{RTML.destAddr.sendMsg(oscMsgName,1)}.defer(delta);
// 		}
// 		{\flash} {
// 		// sending a 1 open, sending a 0 close
// 		RTML.destAddr.sendMsg(oscMsgName,1);
// 		{RTML.destAddr.sendMsg(oscMsgName,0)}.defer(delta);
// 		}*/
// 	}
//
// 	makeGui { |view|
// 		3.do { |i|
// 			var b = Button(view,Rect(view.bounds.width*i/3,0,view.bounds.width/3,view.bounds.height*0.75));
// 			b.states_([["",Color.grey,Color.grey],["",Color.red,Color.red]]);
// 			b.canFocus_(false);
// 			buttons = buttons.put(i,b);
// 		};
//
// 		numberField = NumberBox(view,Rect(0,view.bounds.height*0.75,view.bounds.width,view.bounds.height*0.25));
//
// 	}
// }


PitchFollower : RTMLtracker {

	classvar <abstract = false;
	classvar <synthName = \pitchTracker;
	classvar <sendType =  \continuous;

	var <>sendMode = \continuous;
	var <>registerTrigger = nil; // instance.name of a \trigger tracker
	var <>triggerOn = false;
	var valueSlider;

	var <toneSynth;

	*new{ |channel = 0, monitor = 0|
		^super.new.initSynth(channel).initRTMLtracker(channel);
	}

	initSynth { |channel, monitor|

		parameters = Dictionary.newFrom([\channel,channel, \monitor,monitor, \initFreq,440, \minFreq,60, \maxFreq,4000, \execFreq,100, \maxBinsPerOctave,16, \median,1, \ampThreshold,0.01, \peakThreshold,0.5, \downSample,1, \clar,0, \replyRate,20]);

		this.runSynth;
		//synth = Synth(\pitchMono,parameters.getPairs);
		toneSynth = Synth.newPaused(\tone);
	}

	testSound_ { |value|
		testSound = value;
		if (value) {
			toneSynth.run;
		} {
			toneSynth.run(false);
		}
	}

	oscReceiverFunction { |msg|
		var freq = msg[3];
		if (send) {this.sendMsg(freq)};

		// gui
		{
			if (valueSlider.isNil.not) {
				valueSlider.value = freq;
			}
		}.defer;
	}

	sendMsg { |value=0|
		switch (sendMode)
		{\continuous} {
			RTML.destAddr.sendMsg(oscMsgName,value);
		}
		{\trigger} {
			if (delta==0) { //one value
				if (triggerOn) {
					RTML.destAddr.sendMsg(oscMsgName,value);
				};
				triggerOn = false;
			} { // delta seconds after
				if (triggerOn) {
					RTML.destAddr.sendMsg(oscMsgName,value);
				};
				// triggerOn will be disabled from SystemClock.sched in RTML.oscReceiver
			}
		}
	}

	makeGui { |view|
		valueSlider = EZSlider(view,Rect(0,0,view.bounds.width,view.bounds.height));
		valueSlider.controlSpec = ControlSpec(parameters[\minFreq],parameters[\maxFreq],\exponential,0,parameters[\initFreq]);
	}


}

PeakTracker : RTMLtracker {

	classvar <abstract = false;
	classvar <synthName = \peakTracker;
	classvar <sendType =  \continuous;

	var <>sendMode = \continuous;
	var <>registerTrigger = nil; // instance.name of a \trigger tracker
	var <>triggerOn = false;
	var valueSlider;

	*new{ |channel = 0, monitor = 0|
		^super.new.initSynth(channel).initRTMLtracker(channel);
	}

	initSynth { |channel, monitor|

		parameters = Dictionary.newFrom([\channel,channel, \monitor,monitor, \replyRate,20, \lagTime,0.1, \decay,0.999, \gain, 1]);

		this.runSynth;
		//synth = Synth(\peakFollower,parameters.getPairs);
	}


	oscReceiverFunction { |msg|
		var peak = msg[3];
		if (send) {this.sendMsg(peak)};

		// gui
		{
			if (valueSlider.isNil.not) {
				valueSlider.value = peak;
			}
		}.defer;
	}

	sendMsg { |value=0|
		switch (sendMode)
		{\continuous} {
			RTML.destAddr.sendMsg(oscMsgName,value);
		}
		{\trigger} {
			if (delta==0) { //one value
				if (triggerOn) {
					RTML.destAddr.sendMsg(oscMsgName,value);
				};
				triggerOn = false;
			} { // delta seconds after
				if (triggerOn) {
					RTML.destAddr.sendMsg(oscMsgName,value);
				};
				// triggerOn will be disabled from SystemClock.sched in RTML.oscReceiver
			}
		}
	}

	makeGui { |view|
		valueSlider = EZSlider(view,Rect(0,0,view.bounds.width,view.bounds.height));
		valueSlider.controlSpec = ControlSpec(0,1,\lin,0.01);
	}
}
