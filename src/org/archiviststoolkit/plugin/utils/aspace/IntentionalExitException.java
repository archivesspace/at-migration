package org.archiviststoolkit.plugin.utils.aspace;

/**
 * this exists as an easy way to immediately end the copy process and save URI maps when a problem is identified
 *
 * @author sarah morrissey
 * @version 2.2
 */
public class IntentionalExitException extends Exception {

    public IntentionalExitException() {
        super("Exiting migration ...");
    }

    public IntentionalExitException(String message) {super(message);}
}
