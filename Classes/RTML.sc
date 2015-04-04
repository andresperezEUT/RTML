// marco ardilla: 699556422

// REQUEST LIST
/*- add beatTracker
- add dsps (eq, noise gate), aux tracks view
- join all ffts, onset triggers in server, peak gate in server
- add mixer to gui
- create parametersView at instanciate and hide/show it
- add copy/move elements
- add feature text write
- machine learning?
- joint feedback display
- fft gui*/

RTML {

	classvar <server;
	classvar <oscReceiver;
	classvar <>verbose = true;

	classvar <elements;
	classvar <numElements = 0;
	classvar <nodeIDs;
	classvar <orderedNames;

	classvar <numOnsetDetector = 0;
	classvar <numPitchFollower = 0;
	classvar <numBeatTracker = 0;
	classvar <numPeakTracker = 0;
	classvar <numEq = 0;
	classvar numKeyTracker = 0;
	classvar numChromaTracker = 0;
	classvar numSpectralTracker = 0;
	classvar numMfccTracker = 0;
	classvar numFftTracker = 0;

	classvar <numMfcc = 20; // not possible to set it as a SynthDef arg...

	classvar <destAddr; // osc destination NetAddr

	//classvar defaultDestIP = "127.0.0.1"; // this
	//classvar defaultDestIP = "172.31.13.255"; // fabraicoats broadcast
	classvar defaultDestIP = "255.255.255.255";

	/*classvar defaultDestPort = 7770; // 7770 qlcplus*/
	//classvar defaultDestPort = 12000; // 12000 processing
	classvar defaultDestPort = 57120;

	classvar eqSetting;

	classvar <view;

	classvar <dsps;
	classvar <trackers;
	classvar <numDsps;
	classvar <numTrackers;

	classvar <numInputBuses;

	classvar <elementsByBus;

	*init { |myServer,myDestAddr|

		server = myServer;
		server.reboot;
		// TODO: desconectar system ins...

		// num input audio buses
		numInputBuses = server.options.numInputBusChannels;

		elements = Dictionary.new; // (elementName -> element)
		nodeIDs = Dictionary.new; // (elementName -> element.synth.nodeID)

		elementsByBus = Array.fill(numInputBuses,{List.new}); // array with lists containing elementName list

		orderedNames = List.new; // contains elementNames by order of instanciation

		NetAddr.broadcastFlag_(true);
		destAddr = myDestAddr ? NetAddr(defaultDestIP,defaultDestPort);

		// default equalizer settings
		//eqSetting = EQSetting( EQdef( 'peak', BPeakEQ, 'gain', Gain),[[1000,0.5,12],[-6]]);

		// get element lists
		dsps = RTMLdsp.allSubclasses.select(_.abstract.not);
		trackers = RTMLtracker.allSubclasses.select(_.abstract.not);

		// keep the number of instances of each element type
		numDsps = Array.fill(dsps.size,{0});
		numTrackers = Array.fill(trackers.size,{0});



		// create view
		RTMLparameter.init;
		view = RTMLview.new;

		// osc receivers from server

		// TODO: oscReceiver from /b_setn for the FFTsynths
		// then we avoid duplicate messages

		oscReceiver = OSCFunc({ arg msg, time;
			var nodeID = msg[1];
			var element = this.getElementFromNodeID(nodeID);

			// call oscReceiverFunction from the element instance

			if (element.isNil.not) { //avoid error when reset BeatTracker
				element.oscReceiverFunction(msg);

				if (verbose) {
					msg.postln;
				};
			};

			// look into \continuous elements to see if they have a registerTrigger with the given instance
			elements.values.do({ |instance|
				if (instance.class.sendType == \continuous) {
					if (instance.registerTrigger == element.name) {
						var trigger = instance.triggerOn;
						// if trigger is already on, clear the previous sched
						// so it won't collide with new time extension
						if (trigger == true) {
							SystemClock.clear;
						};
						instance.triggerOn_(true);

						// sched triggerOff if delta>0
						if (instance.sendMode == \trigger) {
							if (instance.delta > 0 ) {
								SystemClock.sched(instance.delta,{
									instance.triggerOn_(false);
								});
							}
						}
					}

				}
			});
			// (_.class.sendType==\continuous).dopostln;
			/*			elements.collect(_.class.sendType==\continuous).do{ |e|
			if (e.registerTrigger == element) {
			"REGISTER".postln;
			}
			};*/

		},'/tr', server.addr); //receive only from localhost server


		// load synthdefs
		this.loadSynthDefs;

	}

	*getElementFromNodeID { |nodeID|
		var synthName = nodeIDs.findKeyForValue(nodeID);
		var element = elements.at(synthName);
		^element;
	}

	*removeElement { |element|
		// element is the instance
		var name = element.name;
		var elementIndex;
		var numElements;

		var channel = element.get(\channel);

		// free the synth
		element.synth.free;

		// get element class index inside trackers/dsps array
		// decrease count of element instances
		if (element.class.isKindOfClass2(RTMLtracker)) {
			elementIndex = trackers.indexOf(element.class);
			numElements= numTrackers.at(elementIndex);
			//numTrackers.put(elementIndex,numElements - 1);
		} {
			elementIndex = dsps.indexOf(element.class);
			numElements= numDsps.at(elementIndex);
			//numDsps.put(elementIndex,numElements - 1);
		};

		// remove the link to then element
		elements.removeAt(name.asSymbol);
		nodeIDs.removeAt(name.asSymbol);
		numElements = numElements-1;
		orderedNames.remove(name.asSymbol);

		// add to by-channel classification
		elementsByBus.at(channel).remove(name.asSymbol);
	}


	*addElement { |element, channel|
		var name = element.class.synthName;

		var elementIndex;
		var numElements;

		// get element class index inside trackers/dsps array
		// increase count of element instances
		if (element.class.isKindOfClass2(RTMLtracker)) {
			elementIndex = trackers.indexOf(element.class);
			numElements= numTrackers.at(elementIndex);
			numTrackers.put(elementIndex,numElements + 1);
		} {
			elementIndex = dsps.indexOf(element.class);
			numElements= numDsps.at(elementIndex);
			numDsps.put(elementIndex,numElements + 1);
		};

		// add to internal element storage
		name = element.class.synthName ++ numElements;


		// TODO: ADD PEAK EQ


		elements.add(name.asSymbol -> element);
		nodeIDs.add(name.asSymbol -> element.nodeID);
		numElements = numElements+1;
		orderedNames.add(name.asSymbol);

		// add to by-channel classification
		elementsByBus.at(channel).add(name.asSymbol);


		// PUT OTHER CLASSES

		^name.asSymbol;
	}

	*cloneElement { |element,channel|

		// create new element from same type
		var name = element.class.new(channel);

		// get instance
		var instance = RTML.elements.at(name);

		// copy current parameters
		element.parameters.keysValuesDo { |key, value|
			instance.set(key,value);
		};

		^name;
	}

	///// RESET /////////

	*reset {
		// free all synths
		elements.do(_.free);

		// remove all existing elements
		elements = Dictionary.new; // (elementName -> element)
		nodeIDs = Dictionary.new; // (elementName -> element.synth.nodeID)
		elementsByBus = Array.fill(numInputBuses,{List.new}); // array with lists containing elementName list
		numElements = 0;
		numDsps = Array.fill(dsps.size,{0});
		numTrackers = Array.fill(trackers.size,{0});

		// reset view
		this.view.reset;

	}

	//////// CLOSE ////

	*close {

		// free all synths
		elements.do(_.free);

		// close window
		// this.view.window.close;

		// quit server
		server.quit;

	}


	////////////////// SAVE ////////////////////

	*save {
		Dialog.savePanel({ |path|
			this.saveAction(path);
		})
	}


	*saveAction { |path|
		var d, rtml;
		var elements;
		var file;

		var parameters;
		var osc;

		// create document
		d = DOMDocument.new;

		// create root element "rtml"
		rtml = d.createElement("rtml");
		d.appendChild(rtml);

		// define elements
		elements = d.createElement("elements");
		rtml.appendChild(elements);

		// elements order is given by instanciate order (through RTML.orderedNames)
		// we assure then that names will be identical every time

		RTML.orderedNames.do { |elementName| // they will be in order at load
			var element = RTML.elements.at(elementName);
			// RTML.elements.do { |element|
			// create new element with element type //////////////// ---------------------
			var e = d.createElement("element");
			e.setAttribute( "type", element.class.asString );
			elements.appendChild(e);

			// child: parameters
			parameters = d.createElement("parameters");
			element.parameters.keysValuesDo { |key, value|
				parameters.setAttribute( key, value.asString);
			};
			e.appendChild(parameters);

			// child: osc
			osc = d.createElement("osc");
			osc.setAttribute("sendMode",element.sendMode.asString);
			osc.setAttribute("delta",element.delta.asString);
			if (element.class.sendType == \continuous) {
				osc.setAttribute("registerTrigger",element.registerTrigger.asString);
			};
			e.appendChild(osc);

		};

		file = File(path, "w");
		d.write(file);
		file.close;
	}


	*load {
		Dialog.openPanel({ |path|
			this.loadAction(path);
		})
	}

	*loadAction { |path|
		var filePath, file;
		var document;
		var xmlContent;
		var elements, e;

		var instances = List.new;
		var i = 0;

		this.reset;

		filePath = path;
		file = File(filePath,"r");
		document = DOMDocument.new;

		xmlContent = String.readNew(file);
		document.parseXML(xmlContent); // parses from string
		file.close;

		elements = document.getDocumentElement.getElement("elements");

		// iterate over all elements
		e = elements.getFirstChild;
		while ( { e != nil }, {
			var type, parameters, osc;
			var name, channel, instance;

			// get class
			type = e.getAttribute("type").asSymbol.asClass;
			// get channel
			parameters = e.getFirstChild;
			channel = parameters.getAttribute("channel").asInteger;
			// instanciate element
			name = RTML.view.addElement(type,channel);
			instance = RTML.elements.at(name);
			instances.add(instance);

			// set parameters: iterate all parameters
			instance.parameters.keysValuesDo { |key,value|
				// get values for each parameter
				var v = parameters.getAttribute(key.asString);
				// casting
				var parType = RTMLparameter.get(key,\valueType);
				// [v,key,parType].postln;

				switch (parType)
				{Integer} {v = v.asInt}
				{Float} {v = v.asFloat}
				{Array} {

					var list = RTMLparameter.get(key,\valueList).postln;
					// check type of list elements
					var vType = list[0].class;
					switch(vType)
					{Integer} {v = v.asInt}
					{Symbol} {v = v.asSymbol};
				};

				// set into instance
				instance.set(key,v);


			};

			// get osc (sibling of parameters)
			osc = parameters.getNextSibling;
			instance.delta_(osc.getAttribute("delta").asFloat);
			instance.sendMode_(osc.getAttribute("sendMode").asSymbol);

			// get next element
			e = e.getNextSibling;
		});

		"-----".postln;
		"-----".postln;
		"-----".postln;

		// once all elements are instanciated, we load the registerTrackers
		// because maybe before there were still not created
		elements = document.getDocumentElement.getElement("elements");
		e = elements.getFirstChild;
		while ( { e != nil }, {
			var osc = e.getFirstChild.getNextSibling;
			if (instances.at(i).class.sendType == \continuous) {
				var r = osc.getAttribute("registerTrigger");
				if (r == "nil") {
					r = nil
				} {
					r = r.asSymbol;
				};
				// set the popupmenu value
				instances.at(i).registerTrigger = r;
			};

			i = i + 1;
			e = e.getNextSibling;
		} )

	}

}
