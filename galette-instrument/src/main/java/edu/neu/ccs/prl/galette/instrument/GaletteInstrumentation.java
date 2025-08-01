package edu.neu.ccs.prl.galette.instrument;

import edu.neu.ccs.prl.galette.internal.patch.Patcher;
import edu.neu.ccs.prl.galette.internal.runtime.Tag;
import edu.neu.ccs.prl.galette.internal.transform.GaletteTransformer;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Instances of this class are created via reflection.
 */
@SuppressWarnings("unused")
public class GaletteInstrumentation implements Instrumentation {
    private GaletteTransformer transformer;
    private Set<File> classPathElements;

    @Override
    public void configure(Properties options) {
        transformer = new GaletteTransformer();
        classPathElements = new HashSet<>();
        classPathElements.add(InstrumentUtil.getClassPathElement(Tag.class));
    }

    @Override
    public Properties getOptions() {
        return new Properties();
    }

    @Override
    public java.util.Set<File> getClassPathElements() {
        return classPathElements;
    }

    @Override
    public byte[] apply(byte[] classFileBuffer) {
        return transformer.transform(classFileBuffer, false);
    }

    @Override
    public BiFunction<String, byte[], byte[]> createPatcher(Function<String, byte[]> entryLocator) {
        Patcher patcher = new Patcher(entryLocator);
        return patcher::patch;
    }

    @Override
    public Set<String> getRequiredModules() {
        return new HashSet<>(Arrays.asList("java.base", "jdk.jdwp.agent", "java.instrument", "jdk.unsupported"));
    }

    @Override
    public boolean shouldPack(String resourceName) {
        return resourceName.startsWith(GaletteTransformer.RUNTIME_PACKAGE_PREFIX)
                || resourceName.startsWith(GaletteTransformer.TRANSFORM_PACKAGE_PREFIX);
    }

    @Override
    public Set<File> getElementsToPack() {
        return classPathElements;
    }
}
