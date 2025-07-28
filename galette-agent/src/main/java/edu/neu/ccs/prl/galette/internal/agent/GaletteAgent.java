package edu.neu.ccs.prl.galette.internal.agent;

import edu.neu.ccs.prl.galette.internal.runtime.*;
import edu.neu.ccs.prl.galette.internal.runtime.frame.SpareFrameStore;
import edu.neu.ccs.prl.galette.internal.transform.GaletteLog;
import edu.neu.ccs.prl.galette.internal.transform.GaletteTransformer;
import edu.neu.ccs.prl.galette.internal.transform.TransformationCache;
import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public final class GaletteAgent {
    static {
        // Thread should be safe to initialize at this point; initialize the spare frame store
        SpareFrameStore.initialize();
    }

    private GaletteAgent() {
        throw new AssertionError();
    }

    @SuppressWarnings("unused")
    public static void premain(String agentArgs, Instrumentation inst, TagFrame frame) throws IOException {
        premain(agentArgs, inst);
    }

    public static void premain(String agentArgs, Instrumentation inst) throws IOException {
        System.out.println("🚀 GaletteAgent.premain() called at " + new java.util.Date());
        System.out.println("📍 Agent args: " + agentArgs);

        GaletteLog.initialize(System.err);

        String cachePath = System.getProperty("galette.cache");
        System.out.println("💾 Cache path: " + cachePath);

        TransformationCache cache = cachePath == null ? null : new TransformationCache(new File(cachePath));
        GaletteTransformer.setCache(cache);

        System.out.println("✅ Adding TransformerWrapper to instrumentation");
        inst.addTransformer(new TransformerWrapper());

        System.out.println("✅ GaletteAgent initialization complete");
    }

    private static final class TransformerWrapper implements ClassFileTransformer {
        private final GaletteTransformer transformer = new GaletteTransformer();

        @SuppressWarnings("unused")
        public byte[] transform(
                ClassLoader loader,
                String className,
                Class<?> classBeingRedefined,
                ProtectionDomain protectionDomain,
                byte[] classFileBuffer,
                TagFrame frame) {
            return transform(loader, className, classBeingRedefined, protectionDomain, classFileBuffer);
        }

        @Override
        public byte[] transform(
                ClassLoader loader,
                String className,
                Class<?> classBeingRedefined,
                ProtectionDomain protectionDomain,
                byte[] classFileBuffer) {

            // Debug transformation for specific classes only to avoid ClassCircularityError
            if (className != null
                    && className.equals("edu/neu/ccs/prl/galette/examples/transformation/BrakeDiscTransformation")) {
                System.out.println("🔍 TransformerWrapper.transform() called for: " + className
                        + ". classBeingRedefined: " + classBeingRedefined + ". classFileBuffer length: "
                        + (classFileBuffer != null ? classFileBuffer.length : "null") + ". loader: " + loader
                        + ". protectionDomain: " + protectionDomain + ". GaletteTransformer: " + transformer);
            }

            if (classBeingRedefined != null) {
                // The class is being redefined or retransformed
                return null;
            }

            byte[] result = null;
            try {
                if (className != null
                        && className.equals(
                                "edu/neu/ccs/prl/galette/examples/transformation/BrakeDiscTransformation")) {
                    System.out.println("🔍 About to call transformer.transform()");
                    System.out.println(
                            "🔍 Transformer class: " + transformer.getClass().getName());
                    System.out.println("🔍 Transformer classloader: "
                            + transformer.getClass().getClassLoader());
                    System.out.println("🔍 Transformer location: "
                            + transformer.getClass().getProtectionDomain().getCodeSource());
                }

                result = transformer.transform(classFileBuffer, false);

                if (className != null
                        && className.equals(
                                "edu/neu/ccs/prl/galette/examples/transformation/BrakeDiscTransformation")) {
                    System.out.println(
                            "🔍 transformer.transform() returned: " + (result != null ? "transformed" : "null"));
                }
            } catch (Throwable t) {
                System.err.println("❌ Exception in transformer.transform() for " + className + ": " + t);
                t.printStackTrace();
                throw t;
            }

            if (className != null
                    && className.equals("edu/neu/ccs/prl/galette/examples/transformation/BrakeDiscTransformation")) {
                System.out.println("🔍 TransformerWrapper.transform() result for " + className + ": "
                        + (result != null ? "transformed (" + result.length + " bytes)" : "null (no transformation)"));
            }

            return result;
        }
    }
}
