package io.moquette;

import java.io.IOException;


public class MainApp {
    public static void main(String... args) {
        try {
            License.generateLicense(args);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

