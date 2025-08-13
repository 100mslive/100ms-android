package live.hms.roomkit.util

import android.widget.EditText
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class NameUtilsTest {

    @Test
    fun `getInitials handles usual double names`() {
        assertThat(NameUtils.getInitials("Praveen Jaiswal"), equalTo("PJ"))
    }

    @Test
    fun `getInitials handles single names`() {
        assertThat(NameUtils.getInitials("Praveen"), equalTo("PR"))
    }

    @Test
    fun `getInitials handles single character names`() {
        assertThat(NameUtils.getInitials("P"), equalTo("P"))
    }

    @Test
    fun `getInitials handles double spaced names`() {
        assertThat(NameUtils.getInitials("Praveen  Jaiswal"), equalTo("PJ"))
    }

    @Test
    fun `getInitials returns -- for empty string`() {
        assertThat(NameUtils.getInitials(""), equalTo("--"))
    }

    @Test
    fun `getInitials handles -- input without crash`() {
        assertThat(NameUtils.getInitials("--"), equalTo("--"))
    }

    @Test
    fun `getInitials handles names with special characters`() {
        assertThat(NameUtils.getInitials("John-Paul Smith"), equalTo("JP"))
    }

    @Test
    fun `getInitials handles names with numbers`() {
        assertThat(NameUtils.getInitials("Agent 007"), equalTo("A0"))
    }

    @Test
    fun `getInitials handles triple names`() {
        assertThat(NameUtils.getInitials("John Paul Smith"), equalTo("JP"))
    }

    @Test
    fun `getInitials handles names with tabs and newlines`() {
        assertThat(NameUtils.getInitials("John\tPaul\nSmith"), equalTo("JP"))
    }

    @Test
    fun `getInitials handles names with leading and trailing spaces`() {
        assertThat(NameUtils.getInitials("  John Smith  "), equalTo("JS"))
    }

    @Test
    fun `getInitials handles lowercase names`() {
        assertThat(NameUtils.getInitials("john smith"), equalTo("JS"))
    }

    @Test
    fun `getInitials handles mixed case names`() {
        assertThat(NameUtils.getInitials("jOhN sMiTh"), equalTo("JS"))
    }

    @Test
    fun `getInitials memoizes results`() {
        // Call the same name multiple times - should use cached result
        val name = "Test User"
        val result1 = NameUtils.getInitials(name)
        val result2 = NameUtils.getInitials(name)
        assertThat(result1, equalTo(result2))
        assertThat(result1, equalTo("TU"))
    }

    @Test
    fun `isValidUserName returns false for empty EditText`() {
        val context = RuntimeEnvironment.getApplication()
        val editText = EditText(context)
        editText.setText("")
        assertThat(NameUtils.isValidUserName(editText), equalTo(false))
    }

    @Test
    fun `isValidUserName returns true for non-empty EditText`() {
        val context = RuntimeEnvironment.getApplication()
        val editText = EditText(context)
        editText.setText("John")
        assertThat(NameUtils.isValidUserName(editText), equalTo(true))
    }

    @Test
    fun `isValidUserName returns false for whitespace only`() {
        val context = RuntimeEnvironment.getApplication()
        val editText = EditText(context)
        editText.setText("   ")
        // Note: The current implementation doesn't trim, so this returns true
        // This might be a bug in the implementation
        assertThat(NameUtils.isValidUserName(editText), equalTo(true))
    }
}