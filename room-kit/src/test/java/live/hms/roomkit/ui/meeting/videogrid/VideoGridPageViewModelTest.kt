package live.hms.roomkit.ui.meeting.videogrid

import live.hms.roomkit.ui.meeting.MeetingTrack
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class VideoGridPageViewModelTest {

    private lateinit var viewModel: VideoGridPageViewModel

    @Before
    fun setup() {
        viewModel = VideoGridPageViewModel()
    }

    @Test
    fun `test initial videos array is empty`() {
        assertThat(viewModel.initialVideos.size, equalTo(0))
    }

    @Test
    fun `test initial onVideoItemClick is null`() {
        assertThat(viewModel.onVideoItemClick, nullValue())
    }

    @Test
    fun `test setting initialVideos`() {
        val track1: MeetingTrack = mock()
        val track2: MeetingTrack = mock()
        val tracks = arrayOf(track1, track2)
        
        viewModel.initialVideos = tracks
        
        assertThat(viewModel.initialVideos.size, equalTo(2))
        assertThat(viewModel.initialVideos[0], equalTo(track1))
        assertThat(viewModel.initialVideos[1], equalTo(track2))
    }

    @Test
    fun `test setting and invoking onVideoItemClick`() {
        val mockTrack: MeetingTrack = mock()
        var clickedTrack: MeetingTrack? = null
        
        viewModel.onVideoItemClick = { track ->
            clickedTrack = track
        }
        
        // Invoke the click handler
        viewModel.onVideoItemClick?.invoke(mockTrack)
        
        assertThat(clickedTrack, equalTo(mockTrack))
    }

    @Test
    fun `test clearing initialVideos`() {
        val track1: MeetingTrack = mock()
        viewModel.initialVideos = arrayOf(track1)
        
        assertThat(viewModel.initialVideos.size, equalTo(1))
        
        viewModel.initialVideos = arrayOf()
        
        assertThat(viewModel.initialVideos.size, equalTo(0))
    }

    @Test
    fun `test updating onVideoItemClick handler`() {
        var firstHandlerCalled = false
        var secondHandlerCalled = false
        
        viewModel.onVideoItemClick = { _ ->
            firstHandlerCalled = true
        }
        
        viewModel.onVideoItemClick = { _ ->
            secondHandlerCalled = true
        }
        
        val mockTrack: MeetingTrack = mock()
        viewModel.onVideoItemClick?.invoke(mockTrack)
        
        assertThat(firstHandlerCalled, equalTo(false))
        assertThat(secondHandlerCalled, equalTo(true))
    }

    @Test
    fun `test setting null onVideoItemClick`() {
        viewModel.onVideoItemClick = { _ -> }
        assertThat(viewModel.onVideoItemClick != null, equalTo(true))
        
        viewModel.onVideoItemClick = null
        assertThat(viewModel.onVideoItemClick, nullValue())
    }

    @Test
    fun `test multiple video tracks assignment`() {
        val tracks1 = arrayOf<MeetingTrack>(mock(), mock())
        val tracks2 = arrayOf<MeetingTrack>(mock(), mock(), mock())
        
        viewModel.initialVideos = tracks1
        assertThat(viewModel.initialVideos.size, equalTo(2))
        
        viewModel.initialVideos = tracks2
        assertThat(viewModel.initialVideos.size, equalTo(3))
    }
}