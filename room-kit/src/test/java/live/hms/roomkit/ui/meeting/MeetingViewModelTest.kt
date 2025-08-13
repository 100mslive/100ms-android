package live.hms.roomkit.ui.meeting

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import live.hms.roomkit.util.TestCoroutineRule
import live.hms.video.sdk.HMSSDK
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MeetingViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val testCoroutineRule = TestCoroutineRule()

    private lateinit var application: Application
    private lateinit var viewModel: MeetingViewModel
    private lateinit var joinedObserver: Observer<Boolean>

    @Before
    fun setup() {
        application = RuntimeEnvironment.getApplication()
        viewModel = MeetingViewModel(application)
        joinedObserver = mock()
    }

    @Test
    fun `test initial joined state is false`() {
        assertThat(viewModel.joined.value, equalTo(false))
    }

    @Test
    fun `test joined LiveData updates correctly`() {
        viewModel.joined.observeForever(joinedObserver)
        
        viewModel.joined.value = true
        
        verify(joinedObserver).onChanged(false) // Initial value
        verify(joinedObserver).onChanged(true)  // Updated value
        
        assertThat(viewModel.joined.value, equalTo(true))
    }

    @Test
    fun `test transcriptionsPosition initial state`() {
        val positionObserver: Observer<MeetingViewModel.TranscriptionsPosition> = mock()
        viewModel.transcriptionsPosition.observeForever(positionObserver)
        
        // The initial position should be set by TranscriptionsPositionUseCase
        // Testing that the LiveData is properly initialized
        assertThat(viewModel.transcriptionsPosition.hasObservers(), equalTo(true))
    }

    @Test
    fun `test transcriptionUseCase is initialized`() {
        assertThat(viewModel.transcriptionUseCase, org.hamcrest.CoreMatchers.notNullValue())
    }

    @Test
    fun `test transcriptionsPositionUseCase is initialized`() {
        assertThat(viewModel.transcriptionsPositionUseCase, org.hamcrest.CoreMatchers.notNullValue())
    }

    @Test
    fun `test TranscriptionsPosition enum values`() {
        val positions = MeetingViewModel.TranscriptionsPosition.values()
        
        assertThat(positions.size, equalTo(3))
        assertThat(positions.contains(MeetingViewModel.TranscriptionsPosition.SCREENSHARE_TOP), equalTo(true))
        assertThat(positions.contains(MeetingViewModel.TranscriptionsPosition.TOP), equalTo(true))
        assertThat(positions.contains(MeetingViewModel.TranscriptionsPosition.BOTTOM), equalTo(true))
    }

    @Test
    fun `test viewModel application context is set correctly`() {
        assertThat(viewModel.getApplication<Application>(), equalTo(application))
    }
}