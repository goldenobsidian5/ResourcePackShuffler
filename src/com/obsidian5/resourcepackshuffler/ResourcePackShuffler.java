package com.obsidian5.resourcepackshuffler;

import com.obsidian5.resourcepackshuffler.utils.FileUtils;
import com.obsidian5.resourcepackshuffler.utils.mojang.Index;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class ResourcePackShuffler {

    public static int mcVersion = 18; // Use the middle number of the version, found here: Minecraft 1.(18).2

    public static void main(String[] args) {
        double timeTaken = System.currentTimeMillis();

        if (args.length > 1 && (args[0].equalsIgnoreCase("--version") || args[0].equalsIgnoreCase("-v"))) {
            try {
                mcVersion = Integer.parseInt(args[1]);
            } catch (Exception ignored) {

            }
        }

        log("Generating shuffled resource pack for Minecraft 1." + mcVersion + "...");

        if (!FileUtils.startup())
            return;

        log("Shuffling images...");

        if (!shuffleImages())
            return;

        log("Shuffling sounds...");

        if (!shuffleSounds())
            return;

        timeTaken = (System.currentTimeMillis() - timeTaken) / 1000;
        log("Done (" + timeTaken + "s)!");
    }

    private static boolean shuffleImages() {
        // get default assets to temp assets folder

        log("Extracting assets from jar... (This may take a while)");

        try {
            FileUtils.unzip(FileUtils.getJarFile(), FileUtils.getTempFolder());
        } catch (IOException exception) {
            quit("unzip jar file");
            exception.printStackTrace();
            return false;
        }

        // get default locations
        List<File> defaultPngFiles = FileUtils.getPngFiles(FileUtils.getAssetsFolder());

        if (defaultPngFiles == null) {
            quit("get Minecraft assets");
            return false;
        }

        List<BufferedImage> defaultBufferedImages;
        try {
            defaultBufferedImages = FileUtils.getPngData(defaultPngFiles);
        } catch (IOException exception) {
            quit("get PNG data");
            exception.printStackTrace();
            return false;
        }

        // move the file to resource pack spot
        for (File file : defaultPngFiles) {
            log("Moving file \"" + file.getName() + "\" to pack...");

            String oldPath = file.getAbsolutePath();
            String newPath = oldPath.replaceFirst("\\\\Shuffle\\\\temp\\\\", "\\\\Shuffle\\\\");

            try {
                Files.move(Paths.get(oldPath), Paths.get(newPath)); // TODO NO SUCH FILE EXCEPTION??? this doesn't work at ALL
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }

        // write a random different image on top of it
        Random random = new Random();
        for (File file : defaultPngFiles) {
            log("Assigning random image to \"" + file.getName() + "\"...");

            BufferedImage image = null;
            if (defaultBufferedImages.size() > 0) {
                int randomSpot = 0;

                try {
                    randomSpot = random.nextInt(defaultBufferedImages.size() - 1);
                } catch (IllegalArgumentException ignored) {

                }

                image = defaultBufferedImages.get(randomSpot);
                defaultBufferedImages.remove(image);
            }

            if (image != null) {
                try {
                    ImageIO.write(image, "png", file);
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
        }

        return true;
    }

    private static boolean shuffleSounds() {
        Index index;

        try {
            index = (Index) FileUtils.getJsonObject(FileUtils.getIndexFile(), Index.class);
        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }

        if (index == null) {
            quit("parse index file");
            return false;
        }

        // do something
        log("oh boy sound shuffling");

        /*

        for (Map.Entry<String, IndexedFile> entry : index.objects.entrySet()) {
            String key = entry.getKey();
            IndexedFile value = entry.getValue();

            String message = "File path: \"" +
                    key +
                    "\" (hash: " +
                    value.hash +
                    ", size: " +
                    value.size +
                    ")";

            System.out.println(message);
        }

         */

        return true;
    }

    public static void quit(String unableTo) {
        log("Unable to " + unableTo + "! quitting...");
    }

    public static void log(String message) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

        String time = timeFormat.format(new Date());

        System.out.println("[" + time + "]: " + message);
    }
}
