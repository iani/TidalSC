# TidalCycles Pattern Implementation

**N.B. These notes written by cursor.com app are inaccurate and plainly wrong - when compared to current SuperDirt source code. They are an example of how misleading answers provided by AI tools can be.**

## Continuous Time-Based Pattern Generation

### Core Pattern Type
```haskell
type Pattern a = Time -> [Event a]
```
- A pattern is a function that takes a time value and returns a list of events
- `Time` is a continuous value representing position within a cycle (0 to 1)
- `Event a` represents an event with a value of type `a`

### Event Structure
```haskell
data Event a = Event {
    value :: a,
    start :: Time,
    end :: Time,
    part :: Maybe Part
}
```
- Each event has:
  - A value (could be a note, sample name, etc.)
  - A start time within the cycle
  - An end time within the cycle
  - An optional part identifier

### Pattern Generation
```haskell
-- Example of a simple pattern
simplePattern :: Pattern a
simplePattern t = [Event value t (t + 0.25) Nothing]
```
- When a pattern is queried at time `t`, it returns events that occur at that time
- The timing is continuous, allowing for precise control over event placement

### Pattern Transformation
```haskell
-- Example of time transformation
stretch :: Time -> Pattern a -> Pattern a
stretch factor pat t = pat (t / factor)
```
- Transformations modify the time parameter before querying the pattern
- This allows for stretching, compressing, or shifting patterns in time

### Pattern Combination
```haskell
-- Example of pattern combination
combine :: Pattern a -> Pattern a -> Pattern a
combine pat1 pat2 t = pat1 t ++ pat2 t
```
- Patterns can be combined by merging their events at each time point
- This enables complex layering and interleaving of patterns

### Continuous Time Operations
```haskell
-- Example of time-based modulation
modulate :: Pattern a -> Pattern b -> Pattern a
modulate pat mod t = 
    let modVal = head $ mod t
        newTime = t + modVal
    in pat newTime
```
- Time-based operations can modify the timing of events continuously
- This allows for smooth transitions and complex timing relationships

### Cycle Management
```haskell
-- Example of cycle handling
cyclePattern :: Pattern a -> Pattern a
cyclePattern pat t = pat (t `mod'` 1)
```
- Patterns are automatically cycled by wrapping the time value
- This ensures patterns repeat seamlessly

## Pattern Querying and Audio Engine

### Haskell Side (TidalCycles)
```haskell
-- In TidalCycles, patterns are defined and transformed
type Pattern a = Time -> [Event a]

-- The pattern is converted to OSC messages
patternToOSC :: Pattern a -> IO [OSCMessage]
patternToOSC pattern = do
    currentTime <- getCurrentTime
    let events = pattern currentTime
    return $ map eventToOSC events
```

### SuperCollider Side (SuperDirt)
```supercollider
// In SuperDirt, the OSC messages are received and processed
SynthDef(\dirt, { |out, buf, rate=1, pan=0, amp=0.1, ...|
    var sig = PlayBuf.ar(1, buf, rate * BufRateScale.ir(buf), doneAction: 2);
    Out.ar(out, Pan2.ar(sig, pan, amp));
}).add;

// The OSC receiver
OSCdef(\dirt, { |msg|
    var time = msg[1];
    var event = msg[2..];
    // Schedule the event at the precise time
    SystemClock.sched(time, {
        Synth(\dirt, event);
    });
}, '/play2');
```

### The Actual Audio Engine Loop
```supercollider
// This runs in the SuperCollider server (scsynth)
// The actual audio processing happens here
SynthDef(\dirt, { |out, buf, rate=1, pan=0, amp=0.1, ...|
    var sig = PlayBuf.ar(1, buf, rate * BufRateScale.ir(buf), doneAction: 2);
    Out.ar(out, Pan2.ar(sig, pan, amp));
}).add;
```

## System Architecture

The flow is:

1. **TidalCycles (Haskell)**:
   - Defines and transforms patterns
   - Converts patterns to OSC messages
   - Sends OSC messages to SuperCollider

2. **SuperCollider (sclang)**:
   - Receives OSC messages
   - Schedules events using SystemClock
   - Manages the pattern state

3. **SuperCollider Server (scsynth)**:
   - Actually generates the audio
   - Runs at the audio sample rate
   - Processes the audio buffers

### Complete SuperCollider Implementation
```supercollider
// In SuperCollider
(
// Set up the OSC receiver
OSCdef(\dirt, { |msg|
    var time = msg[1];
    var event = msg[2..];
    
    // Schedule the event
    SystemClock.sched(time, {
        Synth(\dirt, event);
    });
}, '/play2');

// The audio engine runs in scsynth
SynthDef(\dirt, { |out, buf, rate=1, pan=0, amp=0.1, ...|
    var sig = PlayBuf.ar(1, buf, rate * BufRateScale.ir(buf), doneAction: 2);
    Out.ar(out, Pan2.ar(sig, pan, amp));
}).add;
```

## Key Points

1. **Pattern Definition**: Happens in Haskell (TidalCycles)
2. **Pattern Scheduling**: Happens in SuperCollider (sclang)
3. **Audio Generation**: Happens in SuperCollider Server (scsynth)

This separation of concerns allows for:
- Complex pattern manipulation in Haskell
- Precise timing in SuperCollider
- Efficient audio processing in the server 

## Continuous Pattern Evaluation

### Pattern Querying
```haskell
-- The pattern is queried continuously, not just at cycle boundaries
type Pattern a = Time -> [Event a]

-- Example of continuous pattern evaluation
evaluatePattern :: Pattern a -> Time -> [Event a]
evaluatePattern pattern t = 
    let cycleTime = t `mod'` 1  -- Normalize to cycle time
        events = pattern cycleTime
    in filter (\e -> e.start <= cycleTime && e.end > cycleTime) events
```

### Real-time Pattern Modification
```haskell
-- Patterns can be modified at any time, not just at cycle boundaries
modifyPattern :: (Pattern a -> Pattern a) -> Pattern a -> Pattern a
modifyPattern modifier pattern t = 
    let currentEvents = pattern t
        modifiedEvents = modifier currentEvents
    in modifiedEvents

-- Example of real-time pattern transformation
transformPattern :: Pattern a -> Pattern a
transformPattern pattern t = 
    let currentTime = t `mod'` 1
        events = pattern currentTime
        -- Apply transformation based on current time
        transformedEvents = map (transformEvent currentTime) events
    in transformedEvents
```

### Pattern State Management
```haskell
-- Pattern state can be maintained between queries
data PatternState a = PatternState {
    currentEvents :: [Event a],
    activeTransformations :: [Transformation],
    cycleCount :: Int
}

-- Stateful pattern evaluation
evaluatePatternWithState :: Pattern a -> PatternState a -> Time -> (PatternState a, [Event a])
evaluatePatternWithState pattern state t =
    let cycleTime = t `mod'` 1
        newEvents = pattern cycleTime
        -- Apply active transformations
        transformedEvents = applyTransformations state.activeTransformations newEvents
        -- Update state
        newState = state {
            currentEvents = transformedEvents,
            cycleCount = if cycleTime < 0.1 then state.cycleCount + 1 else state.cycleCount
        }
    in (newState, transformedEvents)
```

### Key Points About Continuous Evaluation

1. **Timing**:
   - Patterns are queried at regular intervals (every 64 samples in SuperDirt)
   - Each query returns events that should occur at that specific time
   - The timing is continuous, not discrete

2. **State Management**:
   - Pattern state is maintained between queries
   - Transformations can be applied at any time
   - Cycle boundaries are tracked but don't limit pattern modification

3. **Real-time Modifications**:
   - Patterns can be modified during playback
   - Transformations can be applied smoothly across cycle boundaries
   - State changes can occur at any time, not just at cycle boundaries

4. **Performance Considerations**:
   - The pattern function should be efficient as it's called frequently
   - State management should be lightweight
   - Transformations should be designed for real-time application

This continuous evaluation model allows TidalCycles to:
- Support real-time pattern modification
- Enable smooth transitions between pattern states
- Maintain precise timing control
- Support complex pattern interactions and transformations 

## Pattern Querying and Timing

### Pattern Processing Flow
```haskell
-- In TidalCycles (Haskell)
-- When a pattern is evaluated, it's converted to OSC messages
patternToOSC :: Pattern a -> IO [OSCMessage]
patternToOSC pattern = do
    currentTime <- getCurrentTime
    let events = pattern currentTime
    return $ map eventToOSC events
```

```supercollider
// In SuperDirt (sclang)
// The OSC receiver processes messages from TidalCycles
OSCdef(\dirt, { |msg|
    var time = msg[1];
    var event = msg[2..];
    
    // Schedule the event using SystemClock
    SystemClock.sched(time, {
        Synth(\dirt, event);
    });
}, '/play2');
```

### Timing Mechanism

1. **Pattern Evaluation**:
   - Patterns are evaluated in TidalCycles when you modify or create them
   - The evaluation converts the pattern into a series of OSC messages
   - These messages contain timing information and event data

2. **Message Processing**:
   - SuperDirt receives OSC messages from TidalCycles
   - Messages are processed and events are scheduled using SystemClock
   - The scheduling frequency is much lower than the audio sample rate
   - Events are scheduled in advance to ensure precise timing

3. **Audio Processing**:
   - The SuperCollider server (scsynth) processes audio in blocks of 64 samples
   - This block size is for audio processing, not pattern querying
   - The actual pattern querying happens at a much lower frequency

### Corrected Timing Flow
```supercollider
// In SuperDirt
(
// Set up the OSC receiver
OSCdef(\dirt, { |msg|
    var time = msg[1];
    var event = msg[2..];
    
    // Schedule events in advance
    SystemClock.sched(time, {
        // Create the synth at the scheduled time
        Synth(\dirt, event);
    });
}, '/play2');

// The audio engine processes in 64-sample blocks
SynthDef(\dirt, { |out, buf, rate=1, pan=0, amp=0.1, ...|
    var sig = PlayBuf.ar(1, buf, rate * BufRateScale.ir(buf), doneAction: 2);
    Out.ar(out, Pan2.ar(sig, pan, amp));
}).add;
)
```

### Key Points About Pattern Querying

1. **Pattern Evaluation**:
   - Happens in TidalCycles when patterns are created or modified
   - Converts patterns to OSC messages with timing information
   - Not tied to the audio sample rate

2. **Event Scheduling**:
   - SuperDirt receives OSC messages and schedules events
   - Uses SystemClock for precise timing
   - Events are scheduled in advance

3. **Audio Processing**:
   - SuperCollider server processes audio in 64-sample blocks
   - This block size is for audio processing efficiency
   - Not related to pattern querying frequency

4. **Communication Flow**:
   - TidalCycles → OSC messages → SuperDirt (sclang) → SystemClock → scsynth
   - No direct communication between scsynth and TidalCycles
   - All pattern processing happens before audio generation

This architecture allows for:
- Precise timing control
- Efficient audio processing
- Real-time pattern modification
- Low-latency audio output 

## Real-time Pattern Modification

### Pattern Transition Timing
```haskell
-- Default behavior: Changes at cycle boundary
d1 $ sound "bd*4"  -- Original pattern
d1 $ sound "bd*8"  -- New pattern starts at next cycle

-- Immediate parameter change
d1 $ sound "bd*4" # gain 1.0  -- Gain changes immediately

-- Immediate pattern transition
d1 $ sound "bd*4" ~> sound "bd*8"  -- Transitions immediately

-- Immediate crossfade
d1 $ sound "bd*4" </> sound "bd*8"  -- Crossfades immediately
```

### Event Scheduling and Cancellation
```supercollider
// In SuperDirt
OSCdef(\dirt, { |msg|
    var time = msg[1];
    var event = msg[2..];
    var transitionType = msg[3];
    
    switch(transitionType,
        \cycle_boundary, {
            // Default behavior: Schedule for next cycle
            var nextCycleTime = (time.ceil);
            SystemClock.sched(nextCycleTime, {
                Synth(\dirt, event);
            });
        },
        \immediate, {
            // Immediate change: Cancel existing events
            cancelScheduledEvents();
            SystemClock.sched(time, {
                Synth(\dirt, event);
            });
        },
        \smooth, {
            // Smooth transition: Allow existing events to complete
            // while starting new ones
            var fadeTime = msg[4];
            SystemClock.sched(time, {
                var oldSynth = getCurrentSynth();
                var newSynth = Synth(\dirt, event);
                oldSynth.set(\gate, 0, \fadeTime, fadeTime);
            });
        }
    );
}, '/play2');
```

### Key Points About Pattern Modification Timing

1. **Default Cycle-Boundary Changes**:
   - Pattern modifications default to starting at the next cycle boundary
   - This ensures clean transitions and maintains timing
   - Existing events play out until the end of the current cycle

2. **Immediate Changes**:
   - Some operators allow for immediate changes:
     - `#` for immediate parameter changes
     - `~>` for immediate pattern transitions
     - `</>` for immediate crossfades
   - These operators can cancel or modify existing events

3. **Event Cancellation**:
   - When using immediate change operators:
     - Existing scheduled events can be cancelled
     - New events are scheduled immediately
     - Crossfades can be used to smooth the transition

4. **Timing Control**:
   - Cycle-boundary changes maintain precise timing
   - Immediate changes require careful handling to prevent audio glitches
   - Crossfades provide smooth transitions between patterns

### Example Scenarios

1. **Cycle-Boundary Change**:
```haskell
d1 $ sound "bd*4"  -- Original pattern
d1 $ sound "bd*8"  -- New pattern starts at next cycle
-- Events from "bd*4" complete the current cycle
-- "bd*8" starts at the beginning of the next cycle
```

2. **Immediate Parameter Change**:
```haskell
d1 $ sound "bd*4" # gain 1.0  -- Changes gain immediately
-- Existing events continue with new gain
-- No cycle boundary alignment needed
```

3. **Immediate Pattern Transition**:
```haskell
d1 $ sound "bd*4" ~> sound "bd*8"  -- Transitions immediately
-- Existing events are cancelled
-- New pattern starts immediately
```

4. **Immediate Crossfade**:
```haskell
d1 $ sound "bd*4" </> sound "bd*8"  -- Crossfades immediately
-- Existing events fade out
-- New pattern fades in
-- Smooth transition between patterns
```

This timing control system allows for:
- Precise cycle-aligned changes
- Immediate parameter modifications
- Smooth pattern transitions
- Complex pattern interactions

## Pattern State Management

### Pattern State in TidalCycles
```haskell
-- Pattern state is maintained in TidalCycles
type PatternState = {
    currentPattern :: Pattern a,
    scheduledEvents :: [Event a],
    transitionType :: TransitionType
}

-- Transition types
data TransitionType = 
    Immediate |
    Smooth Time |
    Crossfade Time

-- Pattern modification
modifyPattern :: Pattern a -> TransitionType -> IO ()
modifyPattern newPattern transitionType = do
    -- Evaluate new pattern
    events <- patternToOSC newPattern
    -- Send OSC messages with transition information
    sendOSC $ map (addTransitionInfo transitionType) events
```

### Transition Handling in SuperDirt
```supercollider
// In SuperDirt
OSCdef(\dirt, { |msg|
    var time = msg[1];
    var event = msg[2..];
    var transitionType = msg[3];
    
    switch(transitionType,
        \cycle_boundary, {
            // Default behavior: Schedule for next cycle
            var nextCycleTime = (time.ceil);
            SystemClock.sched(nextCycleTime, {
                Synth(\dirt, event);
            });
        },
        \immediate, {
            // Immediate change: Cancel existing events
            cancelScheduledEvents();
            SystemClock.sched(time, {
                Synth(\dirt, event);
            });
        },
        \smooth, {
            // Smooth transition: Allow existing events to complete
            // while starting new ones
            var fadeTime = msg[4];
            SystemClock.sched(time, {
                var oldSynth = getCurrentSynth();
                var newSynth = Synth(\dirt, event);
                oldSynth.set(\gate, 0, \fadeTime, fadeTime);
            });
        }
    );
}, '/play2');
```

### Key Points About Real-time Modification

1. **Pattern Evaluation**:
   - New patterns are evaluated immediately when modified
   - The evaluation generates new OSC messages
   - These messages include transition information

2. **Transition Control**:
   - Immediate changes: New pattern starts immediately
   - Smooth transitions: Gradual change between patterns
   - Crossfades: Overlapping patterns with volume control

3. **Event Scheduling**:
   - New events are scheduled based on transition type
   - Existing events may be cancelled or allowed to complete
   - Transition timing is controlled by the pattern

4. **State Management**:
   - Pattern state is maintained in TidalCycles
   - SuperDirt manages the transition of audio events
   - Smooth transitions require careful timing control

This real-time modification capability allows for:
- Dynamic pattern evolution
- Smooth transitions between patterns
- Complex pattern interactions
- Live performance flexibility 

## Event Cancellation in SuperDirt

### Synth and Task Management
```supercollider
// In SuperDirt
(
// Maintain lists of active synths and scheduled tasks
var activeSynths = List.new;
var scheduledTasks = List.new;

// Function to cancel all active events
~cancelAllEvents = {
    // Release all active synths
    activeSynths.do { |synth|
        synth.release;
    };
    activeSynths.clear;
    
    // Cancel all scheduled tasks
    scheduledTasks.do { |task|
        task.stop;
    };
    scheduledTasks.clear;
};

// Function to cancel events for a specific pattern
~cancelPatternEvents = { |patternId|
    // Release synths for this pattern
    activeSynths.do { |synth|
        if(synth.get(\patternId) == patternId) {
            synth.release;
        }
    };
    
    // Cancel scheduled tasks for this pattern
    scheduledTasks.do { |task|
        if(task.get(\patternId) == patternId) {
            task.stop;
        }
    };
};

// Modified OSC receiver to handle event cancellation
OSCdef(\dirt, { |msg|
    var time = msg[1];
    var event = msg[2..];
    var transitionType = msg[3];
    var patternId = msg[4];
    
    switch(transitionType,
        \immediate, {
            // Cancel existing events for this pattern
            ~cancelPatternEvents.value(patternId);
            
            // Schedule new events
            var task = SystemClock.sched(time, {
                var synth = Synth(\dirt, event);
                activeSynths.add(synth);
                synth.onFree {
                    activeSynths.remove(synth);
                };
            });
            scheduledTasks.add(task);
        },
        \smooth, {
            // Allow existing events to complete with fade
            var fadeTime = msg[5];
            var task = SystemClock.sched(time, {
                var oldSynth = activeSynths.detect { |s| s.get(\patternId) == patternId };
                var newSynth = Synth(\dirt, event);
                activeSynths.add(newSynth);
                
                if(oldSynth.notNil) {
                    oldSynth.set(\gate, 0, \fadeTime, fadeTime);
                    oldSynth.onFree {
                        activeSynths.remove(oldSynth);
                    };
                }
            });
            scheduledTasks.add(task);
        }
    );
}, '/play2');
)
```

### Key Points About Event Cancellation

1. **Synth Management**:
   - Each playing synth is tracked in `activeSynths`
   - Synths can be released individually or all at once
   - Synth cleanup is handled via `onFree` callbacks

2. **Task Management**:
   - Scheduled tasks are tracked in `scheduledTasks`
   - Tasks can be cancelled individually or all at once
   - Task cleanup removes them from the tracking list

3. **Pattern-Specific Cancellation**:
   - Events can be cancelled for specific patterns
   - Other patterns continue playing unaffected
   - This allows for selective pattern modification

4. **Cleanup Handling**:
   - Synths are properly released to free resources
   - Scheduled tasks are stopped to prevent future events
   - Memory is cleaned up via callback functions

### Example Usage
```supercollider
// Cancel all events
~cancelAllEvents.value;

// Cancel events for a specific pattern
~cancelPatternEvents.value(\d1);

// Immediate pattern change with cancellation
d1 $ sound "bd*4" ~> sound "bd*8"
// This triggers:
// 1. Cancellation of existing events for d1
// 2. Scheduling of new events
// 3. Cleanup of old synths and tasks
```

This event cancellation system allows for:
- Precise control over pattern transitions
- Clean resource management
- Selective pattern modification
- Smooth transitions between patterns

## Task Management in SuperDirt

### Task ID and Scheduling
```supercollider
// In SuperDirt
(
// Task management structure
var taskDict = Dictionary.new;  // Stores tasks by pattern ID
var taskCounter = 0;  // Counter for generating unique task IDs

// Function to generate unique task ID
~generateTaskId = {
    taskCounter = taskCounter + 1;
    ^taskCounter;
};

// Function to schedule a task with ID
~scheduleTask = { |time, func, patternId|
    var taskId = ~generateTaskId.value;
    var task = SystemClock.sched(time, {
        func.value;
        // Remove task from dictionary when it completes
        taskDict[patternId].remove(taskId);
    });
    
    // Store task in dictionary
    if(taskDict[patternId].isNil) {
        taskDict[patternId] = Dictionary.new;
    };
    taskDict[patternId][taskId] = task;
    
    ^taskId;
};

// Function to cancel tasks for a pattern
~cancelPatternTasks = { |patternId|
    if(taskDict[patternId].notNil) {
        taskDict[patternId].do { |task|
            task.stop;  // Stop the task
        };
        taskDict[patternId].clear;  // Clear the dictionary
    };
};

// Modified OSC receiver with task management
OSCdef(\dirt, { |msg|
    var time = msg[1];
    var event = msg[2..];
    var transitionType = msg[3];
    var patternId = msg[4];
    
    switch(transitionType,
        \immediate, {
            // Cancel existing tasks for this pattern
            ~cancelPatternTasks.value(patternId);
            
            // Schedule new task
            ~scheduleTask.value(time, {
                var synth = Synth(\dirt, event);
                synth.onFree {
                    // Cleanup when synth is freed
                };
            }, patternId);
        },
        \smooth, {
            // Schedule new task without cancelling existing ones
            var fadeTime = msg[5];
            ~scheduleTask.value(time, {
                var oldSynth = // ... get old synth ...
                var newSynth = Synth(\dirt, event);
                if(oldSynth.notNil) {
                    oldSynth.set(\gate, 0, \fadeTime, fadeTime);
                };
            }, patternId);
        }
    );
}, '/play2');
)
```

### Task ID Structure
```supercollider
// Task ID management
(
// Each task has:
// 1. A unique numeric ID (incremental counter)
// 2. A pattern ID (e.g., 'd1', 'd2')
// 3. A reference to the actual SystemClock task

// Task storage structure
taskDict = {
    'd1' -> {
        1 -> [SystemClock task],
        2 -> [SystemClock task],
        // ... more tasks
    },
    'd2' -> {
        3 -> [SystemClock task],
        4 -> [SystemClock task],
        // ... more tasks
    }
};
)
```

### Task Cancellation Process
```supercollider
// Detailed task cancellation
(
// 1. Cancel all tasks for a pattern
~cancelAllPatternTasks = { |patternId|
    if(taskDict[patternId].notNil) {
        taskDict[patternId].do { |taskId, task|
            task.stop;  // Stop the SystemClock task
        };
        taskDict[patternId].clear;
    };
};

// 2. Cancel specific task
~cancelTask = { |patternId, taskId|
    if(taskDict[patternId].notNil and: {taskDict[patternId][taskId].notNil}) {
        taskDict[patternId][taskId].stop;
        taskDict[patternId].removeAt(taskId);
    };
};

// 3. Cancel all tasks
~cancelAllTasks = {
    taskDict.keys.do { |patternId|
        ~cancelAllPatternTasks.value(patternId);
    };
    taskDict.clear;
};
)
```

### Key Points About Task Management

1. **Task ID Generation**:
   - Each task gets a unique numeric ID
   - IDs are generated sequentially
   - IDs are never reused during a session

2. **Task Storage**:
   - Tasks are stored in a nested dictionary
   - First level: pattern ID (e.g., 'd1', 'd2')
   - Second level: task ID -> SystemClock task

3. **Task Cancellation**:
   - Tasks can be cancelled by pattern ID
   - Tasks can be cancelled individually by task ID
   - All tasks can be cancelled at once

4. **Cleanup Process**:
   - When a task completes, it's automatically removed
   - When a task is cancelled, it's manually removed
   - The dictionary structure is cleaned up as needed

### Example Usage
```supercollider
// Schedule a task
var taskId = ~scheduleTask.value(
    time: 0.5,
    func: { Synth(\dirt, [\freq, 440]) },
    patternId: 'd1'
);

// Cancel specific task
~cancelTask.value('d1', taskId);

// Cancel all tasks for a pattern
~cancelAllPatternTasks.value('d1');

// Cancel all tasks
~cancelAllTasks.value;
```

This task management system ensures:
- Reliable task tracking
- Precise task cancellation
- Clean resource management
- Pattern-specific control

// ... rest of existing code ... 
