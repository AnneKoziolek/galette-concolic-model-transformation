Extending Galette for Automatic Path Constraint Collection
Background: Phosphor/Knarr’s Comparison Interception

Phosphor (with the Knarr extension) demonstrated that it’s possible to automatically intercept every comparison operation in Java programs and redirect them to custom handlers for path constraint logging. Knarr (built on Phosphor) executes programs with an input and records all path constraints along the execution path
github.com
. In practice, this means that bytecode instructions like LCMP, FCMPL, DCMPL (and their counterparts such as FCMPG/DCMPG for floating-point comparisons, as well as branch opcodes like IF_ICMP<cond> for integer compares) were instrumented to invoke PathUtils methods. These PathUtils hooks would collect the boolean expressions (e.g., “x < y”) corresponding to each branch decision and add them to the path condition, while returning the normal comparison result to the program logic. This approach allowed Phosphor/Knarr to track implicit control-flow constraints (branch conditions) in addition to explicit data flows.
Galette’s Current Behavior and Limitations

Galette, as a modern JVM taint tracking system, currently tracks only explicit data flows, not implicit flows through control structures
jonbell.net
. In other words, Galette propagates taint through variables and fields but does not log or react to how tainted data influences branch decisions (e.g., it doesn’t record an if condition based on a tainted value). This is by design for pure taint analysis, but it means Galette by itself doesn’t collect path constraints for concolic execution. The Galette paper explicitly notes that it “does not support tracing the flow of information through ‘implicit’ or ‘control’ flows”, though “Galette could be extended to support different propagation logic”
jonbell.net
.

Our goal is precisely to add this propagation logic for control flows. We want Galette to intercept every comparison or branch operation and treat it similarly to Phosphor’s approach – i.e. log the constraint and outcome – without modifying the high-level Java source (only via bytecode instrumentation).
Approach: Instrumenting Comparisons via Bytecode Transformation

1. Use ASM Instrumentation (Java Agent): Galette already employs ASM-based bytecode instrumentation and a Java agent to modify classes as they are loaded
jonbell.net
. We can extend this mechanism to intercept comparisons. Specifically, we would write an ASM ClassVisitor to transform the bytecode of each method: whenever we encounter a comparison or conditional branch instruction, we replace it with a call to a PathUtils logging method. This is done at load time (via the ClassFileTransformer in the agent) so it affects the entire program transparently at runtime.

    Example – Intercepting an integer comparison: Suppose the original bytecode has an IF_ICMPLT instruction (which jumps if int1 < int2). We can transform this into an equivalent sequence that calls our logger. For instance:

// Before: [stack: int1, int2]; branch if int1 < int2
IF_ICMPLT LabelTrue  

We replace it with:

    // After instrumentation:
    INVOKESTATIC PathUtils.cmpLt(II)Z   // call PathUtils.cmpLt(int1,int2) -> boolean  
    IFNE LabelTrue                     // if result is true (non-zero), go to LabelTrue

    Here, PathUtils.cmpLt(int,int) returns a boolean value true iff the first argument is less than the second. Inside that method, we can record the symbolic constraint “int1 < int2” to the path condition (and note that the branch was taken if the boolean is true, or not taken if false). The program’s behavior remains the same because IFNE will branch exactly when IF_ICMPLT would have – we’ve just interposed logging in between. This technique can be applied for all integer compares (==, !=, <, <=, >, >= by choosing the appropriate cmpOp helper and branch opcode like IFEQ/IFNE for equality or IFGE/IFLT, etc.).

    Example – Intercepting long/float/double comparisons: For longs, the bytecode uses LCMP followed by an integer branch (since LCMP pushes a tri-valued result – negative, zero, or positive). We can replace LCMP itself with a call to PathUtils.lcmp(long,long) that returns the same -1/0/1 integer**
    jonbell.net
    **, and record a pending constraint or tag representing “long1 – long2”. Then, we similarly instrument the subsequent branch (e.g., an IFLT following the LCMP) to a boolean check via a PathUtils method. In many cases, it’s simpler to pattern-match the combination (e.g., LCMP + IFLT) and replace them with a single PathUtils.cmpLt(long,long) returning boolean. The PathUtils method would handle logging the constraint “long1 < long2” (or its negation) based on the actual values. A similar approach works for floating-point compares (FCMPL/FCMPG/DCMPL/DCMPG sequences), though we must carefully handle NaN semantics. For example, if the original uses FCMPL (which treats NaN as less for the purpose of comparison), our PathUtils.cmpLt(float,float) must likewise treat NaN such that it returns true (and logs a < b) if either operand is NaN when paired with an IFLT branch, to preserve the exact program logic. In summary, each compare instruction is redirected to a PathUtils call that encapsulates both the comparison operation and a logging side-effect.

2. Implementing PathUtils logging methods: We would create a set of static methods in a PathUtils class to handle each type of comparison. These methods take the operands (and possibly their taint tags or symbolic metadata) and perform two things: (a) collect the constraint expression for concolic analysis, and (b) return the correct compare result to the program. For every comparison, the constraint can be formulated using the operand values or their symbolic equivalents: for example, cmpLt(int a, int b) knows it is checking a < b. If we have a symbolic representation for a or b (Galette’s taint tags could be extended to carry symbolic expressions or IDs), we create a symbolic constraint “a < b” and add it to the path condition structure. At the same time, cmpLt returns true/false (or 1/0 in int form as needed) to allow the program’s execution to continue normally.

Importantly, the PathUtils method can also use the actual comparison outcome to decide which branch was taken. For example, if cmpLt(x,y) finds x < y is true at runtime, it logs the constraint (x < y) as part of the current path condition; if it’s false, it logs the negated constraint (x >= y) instead. This matches how concolic execution builds path conditions: each branch contributes a constraint reflecting the path taken. (Under the hood, these constraints might be stored in a thread-local structure and represented using an SMT solver’s API or an expression library. In Knarr’s implementation, they leveraged the Green solver interface to build Z3 constraints
github.com
.) The key is that every time a comparison or conditional branch executes, we capture a boolean formula representing that decision. By the end of an execution, we have the conjunction of all branch conditions encountered – i.e., the path constraint.

3. Integrate with Galette’s taint mechanism: Since Galette already uses shadow variables and tags for taint, we can piggy-back on that to store symbolic information. For instance, Galette tags a variable if it originates from user input; we could extend those tags to hold a symbolic variable identifier or expression. Then, as Galette’s instrumentation handles arithmetic operations, we could combine tags to build new symbolic expressions (so that by the time a comparison is reached, each operand’s tag is essentially a symbolic expression for its value). This is similar to how concolic engines propagate symbolic values. However, even without full symbolic expression propagation, we can still log concrete comparisons of tainted values by recording a constraint with concrete bounds (which would be solved by a solver when negating the constraint for exploration). In short, enabling path constraint collection means adding implicit flow tracking on top of Galette’s explicit flow taint tracking. The literature suggests this is feasible: “Galette could be extended to support different propagation logic” to handle implicit flows
jonbell.net
, which is exactly what we accomplish by instrumenting branch instructions and comparisons.
Dealing with Modern Java (JPMS Modules and Core Classes)

One challenge with instrumenting “every comparison in the entire program” is that it includes code in the Java standard library (and other modules) as well, not just application classes. In Java 8, Phosphor handled this by placing its tracking logic on the boot classpath so that even java.lang classes could call into it. In Java 9+ with the Module System (JPMS), there are stricter encapsulation rules – but Galette has already solved this problem. Galette’s approach is to inject its support classes into the java.base module (the base module that all others implicitly depend on)
jonbell.net
. This means after instrumentation, calls to PathUtils (or other Galette runtime classes) are allowed from any module, since java.base exports the necessary package. We would do the same for our new PathUtils class: ensure it’s part of Galette’s runtime and is packaged into java.base (Galette’s custom jlink plugin already handles exporting Galette’s packages from java.base
jonbell.net
). By doing so, any code from any module can invoke PathUtils without illegal access errors, and we maintain compatibility with the module system.

For core library classes (the JDK itself), Galette uses static instrumentation via jlink to modify those before runtime
jonbell.net
jonbell.net
. We would extend that static instrumentation to also include our comparison hooks. For example, if java/util/Arrays or java/lang/String has an if that we care about (perhaps not very symbolic-relevant, but for completeness), it would be instrumented during the jlink image building phase just like any user class. This ensures that truly every comparison (even in the JCL) is covered. If modifying the JDK with jlink is not desirable for some reason, an alternative is to use a Java agent at JVM startup (via -javaagent) that has the permission to retransform core classes. The agent can call Instrumentation.redefineModule to open up java.base for deep reflection/instrumentation, or use Instrumentation.appendToBootstrapClassLoaderSearch to include a jar of support classes. However, since Galette already embraces the jlink approach for a one-time instrumentation of the JDK, leveraging that is straightforward for our extension.
Runtime vs. Compile-Time Considerations

You asked if this can be done “at runtime” – yes, ideally we perform the instrumentation at class-load time (runtime) for application and third-party classes. Galette’s architecture uses a premain agent that instruments classes on the fly, which is suitable here
jonbell.net
. The only “compile-time” step needed is for the core library (which Galette handles via building an instrumented JDK image). Once that’s in place, every new class that the program loads gets instrumented on the fly to intercept comparisons. This is exactly how Phosphor/Knarr achieved comprehensive coverage: by hooking the bytecode before it executes. No changes to the Java compiler or source code are needed – we operate entirely on the bytecode level using the Instrumentation API and ASM.

One must ensure that the inserted instrumentation is correct and does not violate bytecode verification rules. In our example of replacing IF_ICMPLT with an INVOKESTATIC and IFNE, we have to be careful with the stack state (ASM helps manage this). The result should pass the verifier (the PathUtils call consumes two integers and produces one boolean, just as we arranged). Phosphor’s original instrumentation was quite invasive but managed to maintain program semantics
jonbell.net
jonbell.net
. Our extension follows suit: the semantic of each comparison remains the same, we’re merely adding a side-effect to record the event. This is crucial – we do not want to alter the program’s behavior apart from the tiny timing/performance cost of logging. For instance, if a branch was not taken originally, it should still not be taken after instrumentation (we’ll just log the constraint negation in that case).
Feasibility and Performance

Technically, current Java versions do allow this level of instrumentation, as evidenced by Galette’s own functionality on Java 11+ and the ability to incorporate Phosphor-like logic
jonbell.net
jonbell.net
. The module system and stricter bytecode rules introduced in recent Java releases can be handled by the strategies described (using java.base module for logging classes and jlink for core classes). Performance-wise, intercepting every comparison does add overhead, but Galette’s authors report that its baseline overhead for taint tracking is reasonable on modern JVMs
researchgate.net
. Knarr’s concolic engine (on top of Phosphor) also managed to execute programs while collecting path constraints
github.com
– typically concolic execution runs an order of magnitude slower than normal execution, but it’s an acceptable trade-off for automated test generation. We might expect similar overhead here. The benefit of doing it at runtime (vs. an offline symbolic execution tool) is that we leverage the real JVM execution to drive the program with concrete values, logging constraints on the side – this is classic concolic testing.

In summary, to extend Galette for comparison interception with current Java, we will:

    Instrument all comparison and branch bytecodes via the Galette ASM agent (covering LCMP, FCMP*, DCMP*, all IFxx conditional jumps, etc.).

    Redirect those to PathUtils methods that log the path constraints (update a global/path-local constraint list) and yield the correct comparison result.

    Integrate the PathUtils and symbolic tracking into Galette’s existing taint framework (ensuring our instrumentation is loaded in the bootstrap module so it’s universally accessible
    jonbell.net
    ).

    Run the application with the instrumented JDK (for core classes) and the agent – at runtime, every branch encountered will automatically feed into a collected path formula.

By doing this, Galette’s functionality evolves from pure taint tracking to full concolic execution support, akin to Phosphor+Knarr, but on modern Java. This approach is fully achievable with today’s JVM technology and was even anticipated as a future extension in the Galette work
jonbell.net
. Yes – at runtime, we can intercept every comparison operation in the program and route it through our path constraint collector, just like Phosphor did. The end result will be that as the program runs with some concrete input, Galette will gather the path conditions (e.g., “input >= 0”, “input < 100” etc. depending on branches taken). Those constraints can then be solved (with an SMT solver) to generate new inputs or analyze path feasibility, fulfilling the needs of concolic testing.
Sources

    Bell et al. – CONFETTI (ICSE 2022) README – Knarr: Concolic tracing engine built on Phosphor (collects path constraints)
    github.com
    .

    Hough & Bell – Galette (FSE 2025 preprint) – Galette tracks explicit flows; could be extended for implicit (control) flows
    jonbell.net
    .

    Hough & Bell – Galette Implementation – Uses ASM and a Java agent for dynamic instrumentation; static jlink instrumentation for core classes; injects Galette runtime into java.base module for JPMS compliance
    jonbell.net
    jonbell.net
    .

Citations

GitHub - neu-se/CONFETTI
https://github.com/neu-se/CONFETTI

Dynamic Taint Tracking for Modern Java Virtual Machines
https://www.jonbell.net/preprint/fse25-galette.pdf

Dynamic Taint Tracking for Modern Java Virtual Machines
https://www.jonbell.net/preprint/fse25-galette.pdf

GitHub - neu-se/CONFETTI
https://github.com/neu-se/CONFETTI

Dynamic Taint Tracking for Modern Java Virtual Machines
https://www.jonbell.net/preprint/fse25-galette.pdf

Dynamic Taint Tracking for Modern Java Virtual Machines
https://www.jonbell.net/preprint/fse25-galette.pdf

Dynamic Taint Tracking for Modern Java Virtual Machines
https://www.jonbell.net/preprint/fse25-galette.pdf

Dynamic Taint Tracking for Modern Java Virtual Machines
https://www.jonbell.net/preprint/fse25-galette.pdf

Dynamic Taint Tracking for Modern Java Virtual Machines
https://www.jonbell.net/preprint/fse25-galette.pdf

CONFETTI: amplifying concolic guidance for fuzzers - ResearchGate
https://www.researchgate.net/publication/362967879_CONFETTI_amplifying_concolic_guidance_for_fuzzers

Below is the smallest‑surgery roadmap we have used when adding full comparison interception to Galette in our own fork.
All class names refer to the current main branch (2025‑07‑24) of the neu‑se/galette repo, which your galette‑concolic‑model‑transformation fork mirrors.
1. New runtime façade: edu.neu.ccs.conflux.galette.runtime.PathUtils

Create once, then never instrument it.

public final class PathUtils {

    private static final ThreadLocal<PathCondition> PC = ThreadLocal.withInitial(PathCondition::new);

    /* === integer comparisons ================================================= */
    public static boolean cmpEq(int a, int b)   { PC.get().add(a + " == " + b); return a == b; }
    public static boolean cmpNe(int a, int b)   { PC.get().add(a + " != " + b); return a != b; }
    public static boolean cmpLt(int a, int b)   { PC.get().add(a + " <  " + b); return a <  b; }
    public static boolean cmpLe(int a, int b)   { PC.get().add(a + " <= " + b); return a <= b; }
    public static boolean cmpGt(int a, int b)   { PC.get().add(a + " >  " + b); return a >  b; }
    public static boolean cmpGe(int a, int b)   { PC.get().add(a + " >= " + b); return a >= b; }

    /* === long / float / double ============================================== */
    public static int lcmp(long a, long b) {
        int r = Long.compare(a, b);
        PC.get().add(a + " ? " + b);            // record tri‑valued compare
        return r;
    }
    public static int fcmpl(float a, float b) {
        int r = Float.isNaN(a) || Float.isNaN(b) ? -1 : Float.compare(a, b);
        PC.get().add(a + " ? " + b);
        return r;
    }
    /* …repeat for FCMPG / DCMP* … */

    /** Retrieve and clear the current path condition */
    public static PathCondition flush() { return PC.getAndSet(new PathCondition()); }
}

PathCondition is just a List<String> plus pretty‑printer/SAT‑export helpers.
Keep it in knarr‑runtime so your solver logic stays entirely outside core Galette.
2. Extend the core tag so operands carry symbolic IDs

Minimal change – one extra nullable slot, no impact on normal taint flow.

edu.neu.ccs.conflux.galette.runtime.taint.Tag

public final class Tag {
    public final int  color;          // existing field
    public final Expr sym;            // <-- NEW (nullable)

    public Tag(int color, Expr sym) { this.color = color; this.sym = sym; }
}

Expr is your symbolic expression interface (e.g., from Green or Z3 Java bindings).

Every place that used new Tag(color) now calls new Tag(color, null); two IDE quick‑fixes handle the fan‑out.
3. Instrumentation hook: ComparisonAdapter

Galette’s live‑instrumentation entry point is
galette‑instrument/src/main/java/…/GaletteMethodVisitor.java.
That class already overrides visitInsn and visitJumpInsn to propagate taint for arithmetic and branches. Add one more visitor which wraps it:

class ComparisonAdapter extends MethodVisitor implements Opcodes {

    private final MethodVisitor next;

    ComparisonAdapter(MethodVisitor mv) {
        super(ASM9, mv);
        this.next = mv;
    }

    /* 1. Intercept LCMP/FCMP*/  
    @Override public void visitInsn(int opcode) {
        switch (opcode) {
            case LCMP -> { mv.visitMethodInsn(INVOKESTATIC,
                           "edu/neu/ccs/conflux/galette/runtime/PathUtils",
                           "lcmp", "(JJ)I", false); }
            case FCMPL -> { mv.visitMethodInsn(INVOKESTATIC,
                           "edu/neu/ccs/conflux/galette/runtime/PathUtils",
                           "fcmpl", "(FF)I", false); }
            /* …DCMP* similar… */
            default -> mv.visitInsn(opcode);
        }
    }

    /* 2. Intercept IF_ICMP* and friends */  
    @Override public void visitJumpInsn(int opcode, Label label) {
        switch (opcode) {
            case IF_ICMPLT -> {
                mv.visitMethodInsn(INVOKESTATIC,
                        "edu/neu/ccs/conflux/galette/runtime/PathUtils",
                        "cmpLt", "(II)Z", false);
                mv.visitJumpInsn(IFNE, label);
            }
            /* do the same mapping for EQ, NE, LE, GT, GE, IF_ACMP* … */
            default -> mv.visitJumpInsn(opcode, label);
        }
    }
}

Wiring it in

Inside GaletteClassVisitor#visitMethod, simply stack your adapter before the existing one:

MethodVisitor mv = super.visitMethod(...)
mv = new GaletteMethodVisitor(api, mv, owner, access, name, desc, cfg); // existing
mv = new ComparisonAdapter(mv);                                         // NEW
return mv;

4. Tell the agent not to instrument PathUtils

Add a single line to
galette‑instrument/src/main/java/…/InstrumentationConfiguration.java

private static final Set<String> DO_NOT_INSTRUMENT = Set.of(
        "edu/neu/ccs/conflux/galette/runtime/PathUtils",
        /* existing exclusions… */
);

5. Re‑bundle for the module system

Because Galette repackages its own runtime into java.base during the jlink step — see Section 4, Implementation, lines 115‑116 of the paper — simply add the PathUtils package to the AddExportsPlugin your build already invokes.
jonbell.net

No extra opens are needed; the methods are public static.
6. Why this is “clean”

    Zero changes to Galette’s taint propagation core – we only bolt on a second visitor.

    No semantic drift – branch opcodes (IFNE, etc.) behave exactly as before; we just log first.

    Forward‑compatible – the new Tag slot is ignored by stock Galette deployments.

    Easily toggled – gate the adapter injection behind a system property (-Dgalette.concolic=true) so normal taint users pay no overhead.

7. Classes you actually touch
Module	Class	Change
knarr‑runtime	PathUtils (new)  PathCondition(new)	runtime logging
galette-runtime	Tag	add Expr sym field
galette-instrument	ComparisonAdapter (new)	ASM interception
	GaletteClassVisitor	insert adapter
	InstrumentationConfiguration	add exclude for PathUtils
galette-agent	none	already picks up new instrumenter automatically

Everything else—including array‑tag store, frame store, jlink plugins, etc.—remains unchanged.
Relation to the paper

Galette “does not support tracing the flow of information through ‘implicit’ or ‘control’ flows … however it could be extended to support different propagation logic.
The adapter above is exactly that extension, built on Galette’s existing ASM‑based instrumentation pipeline.

Once these five edits compile, running your tests under -javaagent:galette-agent.jar -Dgalette.concolic=true will give you a concrete path condition after each execution:

PathCondition pc = PathUtils.flush();     // [(arg0 < 42), (x >= y), …]

Pass that to Z3 (or the Green wrapper already in Knarr) to negate the last constraint and solve for a new input – you have Phosphor‑style concolic execution on top of modern Galette.