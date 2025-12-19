package com.daclink.mydemoapplication.intent;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.widget.Button;
import android.widget.LinearLayout;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.daclink.mydemoapplication.AddUserActivity;
import com.daclink.mydemoapplication.DashboardActivity;
import com.daclink.mydemoapplication.FlashcardsActivity;
import com.daclink.mydemoapplication.R;
import com.daclink.mydemoapplication.UserListActivity;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DashboardIntentTest {

    private ActivityScenario<DashboardActivity> scenario;

    @Before
    public void setUp() {
        Intents.init();

        // Launch DashboardActivity with userId = 1 (your seeded admin user)
        scenario = ActivityScenario.launch(
                DashboardActivity.dashboardIntentFactory(
                        ApplicationProvider.getApplicationContext(),
                        1
                )
        );
    }

    @After
    public void tearDown() {
        if (scenario != null) scenario.close();
        Intents.release();
    }

    /**
     * Dashboard -> AddUserActivity
     */
    @Test
    public void clickingAddUserButton_launchesAddUserActivity() {
        onView(withId(R.id.addUserButton)).perform(click());
        intended(hasComponent(AddUserActivity.class.getName()));
    }

    /**
     * Dashboard -> UserListActivity
     */
    @Test
    public void clickingUserListButton_launchesUserListActivity() {
        onView(withId(R.id.userListButton)).perform(click());
        intended(hasComponent(UserListActivity.class.getName()));
    }

    /**
     * Dashboard -> FlashcardsActivity (click the first course button in the courseContainer)
     *
     * This avoids relying on hard-coded course names.
     */
    @Test
    public void clickingFirstCourse_launchesFlashcardsActivity() {
        scenario.onActivity(activity -> {
            LinearLayout container = activity.findViewById(R.id.courseContainer);
            Assert.assertNotNull("courseContainer not found in layout.", container);
            Assert.assertTrue("No courses found in courseContainer.", container.getChildCount() > 0);

            // Your Dashboard adds Button views to courseContainer
            Button firstCourseButton = (Button) container.getChildAt(0);
            firstCourseButton.performClick();
        });

        intended(hasComponent(FlashcardsActivity.class.getName()));
    }
}
