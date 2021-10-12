package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var remindersRepository: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    @Before
    fun setup() {
        // Using an in-memory database so that the information stored here disappears when the
        // process is killed.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        remindersRepository = RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @After
    fun cleanUp() {
        database.close()
    }

    private val reminder = ReminderDTO(
        title = "title",
        description = "description",
        location = "location",
        latitude = 42.08780700912105,
        longitude = -87.78266481484444
    )

    @Test
    fun saveReminder_retrieveReminder() = runBlocking {
        // GIVEN - A new reminder saved in the database.
        remindersRepository.saveReminder(reminder)

        // WHEN - Reminder retrieved by ID.
        val result = remindersRepository.getReminder(reminder.id)

        // THEN - The same reminder is returned.
        assertThat(result is Result.Success, `is`(true))
        result as Result.Success
        assertThat(result.data.title, `is`(reminder.title))
        assertThat(result.data.description, `is`(reminder.description))
        assertThat(result.data.latitude, `is`(reminder.latitude))
        assertThat(result.data.longitude, `is`(reminder.longitude))
        assertThat(result.data.location, `is`(reminder.location))
    }

    @Test
    fun deleteAllReminders_getReminderById_getError() = runBlocking {
        // GIVEN - All reminders are deleted from the database
        remindersRepository.saveReminder(reminder)
        remindersRepository.deleteAllReminders()

        // WHEN - Get the reminder by id from the database
        val result = remindersRepository.getReminder(reminder.id)

        // THEN - The error message is received
        assertThat(result is Result.Error, `is`(true))
        result as Result.Error
        assertThat(result.message, `is`("Reminder not found!"))
    }

}