// workshop mira



(

GUI.qt;
// values for the main onset ("on beat")
~minTh = 0.5;
~maxTh = 2;
~channel = 2;
~gain = 1;
Task({

	// init RTML
	RTML.init(s);
	2.wait;

	// mix channels 0 and 1

/*	SynthDef(\mix,{ |outBus=2|
		var in0 = SoundIn.ar(0);
		var in1 = SoundIn.ar(1);
		Out.ar(outBus,Mix.new([in0,in1]));
	}).add;*/

	// read only channel 0

	SynthDef(\mix,{ |outBus=2,gainIn=1|
		var in0 = SoundIn.ar(0);
		Out.ar(outBus,in0*gainIn);
	}).add;

	// manage jack connexions
	2.wait;
	//"jack_disconnect system:capture_1 SuperCollider:in_1".systemCmd;
	//"jack_disconnect system:capture_2 SuperCollider:in_2".systemCmd;
	"jack_connect SuperCollider:out_3 SuperCollider:in_3".systemCmd;



	2.wait;
	RTML.verbose_(true);
	~synth=Synth(\mix);

	// instanciate 3 onset detectors

	~mainTh = 1;
	OnsetDetector.new(channel:~channel,monitor:0);
	RTML.elements[\onset_0].set(\threshold,~mainTh/2);

	OnsetDetector.new(channel:~channel,monitor:0);
	RTML.elements[\onset_1].set(\threshold,~mainTh);

	OnsetDetector.new(channel:~channel,monitor:0);
	RTML.elements[\onset_2].set(\threshold,~mainTh*2);

	"NOW-------------------------------------------------".postln;
}).play;

~setTh = { |th|
	RTML.elements[\onset_0].set(\threshold,th/2);
	RTML.elements[\onset_1].set(\threshold,th);
	RTML.elements[\onset_2].set(\threshold,th*2);
};

// create interface

~window = Window.new("MIRA",bounds:Rect(300,300,300,300)).front;
~window.alwaysOnTop_(true);
~slider=Slider.new(~window,Rect(150,0,150,225)).value_(0.5);
~slider.action_({|slider|
	~mainTh = slider.value.linlin(0,1,~minTh,~maxTh); // de 0 a 2
	~label.string_("THRESHOLD: "++~mainTh.asString);

	// set th value
	~setTh.value(~mainTh);
});


~label=StaticText.new(~window,Rect(150,225,250,25)).string_("THRESHOLD: 1");

~sliderGain=Slider.new(~window,Rect(0,0,150,225)).value_(1);
~sliderGain.action_({|slider|
	~gain = slider.value;
	~labelGain.string_("INPUT GAIN: "++~gain.asString);

	// set th value

});


~labelGain=StaticText.new(~window,Rect(0,225,250,25)).string_("INPUT GAIN: 1");

~button0 = Button.new(~window,Rect(0,250,100,50)).canFocus_(false);
~button0.states_([["",Color.black,Color.black],["",Color.red,Color.red]]);
~button1 = Button.new(~window,Rect(100,250,100,50)).canFocus_(false);
~button1.states_([["",Color.black,Color.black],["",Color.yellow,Color.yellow]]);
~button2 = Button.new(~window,Rect(200,250,100,50)).canFocus_(false);
~button2.states_([["",Color.black,Color.black],["",Color.green,Color.green]]);

~buttonOn = { |n|
	var b = switch(n)
	{0} {
		// {
		Task({
			~button0.value_(1);
			0.2.wait;
			~button0.value_(0);
		}).play(AppClock);
		// }.defer;
	}
	{1} {
		// {
		Task({
			~button1.value_(1);
			0.2.wait;
			~button1.value_(0);
		}).play(AppClock);
		// }.defer;
	}
	{2} {
		// {
		Task({
			~button2.value_(1);
			0.2.wait;
			~button2.value_(0);
		}).play(AppClock);
		// }.defer;
	};

};

// create osc responder for gui

OSCdef(\tr0,{|msg|
	var src = msg@1;
	//src.class.postln;
	switch(src)
	{1001} {~buttonOn.(0)}
	{1002} {~buttonOn.(1)}
	{1003} {~buttonOn.(2)};
	//msg.postln;
},'/tr');

// garagekey management

g=GarageKey.new;
g.addAction(\pad,5,{
	// simulate onset_0
	NetAddr.localAddr.sendMsg('/tr',1001,0,0);
	// sent through net
	RTML.elements[\onset_0].sendMsg;
});
g.addAction(\pad,6,{
	// simulate onset_1
	NetAddr.localAddr.sendMsg('/tr',1002,0,0);
	// sent through net
	RTML.elements[\onset_1].sendMsg;
});
g.addAction(\pad,7,{
	// simulate onset_2
	NetAddr.localAddr.sendMsg('/tr',1003,0,0);
	// sent through net
	RTML.elements[\onset_2].sendMsg;
});
g.addAction(\pad,8,{
	// simulate all onsets
	NetAddr.localAddr.sendMsg('/tr',1001,0,0);
	NetAddr.localAddr.sendMsg('/tr',1002,0,0);
	NetAddr.localAddr.sendMsg('/tr',1003,0,0);
	// sent through net
	RTML.elements[\onset_0].sendMsg;
	RTML.elements[\onset_1].sendMsg;
	RTML.elements[\onset_2].sendMsg;
});
g.addAction(\fader,4,{ |v|
	// change threshold
	v = v.linlin(0,127,0,1);
	{~slider.valueAction_(v)}.defer;
});
g.addAction(\fader,1,{ |v|
	// change gain
	~gain = v.linlin(0,127,0,1);
	~synth.set(\gainIn,~gain);

	{~sliderGain.valueAction_(~gain)}.defer;
});



)
/*thisProcess.addOSCRecvFunc(_.postln);

RTML.elements[\onset_0].synth.set(\a,1)
RTML.elements[\onset_0].send_(false)
RTML.elements[\onset_0].msgType_(\random)
RTML.elements[\onset_0].set(\threshold,1)
RTML.elements[\onset_0].send_(true)

RTML.elements[\onset_0].set(\monitor,1)
RTML.elements[\onset_0].set(\channel,0)
RTML.elements[\onset_2].testSound_(true)

)*/
