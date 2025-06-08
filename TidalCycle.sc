// 土  7  6 2025 21:44
// See README.org

TidalCycle {
	var <pattern;
	var <>cps = 1;
	var <events;
	var <startTime;
	var <>isPlaying = false;
	var <>playFunc;
	var <cycleTask; // repeat playing at every cycle
	var <eventTask; // play all events
	var <cycleCount = 0;

	*initClass {
		StartUp add: {
			ServerBoot add: {
				this.loadSynthDefs;
				// this.activatePreprocessor;
			}
		}
	}

	*loadSynthDefs {
		SynthDef(\ping, { | freq = 440, dur = 0.1, amp = 0.1 |
			var env, src;
			env = Env.perc(0.02, dur - 0.02 max: 0.01);
			src = SinOsc.ar(freq, 0, amp);
			Out.ar(0, src * env.kr(1, doneAction: 2))
		}).add;
	}

	*activateParser { // incomplete
		thisProcess.interpreter.preProcessor = { | code |
			this.checkCode(code);
		}
	}

	*deactivateParser { // incomplete
		thisProcess.interpreter.preProcessor = { | c | c;}
	}

	*checkCode { | code | // check for tidal header
		// if tidal header present, then parse code as tidal
		var header;
		header = code[..7];
		if (header == "//:tidal") { // incomplete
			// TidalParser.parse(code, this);
			// ^"";
			"I should interpret this as tidal code".postln;
		};
		^code;
	}


	*new { | pattern = #[1], cps = 1 |
		^this.newCopyArgs(pattern, cps);
	}

	dur { ^cps.reciprocal }

	play {
		if (isPlaying) { ^postln("Cannot restart playing cycle") };
		isPlaying = true;
		cycleTask.stop;
		cycleTask = fork {
			inf do: { | i |
				cycleCount = i;
				this.playCycle;
				this.dur.wait;
			}
		};
	}

	playCycle {
		startTime = Clock.seconds;
		this.calcEvents;
		this.playEvents;
	}

	calcEvents {
		var eventDur, onsets;
		eventDur = this.dur / pattern.size;
		onsets = pattern.collect({ | p, i |
			i * eventDur;
		}) + startTime;
		events = pattern collect: { | p, i |
			PitchEvent(onsets[i], eventDur, (p + 60).midicps);
		}
	}

	playEvents {
		var index, now, nextEvent;
		eventTask.stop; // stop previous task if running
		now = Clock.seconds;
		nextEvent = events detect: { | e | e.onset >= now; };
		// if no event left in cycle, wait till next cycle.
		nextEvent ?? { ^this };
		index = events indexOf: nextEvent;
		eventTask = fork {
			wait(events[index].onset - now);
			events[index..] do: { | e |
				e.play;
				e.dur.wait;
			};
		};
	}

	pattern_ { | argPattern |
		pattern = argPattern;
		this.calcEvents;
		if (isPlaying) { this.playEvents };
	}

}

TidalEvent {
	var <>onset; // absolute onset time in System Clock time
	var <>dur; // duration from beginning to next event
}

PitchEvent : TidalEvent {
	var freq = 440;
	var <synth;

	*new { | onset, dur = 1, freq = 440 |
		^this.newCopyArgs(onset, dur, freq);
	}

	play {
		synth = Synth(\ping, [freq: freq, dur: dur]);
	}
}