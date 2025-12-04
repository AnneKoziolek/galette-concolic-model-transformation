import edu.neu.ccs.prl.galette.concolic.knarr.runtime.GaletteSymbolicator;
import edu.neu.ccs.prl.galette.concolic.knarr.runtime.PathConditionWrapper;
import edu.neu.ccs.prl.galette.concolic.knarr.runtime.PathUtils;
import edu.neu.ccs.prl.galette.internal.runtime.Tag;
import edu.neu.ccs.prl.galette.internal.runtime.Tainter;

public class TestTagFiltering {
    public static void main(String[] args) {
        System.out.println("=== Testing Tag-Based Filtering ===");
        
        // Reset state
        GaletteSymbolicator.reset();
        
        // Create a symbolic value
        double thickness = 12.0;
        Tag tag = GaletteSymbolicator.makeSymbolicDouble("thickness_test", thickness);
        
        // Tag the value
        double taggedValue = Tainter.setTag(thickness, tag);
        
        // Verify tag was set
        Tag verifyTag = Tainter.getTag(taggedValue);
        System.out.println("Tag verification: " + (verifyTag != null ? "has tag" : "no tag"));
        
        // Test comparison
        System.out.println("\nTesting comparison with tagged value:");
        boolean result = taggedValue > 10.0;
        System.out.println("Comparison result: " + taggedValue + " > 10.0 = " + result);
        
        // Check if constraints were collected
        PathConditionWrapper pc = PathUtils.getCurPCWithGalette();
        System.out.println("Constraints collected: " + (pc != null && !pc.isEmpty() ? pc.size() : "none"));
        
        if (pc != null && !pc.isEmpty()) {
            System.out.println("Path constraints: " + pc.toString());
        } else {
            System.out.println("No path constraints collected");
        }
        
        System.out.println("\n=== Test Complete ===");
    }
}