package org.example.Utils;

import java.util.Random;

public class UploadUtils {

    public static int generatePort(){
        int START_PORT = 50000;
        int END_PORT = 65000;
        Random random = new Random();
        return random.nextInt(END_PORT-START_PORT)+START_PORT;
    }
}
