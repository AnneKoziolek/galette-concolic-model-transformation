Filtering Path Constraints to Only Tagged Symbolic Variables
Background: Galette Tagging and Tainter.getTag Behavior
(see https://chatgpt.com/s/dr_688869be59c48191be8641e315e86932 for formatted version)

Galette uses taint tags (Tag objects) to mark and propagate symbolic (tainted) data, analogous to Phosphor’s Taint mechanism
raw.githubusercontent.com
. However, the Tainter.getTag() and Tainter.setTag() methods in Galette are designed as placeholders – they are meant to be transformed by Galette’s instrumentation at runtime. In the un-instrumented form, these methods do nothing useful. For example, the no-argument version of Tainter.getTag(x) simply returns null (a stub)
raw.githubusercontent.com
, and similarly Tainter.setTag(x, tag) just returns the value without actually attaching any tag
raw.githubusercontent.com
. Under full Galette instrumentation, calls to these placeholder methods are replaced with real tag operations (passing a hidden TagFrame parameter under the hood). For instance, the instrumented version of Tainter.getTag(x) will invoke Tainter.getTag(x, frame), which fetches the actual tag from the current tag frame (e.g. via frame.get(0))
raw.githubusercontent.com
. This is how Galette’s integration tests successfully retrieve tags – the instrumentation automatically supplies the correct TagFrame and the placeholder returns are swapped out.

In your case, the custom code in PathUtils.mightBeSymbolic(...) was not being instrumented, so calls like Tainter.getTag(value) remained as-is (placeholders returning null). This explains why mightBeSymbolic() “cannot detect whether a value has been tagged” – it’s always getting null back. The core issue is that PathUtils is likely not included in the set of classes Galette instruments (possibly because it’s part of the Galette agent/runtime package or loaded before instrumentation), so the transformation that normally makes Tainter.getTag work never applied
raw.githubusercontent.com
. In short, the filter method was blind to tags because it wasn’t running under the taint-tracking context.

Option 1: Instrument PathUtils (Enable Real Tag Access)

The most direct solution is to ensure that PathUtils (and your mightBeSymbolic method) runs with Galette instrumentation enabled. If you modify the Galette agent configuration to not exclude your concolic runtime classes from instrumentation, then Tainter.getTag calls inside PathUtils will be transformed at load time. This would allow mightBeSymbolic(x) to actually retrieve the tag of x from the current TagFrame and check if it’s non-null. In practice, that means adjusting the agent’s instrumentation filters or using the Galette Maven plugin to instrument your classes as well (not just the JDK). Under instrumentation, a call like:

Tag t = Tainter.getTag(someValue);

would be converted to something like:

Tag t = Tainter.getTag(someValue, currentFrame);

at the bytecode level, and Tainter.getTag(value, TagFrame) will return the tag of value (since the tag is passed via the TagFrame)
raw.githubusercontent.com
. Galette achieves this by passing a tag frame as an extra hidden argument to every method call
jonbell.net
. Instrumenting PathUtils ensures it has access to that mechanism.

Pros: This leverages Galette’s built-in tag handling directly. Once instrumented, mightBeSymbolic() would correctly indicate symbolic values (non-null tags) without further changes.

Cons: Instrumenting agent/internal classes can be tricky. You must avoid instrumenting Galette’s core in a way that causes recursion or conflicts. If PathUtils is tightly integrated with the agent, you need to be careful to not introduce an instrumentation cycle. That said, since you mentioned you’re comfortable modifying Galette, you could include your knarr-runtime package in the instrumentation whitelist. This way, during runtime, Tainter.getTag in PathUtils will function as expected (just as it does in the integration tests with full instrumentation).

Option 2: Filter by Checking Tag Contents or Symbolic Tag Set

Another approach is to avoid calling Tainter.getTag in the filter altogether, and instead determine symbolicity by examining the Tag objects that are already available in your comparison logic. In Galette, a Tag is essentially an immutable set of label objects
raw.githubusercontent.com
. When you mark a variable symbolic via GaletteSymbolicator.makeSymbolicX(...), you create a new Tag (via Tag.of(label)), typically labeled with a unique identifier (e.g., the user-provided label like "thickness" in your example)
raw.githubusercontent.com
. Galette’s Tag can hold one or more labels (it supports union of tags) and provides a method getLabels() to retrieve the underlying label objects
raw.githubusercontent.com
.

You are already tracking the labels of interest in your code. The PathUtils.usedLabels set collects all labels used to mark symbolic inputs
raw.githubusercontent.com
(via PathUtils.checkLabelAndInitJPF(label) which adds the label to the set). Therefore, to decide if a given value is “one of the variables I tagged as symbolic,” you can check whether its tag contains any of those labels. In practice, you can implement a utility like:

public static boolean isUserSymbolicTag(Tag tag) {
    if (tag == null || tag.isEmpty()) return false;
    for(Object lbl : tag.getLabels()) {
        if(PathUtils.usedLabels.contains(lbl)) {
            return true;  // tag contains a user-defined symbolic label
        }
    }
    return false;
}

Using this, you don’t need to query the tag of a value at runtime; instead you use the Tag that is already propagated with the value in question. For example, in your SymbolicComparison methods, you receive leftTag and rightTag for the operands. You can simply check those tags’ labels:

if ((leftTag != null || rightTag != null) 
     && (isUserSymbolicTag(leftTag) || isUserSymbolicTag(rightTag))) {
    // Collect path constraint since at least one operand’s tag is a user-tagged symbolic
    ...
}

This ensures you only collect constraints when the branch involves a symbolically-tagged variable (as opposed to some incidental taint or auto-generated tag that you might consider “unrelated”). In other words, you tighten the filter from “tag is non-null” to “tag originates from one of my explicit symbolic inputs.”

Pros: This approach doesn’t require the PathUtils class itself to be instrumented or to call any Galette internals at runtime. You leverage the information already carried by the Tag object. It’s similar in spirit to how Knarr/Phosphor filtered path conditions: they only added a constraint if the branch condition’s taint was associated with a symbolic input. In the original Knarr (Phosphor-based) system, a branch predicate would carry a non-null Taint only if it was influenced by a symbolic input, and then PathUtils would record the constraint
github.com
. Here, using Tag labels achieves the same – you verify the taint’s provenance. This is also aligned with the Confetti tool’s philosophy: confetti only guides fuzzing for branches guarded by tainted (input-derived) predicates
jonbell.net
. By checking the Tag’s label, you ensure the path constraint is indeed related to an input you marked as symbolic, not some unrelated internal condition.

Cons: You must maintain the label tracking correctly. If you create tags with unique labels for each symbolic variable (which you are doing via Tag.of(label)), and store those labels, this works well. One edge case to consider is tag union: if your symbolic value combines with another tainted value, Galette’s taint propagation might produce a Tag that contains multiple labels (a union of tags)
raw.githubusercontent.com
. The above check still covers that – as long as any label in the composite tag is one of your original symbols, you treat it as symbolic. This way, if (for example) a symbolic input flows into a calculation and combines with a constant tag, the resulting Tag still contains the original input’s label, so it remains “symbolic” from your perspective.

In implementation, you could incorporate this check either in your SymbolicComparison.collectComparisonConstraint logic or even within a custom ConcolicTaintListener callback (see Option 3). The key is that you use the Tag’s metadata (labels) rather than Tainter.getTag at runtime.

Option 3: Use a Taint Listener Hook (Advanced)

Galette provides a mechanism to listen to taint propagation events, similar to Phosphor’s DerivedTaintListener. In your project, you’ve defined a ConcolicTaintListener interface with callbacks for branch encounters, comparisons, etc.
raw.githubusercontent.com
raw.githubusercontent.com
. Another strategy is to hook into these events to decide when to record path constraints. For example, if Galette’s instrumentation or runtime can call ConcolicTaintListener.onBranch(Tag conditionTag, boolean taken) whenever a conditional branch is executed, you could implement this method to only record the branch’s constraint if conditionTag corresponds to a user-tagged symbolic input. The Tag condition passed in would be the union of all tags that influenced the branch condition. You could check it with the label-based approach above (isUserSymbolicTag(condition)). If false, you ignore that branch (treat it as “unrelated path”); if true, you convert the condition into a constraint (using your Green solver bridge) and add it via PathUtils.

This listener approach abstracts the filtering away from the low-level comparison logic. It is actually how concolic execution is often integrated: the system monitors each branch and collects a path predicate if the branch expression is tainted by a symbolic input
jonbell.net
. Confetti’s concolic fuzzing, for instance, uses the taint engine to know which branches depend on input bytes and only then generates hints
jonbell.net
. In your Galette-based setup, implementing onComparison or onBranch to inspect the tags would achieve the same effect.

Pros: It centralizes the path constraint filtering in one place (the listener). You don’t have to modify the comparison methods further; the listener can decide to call PathUtils.addConstraint(...) only when appropriate. It also makes it easier to handle other events (like symbolic arithmetic or method calls) uniformly if needed. Since you have already extended Galette’s internals, you could integrate this listener by registering it with Galette’s taint propagation system (for example, Galette might allow setting a static listener that gets notified on taint events). The interface you defined suggests you intended to use it for this purpose.

Cons: This requires that Galette’s instrumentation actually triggers these callbacks. You may need to modify the Galette agent to call ConcolicTaintListener.onComparison (etc.) at the right points (e.g., after a comparison operation or branch). If you haven’t already wired that up, it’s additional work. Essentially, it’s a more elaborate version of Option 2 – under the hood it would still check whether a Tag is symbolic before logging the constraint, just in a callback rather than inline in the comparison.
Recommendation

In summary, the fundamental problem is that your filter needs a reliable way to tell if a runtime value’s tag is one of the “symbolic” ones of interest. Because direct use of Tainter.getTag failed in an un-instrumented context
raw.githubusercontent.com
, you should either bring that context under instrumentation or use out-of-band knowledge of the tags.

If it’s feasible, instrumenting PathUtils (Option 1) is a straightforward fix – it makes Galette do the work of supplying the TagFrame so you can call Tainter.getTag normally. This would align with how Galette is meant to be used (since normally user code is instrumented). Just verify that including your concolic.knarr.runtime package in the instrumentation process doesn’t introduce any bootstrap issues.

On the other hand, Option 2 (checking tag labels or IDs) is a very robust solution that doesn’t rely on the instrumentation at all for the filter. Given that you already maintain a set of symbolic labels and create tags with those labels, leveraging that is simple and effective. For example, when a branch uses variables that were not explicitly tagged, their Tag will either be null or contain no user labels, and your check will naturally skip adding any constraint. This eliminates the “unrelated path” constraints. The Galette paper notes that tag frames ensure tags stay aligned with the correct data
jonbell.net
, so if a branch does involve your symbolic data, the Tag passed along will carry your label and be caught by the filter.

Lastly, for a more architectural solution, integrating the taint listener (Option 3) would be the way to go if you foresee extending this to many different operations (branches, arithmetic, array indices, etc.). It’s a bit more involved, but it’s how one would generally implement concolic execution on top of a taint engine: only respond to events where taint is present. In fact, the original Knarr (on Phosphor) did exactly this – it used Phosphor’s taint tracking to intercept comparisons and record path constraints only when the operands had a Taint
github.com
. You can mirror that design in Galette by using the Tag and listener system.

In conclusion, to fix the immediate problem: use the tag information more directly. Either enable Galette to give you the tag (via instrumentation) or check the tag’s contents against your known symbolic markers. Both approaches will let you filter out constraints from branches that don’t involve your symbolic inputs, thereby collecting only the relevant path constraints for concolic execution. This aligns with the guidance from similar tools – e.g., Confetti only considers branches where the predicate depends on input bytes (taint)
jonbell.net
– and will solve the “unrelated paths” issue in your integration.

Sources:

    Galette internal Tainter implementation (placeholders vs. instrumented behavior)
    raw.githubusercontent.com
    raw.githubusercontent.com

    Galette design: passing tag frames to methods for real tag access
    jonbell.net

    Galette Tag structure (labels and union)
    raw.githubusercontent.com
    raw.githubusercontent.com

    Your integration code references (symbolic tag creation and label tracking)
    raw.githubusercontent.com
    raw.githubusercontent.com

    Knarr/Phosphor and Confetti context on taint-guided branch constraint collection
    github.com
    jonbell.net

Citations

raw.githubusercontent.com
https://raw.githubusercontent.com/AnneKoziolek/galette-concolic-model-transformation/main/knarr-integration-plan.md

raw.githubusercontent.com
https://raw.githubusercontent.com/AnneKoziolek/galette-concolic-model-transformation/comparison-interception-internal/galette-agent/src/main/java/edu/neu/ccs/prl/galette/internal/runtime/Tainter.java

raw.githubusercontent.com
https://raw.githubusercontent.com/AnneKoziolek/galette-concolic-model-transformation/comparison-interception-internal/galette-agent/src/main/java/edu/neu/ccs/prl/galette/internal/runtime/Tainter.java

raw.githubusercontent.com
https://raw.githubusercontent.com/AnneKoziolek/galette-concolic-model-transformation/comparison-interception-internal/galette-agent/src/main/java/edu/neu/ccs/prl/galette/internal/runtime/Tainter.java

Dynamic Taint Tracking for Modern Java Virtual Machines
https://www.jonbell.net/preprint/fse25-galette.pdf

raw.githubusercontent.com
https://raw.githubusercontent.com/neu-se/galette/main/galette-agent/src/main/java/edu/neu/ccs/prl/galette/internal/runtime/Tag.java

raw.githubusercontent.com
https://raw.githubusercontent.com/AnneKoziolek/galette-concolic-model-transformation/comparison-interception-internal/knarr-runtime/src/main/java/edu/neu/ccs/prl/galette/concolic/knarr/runtime/GaletteSymbolicator.java

raw.githubusercontent.com
https://raw.githubusercontent.com/neu-se/galette/main/galette-agent/src/main/java/edu/neu/ccs/prl/galette/internal/runtime/Tag.java

raw.githubusercontent.com
https://raw.githubusercontent.com/AnneKoziolek/galette-concolic-model-transformation/comparison-interception-internal/knarr-runtime/src/main/java/edu/neu/ccs/prl/galette/concolic/knarr/runtime/PathUtils.java

GitHub - gmu-swe/knarr
https://github.com/gmu-swe/knarr

CONFETTI: Amplifying Concolic Guidance for Fuzzers
https://jonbell.net/publications/confetti

raw.githubusercontent.com
https://raw.githubusercontent.com/neu-se/galette/main/galette-agent/src/main/java/edu/neu/ccs/prl/galette/internal/runtime/Tag.java

raw.githubusercontent.com
https://raw.githubusercontent.com/AnneKoziolek/galette-concolic-model-transformation/comparison-interception-internal/knarr-runtime/src/main/java/edu/neu/ccs/prl/galette/concolic/knarr/listener/ConcolicTaintListener.java

raw.githubusercontent.com
https://raw.githubusercontent.com/AnneKoziolek/galette-concolic-model-transformation/comparison-interception-internal/knarr-runtime/src/main/java/edu/neu/ccs/prl/galette/concolic/knarr/listener/ConcolicTaintListener.java

CONFETTI: Amplifying Concolic Guidance for Fuzzers
https://jonbell.net/publications/confetti
All Sources
raw.gith...ercontent
jonbell
github
jonbell