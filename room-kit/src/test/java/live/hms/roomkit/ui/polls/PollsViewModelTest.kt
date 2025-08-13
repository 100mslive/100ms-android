package live.hms.roomkit.ui.polls

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test

class PollsViewModelTest {

    private lateinit var viewModel: PollsViewModel

    @Before
    fun setup() {
        viewModel = PollsViewModel()
    }

    @Test
    fun `test initial poll creation info has default values`() {
        val info = viewModel.getPollsCreationInfo()
        
        assertThat(info.timer, equalTo(false))
        assertThat(info.anon, equalTo(false))
        assertThat(info.hideVote, equalTo(false))
        assertThat(info.isPoll, equalTo(false))
        assertThat(info.pollTitle, equalTo(""))
    }

    @Test
    fun `test setTitle updates poll title`() {
        val title = "Test Poll Title"
        viewModel.setTitle(title)
        
        val info = viewModel.getPollsCreationInfo()
        assertThat(info.pollTitle, equalTo(title))
    }

    @Test
    fun `test setTimer updates timer status`() {
        viewModel.setTimer(true)
        
        val info = viewModel.getPollsCreationInfo()
        assertThat(info.timer, equalTo(true))
        
        viewModel.setTimer(false)
        
        val updatedInfo = viewModel.getPollsCreationInfo()
        assertThat(updatedInfo.timer, equalTo(false))
    }

    @Test
    fun `test isAnon updates anonymous status`() {
        viewModel.isAnon(true)
        
        val info = viewModel.getPollsCreationInfo()
        assertThat(info.anon, equalTo(true))
        
        viewModel.isAnon(false)
        
        val updatedInfo = viewModel.getPollsCreationInfo()
        assertThat(updatedInfo.anon, equalTo(false))
    }

    @Test
    fun `test markHideVoteCount updates hide vote status`() {
        viewModel.markHideVoteCount(true)
        
        val info = viewModel.getPollsCreationInfo()
        assertThat(info.hideVote, equalTo(true))
        
        viewModel.markHideVoteCount(false)
        
        val updatedInfo = viewModel.getPollsCreationInfo()
        assertThat(updatedInfo.hideVote, equalTo(false))
    }

    @Test
    fun `test setPollOrQuiz updates poll type`() {
        viewModel.setPollOrQuiz(true)
        
        val info = viewModel.getPollsCreationInfo()
        assertThat(info.isPoll, equalTo(true))
        
        viewModel.setPollOrQuiz(false)
        
        val updatedInfo = viewModel.getPollsCreationInfo()
        assertThat(updatedInfo.isPoll, equalTo(false))
    }

    @Test
    fun `test isPoll returns correct poll status`() {
        assertThat(viewModel.isPoll(), equalTo(false))
        
        viewModel.setPollOrQuiz(true)
        assertThat(viewModel.isPoll(), equalTo(true))
        
        viewModel.setPollOrQuiz(false)
        assertThat(viewModel.isPoll(), equalTo(false))
    }

    @Test
    fun `test multiple property updates preserve other values`() {
        viewModel.setTitle("Poll 1")
        viewModel.setTimer(true)
        viewModel.isAnon(true)
        
        val info1 = viewModel.getPollsCreationInfo()
        assertThat(info1.pollTitle, equalTo("Poll 1"))
        assertThat(info1.timer, equalTo(true))
        assertThat(info1.anon, equalTo(true))
        assertThat(info1.hideVote, equalTo(false))
        assertThat(info1.isPoll, equalTo(false))
        
        // Update one property
        viewModel.markHideVoteCount(true)
        
        val info2 = viewModel.getPollsCreationInfo()
        // Previous values should be preserved
        assertThat(info2.pollTitle, equalTo("Poll 1"))
        assertThat(info2.timer, equalTo(true))
        assertThat(info2.anon, equalTo(true))
        // New value should be updated
        assertThat(info2.hideVote, equalTo(true))
        assertThat(info2.isPoll, equalTo(false))
    }

    @Test
    fun `test setting all properties`() {
        viewModel.setTitle("Complete Poll")
        viewModel.setTimer(true)
        viewModel.isAnon(true)
        viewModel.markHideVoteCount(true)
        viewModel.setPollOrQuiz(true)
        
        val info = viewModel.getPollsCreationInfo()
        assertThat(info.pollTitle, equalTo("Complete Poll"))
        assertThat(info.timer, equalTo(true))
        assertThat(info.anon, equalTo(true))
        assertThat(info.hideVote, equalTo(true))
        assertThat(info.isPoll, equalTo(true))
    }

    @Test
    fun `test empty title sets empty string`() {
        viewModel.setTitle("Initial")
        viewModel.setTitle("")
        
        val info = viewModel.getPollsCreationInfo()
        assertThat(info.pollTitle, equalTo(""))
    }

    @Test
    fun `test long title is preserved correctly`() {
        val longTitle = "This is a very long poll title that contains many words and characters to test if the viewmodel correctly handles long strings without any issues"
        viewModel.setTitle(longTitle)
        
        val info = viewModel.getPollsCreationInfo()
        assertThat(info.pollTitle, equalTo(longTitle))
    }
}