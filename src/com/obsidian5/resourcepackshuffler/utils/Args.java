package com.obsidian5.resourcepackshuffler.utils;

import com.obsidian5.resourcepackshuffler.ResourcePackShuffler;

public class Args {

    public static void parse(String[] args) { // TODO add a --noShuffle arg
        if (args.length > 1 && (args[0].equalsIgnoreCase("--version") || args[0].equalsIgnoreCase("-v"))) {
            try {
                ResourcePackShuffler.mcVersion = Integer.parseInt(args[1]);
            } catch (Exception exception) {
                ResourcePackShuffler.log("Could not determine requested Minecraft version, using 1." + ResourcePackShuffler.mcVersion + " instead.");
            }
        }

        ResourcePackShuffler.setNoShuffle(false);
    }
}
