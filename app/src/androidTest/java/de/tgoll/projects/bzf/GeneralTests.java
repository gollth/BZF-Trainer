package de.tgoll.projects.bzf;

import android.content.Context;
import android.graphics.Color;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4ClassRunner.class)
public class GeneralTests {

    private Context context;

    @Before
    public void Setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void useAppContext() {
        // Context of the app under test.
        assertEquals("de.tgoll.projects.bzf", context.getPackageName());
    }

    @Test
    public void desaturateColorCompletely() {
        int originalColor = Color.HSVToColor(new float[]{ 0.123f, 0.8f, 0.5f });
        int desaturated = Util.desaturate(originalColor);
        assertEquals(desaturated, Color.HSVToColor(new float[]{ 0.123f, 0, 0.5f }));
    }

    @Test
    public void desaturateColorPartially() {
        int originalColor = Color.HSVToColor(new float[]{ 0.456f, 0.75f, 0.25f });
        int desaturated = Util.desaturate(originalColor, 0.4f);
        assertEquals(desaturated, Color.HSVToColor(new float[] { 0.456f, 0.4f, 0.25f }));
    }

    @Test
    public void lookupColor() {
        int black = Util.lookupColor(context, R.color.black);
        int NO_ALPHA_MASK = 0x00FFFFFF;
        assertEquals(black, Color.rgb(0,0,0) & NO_ALPHA_MASK);
    }

    @Test
    public void saturate() {
        assertEquals(Util.saturate(0, 0, 1), 0, 0);
        assertEquals(Util.saturate(1, 0, 1), 1, 0);
        assertEquals(Util.saturate(5, 0, 1), 1, 0);
        assertEquals(Util.saturate(-1, 0, 1), 0, 0);
    }
}