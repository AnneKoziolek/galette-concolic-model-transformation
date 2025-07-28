import edu.neu.ccs.prl.galette.internal.runtime.Tag;
import edu.neu.ccs.prl.galette.internal.runtime.Tainter;

/**
 * Simple test to verify tag-based filtering works correctly.
 */
public class SimpleTagTest {
    public static void main(String[] args) {
        System.out.println("=== Simple Tag-Based Filtering Test ===");
        
        // Test 1: Untagged values (should NOT generate constraints)
        System.out.println("Test 1: Untagged comparison (should be silent)");
        double a = 12.0;
        double b = 60.0;
        boolean result1 = a > b; // This should NOT generate constraints
        System.out.println("Untagged: " + a + " > " + b + " = " + result1);
        
        // Test 2: Tagged values (should generate constraints)
        System.out.println("\nTest 2: Tagged comparison (should generate constraints)");
        Tag tag = Tag.of("thickness");
        double taggedA = Tainter.setTag(12.0, tag);
        double taggedB = Tainter.setTag(60.0, tag);
        
        // Force DCMPL instruction by using variables in comparison
        boolean result2 = taggedA > taggedB; // This SHOULD generate constraints
        System.out.println("Tagged: " + taggedA + " > " + taggedB + " = " + result2);
        
        // Also test with one tagged, one untagged (should still generate constraints)
        boolean result3 = taggedA > 10.0;
        System.out.println("Mixed: " + taggedA + " > 10.0 = " + result3);
        
        System.out.println("\n=== Test Complete ===");
    }
}