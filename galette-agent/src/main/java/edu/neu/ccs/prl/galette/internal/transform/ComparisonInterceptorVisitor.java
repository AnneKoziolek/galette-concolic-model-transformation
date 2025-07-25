package edu.neu.ccs.prl.galette.internal.transform;

import org.objectweb.asm.*;

/**
 * ASM visitor that intercepts comparison operations for automatic path constraint collection.
 * Uses replacement strategy to avoid complex stack manipulation.
 *
 * This visitor transforms Java comparison bytecode operations to enable automatic
 * constraint collection without requiring code changes in the target application.
 *
 * @author Implementation based on claude-copilot-combined-comparison-interception-plan-3.md
 */
public class ComparisonInterceptorVisitor extends ClassVisitor {

    private static final String PATH_UTILS_CLASS = "edu/neu/ccs/prl/galette/internal/runtime/PathUtils";

    private String className;

    public ComparisonInterceptorVisitor(ClassVisitor cv) {
        super(GaletteTransformer.ASM_VERSION, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(
            int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new ComparisonMethodVisitor(mv, className);
    }

    private static class ComparisonMethodVisitor extends MethodVisitor {

        private final String className;

        public ComparisonMethodVisitor(MethodVisitor mv, String className) {
            super(GaletteTransformer.ASM_VERSION, mv);
            this.className = className;
        }

        @Override
        public void visitInsn(int opcode) {
            // Debug: Log all instructions we see in classes that might contain comparisons
            if (className != null && className.contains("BrakeDisc")) {
                System.out.println("🔍 ComparisonInterceptor: visitInsn opcode=" + opcode + " (" + opcodeToName(opcode)
                        + ") in " + className);
            }

            switch (opcode) {
                case Opcodes.LCMP:
                    System.out.println("🔧 Intercepting LCMP in " + className);
                    // Replace LCMP entirely with instrumented version
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, PATH_UTILS_CLASS, "instrumentedLcmp", "(JJ)I", false);
                    break;

                case Opcodes.FCMPL:
                    System.out.println("🔧 Intercepting FCMPL in " + className);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, PATH_UTILS_CLASS, "instrumentedFcmpl", "(FF)I", false);
                    break;

                case Opcodes.FCMPG:
                    System.out.println("🔧 Intercepting FCMPG in " + className);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, PATH_UTILS_CLASS, "instrumentedFcmpg", "(FF)I", false);
                    break;

                case Opcodes.DCMPL:
                    System.out.println("🔧 Intercepting DCMPL in " + className);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, PATH_UTILS_CLASS, "instrumentedDcmpl", "(DD)I", false);
                    break;

                case Opcodes.DCMPG:
                    System.out.println("🔧 Intercepting DCMPG in " + className);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, PATH_UTILS_CLASS, "instrumentedDcmpg", "(DD)I", false);
                    break;

                default:
                    super.visitInsn(opcode);
            }
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            switch (opcode) {
                case Opcodes.IF_ICMPEQ:
                case Opcodes.IF_ICMPNE:
                case Opcodes.IF_ICMPLT:
                case Opcodes.IF_ICMPGE:
                case Opcodes.IF_ICMPGT:
                case Opcodes.IF_ICMPLE:
                    // Replace with instrumented version
                    mv.visitLdcInsn(opcodeToString(opcode));
                    mv.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            PATH_UTILS_CLASS,
                            "instrumentedIcmpJump",
                            "(IILjava/lang/String;)Z",
                            false);
                    mv.visitJumpInsn(Opcodes.IFNE, label);
                    break;

                case Opcodes.IF_ACMPEQ:
                case Opcodes.IF_ACMPNE:
                    mv.visitLdcInsn(opcodeToString(opcode));
                    mv.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            PATH_UTILS_CLASS,
                            "instrumentedAcmpJump",
                            "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)Z",
                            false);
                    mv.visitJumpInsn(Opcodes.IFNE, label);
                    break;

                default:
                    super.visitJumpInsn(opcode, label);
            }
        }

        private String opcodeToString(int opcode) {
            switch (opcode) {
                case Opcodes.IF_ICMPEQ:
                    return "EQ";
                case Opcodes.IF_ICMPNE:
                    return "NE";
                case Opcodes.IF_ICMPLT:
                    return "LT";
                case Opcodes.IF_ICMPGE:
                    return "GE";
                case Opcodes.IF_ICMPGT:
                    return "GT";
                case Opcodes.IF_ICMPLE:
                    return "LE";
                case Opcodes.IF_ACMPEQ:
                    return "ACMP_EQ";
                case Opcodes.IF_ACMPNE:
                    return "ACMP_NE";
                default:
                    return "UNKNOWN";
            }
        }

        private String opcodeToName(int opcode) {
            switch (opcode) {
                case Opcodes.LCMP:
                    return "LCMP";
                case Opcodes.FCMPL:
                    return "FCMPL";
                case Opcodes.FCMPG:
                    return "FCMPG";
                case Opcodes.DCMPL:
                    return "DCMPL";
                case Opcodes.DCMPG:
                    return "DCMPG";
                default:
                    return "OPCODE_" + opcode;
            }
        }
    }
}
