package org.archiviststoolkit.plugin.utils.aspace;

public class IntentionalExitException extends Exception {

    public IntentionalExitException() {
        super("Exiting migration ...");
    }

    public IntentionalExitException(String message) {super(message);}
}
