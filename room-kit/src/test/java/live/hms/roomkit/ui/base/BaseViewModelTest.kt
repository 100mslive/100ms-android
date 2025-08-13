package live.hms.roomkit.ui.base

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import live.hms.roomkit.util.TestCoroutineRule
import org.junit.Rule

/**
 * Base class for ViewModel tests that provides common test rules and utilities
 */
@ExperimentalCoroutinesApi
abstract class BaseViewModelTest {

    /**
     * Rule to make LiveData execute synchronously in tests
     */
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    /**
     * Rule to handle coroutines in tests
     */
    @get:Rule
    val testCoroutineRule = TestCoroutineRule()

    /**
     * Helper function to get value from LiveData synchronously
     */
    protected fun <T> androidx.lifecycle.LiveData<T>.getOrAwaitValue(): T? {
        var data: T? = null
        val observer = androidx.lifecycle.Observer<T> { o ->
            data = o
        }
        this.observeForever(observer)
        this.removeObserver(observer)
        return data
    }

    /**
     * Helper function to test LiveData observers
     */
    protected fun <T> androidx.lifecycle.LiveData<T>.observeForTesting(block: () -> Unit) {
        val observer = androidx.lifecycle.Observer<T> { }
        try {
            observeForever(observer)
            block()
        } finally {
            removeObserver(observer)
        }
    }
}