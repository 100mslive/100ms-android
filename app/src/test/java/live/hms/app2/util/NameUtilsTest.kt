package live.hms.app2.util

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert
import org.junit.Test

class NameUtilsTest {

    @Test
    fun `handles usual double names`() {
        MatcherAssert.assertThat(NameUtils.getInitials("Praveen Jaiswal"), equalTo("PJ"))
    }

    @Test
    fun `handles single names`() {
        MatcherAssert.assertThat(NameUtils.getInitials("Praveen"), equalTo("PR"))
    }

    @Test
    fun `handles single character names`() {
        MatcherAssert.assertThat(NameUtils.getInitials("P"), equalTo("P"))
    }


    @Test
    fun `handles double spaced names`() {
        MatcherAssert.assertThat(NameUtils.getInitials("Praveen  Jaiswal"), equalTo("PJ"))
    }

    @Test
    fun `empty returns --`() {
        MatcherAssert.assertThat(NameUtils.getInitials(""), equalTo("--"))
    }

    @Test
    fun `-- does not crash`(){
        MatcherAssert.assertThat(NameUtils.getInitials("--"), equalTo("--"))
    }
}