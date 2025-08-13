package live.hms.roomkit.util

import android.os.Looper
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ThreadUtilsTest {

    @Test
    fun `getThreadInfo returns thread information`() {
        val threadInfo = ThreadUtils.getThreadInfo()
        assertThat(threadInfo.contains("@[name="), equalTo(true))
        assertThat(threadInfo.contains("id="), equalTo(true))
    }

    @Test
    fun `getThreadInfo includes current thread name`() {
        val currentThreadName = Thread.currentThread().name
        val threadInfo = ThreadUtils.getThreadInfo()
        assertThat(threadInfo, containsString(currentThreadName))
    }

    @Test
    fun `getThreadInfo includes current thread id`() {
        val currentThreadId = Thread.currentThread().id
        val threadInfo = ThreadUtils.getThreadInfo()
        assertThat(threadInfo, containsString(currentThreadId.toString()))
    }

    @Test(expected = IllegalStateException::class)
    fun `checkIsOnMainThread throws when not on main thread`() {
        // In Robolectric tests, we're not on the main thread by default
        ThreadUtils.checkIsOnMainThread()
    }

    @Test
    fun `checkIsOnMainThread succeeds when on main thread`() {
        // Simulate being on the main thread
        shadowOf(Looper.getMainLooper()).idle()
        
        // Run on main thread
        val mainLooper = Looper.getMainLooper()
        mainLooper.queue.addIdleHandler {
            ThreadUtils.checkIsOnMainThread()
            false
        }
    }

    @Test
    fun `getThreadInfo format is correct`() {
        val threadInfo = ThreadUtils.getThreadInfo()
        val pattern = "@\\[name=.+, id=\\d+\\]".toRegex()
        assertThat(pattern.matches(threadInfo), equalTo(true))
    }
}