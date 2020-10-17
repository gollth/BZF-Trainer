package de.tgoll.projects.bzf;

import android.content.Context;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4ClassRunner.class)
public class SimulatorTests {

    private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz".toCharArray();
    static String randomWord(int length) {
        StringBuilder result = new StringBuilder();
        Random rng = new Random();
        for (int i = 0; i < length; i++) {
            int index = rng.nextInt(ALPHABET.length);
            result.append(ALPHABET[index]);
        }
        return result.toString();
    }

    private Context context;

    @Before
    public void Setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void resolveParamsReplacesMetaAirportVariable() {
        String needle = randomWord(5);
        String pre = randomWord(10);
        String post = randomWord(10);
        Phrase.Params.AIRPORT = needle;
        assertThat(Phrase.resolveParams(pre + "#airport" + post), containsString(needle));
    }
    @Test
    public void resolveParamsReplacesMetaAircraftVariable() {
        String needle = randomWord(5);
        String pre = randomWord(10);
        String post = randomWord(10);
        Phrase.Params.AIRCRAFT = needle;
        assertThat(Phrase.resolveParams(pre + "#aircraft" + post), containsString(needle));
    }
    @Test
    public void resolveParamsReplacesMetaAltitudeVariable() {
        String needle = randomWord(5);
        String pre = randomWord(10);
        String post = randomWord(10);
        Phrase.Params.ALTITUDE = needle;
        assertThat(Phrase.resolveParams(pre + "#altitude" + post), containsString(needle));
    }
    @Test
    public void resolveParamsReplacesMetaCallsignVariable() {
        String needle = randomWord(5);
        String pre = randomWord(10);
        String post = randomWord(10);
        Phrase.Params.CALLSIGN = needle;
        assertThat(Phrase.resolveParams(pre + "#callsign" + post), containsString(needle));
    }
    @Test
    public void resolveParamsReplacesMetaAtisVariable() {
        String needle = randomWord(5);
        String pre = randomWord(10);
        String post = randomWord(10);
        Phrase.Params.ATIS = needle;
        assertThat(Phrase.resolveParams(pre + "#atis" + post), containsString(needle));
    }
    @Test
    public void resolveParamsReplacesMetaQnhVariable() {
        String needle = randomWord(5);
        String pre = randomWord(10);
        String post = randomWord(10);
        Phrase.Params.QNH = needle;
        assertThat(Phrase.resolveParams(pre + "#qnh" + post), containsString(needle));
    }
    @Test
    public void resolveParamsReplacesMetaRunwayVariable() {
        String needle = randomWord(5);
        String pre = randomWord(10);
        String post = randomWord(10);
        Phrase.Params.RUNWAY = needle;
        assertThat(Phrase.resolveParams(pre + "#runway" + post), containsString(needle));
    }
    @Test
    public void resolveParamsReplacesMetaRunway2Variable() {
        String needle = randomWord(5);
        String pre = randomWord(10);
        String post = randomWord(10);
        Phrase.Params.RUNWAY2 = needle;
        assertThat(Phrase.resolveParams(pre + "#runway2" + post), containsString(needle));
    }
    @Test
    public void resolveParamsReplacesMetaTaxiRouteVariable() {
        String needle = randomWord(5);
        String pre = randomWord(10);
        String post = randomWord(10);
        Phrase.Params.TAXI_ROUTE = needle;
        assertThat(Phrase.resolveParams(pre + "#taxi_route" + post), containsString(needle));
    }
    @Test
    public void resolveParamsReplacesMetaFreqVariable() {
        String needle = randomWord(5);
        String pre = randomWord(10);
        String post = randomWord(10);
        Phrase.Params.FREQ = needle;
        assertThat(Phrase.resolveParams(pre + "#freq" + post), containsString(needle));
    }
    @Test
    public void resolveParamsReplacesMetaFixpointVariable() {
        String needle = randomWord(5);
        String pre = randomWord(10);
        String post = randomWord(10);
        Phrase.Params.FIXPOINT = needle;
        assertThat(Phrase.resolveParams(pre + "#fixpoint" + post), containsString(needle));
    }
    @Test
    public void resolveParamsReplacesMetaSquawkVariable() {
        String needle = randomWord(5);
        String pre = randomWord(10);
        String post = randomWord(10);
        Phrase.Params.SQUAWK = needle;
        assertThat(Phrase.resolveParams(pre + "#squawk" + post), containsString(needle));
    }
    @Test
    public void resolveParamsReplacesMetaWindDirVariable() {
        String needle = randomWord(5);
        String pre = randomWord(10);
        String post = randomWord(10);
        Phrase.Params.WIND_DIR = needle;
        assertThat(Phrase.resolveParams(pre + "#wind_dir" + post), containsString(needle));
    }
    @Test
    public void resolveParamsReplacesMetaWindKnVariable() {
        String needle = randomWord(5);
        String pre = randomWord(10);
        String post = randomWord(10);
        Phrase.Params.WIND_KN = needle;
        assertThat(Phrase.resolveParams(pre + "#wind_kn" + post), containsString(needle));
    }

    @Test
    public void convertABCPronouncesLetterByLetterInEnglish() {
        Phrase.initialize(context, true);
        // Test all letters which are pronounced differently in german as well
        assertEquals("", Phrase.convertABC(""));
        assertEquals("hotel echo lima lima oscar whiskey quebec romeo lima delta", Phrase.convertABC("HeLlOWqRlD"));
    }

    @Test
    public void convertABCPronouncesLetterByLetterInGerman() {
        Phrase.initialize(context, false);
        // Test all letters which are pronounced differently in german as well
        assertEquals("", Phrase.convertABC(""));
        assertEquals("hotel ecko lima lima oscar wiski kebeck romeo lima delta", Phrase.convertABC("HeLlOWqRlD"));
    }

    @Test
    public void convertNumberPronouncesSimpleNumberOneByOneInEnglish() {
        Phrase.initialize(context, true);
        assertEquals("two", Phrase.convertNumber("2"));
        assertEquals("five", Phrase.convertNumber("5"));
        assertEquals("niner", Phrase.convertNumber("9"));
    }

    @Test
    public void convertNumberPronouncesSimpleNumberOneByOneInGerman() {
        Phrase.initialize(context, false);
        assertEquals("zwo", Phrase.convertNumber("2"));
        assertEquals("fünf", Phrase.convertNumber("5"));
        assertEquals("neun", Phrase.convertNumber("9"));
    }

    @Test
    public void convertNumberAbbreviatesHundredthsCorrectlyInEnglish() {
        Phrase.initialize(context, true);
        assertEquals("one hundred", Phrase.convertNumber("100"));
        assertEquals("niner hundred", Phrase.convertNumber("900"));
        assertEquals("two six zero", Phrase.convertNumber("260"));
    }

    @Test
    public void convertNumberAbbreviatesHundredthsCorrectlyInGerman() {
        Phrase.initialize(context, false);
        assertEquals("ein hundert", Phrase.convertNumber("100"));
        assertEquals("zwo hundert", Phrase.convertNumber("200"));
        assertEquals("zwo sechs null", Phrase.convertNumber("260"));
    }

    @Test
    public void convertNumberAbbreviatesThousandsCorrectlyInEnglish() {
        Phrase.initialize(context, true);
        assertEquals("one thousand", Phrase.convertNumber("1000"));
        assertEquals("niner thousand five hundred", Phrase.convertNumber("9500"));
        assertEquals("two six eight zero", Phrase.convertNumber("2680"));
    }

    @Test
    public void convertNumberAbbreviatesThousandssCorrectlyInGerman() {
        Phrase.initialize(context, false);
        assertEquals("ein tausend", Phrase.convertNumber("1000"));
        assertEquals("zwo tausend fünf hundert", Phrase.convertNumber("2500"));
        assertEquals("zwo sechs acht null", Phrase.convertNumber("2680"));
    }

    @Test
    public void convertNumberPronouncesDecimalsCorrectlyInEnglish() {
        Phrase.initialize(context, true);
        assertEquals("one two three decimal four niner seven", Phrase.convertNumber("123.497"));
    }

    @Test
    public void convertNumberPronouncesDecimalsCorrectlyInGerman() {
        Phrase.initialize(context, false);
        assertEquals("eins zwo drei punkt vier neun sieben", Phrase.convertNumber("123.497"));
    }

    @Test
    public void convertAirportPronouncesAllAirportsByNameInEnglish() {
        Phrase.initialize(context, true);
        assertEquals("Munich", Phrase.convertAirport("EDDM"));
        assertEquals("Frankfurt", Phrase.convertAirport("EDDF"));
        assertEquals("Berlin", Phrase.convertAirport("EDDB"));
        assertEquals("Hamburg", Phrase.convertAirport("EDDH"));
    }

    @Test
    public void convertAirportPronouncesAllAirportsByNameInGerman() {
        Phrase.initialize(context, false);
        assertEquals("München", Phrase.convertAirport("EDDM"));
        assertEquals("Frankfurt", Phrase.convertAirport("EDDF"));
        assertEquals("Berlin", Phrase.convertAirport("EDDB"));
        assertEquals("Hamburg", Phrase.convertAirport("EDDH"));
    }
}
