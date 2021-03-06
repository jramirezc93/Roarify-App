package com.polimi.jaj.roarify;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.rule.ActivityTestRule;

import com.polimi.jaj.roarify.activity.HomeActivity;

import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerMatchers.isOpen;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;

/**
 * Created by Jorge on 15/02/2017.
 */
/*This test tests the "Delete own messages" option in Settings by creating three messages and the deleting all the messages created by
* the tester.*/

public class DeleteOwnMessagesTest {

    @Rule
    public ActivityTestRule activityRule = new ActivityTestRule<>(
            HomeActivity.class);

    @Test
    public void uploadMessage(){

        //Create own messages
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
        onView(withId(R.id.drawer_layout)).check(matches(isOpen()));
        onView(withText(R.string.home)).perform(click());
        //Message1
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.new_message)).perform(typeText("My message 1"));
        onView(withText("Roar!")).perform(click());
        //Message2
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.new_message)).perform(typeText("My message 2"));
        onView(withText("Roar!")).perform(click());
        //Message3
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.new_message)).perform(typeText("My message "));
        onView(withText("Roar!")).perform(click());

        //Open "three dots menu", click settings and delete all own messages.
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());
        onView(withText(R.string.action_settings)).perform(click());
        onView(withText(R.string.title_delete_messages)).perform(click());
        onView(withId(android.R.id.button1)).perform(click());

    }
}
