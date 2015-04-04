// TODO: equalizer, random, tap-tempo, presets, parameter mappings

RTML.init(s);

RTML.verbose_(false)

OnsetDetector.new(channel:0,monitor:1)

RTML.trackers[\onset_0].synth.set(\a,1)
RTML.trackers[\onset_0].send_(false)
RTML.trackers[\onset_0].msgType_(\random)
RTML.trackers[\onset_0].set(\threshold,1)
RTML.trackers[\onset_0].send_(true)

RTML.trackers[\onset_0].set(\monitor,1)
RTML.trackers[\onset_0].set(\channel,3)
RTML.trackers[\onset_0].testSound_(false)



BeatTracker.new(0,1)
RTML.trackers[\beat_0].testSound_(false)
RTML.trackers[\beat_0].set(\monitor,0)
RTML.trackers[\beat_0].set(\channel,1)
RTML.trackers[\beat_0].send_(false)
RTML.trackers[\beat_0].reset
RTML.trackers[\beat_0].set(\lock,1)
RTML.trackers[\beat_0].set(\lock,0)

PitchFollower.new(0,1)

RTML.trackers[\pitch_0].testSound_(false)
RTML.trackers[\pitch_0].send_(false)

RTML.trackers[\pitch_0].mode_(\pitch1)
RTML.trackers[\pitch_0].mode_(\pitch2)
RTML.trackers[\pitch_0].mode_(\height)

PeakTracker.new(0,1)

RTML.trackers[\peak_0].set(\gain,2)
RTML.trackers[\peak_0].set(\lagTime,1)
RTML.trackers[\peak_0].set(\decay,0.999)
RTML.trackers[\peak_0].send_(false)
thisProcess.addOSCRecvFunc(_.postln)
a.class==OnsetDetector.class


// generate responders
(
["onset","beat","pitch","peak"].do{ |n|
	(0..15).do {|i|
		NetAddr("127.0.0.1",7770).sendMsg("/rtml" +/+ n ++"_"++i,1)
	}
}
)

thisProcess.addOSCRecvFunc(_.postln)
(
NetAddr("127.0.0.1",57120).sendMsg("/rtml/peak_0",110.asInt);
NetAddr("127.0.0.1",7770).sendMsg("/rtml/peak",110.asInt,0,0,2)


)
NetAddr("127.0.0.1",7770).sendMsg("/rtml/onset_3",1)


NetAddr("127.0.0.1",7770).sendBundle(0,["/rtml/peak_0",110.asInt,0,0,2,0,0,0,1000,2])
;
a.class


b=BeatTracking.new
b.lock
b.unlock
b.restart


p=PitchFollower.new

p.synth.set(\monitor,0)
p.synth.set(\peakThreshold,1)

x=SynthDef(\a,{|freq=100|Out.ar(0,SinOsc.ar(freq))}).play
// MONITOR OSC -- integrar esto en RTML




s.synth
