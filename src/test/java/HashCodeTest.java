import org.junit.jupiter.api.Test;

/**
 * @author gongdewei 2023/4/29
 */
public class HashCodeTest {
    @Test
    public void testHashCode() {
        testIdentityHashCode("target");
        testIdentityHashCode("target");
        testIdentityHashCode("target");
    }

    private void testIdentityHashCode(String s) {
        String s1 = new String(s);
        int hashCode = System.identityHashCode(s1);
        System.out.println(hashCode);
        hashCode = System.identityHashCode(s1);
        System.out.println(hashCode);
    }
}
