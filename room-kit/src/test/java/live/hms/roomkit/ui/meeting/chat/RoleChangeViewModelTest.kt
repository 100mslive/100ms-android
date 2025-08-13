package live.hms.roomkit.ui.meeting.chat

import android.content.DialogInterface
import android.view.View
import android.widget.AdapterView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import live.hms.video.sdk.models.role.HMSRole
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*

class RoleChangeViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: RoleChangeViewModel
    private lateinit var getRolesMock: () -> List<HMSRole>
    private lateinit var bulkRoleChangeMock: (HMSRole, List<HMSRole>) -> Unit
    private lateinit var role1: HMSRole
    private lateinit var role2: HMSRole
    private lateinit var role3: HMSRole

    @Before
    fun setup() {
        role1 = mock()
        role2 = mock()
        role3 = mock()
        
        whenever(role1.name).thenReturn("host")
        whenever(role2.name).thenReturn("guest")
        whenever(role3.name).thenReturn("viewer")
        
        val rolesList = listOf(role1, role2, role3)
        
        getRolesMock = { rolesList }
        bulkRoleChangeMock = mock()
        
        viewModel = RoleChangeViewModel(getRolesMock, bulkRoleChangeMock)
    }

    @Test
    fun `test rolesList returns correct roles`() {
        val roles = viewModel.rolesList
        
        assertThat(roles.size, equalTo(3))
        assertThat(roles[0].name, equalTo("host"))
        assertThat(roles[1].name, equalTo("guest"))
        assertThat(roles[2].name, equalTo("viewer"))
    }

    @Test
    fun `test rolesList is lazy and cached`() {
        val firstCall = viewModel.rolesList
        val secondCall = viewModel.rolesList
        
        // Both calls should return the same instance (lazy initialization)
        assertThat(firstCall === secondCall, equalTo(true))
    }

    @Test
    fun `test onItemSelected updates selected role`() {
        val adapter: AdapterView<*> = mock()
        val rolePresenter = RolePresenter(role1)
        whenever(adapter.getItemAtPosition(0)).thenReturn(rolePresenter)
        
        viewModel.onItemSelected(adapter, null, 0, 0L)
        
        // The selected role should be updated
        // Note: We can't directly access selectedRole as it's private
        // but we can verify it through changeRoles behavior
    }

    @Test
    fun `test onClick adds role when checked`() {
        val dialog: DialogInterface = mock()
        
        viewModel.onClick(dialog, 0, true)  // Check "host"
        viewModel.onClick(dialog, 1, true)  // Check "guest"
        
        viewModel.rolesToChangeSelected()
        
        val selectedRoles = viewModel.selectedRolesToChange.value
        assertThat(selectedRoles?.contains("host"), equalTo(true))
        assertThat(selectedRoles?.contains("guest"), equalTo(true))
    }

    @Test
    fun `test onClick removes role when unchecked`() {
        val dialog: DialogInterface = mock()
        
        viewModel.onClick(dialog, 0, true)   // Check "host"
        viewModel.onClick(dialog, 1, true)   // Check "guest"
        viewModel.onClick(dialog, 0, false)  // Uncheck "host"
        
        viewModel.rolesToChangeSelected()
        
        val selectedRoles = viewModel.selectedRolesToChange.value
        assertThat(selectedRoles?.contains("host"), equalTo(false))
        assertThat(selectedRoles?.contains("guest"), equalTo(true))
    }

    @Test
    fun `test rolesToChangeSelected formats roles correctly`() {
        val dialog: DialogInterface = mock()
        val observer: Observer<String> = mock()
        
        viewModel.selectedRolesToChange.observeForever(observer)
        
        viewModel.onClick(dialog, 0, true)  // Check "host"
        viewModel.onClick(dialog, 2, true)  // Check "viewer"
        
        viewModel.rolesToChangeSelected()
        
        verify(observer).onChanged("host, viewer")
    }

    @Test
    fun `test rolesToChangeSelected with no selection`() {
        val observer: Observer<String> = mock()
        viewModel.selectedRolesToChange.observeForever(observer)
        
        viewModel.rolesToChangeSelected()
        
        verify(observer).onChanged("")
    }

    @Test
    fun `test changeRoles calls bulkRoleChange with correct parameters`() {
        val adapter: AdapterView<*> = mock()
        val rolePresenter = RolePresenter(role1)
        val dialog: DialogInterface = mock()
        
        whenever(adapter.getItemAtPosition(0)).thenReturn(rolePresenter)
        
        // Select target role
        viewModel.onItemSelected(adapter, null, 0, 0L)
        
        // Select roles to change
        viewModel.onClick(dialog, 1, true)  // guest
        viewModel.onClick(dialog, 2, true)  // viewer
        
        // Execute role change
        viewModel.changeRoles()
        
        // Verify bulkRoleChange was called with correct parameters
        verify(bulkRoleChangeMock).invoke(
            eq(role1),  // target role (host)
            argThat { 
                this.size == 2 && 
                this.any { it.name == "guest" } && 
                this.any { it.name == "viewer" }
            }
        )
    }

    @Test
    fun `test onNothingSelected does nothing`() {
        val adapter: AdapterView<*> = mock()
        
        // Should not throw any exception
        viewModel.onNothingSelected(adapter)
        
        // No interactions expected
        verifyNoInteractions(bulkRoleChangeMock)
    }

    @Test
    fun `test multiple selections and deselections`() {
        val dialog: DialogInterface = mock()
        
        // Add all roles
        viewModel.onClick(dialog, 0, true)
        viewModel.onClick(dialog, 1, true)
        viewModel.onClick(dialog, 2, true)
        
        viewModel.rolesToChangeSelected()
        var selectedRoles = viewModel.selectedRolesToChange.value
        assertThat(selectedRoles?.split(", ")?.size, equalTo(3))
        
        // Remove all roles
        viewModel.onClick(dialog, 0, false)
        viewModel.onClick(dialog, 1, false)
        viewModel.onClick(dialog, 2, false)
        
        viewModel.rolesToChangeSelected()
        selectedRoles = viewModel.selectedRolesToChange.value
        assertThat(selectedRoles, equalTo(""))
    }
}