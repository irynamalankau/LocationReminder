package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorFragment
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@MediumTest
class ReminderListFragmentTest {

    private lateinit var repository: ReminderDataSource
    private val dataBindingIdlingResource = DataBindingIdlingResource()
    private lateinit var appContext: Application

    // Executes each task synchronously using Architecture Components.
    // This rule ensures that the test results happen synchronously and in a repeatable order.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun registerIdlingResource(): Unit = IdlingRegistry.getInstance().run{
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    //Unregister your Idling Resource so it can be garbage collected and does not leak any memory.
    @After
    fun unregisterIdlingResource(): Unit = IdlingRegistry.getInstance().run {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Before
    fun init() {
        stopKoin()
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                        appContext,
                        get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                        appContext,
                        get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get the real repository
        repository = GlobalContext.get().koin.get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    private val reminder = ReminderDTO(
            title = "title",
            description = "description",
            location = "location",
            latitude = 42.08780700912105,
            longitude = -87.78266481484444)


    //Test the navigation of the fragments.

    @Test
    fun navigation_clickOnFab_getsToSaveReminderFragment() {
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(scenario)

        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        // Click on Fab to get to SaveReminderFragment.
        onView(withId(R.id.addReminderFAB)).perform(click())

        // verify navigation to SaveReminderFragment
        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }

    //Test the displayed data on the UI.
    @Test
    fun addReminder_showsOnScreen() {
        runBlocking {
            repository.saveReminder(reminder)
        }
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle.EMPTY, R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(scenario)

        onView(withText(reminder.title)).check(ViewAssertions.matches(isDisplayed()))
        onView(withText(reminder.description)).check(ViewAssertions.matches(isDisplayed()))
        onView(withText(reminder.location)).check(ViewAssertions.matches(isDisplayed()))
    }

    //Add testing for the error messages.
    @Test
    fun noReminder_showsNoData() {
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle.EMPTY, R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(scenario)

        onView(withId(R.id.noDataTextView)).check(ViewAssertions.matches(isDisplayed()))
    }

}