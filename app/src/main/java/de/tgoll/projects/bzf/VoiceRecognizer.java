package de.tgoll.projects.bzf;

import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import com.google.android.material.snackbar.Snackbar;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

public class VoiceRecognizer implements RecognitionListener {

    private final SimulatorFragment simulator;
    private final View container;

    VoiceRecognizer(SimulatorFragment simulator, View container) {
        this.simulator = simulator;
        this.container = container;
    }


    @Override
    public void onBeginningOfSpeech() { }

    @Override
    public void onBufferReceived(byte[] buffer) { }

    @Override
    public void onEndOfSpeech() { }

    @Override
    public void onError(int error) {
        String message;
        Log.i("VoiceRecognizer", "onError");
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio Recording Error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Fehlende Berechtigung";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Netzwerk Error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Netzwerk Timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH: case SpeechRecognizer.ERROR_CLIENT:
                return;     // Bug in Google API, ignore these errors for now
                //message = "Nichts verstanden, bitte deutlicher oder lauter sprechen";
                //break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "Spracherkennung ausgelastet";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                //return;
                message = "Server Error";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "Leider wurde nichts verstanden";
                //simulator.onHeard("");      // TODO REMOVE BEFORE RELEASE, ONLY DEBUGGING
                break;
            default:
                message = "Das habe ich leider nicht verstanden #&8230";
                break;
        }
        Snackbar.make(container, message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onEvent(int eventType, Bundle params) { }

    @Override
    public void onPartialResults(Bundle partialResults) {
        //Log.d("VoiceRecognizer", "I heard:" + partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0));
    }

    @Override
    public void onReadyForSpeech(Bundle params) { }

    @Override
    public void onResults(Bundle results) {
        ArrayList<String> thingsYouSaid = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (thingsYouSaid == null || thingsYouSaid.size() <= 0) return;
        String msg = thingsYouSaid.get(0);
        simulator.onHeard(msg);
    }

    @Override
    public void onRmsChanged(float rmsdB) {    }
}
