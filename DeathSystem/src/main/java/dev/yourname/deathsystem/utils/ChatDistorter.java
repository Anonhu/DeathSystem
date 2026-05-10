package dev.yourname.deathsystem.utils;

import java.util.Random;

public class ChatDistorter {

    private static final char[] DISTORT_CHARS = {'.', ',', '*', '+', '!', '?', '~', '-'};
    private static final Random RANDOM = new Random();

    public static String distort(String text, int everyN) {
        if (text == null || text.isEmpty()) return text;

        StringBuilder sb = new StringBuilder();
        int counter = 0;

        for (char c : text.toCharArray()) {
            sb.append(c);
            counter++;

            if (counter >= everyN && c != ' ') {
                sb.append(DISTORT_CHARS[RANDOM.nextInt(DISTORT_CHARS.length)]);
                counter = 0;
            }
        }

        return sb.toString();
    }
}
