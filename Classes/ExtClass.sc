// from Marije Baalman
// renamed to not collide with FileLog quark

+ Class {

	isKindOfClass2{ |otherClass|
		if ( this.asClass == otherClass ){ ^true };
		this.superclasses.do{ |it| if ( it == otherClass ){ ^true } };
		^false;
	}

}
