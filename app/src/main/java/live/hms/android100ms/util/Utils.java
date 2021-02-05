package live.hms.android100ms.util;

/**
 * HMSUtils provides helper functions for managing thread safety.
 */
public final class Utils {
  private Utils() {
  }

  /**
   * Helper method which throws an exception  when an assertion has failed.
   */
  public static void assertIsTrue(boolean condition) {
    if (!condition) {
      throw new AssertionError("Expected condition to be true");
    }
  }

}
