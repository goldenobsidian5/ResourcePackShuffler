package com.obsidian5.resourcepackshuffler.utils;

public class Version {

    public static int getPackFormat(int mcVersion) {
        if (mcVersion <= 8)
            return 1;

        if (mcVersion <= 10)
            return 2;

        if (mcVersion <= 12)
            return 3;

        if (mcVersion <= 14)
            return 4;

        if (mcVersion == 15)
            return 5;

        if (mcVersion == 16)
            return 6;

        if (mcVersion == 17)
            return 7;

        if (mcVersion == 18)
            return 8;

        return mcVersion - 10;
    }
}
