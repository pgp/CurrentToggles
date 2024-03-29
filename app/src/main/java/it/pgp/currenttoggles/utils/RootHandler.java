package it.pgp.currenttoggles.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;

/**
 * Adapted from it.pgp.xfiles.roothelperclient.RootHandler
 */

public class RootHandler {

    public static synchronized long getPidOfProcess(Process p) {
        long pid;

        try {
            // on Android: java.lang.ProcessManager$ProcessImpl
//            if (p.getClass().getName().equals("java.lang.UNIXProcess")) {
                Field f = p.getClass().getDeclaredField("pid");
                f.setAccessible(true);
                pid = f.getLong(p);
                f.setAccessible(false);
                return pid;
//            }
        } catch (Exception e) {
            return -1;
        }
    }

    public static boolean isRootAvailableAndGranted = false;

    public static boolean isRooted() {
        return findBinary("su");
    }

    private static boolean findBinary(String binaryName) {
        boolean found = false;
        String[] places = {"/sbin/", "/system/bin/", "/system/xbin/", "/data/local/xbin/",
                "/data/local/bin/", "/system/sd/xbin/", "/system/bin/failsafe/", "/data/local/"};
        for (String where : places) {
            if ( new File( where + binaryName ).exists() ) {
                found = true;
                Log.d(RootHandler.class.getName(),"su binary found at "+where);
                break;
            }
        }
        return found;
    }

    public static Process executeCommand(String commandString, File workingDir, boolean runAsSuperUser) throws IOException {
        Process p;
        if(runAsSuperUser) {
            p = Runtime.getRuntime().exec("su");
            DataOutputStream dos = new DataOutputStream(p.getOutputStream());
            if (workingDir != null) {
                dos.writeBytes("cd " + workingDir +"\n");
            }
            dos.writeBytes(commandString + "\n");
            dos.writeBytes("exit\n");
            dos.flush();
            dos.close();
        }
        else {
            p = (workingDir==null)?
                    Runtime.getRuntime().exec(commandString):
                    Runtime.getRuntime().exec(commandString,null,workingDir);
        }
        return p; // p started, not joined
    }

    public static int executeCommandAndWaitFor(String commandString, File workingDir, boolean runAsSuperUser, StringBuilder output) throws IOException {
        Process p = executeCommand(commandString, workingDir, runAsSuperUser);

        // capture output from process
        if(output != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while((line = reader.readLine()) != null) output.append(line).append("\n");
        }

        int exitValue;
        try {
            exitValue = p.waitFor();
        }
        catch (Exception e) {
            e.printStackTrace();
            exitValue = -1;
        }

        return exitValue;
    }
}
