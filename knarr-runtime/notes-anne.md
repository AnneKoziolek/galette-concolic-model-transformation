Sorry, I was wrong. I checked the old version of GalettePathConstraitBridge and instead of isSymbolic, it just checked for  if (intVal >= -10 && intVal <= 10)  If true, it created a constant and if false a variable. So it just assumed that variables are between -10 and 10 and everything else is constants... 

So let us revisit your earlier plan. How can we detect that a value is tagged? Currently we do 

        try {
            Class<?> tainterClass = Class.forName("edu.neu.ccs.prl.galette.internal.runtime.Tainter");
            Method getTagMethod = tainterClass.getMethod("getTag", Object.class);
            Object tag = getTagMethod.invoke(null, value);
            isSymbolic = (tag != null);
        } catch (Exception e) {
            // Cannot check tags, fall back to heuristic
            // Assume small integers are constants, large values might be symbolic
        }

I checked another branch. There, we were able to get the tag by calling Tainter.getTag. But we have to do this in an instrumented environment so that the TagFrame is passed as well. The normal getTag(value) returns null, only getTag(value, TagFrame) returns a tag. 

We cannot access this from Galette classes as these are filtered out (do they have to?). But we can probably from ModelTransformationExample


TODO from Claude:

Summary: Where GaletteSymbolicator is Called From
GaletteSymbolicator is called from ModelTransformationExample in 3 places:
1. During Setup (executeConcolic() method, lines 448-452):
GaletteSymbolicator.reset();  // Clear previous state
Tag symbolicTag = GaletteSymbolicator.makeSymbolicDouble(label, thickness);  // Create symbolic value
This creates the symbolic value with a label like "thickness_1" for the input.
2. During Analysis (analyzeBoundaryConditions() method, line 686):
GaletteSymbolicator.InputSolution solution = GaletteSymbolicator.solvePathCondition();
This tries to solve the collected path constraints to find alternative inputs, but it's called at the end after all exploration iterations.
3. NOT Called During Main Exploration Loop
The main exploration loop in performConcolicAnalysis() does NOT use GaletteSymbolicator.solvePathCondition(). Instead, it uses its own generateAlternativeInput() method (lines 493-568) which:
Extracts thresholds from constraints manually
Uses heuristics to generate new inputs
Doesn't rely on the Green solver
The Key Issue:
The exploration logic is split and not properly integrated:
ModelTransformationExample has its own exploration logic that doesn't use the solver effectively
GaletteSymbolicator.solvePathCondition() is only called at the end for analysis, not during exploration
Even when called, it can't find variables because constraints contain concrete values (12.0) not variable names (thickness_1)
This explains why exploration doesn't work properly - the constraint solver that should drive exploration isn't being used in the main loop, and even when it is used, it can't identify variables to mutate!