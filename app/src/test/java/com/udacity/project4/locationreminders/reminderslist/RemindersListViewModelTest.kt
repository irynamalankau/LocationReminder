package com.udacity.project4.locationreminders.reminderslist

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.O])
@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    // Subject under test
    private lateinit var remindersListViewModel: RemindersListViewModel

    // Use a fake data source to be injected into the view model.
    private lateinit var fakeDataSource: FakeDataSource

    private val remindersList = mutableListOf(
        ReminderDTO(
            title = "title",
            description = "description",
            location = "location",
            latitude = 42.08780700912105,
            longitude = -87.78266481484444
        ), ReminderDTO(
            title = "title",
            description = "description",
            location = "location",
            latitude = 42.08780700912105,
            longitude = -87.78266481484444
        )
    )

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setupViewModel() {
        stopKoin()
        fakeDataSource = FakeDataSource(remindersList)
        remindersListViewModel =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
    }

    @Test
    fun loadReminders_loading() = runBlockingTest {
        // Pause dispatcher so you can verify initial values.
        mainCoroutineRule.pauseDispatcher()

        // Load the reminder in the view model.
        remindersListViewModel.loadReminders()

        // Then assert that the progress indicator is shown.
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(true))

        // Execute pending coroutines actions.
        mainCoroutineRule.resumeDispatcher()

        // Then assert that the progress indicator is hidden.
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false))
        assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun loadReminders_dataSourceReturnsError_showSnackBarErrorMessage() = runBlockingTest {
        fakeDataSource.setReturnError(true)
        remindersListViewModel.loadReminders()

        assertThat(
            remindersListViewModel.showSnackBar.getOrAwaitValue(),
            `is`("Reminders not found")
        )
    }

    @Test
    fun invalidateShowNoData_emptyDataSource_showNoDataTrue() = runBlockingTest{
        fakeDataSource.deleteAllReminders()
        remindersListViewModel.loadReminders()
        assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is`(true))
    }
}