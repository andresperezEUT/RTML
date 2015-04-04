// TODO: show entire numer without 0.01 round in slider's numberbox

RTMLview {

	var <window;
	var bounds;

	var optionsView;
	var elementsView;
	var dspsView, trackersView;
	var elementsViewSize = 100;
	var elementsViewMargin = 50;
	var elementsViewGap = 0;
	var drawView;
	var canvasView;
	var descriptionView;
	var feedbackView;

	var audioBusWidth = 200;
	var audioBusHeight = 50;
	var <audioBusViews;
	var <audioBusElements; // saves text accessing the element
	var audioBusMeters;
	var meterWidth = 20;

	var <selectedElement; // holds a class, for instanciating
	var <currentElement; // holds an element instance

	var <propertiesViewList;

	var saveButton;
	var loadButton;

	*new{
		^super.new.init;
	}

	init {

		GUI.qt;

		bounds = Window.availableBounds;
		window = Window.new("RTML",bounds,resizable:false).front;
		window.onClose_({RTML.close});

		//----------------------------------------------------------------
		// window layout

		// options bar
		optionsView = View(window,Rect(0,0,bounds.width,bounds.height/20));
		optionsView.background = Color.grey(0.6);

		optionsView.addFlowLayout(0@0,0@0);
		saveButton = Button.new(optionsView,Rect(0,0,100,bounds.height/20)).states_([["SAVE"]]);
		saveButton.canFocus_(false);
		saveButton.action = {RTML.save};

		loadButton = Button.new(optionsView,Rect(0,0,100,bounds.height/20)).states_([["LOAD"]]);
		loadButton.canFocus_(false);
		loadButton.action = {RTML.load};

		drawView = View(window,Rect(0,bounds.height/20,bounds.width,bounds.height*19/20));
		drawView.background = Color.grey(0.4);

		// items bar
		elementsView = View(drawView,Rect(0,0,bounds.width,drawView.bounds.height/16));
		elementsView.background = Color.blue;


		trackersView = View(elementsView,Rect(0,0,bounds.width,elementsView.bounds.height));
		trackersView.background = Color.grey(0.7);


		//// UNCOMMENT THESE FOR THE DSPS VIEW
		/*
		elementsView = View(drawView,Rect(0,0,bounds.width,drawView.bounds.height/8));
		elementsView.background = Color.blue;*/

		/*		dspsView = View(elementsView,Rect(0,0,bounds.width,elementsView.bounds.height/2));
		dspsView.background = Color.red;*/
		/*		trackersView = View(elementsView,Rect(0,elementsView.bounds.height/2,bounds.width,elementsView.bounds.height/2));
		trackersView.background = Color.cyan;*/

		// canvas
		/*		canvasView = ScrollView(drawView,Rect(0,drawView.bounds.height/8,bounds.width*0.66,drawView.bounds.height*7/8));
		canvasView.background = Color.grey(0.9);*/

		canvasView = ScrollView(drawView,Rect(0,drawView.bounds.height/16,bounds.width*0.66,drawView.bounds.height*15/16));
		canvasView.background = Color.grey(0.9);

		// input audio buses
		canvasView.addFlowLayout(margin:0@0, gap: 0@0);
		audioBusViews = Array.newClear(RTML.numInputBuses);
		audioBusElements = Array.newClear(RTML.numInputBuses);
		audioBusMeters = Array.newClear(RTML.numInputBuses);

		propertiesViewList = List.new;

		RTML.numInputBuses.do { |i|

			var view, meter;

			view = UserView(canvasView, Rect(0,0,canvasView.bounds.width,audioBusHeight)).background_(Color.rand(0.25,0.75)); // ---> TRACK COLOR!!!!!

			meter = LevelIndicator(view,Rect(0,0,meterWidth,view.bounds.height)).drawsPeak_(true);
			StaticText(view,Rect(0,0,meterWidth,20)).string_("In "++i.asString);

			view.addFlowLayout(meterWidth@0,0@0);

			audioBusViews.put(i,view);
			audioBusElements.put(i,List.new);
			audioBusMeters.put(i,meter);
		};

		// server meters
		{
			SynthDef("RTMLInputLevels", {
				var in = SoundIn.ar((0..RTML.numInputBuses-1));
				SendPeakRMS.kr(in, 10, 3, "/" ++"RTMLInLevels")
			}).play(RootNode(RTML.server), nil, \addToHead);


			// osc responders
			OSCFunc({arg msg;
				var dBLow = -80;
				var channelCount = msg.size - 3 / 2;

				/*				msg.postln;
				channelCount.postln;*/

				channelCount.do {|channel|
					var baseIndex = 3 + (2*channel);
					var peakLevel = msg.at(baseIndex);
					var rmsValue  = msg.at(baseIndex + 1);
					var meter = audioBusMeters.at(channel);
					if (meter.isClosed.not) {
						{
							meter.peakLevel = peakLevel.ampdb.linlin(dBLow, 0, 0, 1, \min);
							meter.value = rmsValue.ampdb.linlin(dBLow, 0, 0, 1);
						}.defer;
					}
				}
			}, '/RTMLInLevels', RTML.server.addr);

		}.defer(1); // give time to the server start

		/*		descriptionView = View(drawView,Rect(drawView.bounds.width*0.66,drawView.bounds.height/16,drawView.bounds.width*0.34,canvasView.bounds.height*0.75)).background_(Color.grey(0.5));
		descriptionView.addFlowLayout(0@0,0@0);

		// feedbackView
		feedbackView = View(drawView,Rect(drawView.bounds.width*0.66,drawView.bounds.height/16+canvasView.bounds.height*0.75, drawView.bounds.width*0.34,canvasView.bounds.height*0.25)).background_(Color.grey(0.55));*/




		////////////// UNCOMMENT F0R DSPSVIEW -----------------------------<>>>>>>>>>>>>>>>>>>>>>>>>!!!!!!

		//descriptionView
		descriptionView = View(drawView,Rect(drawView.bounds.width*0.66,drawView.bounds.height/8,drawView.bounds.width*0.34,canvasView.bounds.height*0.80)).background_(Color.grey(0.5));
		descriptionView.addFlowLayout(0@0,0@0);

		// feedbackView
		feedbackView = View(drawView,Rect(drawView.bounds.width*0.66,drawView.bounds.height/8+(canvasView.bounds.height*0.80), drawView.bounds.width*0.34,canvasView.bounds.height*0.10)).background_(Color.grey(0.55));



		//----------------------------------------------------------------
		// elements
		/*		dspsView.addFlowLayout(margin:elementsViewMargin@0,gap:elementsViewGap@0);
		RTML.dsps.do { |e|
		var string = e.asString;
		StaticText(dspsView,Rect(0,0,elementsViewSize,dspsView.bounds.height)).string_(string);
		};*/

		trackersView.addFlowLayout(margin:elementsViewMargin@0,gap:elementsViewGap@0);
		RTML.trackers.do { |e|
			var string = e.asString;
			StaticText(trackersView,Rect(0,0,elementsViewSize,trackersView.bounds.height)).string_(string);
		};



		//----------------------------------------------------------------
		// actions
		/*		dspsView.mouseDownAction = { |view, x, y, modifiers, buttonNumber, clickCount|
		var dspIndex = (x - (elementsViewMargin) / elementsViewSize).floor;
		[dspIndex].postln;
		};*/

		trackersView.mouseDownAction = { |view, x, y, modifiers, buttonNumber, clickCount|
			var trackerIndex = (x - (elementsViewMargin) / elementsViewSize).floor;
			// [trackerIndex].postln;

			if ( trackerIndex >= 0 and: { trackerIndex < RTML.trackers.size }) {
				selectedElement = RTML.trackers[trackerIndex].postln;
			}
		};


		drawView.mouseUpAction = { |view, x, y, modifiers|
			//["draw",x,y].postln;
			//(y-(drawView.bounds.height/8)).postln;


			// place element in bus
			var bus;
			var optionsHeight = drawView.bounds.height/16; //<----------------- CHANGE FOR DSPSVIEW!!
			var offsetY = y - optionsHeight;

			if ( offsetY > 0 ) {
				bus = (offsetY / audioBusHeight).floor;

				if ( bus < RTML.numInputBuses) {
					bus.postln;

					if ( selectedElement.isNil.not ) {

						//RTML.add
						this.addElement(selectedElement,bus);

					}
				};
			};

			// deselect elements
			selectedElement = nil;
		};

		canvasView.mouseUpAction = { |view, x, y, modifiers|
			["canvas",x,y].postln;
			// deselect elements
			selectedElement = nil;
		};

		audioBusViews.do { |view,i|

			// add/remove elements with click up
			view.mouseDownAction = { |view, x, y, modifiers|

				var bus = i;
				var element = ((x - meterWidth) / elementsViewSize).floor;

				if ( element >= 0 ) {
					if ( element < RTML.elementsByBus[bus].size ) {

						// show element properties
						currentElement = RTML.elementsByBus[bus][element]; //it's a string
						this.clearProperties;
						this.showProperties(currentElement,bus);

						// remove element with Control+click
						if (modifiers == 262144) {
							this.removeElement(currentElement,bus);
							currentElement = nil;
						}


					} {
						// clear
						currentElement = nil;
						this.clearProperties;
					}
				};
				// deselect elements
				selectedElement = nil;
			};

			////////////////////// EXPERIMENTAL //////////////////////////////

			/*			// copy elements with mouseUp
			view.mouseUpAction = { |view, x, y, modifiers|
			var bus = i;
			if (currentElement.isNil.not) {
			this.cloneElement(currentElement,bus);
			// this.removeElement(currentElement,bus);
			};
			// this.moveElement(currentElement,i);

			};*/


		};

	}

	addElement { |elementClass, bus|

		var name;
		//show
		var text = StaticText(audioBusViews.at(bus),Rect(0,0,elementsViewSize,audioBusHeight));
		text.string_(elementClass/*.name*/);
		audioBusElements.at(bus).add(text);

		//add element
		name=elementClass.new(bus).postln;

		// create instance in propertiesViewList
		// TODO!!!
		// propertiesViewList.add( name -> this.createProperties(name,bus).visible_(false));

		^name;
	}


	////////////////////// EXPERIMENTAL //////////////////////////////

	moveElement {  |instance,destBus|

		var initBus;
		// clone element into final bus
		this.addElement(instance.class,destBus);
		initBus = instance.channel;
		// delete instance
		this.removeElement(instance.name,initBus);
	}

	cloneElement { |element,destBus|
		var text;
		var instance = RTML.elements.at(element);
		RTML.cloneElement(instance,3);

		// show
		text = StaticText(audioBusViews.at(destBus),Rect(0,0,elementsViewSize,audioBusHeight));
		text.string_(instance.class);
		audioBusElements.at(destBus).add(text);
	}

	////////////////////// EXPERIMENTAL //////////////////////////////


	removeElement { |element,bus|

		var text;

		// remove element from RTML lists
		RTML.removeElement(RTML.elements.at(element));

		// audioBusElements.at(bus).do{|e|e.class.postln;e.remove}; //???

		// remove all existing views
		audioBusViews.at(bus).removeAll;

		// remove flowLayout
		audioBusViews.at(bus).decorator.left_(0);

		// instanciate again peak meter
		// remove old instance
		audioBusMeters.at(bus).remove;
		// create new inside audioBusMeters, so the osc responder can actuate
		audioBusMeters.put(bus, LevelIndicator(audioBusViews.at(bus), Rect(0,0,meterWidth,audioBusViews.at(bus).bounds.height)).drawsPeak_(true););

		// instanciate again bus info text
		audioBusViews.at(bus).decorator.left_(0);
		StaticText(audioBusViews.at(bus),Rect(0,0,meterWidth,20)).string_("In "++bus.asString);


		// create new elements list (delete all previous existing)
		audioBusElements.put(bus,List.new);
		// fill elements list with RTML instances
		RTML.elementsByBus.at(bus).do{|element,i|
			text = StaticText(audioBusViews.at(bus),Rect(0,0,elementsViewSize,audioBusHeight));
			text.string_(RTML.elements.at(element).class);
			element.postln;
		};

		// clear properties window
		this.clearProperties;
	}



	//----------------------------------------------------------------
	// properties tab

	clearProperties {
		descriptionView.removeAll;
		feedbackView.remove; /// ......----------------------->>>>>>>>>>>>>>>>>>>>>>< // removeAll
	}



	// TODO: save views and only show them every time, not create them every time
	/*createProperties { |elementName,channel|
	// new descriptionView
	var v = View(drawView, Rect(drawView.bounds.width*0.66,drawView.bounds.height/8, drawView.bounds.width*0.34,canvasView.bounds.height));
	var w = descriptionView.bounds.width;
	var h = descriptionView.bounds.height;

	var buttonList = List.new;

	var instance = RTML.elements.at(elementName);
	var textHeight = 25;
	v.background_(Color.grey(0.5));
	descriptionView.addFlowLayout(0@0,0@0);

	// elementName.postln;

	/*		// clear previous description
	v.removeAll;
	v.addFlowLayout(0@0,0@0);*/

	// fill descriptionView
	StaticText(v,Rect(0,0,w,textHeight*2)).string_("PARAMETERS");

	StaticText(v,Rect(0,0,w/2,textHeight)).string_("instance name");
	StaticText(v,Rect(0,0,w/2,textHeight)).string_(elementName);

	StaticText(v,Rect(0,0,w/2,textHeight)).string_("channel");
	StaticText(v,Rect(0,0,w/2,textHeight)).string_(channel);

	StaticText(v,Rect(0,0,w/2,textHeight)).string_("oscMessageName");
	StaticText(v,Rect(0,0,w/2,textHeight)).string_(instance.oscMsgName);

	// separator
	StaticText(v,Rect(0,0,w,textHeight*2)).string_("");

	// all default button
	Button(v,Rect(0,0,w/8,textHeight)).states_([["default"]]).action_({
	buttonList.do(_.doAction)
	});


	// place widgets according to RTMLparameter definitions
	instance.parameters.keysValuesDo{ |key,value|
	var gui;
	var button;

	if ( key != \channel ) {
	if ( key != \buffer ) {

	// default value button
	v.decorator.nextLine;
	button = Button(v,Rect(0,0,w/16,textHeight));
	buttonList.add( button );

	// search in RTMLparameter the gui definition and attributes
	switch(RTMLparameter.get(key,\guiType))

	{Slider} {
	var minVal = RTMLparameter.get(key,\minVal);
	var maxVal = RTMLparameter.get(key,\maxVal);
	var warp = RTMLparameter.get(key,\warp);
	var valueType = RTMLparameter.get(key,\valueType);
	var step = RTMLparameter.get(key,\step);
	// however the value we got it from instance.parameters value
	var spec = ControlSpec(minVal,maxVal,warp,step);

	gui = EZSlider(v,Rect(0,0,w*15/16,textHeight),key,labelWidth:w/4,numberWidth:w/4);
	gui.controlSpec = spec;
	gui.value = value;

	gui.round_(0.000001); //?

	gui.action = { |obj|
	var value = obj.value.postln;
	instance.set(key,value);
	};

	button.action_({
	gui.valueAction_(instance.defaultParameters.at(key));
	});
	}

	{PopUpMenu} {
	var items = RTMLparameter.get(key,\valueList);
	gui = EZPopUpMenu(v,Rect(0,0,w*15/16,textHeight),key,labelWidth:w/4);
	gui.items = items;
	gui.value = items.indexOf(value);
	gui.globalAction = {|obj|obj.value.postln};

	button.action_({
	gui.valueAction_(items.indexOf(instance.defaultParameters.at(key)));
	});
	};
	}
	}
	};

	//return the view
	^v;
	}*/

	showProperties { |elementName,channel|
		var v = descriptionView;
		var w = descriptionView.bounds.width;
		var h = descriptionView.bounds.height;

		var buttonList = List.new;

		var instance = RTML.elements.at(elementName);
		var textHeight = 25;

		// elementName.postln;

		// clear previous description
		v.removeAll;
		v.addFlowLayout(0@0,0@0);

		// fill descriptionView
		StaticText(v,Rect(0,0,w/4,textHeight)).string_("instance name");
		StaticText(v,Rect(0,0,w/2,textHeight)).string_(elementName);
		v.decorator.nextLine;

		StaticText(v,Rect(0,0,w/4,textHeight)).string_("channel");
		StaticText(v,Rect(0,0,w/2,textHeight)).string_(channel);
		v.decorator.nextLine;

		StaticText(v,Rect(0,0,w/4,textHeight)).string_("oscMessageName");
		StaticText(v,Rect(0,0,w/2,textHeight)).string_(instance.oscMsgName);
		v.decorator.nextLine;
		/*		Button(v,Rect(0,0,w/4,textHeight)).states_([["test"]]).action_({
		instance.sendMsg;
		});*/
		StaticText(v,Rect(0,0,w/4,textHeight)).string_("oscSend");
		PopUpMenu(v,Rect(0,0,w/4,textHeight)).items_(["off","on"]).value_(instance.send.asInt).action_({ |v|
			instance.send_(v.value.asBoolean);
		});
		Button(v,Rect(0,0,w/4,textHeight)).states_([["test"]]).action_({
			instance.sendMsg;
		});
		v.decorator.nextLine;

		StaticText(v,Rect(0,0,w/4,textHeight)).string_("oscSendType");
		StaticText(v,Rect(0,0,w/4,textHeight)).string_(instance.class.sendType);
		v.decorator.nextLine;

		StaticText(v,Rect(0,0,w/4,textHeight)).string_("oscSendMode");
		switch(instance.class.sendType)
		{\trigger} {
			var p, s;
			// trigger mode
			p = PopUpMenu(v,Rect(0,0,w/4,textHeight));
			p.items_([\oneshot,\onoff,\button]);
			p.value_(p.items.indexOf(instance.sendMode));
			p.action_({ |v|
				instance.sendMode_(v.item);
			});
			// delta value
			s = EZSlider(v,Rect(0,0,w/2,textHeight),"delta",layout:\horz,labelWidth:w/10,numberWidth:w/10);
			s.value_(instance.delta);
			s.action_({ |s|
				instance.delta_(s.value);
			});
		}
		{\continuous} {
			var p,q, triggers,s;
			// trigger mode
			p = PopUpMenu(v,Rect(0,0,w/4,textHeight));
			p.items_([\continuous,\trigger]);
			p.value_(p.items.indexOf(instance.sendMode));
			p.action_({ |v|
				instance.sendMode_(v.item);
			});


			// instance register
			q = PopUpMenu(v,Rect(0,0,w/4,textHeight));
			// get trigger instances
			triggers = List.new;
			RTML.elements.values.do({ |instance|
				if (instance.class.sendType == \trigger) {
					triggers.add(instance.name);
				}
			});
			q.items_( ["nil"]++triggers.asArray);
			if (instance.registerTrigger.isNil) {
				q.value_(0);
			} {
				q.value_(q.items.indexOf(instance.registerTrigger));
			};
			// set action
			q.action_({ |v|
				instance.registerTrigger = v.item;
			});
			v.decorator.nextLine;
			StaticText(v,Rect(0,0,w/4,textHeight)).string_("");

			// delta value
			s = EZSlider(v,Rect(0,0,w/2,textHeight),"delta",layout:\horz,labelWidth:w/10,numberWidth:w/10);
			s.controlSpec = ControlSpec(0,5,\lin,0.1);
			s.value_(instance.delta);
			s.action_({ |s|
				instance.delta_(s.value);
			});
		};
		v.decorator.nextLine;
		/*		// separator
		StaticText(v,Rect(0,0,w,textHeight*2)).string_("");*/

		// all default button
		Button(v,Rect(0,0,w/8,textHeight)).states_([["default"]]).action_({
			buttonList.do(_.doAction)
		});


		// place widgets according to RTMLparameter definitions
		instance.parameters.keysValuesDo{ |key,value|
			var gui;
			var button;

			if ( key != \channel ) {
				if ( key != \buffer ) {

					// default value button
					v.decorator.nextLine;
					button = Button(v,Rect(0,0,w/16,textHeight));
					buttonList.add( button );

					// search in RTMLparameter the gui definition and attributes
					switch(RTMLparameter.get(key,\guiType))

					{Slider} {
						var minVal = RTMLparameter.get(key,\minVal);
						var maxVal = RTMLparameter.get(key,\maxVal);
						var warp = RTMLparameter.get(key,\warp);
						var valueType = RTMLparameter.get(key,\valueType);
						var step = RTMLparameter.get(key,\step);
						// however the value we got it from instance.parameters value
						var spec = ControlSpec(minVal,maxVal,warp,step);

						gui = EZSlider(v,Rect(0,0,w*15/16,textHeight),key,labelWidth:w/4,numberWidth:w/4);
						gui.controlSpec = spec;
						gui.value = value;

						gui.round_(0.000001); //?

						gui.action = { |obj|
							var value = obj.value.postln;
							instance.set(key,value);
						};

						button.action_({
							gui.valueAction_(instance.defaultParameters.at(key));
						});
					}

					{PopUpMenu} {
						var items = RTMLparameter.get(key,\valueList);
						gui = EZPopUpMenu(v,Rect(0,0,w*15/16,textHeight),key,labelWidth:w/4);
						gui.items = items;
						gui.value = items.indexOf(value);
						gui.globalAction = {|obj|
							var value = obj.value.postln;
							instance.set(key,items.indexOf(value));
						};

						button.action_({
							gui.valueAction_(items.indexOf(instance.defaultParameters.at(key)));
						});
					};
				}
			}
		};

		// separator
		// StaticText(v,Rect(0,0,w,textHeight*2)).string_("");
		// feedbackView = View(v,Rect(0,0,w,200)).background_(Color.red);

		feedbackView = View(drawView,Rect(drawView.bounds.width*0.66,drawView.bounds.height/8+(canvasView.bounds.height*0.75), drawView.bounds.width*0.34,canvasView.bounds.height*0.15)).background_(Color.grey(0.55));

		// visualization
		instance.makeGui(feedbackView);
	}

	reset {
		RTML.numInputBuses.do { |bus|

			audioBusElements.put(bus,List.new);

			audioBusViews.at(bus).removeAll;
			// remove flowLayout
			audioBusViews.at(bus).decorator.left_(0);

			// instanciate again peak meter
			// remove old instance
			audioBusMeters.at(bus).remove;
			// create new inside audioBusMeters, so the osc responder can actuate
			audioBusMeters.put(bus, LevelIndicator(audioBusViews.at(bus), Rect(0,0,meterWidth,audioBusViews.at(bus).bounds.height)).drawsPeak_(true););

			// instanciate again bus info text
			audioBusViews.at(bus).decorator.left_(0);
			StaticText(audioBusViews.at(bus),Rect(0,0,meterWidth,20)).string_("In "++bus.asString);

			// remove properties view
			this.clearProperties;


		};
	}
}

// TODO: show entire numer without 0.01 round in slider's numberbox
//
// RTMLview {
//
// 	var <window;
// 	var bounds;
//
// 	var optionsView;
// 	var elementsView;
// 	var dspsView, trackersView;
// 	var elementsViewSize = 100;
// 	var elementsViewMargin = 50;
// 	var elementsViewGap = 0;
// 	var drawView;
// 	var canvasView;
// 	var descriptionView;
// 	var feedbackView;
//
// 	var audioBusWidth = 200;
// 	var audioBusHeight = 50;
// 	var <audioBusViews;
// 	var <audioBusElements; // saves text accessing the element
// 	var audioBusMeters;
// 	var meterWidth = 20;
//
// 	var <selectedElement; // holds a class, for instanciating
// 	var <currentElement; // holds an element instance
//
// 	var <propertiesViewList;
//
// 	var saveButton;
// 	var loadButton;
//
// 	*new{
// 		^super.new.init;
// 	}
//
// 	init {
//
// 		GUI.qt;
//
// 		bounds = Window.availableBounds;
// 		window = Window.new("RTML",bounds,resizable:false).front;
// 		window.onClose_({RTML.close});
//
// 		//----------------------------------------------------------------
// 		// window layout
//
// 		// options bar
// 		optionsView = View(window,Rect(0,0,bounds.width,bounds.height/20));
// 		optionsView.background = Color.grey(0.6);
//
// 		optionsView.addFlowLayout(0@0,0@0);
// 		saveButton = Button.new(optionsView,Rect(0,0,100,bounds.height/20)).states_([["SAVE"]]);
// 		saveButton.canFocus_(false);
// 		saveButton.action = {RTML.save};
//
// 		loadButton = Button.new(optionsView,Rect(0,0,100,bounds.height/20)).states_([["LOAD"]]);
// 		loadButton.canFocus_(false);
// 		loadButton.action = {RTML.load};
//
// 		drawView = View(window,Rect(0,bounds.height/20,bounds.width,bounds.height*19/20));
// 		drawView.background = Color.grey(0.4);
//
// 		// items bar
// 		elementsView = View(drawView,Rect(0,0,bounds.width,drawView.bounds.height/8));
// 		elementsView.background = Color.blue;
//
// 		dspsView = View(elementsView,Rect(0,0,bounds.width,elementsView.bounds.height/2));
// 		dspsView.background = Color.red;
//
//
// 		trackersView = View(elementsView,Rect(0,elementsView.bounds.height/2,bounds.width,elementsView.bounds.height/2));
// 		trackersView.background = Color.cyan;
//
// 		// canvas
// 		canvasView = ScrollView(drawView,Rect(0,drawView.bounds.height/8,bounds.width*0.66,drawView.bounds.height*7/8));
// 		canvasView.background = Color.grey(0.9);
//
// 		// input audio buses
// 		canvasView.addFlowLayout(margin:0@0, gap: 0@0);
// 		audioBusViews = Array.newClear(RTML.numInputBuses);
// 		audioBusElements = Array.newClear(RTML.numInputBuses);
// 		audioBusMeters = Array.newClear(RTML.numInputBuses);
//
// 		propertiesViewList = List.new;
//
// 		RTML.numInputBuses.do { |i|
//
// 			var view, meter;
//
// 			view = UserView(canvasView, Rect(0,0,canvasView.bounds.width,audioBusHeight)).background_(Color.rand);
// 			meter = LevelIndicator(view,Rect(0,0,meterWidth,view.bounds.height)).drawsPeak_(true);
// 			StaticText(view,Rect(0,0,meterWidth,20)).string_("In "++i.asString);
//
// 			view.addFlowLayout(meterWidth@0,0@0);
//
// 			audioBusViews.put(i,view);
// 			audioBusElements.put(i,List.new);
// 			audioBusMeters.put(i,meter);
// 		};
//
// 		// server meters
// 		{
// 			SynthDef("RTMLInputLevels", {
// 				var in = SoundIn.ar((0..RTML.numInputBuses-1));
// 				SendPeakRMS.kr(in, 10, 3, "/" ++"RTMLInLevels")
// 			}).play(RootNode(RTML.server), nil, \addToHead);
//
//
// 			// osc responders
// 			OSCFunc({arg msg;
// 				var dBLow = -80;
// 				var channelCount = msg.size - 3 / 2;
//
// 				/*				msg.postln;
// 				channelCount.postln;*/
//
// 				channelCount.do {|channel|
// 					var baseIndex = 3 + (2*channel);
// 					var peakLevel = msg.at(baseIndex);
// 					var rmsValue  = msg.at(baseIndex + 1);
// 					var meter = audioBusMeters.at(channel);
// 					if (meter.isClosed.not) {
// 						{
// 							meter.peakLevel = peakLevel.ampdb.linlin(dBLow, 0, 0, 1, \min);
// 							meter.value = rmsValue.ampdb.linlin(dBLow, 0, 0, 1);
// 						}.defer;
// 					}
// 				}
// 			}, '/RTMLInLevels', RTML.server.addr);
//
// 		}.defer(1); // give time to the server start
//
//
//
// 		// descriptionView
// 		descriptionView = View(drawView,Rect(drawView.bounds.width*0.66,drawView.bounds.height/8,drawView.bounds.width*0.34,canvasView.bounds.height*0.85)).background_(Color.grey(0.5));
// 		descriptionView.addFlowLayout(0@0,0@0);
//
// 		// feedbackView
// 		feedbackView = View(drawView,Rect(drawView.bounds.width*0.66,drawView.bounds.height/8+(canvasView.bounds.height*0.85), drawView.bounds.width*0.34,canvasView.bounds.height*0.10)).background_(Color.grey(0.55));
//
//
//
// 		//----------------------------------------------------------------
// 		// elements
// 		dspsView.addFlowLayout(margin:elementsViewMargin@0,gap:elementsViewGap@0);
// 		RTML.dsps.do { |e|
// 			var string = e.asString;
// 			StaticText(dspsView,Rect(0,0,elementsViewSize,dspsView.bounds.height)).string_(string);
// 		};
//
// 		trackersView.addFlowLayout(margin:elementsViewMargin@0,gap:elementsViewGap@0);
// 		RTML.trackers.do { |e|
// 			var string = e.asString;
// 			StaticText(trackersView,Rect(0,0,elementsViewSize,trackersView.bounds.height)).string_(string);
// 		};
//
//
//
// 		//----------------------------------------------------------------
// 		// actions
// 		dspsView.mouseDownAction = { |view, x, y, modifiers, buttonNumber, clickCount|
// 			var dspIndex = (x - (elementsViewMargin) / elementsViewSize).floor;
// 			[dspIndex].postln;
// 		};
//
// 		trackersView.mouseDownAction = { |view, x, y, modifiers, buttonNumber, clickCount|
// 			var trackerIndex = (x - (elementsViewMargin) / elementsViewSize).floor;
// 			// [trackerIndex].postln;
//
// 			if ( trackerIndex >= 0 and: { trackerIndex < RTML.trackers.size }) {
// 				selectedElement = RTML.trackers[trackerIndex].postln;
// 			}
// 		};
//
//
// 		drawView.mouseUpAction = { |view, x, y, modifiers|
// 			//["draw",x,y].postln;
// 			//(y-(drawView.bounds.height/8)).postln;
//
//
// 			// place element in bus
// 			var bus;
// 			var optionsHeight = drawView.bounds.height/8;
// 			var offsetY = y - optionsHeight;
//
// 			if ( offsetY > 0 ) {
// 				bus = (offsetY / audioBusHeight).floor;
//
// 				if ( bus < RTML.numInputBuses) {
// 					bus.postln;
//
// 					if ( selectedElement.isNil.not ) {
//
// 						//RTML.add
// 						this.addElement(selectedElement,bus);
//
// 					}
// 				};
// 			};
//
// 			// deselect elements
// 			selectedElement = nil;
// 		};
//
// 		canvasView.mouseUpAction = { |view, x, y, modifiers|
// 			["canvas",x,y].postln;
// 			// deselect elements
// 			selectedElement = nil;
// 		};
//
// 		audioBusViews.do { |view,i|
//
// 			// add/remove elements with click up
// 			view.mouseDownAction = { |view, x, y, modifiers|
//
// 				var bus = i;
// 				var element = ((x - meterWidth) / elementsViewSize).floor;
//
// 				if ( element >= 0 ) {
// 					if ( element < RTML.elementsByBus[bus].size ) {
//
// 						// show element properties
// 						currentElement = RTML.elementsByBus[bus][element]; //it's a string
// 						this.clearProperties;
// 						this.showProperties(currentElement,bus);
//
// 						// remove element with Control+click
// 						if (modifiers == 262144) {
// 							this.removeElement(currentElement,bus);
// 							currentElement = nil;
// 						}
//
//
// 					} {
// 						// clear
// 						currentElement = nil;
// 						this.clearProperties;
// 					}
// 				};
// 				// deselect elements
// 				selectedElement = nil;
// 			};
//
// 			////////////////////// EXPERIMENTAL //////////////////////////////
//
// 			/*			// copy elements with mouseUp
// 			view.mouseUpAction = { |view, x, y, modifiers|
// 			var bus = i;
// 			if (currentElement.isNil.not) {
// 			this.cloneElement(currentElement,bus);
// 			// this.removeElement(currentElement,bus);
// 			};
// 			// this.moveElement(currentElement,i);
//
// 			};*/
//
//
// 		};
//
// 	}
//
// 	addElement { |elementClass, bus|
//
// 		var name;
// 		//show
// 		var text = StaticText(audioBusViews.at(bus),Rect(0,0,elementsViewSize,audioBusHeight));
// 		text.string_(elementClass/*.name*/);
// 		audioBusElements.at(bus).add(text);
//
// 		//add element
// 		name=elementClass.new(bus);
//
// 		// create instance in propertiesViewList
// 		// TODO!!!
// 		// propertiesViewList.add( name -> this.createProperties(name,bus).visible_(false));
//
// 		^name;
// 	}
//
//
// 	////////////////////// EXPERIMENTAL //////////////////////////////
//
// 	moveElement {  |instance,destBus|
//
// 		var initBus;
// 		// clone element into final bus
// 		this.addElement(instance.class,destBus);
// 		initBus = instance.channel;
// 		// delete instance
// 		this.removeElement(instance.name,initBus);
// 	}
//
// 	cloneElement { |element,destBus|
// 		var text;
// 		var instance = RTML.elements.at(element);
// 		RTML.cloneElement(instance,3);
//
// 		// show
// 		text = StaticText(audioBusViews.at(destBus),Rect(0,0,elementsViewSize,audioBusHeight));
// 		text.string_(instance.class);
// 		audioBusElements.at(destBus).add(text);
// 	}
//
// 	////////////////////// EXPERIMENTAL //////////////////////////////
//
//
// 	removeElement { |element,bus|
//
// 		var text;
//
// 		// remove element from RTML lists
// 		RTML.removeElement(RTML.elements.at(element));
//
// 		// audioBusElements.at(bus).do{|e|e.class.postln;e.remove}; //???
//
// 		// remove all existing views
// 		audioBusViews.at(bus).removeAll;
//
// 		// remove flowLayout
// 		audioBusViews.at(bus).decorator.left_(0);
//
// 		// instanciate again peak meter
// 		// remove old instance
// 		audioBusMeters.at(bus).remove;
// 		// create new inside audioBusMeters, so the osc responder can actuate
// 		audioBusMeters.put(bus, LevelIndicator(audioBusViews.at(bus), Rect(0,0,meterWidth,audioBusViews.at(bus).bounds.height)).drawsPeak_(true););
//
// 		// instanciate again bus info text
// 		audioBusViews.at(bus).decorator.left_(0);
// 		StaticText(audioBusViews.at(bus),Rect(0,0,meterWidth,20)).string_("In "++bus.asString);
//
//
// 		// create new elements list (delete all previous existing)
// 		audioBusElements.put(bus,List.new);
// 		// fill elements list with RTML instances
// 		RTML.elementsByBus.at(bus).do{|element,i|
// 			text = StaticText(audioBusViews.at(bus),Rect(0,0,elementsViewSize,audioBusHeight));
// 			text.string_(RTML.elements.at(element).class);
// 			element.postln;
// 		};
//
// 		// clear properties window
// 		this.clearProperties;
// 	}
//
//
//
// 	//----------------------------------------------------------------
// 	// properties tab
//
// 	clearProperties {
// 		descriptionView.removeAll;
// 		feedbackView.remove;
// 		// feedbackView.removeAll; /////////////// ----------------------------<<<<<<<<<<<<<<<<<<<<
// 	}
//
//
//
// 	// TODO: save views and only show them every time, not create them every time
// 	/*createProperties { |elementName,channel|
// 	// new descriptionView
// 	var v = View(drawView, Rect(drawView.bounds.width*0.66,drawView.bounds.height/8, drawView.bounds.width*0.34,canvasView.bounds.height));
// 	var w = descriptionView.bounds.width;
// 	var h = descriptionView.bounds.height;
//
// 	var buttonList = List.new;
//
// 	var instance = RTML.elements.at(elementName);
// 	var textHeight = 25;
// 	v.background_(Color.grey(0.5));
// 	descriptionView.addFlowLayout(0@0,0@0);
//
// 	// elementName.postln;
//
// 	/*		// clear previous description
// 	v.removeAll;
// 	v.addFlowLayout(0@0,0@0);*/
//
// 	// fill descriptionView
// 	StaticText(v,Rect(0,0,w,textHeight*2)).string_("PARAMETERS");
//
// 	StaticText(v,Rect(0,0,w/2,textHeight)).string_("instance name");
// 	StaticText(v,Rect(0,0,w/2,textHeight)).string_(elementName);
//
// 	StaticText(v,Rect(0,0,w/2,textHeight)).string_("channel");
// 	StaticText(v,Rect(0,0,w/2,textHeight)).string_(channel);
//
// 	StaticText(v,Rect(0,0,w/2,textHeight)).string_("oscMessageName");
// 	StaticText(v,Rect(0,0,w/2,textHeight)).string_(instance.oscMsgName);
//
// 	// separator
// 	StaticText(v,Rect(0,0,w,textHeight*2)).string_("");
//
// 	// all default button
// 	Button(v,Rect(0,0,w/8,textHeight)).states_([["default"]]).action_({
// 	buttonList.do(_.doAction)
// 	});
//
//
// 	// place widgets according to RTMLparameter definitions
// 	instance.parameters.keysValuesDo{ |key,value|
// 	var gui;
// 	var button;
//
// 	if ( key != \channel ) {
// 	if ( key != \buffer ) {
//
// 	// default value button
// 	v.decorator.nextLine;
// 	button = Button(v,Rect(0,0,w/16,textHeight));
// 	buttonList.add( button );
//
// 	// search in RTMLparameter the gui definition and attributes
// 	switch(RTMLparameter.get(key,\guiType))
//
// 	{Slider} {
// 	var minVal = RTMLparameter.get(key,\minVal);
// 	var maxVal = RTMLparameter.get(key,\maxVal);
// 	var warp = RTMLparameter.get(key,\warp);
// 	var valueType = RTMLparameter.get(key,\valueType);
// 	var step = RTMLparameter.get(key,\step);
// 	// however the value we got it from instance.parameters value
// 	var spec = ControlSpec(minVal,maxVal,warp,step);
//
// 	gui = EZSlider(v,Rect(0,0,w*15/16,textHeight),key,labelWidth:w/4,numberWidth:w/4);
// 	gui.controlSpec = spec;
// 	gui.value = value;
//
// 	gui.round_(0.000001); //?
//
// 	gui.action = { |obj|
// 	var value = obj.value.postln;
// 	instance.set(key,value);
// 	};
//
// 	button.action_({
// 	gui.valueAction_(instance.defaultParameters.at(key));
// 	});
// 	}
//
// 	{PopUpMenu} {
// 	var items = RTMLparameter.get(key,\valueList);
// 	gui = EZPopUpMenu(v,Rect(0,0,w*15/16,textHeight),key,labelWidth:w/4);
// 	gui.items = items;
// 	gui.value = items.indexOf(value);
// 	gui.globalAction = {|obj|obj.value.postln};
//
// 	button.action_({
// 	gui.valueAction_(items.indexOf(instance.defaultParameters.at(key)));
// 	});
// 	};
// 	}
// 	}
// 	};
//
// 	//return the view
// 	^v;
// 	}*/
//
// 	showProperties { |elementName,channel|
// 		var v = descriptionView;
// 		var w = descriptionView.bounds.width;
// 		var h = descriptionView.bounds.height;
//
// 		var buttonList = List.new;
//
// 		var instance = RTML.elements.at(elementName);
// 		var textHeight = 25;
//
// 		// elementName.postln;
//
// 		// clear previous description
// 		v.removeAll;
// 		v.addFlowLayout(0@0,0@0);
//
// 		// fill descriptionView
// 		StaticText(v,Rect(0,0,w/4,textHeight)).string_("instance name");
// 		StaticText(v,Rect(0,0,w/2,textHeight)).string_(elementName);
// 		v.decorator.nextLine;
//
// 		StaticText(v,Rect(0,0,w/4,textHeight)).string_("channel");
// 		StaticText(v,Rect(0,0,w/2,textHeight)).string_(channel);
// 		v.decorator.nextLine;
//
// 		StaticText(v,Rect(0,0,w/4,textHeight)).string_("oscMessageName");
// 		StaticText(v,Rect(0,0,w/2,textHeight)).string_(instance.oscMsgName);
// 		v.decorator.nextLine;
// 		/*		Button(v,Rect(0,0,w/4,textHeight)).states_([["test"]]).action_({
// 		instance.sendMsg;
// 		});*/
// 		StaticText(v,Rect(0,0,w/4,textHeight)).string_("oscSend");
// 		PopUpMenu(v,Rect(0,0,w/4,textHeight)).items_(["off","on"]).value_(instance.send.asInt).action_({ |v|
// 			instance.send_(v.value.asBoolean);
// 		});
// 		Button(v,Rect(0,0,w/4,textHeight)).states_([["test"]]).action_({
// 			instance.sendMsg;
// 		});
// 		v.decorator.nextLine;
//
// 		StaticText(v,Rect(0,0,w/4,textHeight)).string_("oscSendType");
// 		StaticText(v,Rect(0,0,w/4,textHeight)).string_(instance.class.sendType);
// 		v.decorator.nextLine;
//
// 		StaticText(v,Rect(0,0,w/4,textHeight)).string_("oscSendMode");
// 		switch(instance.class.sendType)
// 		{\trigger} {
// 			var p, s;
// 			// trigger mode
// 			p = PopUpMenu(v,Rect(0,0,w/4,textHeight));
// 			p.items_([\oneshot,\onoff,\button]);
// 			p.value_(p.items.indexOf(instance.sendMode));
// 			p.action_({ |v|
// 				instance.sendMode_(v.item);
// 			});
// 			// delta value
// 			s = EZSlider(v,Rect(0,0,w/2,textHeight),"delta",layout:\horz,labelWidth:w/10,numberWidth:w/10);
// 			s.value_(instance.delta);
// 			s.action_({ |s|
// 				instance.delta_(s.value);
// 			});
// 		}
// 		{\continuous} {
// 			var p,q, triggers,s;
// 			// trigger mode
// 			p = PopUpMenu(v,Rect(0,0,w/4,textHeight));
// 			p.items_([\continuous,\trigger]);
// 			p.value_(p.items.indexOf(instance.sendMode));
// 			p.action_({ |v|
// 				instance.sendMode_(v.item);
// 			});
//
//
// 			// instance register
// 			q = PopUpMenu(v,Rect(0,0,w/4,textHeight));
// 			// get trigger instances
// 			triggers = List.new;
// 			RTML.elements.values.do({ |instance|
// 				if (instance.class.sendType == \trigger) {
// 					triggers.add(instance.name);
// 				}
// 			});
// 			q.items_( ["nil"]++triggers.asArray);
// 			if (instance.registerTrigger.isNil) {
// 				q.value_(0);
// 			} {
// 				q.value_(q.items.indexOf(instance.registerTrigger));
// 			};
// 			// set action
// 			q.action_({ |v|
// 				instance.registerTrigger = v.item;
// 			});
// 			v.decorator.nextLine;
// 			StaticText(v,Rect(0,0,w/4,textHeight)).string_("");
//
// 			// delta value
// 			s = EZSlider(v,Rect(0,0,w/2,textHeight),"delta",layout:\horz,labelWidth:w/10,numberWidth:w/10);
// 			s.controlSpec = ControlSpec(0,5,\lin,0.1);
// 			s.value_(instance.delta);
// 			s.action_({ |s|
// 				instance.delta_(s.value);
// 			});
// 		};
// 		v.decorator.nextLine;
// 		/*		// separator
// 		StaticText(v,Rect(0,0,w,textHeight*2)).string_("");*/
//
// 		// all default button
// 		Button(v,Rect(0,0,w/8,textHeight)).states_([["default"]]).action_({
// 			buttonList.do(_.doAction)
// 		});
//
//
// 		// place widgets according to RTMLparameter definitions
// 		instance.parameters.keysValuesDo{ |key,value|
// 			var gui;
// 			var button;
//
// 			if ( key != \channel ) {
// 				if ( key != \buffer ) {
//
// 					// default value button
// 					v.decorator.nextLine;
// 					button = Button(v,Rect(0,0,w/16,textHeight));
// 					buttonList.add( button );
//
// 					// search in RTMLparameter the gui definition and attributes
// 					switch(RTMLparameter.get(key,\guiType))
//
// 					{Slider} {
// 						var minVal = RTMLparameter.get(key,\minVal);
// 						var maxVal = RTMLparameter.get(key,\maxVal);
// 						var warp = RTMLparameter.get(key,\warp);
// 						var valueType = RTMLparameter.get(key,\valueType);
// 						var step = RTMLparameter.get(key,\step);
// 						// however the value we got it from instance.parameters value
// 						var spec = ControlSpec(minVal,maxVal,warp,step);
//
// 						gui = EZSlider(v,Rect(0,0,w*15/16,textHeight),key,labelWidth:w/4,numberWidth:w/4);
// 						gui.controlSpec = spec;
// 						gui.value = value;
//
// 						gui.round_(0.000001); //?
//
// 						gui.action = { |obj|
// 							var value = obj.value.postln;
// 							instance.set(key,value);
// 						};
//
// 						button.action_({
// 							gui.valueAction_(instance.defaultParameters.at(key));
// 						});
// 					}
//
// 					{PopUpMenu} {
// 						var items = RTMLparameter.get(key,\valueList);
// 						gui = EZPopUpMenu(v,Rect(0,0,w*15/16,textHeight),key,labelWidth:w/4);
// 						gui.items = items;
// 						gui.value = items.indexOf(value);
// 						gui.globalAction = {|obj|
// 							var value = obj.value.postln;
// 							instance.set(key,items.indexOf(value));
// 						};
//
// 						button.action_({
// 							gui.valueAction_(items.indexOf(instance.defaultParameters.at(key)));
// 						});
// 					};
// 				}
// 			}
// 		};
//
// 		// separator
// 		// StaticText(v,Rect(0,0,w,textHeight*2)).string_("");
// 		// feedbackView = View(v,Rect(0,0,w,200)).background_(Color.red);
//
// 		// make new feedbackView
// 		feedbackView = View(drawView,Rect(drawView.bounds.width*0.66,drawView.bounds.height/8+(canvasView.bounds.height*0.85), drawView.bounds.width*0.34,canvasView.bounds.height*0.10)).background_(Color.grey(0.55));
//
// 		// visualization
// 		instance.makeGui(feedbackView);
// 	}
//
// 	reset {
// 		RTML.numInputBuses.do { |bus|
//
// 			audioBusElements.put(bus,List.new);
//
// 			audioBusViews.at(bus).removeAll;
// 			// remove flowLayout
// 			audioBusViews.at(bus).decorator.left_(0);
//
// 			// instanciate again peak meter
// 			// remove old instance
// 			audioBusMeters.at(bus).remove;
// 			// create new inside audioBusMeters, so the osc responder can actuate
// 			audioBusMeters.put(bus, LevelIndicator(audioBusViews.at(bus), Rect(0,0,meterWidth,audioBusViews.at(bus).bounds.height)).drawsPeak_(true););
//
// 			// instanciate again bus info text
// 			audioBusViews.at(bus).decorator.left_(0);
// 			StaticText(audioBusViews.at(bus),Rect(0,0,meterWidth,20)).string_("In "++bus.asString);
//
// 			// remove properties view
// 			this.clearProperties;
//
//
// 		};
// 	}
// }
