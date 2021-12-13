package com.obsidian5.resourcepackshuffler;

import com.obsidian5.resourcepackshuffler.utils.Args;
import com.obsidian5.resourcepackshuffler.utils.FileUtils;
import com.obsidian5.resourcepackshuffler.utils.mojang.Index;
import com.obsidian5.resourcepackshuffler.utils.mojang.IndexedFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ResourcePackShuffler {

    public static int mcVersion = 18; // Use the middle number of the version, found here: Minecraft 1.(18).2
    private static boolean noShuffle = false;

    private static Random random;

    private static File minecraftFolder;
    private static File packFolder;
    private static File assetsFolder;
    private static File tempFolder;
    private static File tempAssetsFolder;
    private static File objectsFolder;
    private static File jarFile;
    private static File indexFile;

    public static void setNoShuffle(boolean noSh) {
        noShuffle = noSh;
    }

    public static void main(String[] args) {
        double timeTaken = System.currentTimeMillis();

        Args.parse(args);

        log("Generating shuffled resource pack for Minecraft 1." + mcVersion + "...");

        if (!FileUtils.startup())
            return;

        random = new Random();

        minecraftFolder = FileUtils.getMinecraftFolder();
        packFolder = FileUtils.getPackFolder();
        assetsFolder = FileUtils.getAssetsFolder();
        tempFolder = FileUtils.getTempFolder();
        tempAssetsFolder = FileUtils.getTempAssetsFolder();
        objectsFolder = FileUtils.getObjectsFolder();
        jarFile = FileUtils.getJarFile();
        indexFile = FileUtils.getIndexFile();

        log("Shuffling images...");
        if (!shuffleImages())
            return;

        log("Shuffling sounds...");
        if (!shuffleSounds())
            return;

        log("Adding required pack files...");
        if (!addPackFiles())
            return;

        log("Cleaning up...");
        if (!cleanup())
            log("Failed to clean up folder! There might be some empty directories.");

        log("Zipping...");
        if (!zip())
            log("Failed to zip pack! Leaving it as a folder");

        timeTaken = (System.currentTimeMillis() - timeTaken) / 1000;
        log("Done (" + timeTaken + "s)!");
    }

    private static boolean shuffleImages() {
        // get default assets to temp assets folder
        log("Extracting assets from jar... (This may take a while)");
        try {
            FileUtils.unzip(jarFile, tempFolder);
        } catch (IOException exception) {
            quit("unzip jar file");
            exception.printStackTrace();
            return false;
        }

        // get default locations
        List<File> assetsFiles = FileUtils.getFilesInFolder(tempAssetsFolder);

        if (assetsFiles == null) {
            quit("get Minecraft assets");
            return false;
        }

        // delete anything that isn't a png from assets
        log("Stripping temp assets to only PNGs...");
        for (File file : assetsFiles) {
            if (!file.getName().endsWith(".png")) {
                assetsFiles.remove(file);

                if (file.delete())
                    log("Deleting file \"" + file.getName() + "\"...");
                else
                    log("File " + file.getAbsolutePath() + " failed to delete! Find it and delete it yourself, if you wish to save space.");
            } else
                log("Keeping file \"" + file.getName() + "\"...");
        }

        // move the assets to resource pack spot
        log("Moving assets folder to pack...");
        try {
            File src = new File(tempAssetsFolder.getPath());
            File dest = new File(assetsFolder.getPath());

            Files.move(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AccessDeniedException ignored) {

        } catch (IOException exception) {
            exception.printStackTrace();
        }

        // delete the temp folder
        log("Deleting unused assets...");
        if (!FileUtils.deleteFolder(tempFolder))
            log("Temp folder failed to delete! You might wanna delete that");

        if (noShuffle)
            return true;

        // get image data from the files
        log("Getting PNG data...");
        List<File> pngFiles = FileUtils.getFilesInFolder(assetsFolder);
        List<BufferedImage> defaultBufferedImages;
        try {
            defaultBufferedImages = FileUtils.getPngData(pngFiles);
        } catch (IOException exception) {
            quit("get PNG data");
            exception.printStackTrace();
            return false;
        }

        if (defaultBufferedImages == null)
            return false;

        // write a random different image on top of each image
        log("Shuffling images...");
        for (File file : pngFiles) {
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
        // get index
        log("Getting version assets index...");
        Index index;

        try {
            index = (Index) FileUtils.getJsonObject(indexFile, Index.class);
        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }

        if (index == null) {
            quit("parse index file");
            return false;
        }

        // remove anything that isn't a sound
        log("Stripping index objects to only sounds...");
        Map<String, IndexedFile> objects = new HashMap<>();

        for (Map.Entry<String, IndexedFile> entry : index.getObjects().entrySet()) {
            String filePath = entry.getKey();
            IndexedFile indexedFile = entry.getValue();

            if (filePath.endsWith(".ogg")) {
                objects.put(filePath, indexedFile);
            }
        }

        log(objects.toString() + "--------------");

        // get hashed sound files and their destinations
        log("Getting sound files...");
        List<File> possibleSources = new CopyOnWriteArrayList<>();
        List<File> possibleDestinations = new CopyOnWriteArrayList<>();
        int count = 0;
        int total = objects.size();
        for (Map.Entry<String, IndexedFile> entry : objects.entrySet()) {
            count++;
            String key = entry.getKey();
            IndexedFile indexedFile = entry.getValue();
            String hash = indexedFile.getHash();

            File src = new File(objectsFolder + "\\" + hash.substring(0, 2) + "\\" + indexedFile.getHash());
            File dest = new File(assetsFolder + "\\" + key);

            log("Getting sound file \"" + dest.getAbsolutePath() + "\"... (" + count + " of " + total + ")");

            possibleSources.add(src);
            possibleDestinations.add(dest);
        }

        // write a random sound to each destination
        log("Writing sounds randomly...");
        count = 0;
        total = possibleDestinations.size();
        for (File dest : possibleDestinations) {
            count++;
            File soundFile = null;
            if (possibleSources.size() > 0) {
                int randomSpot = 0;

                try {
                    randomSpot = random.nextInt(possibleSources.size() - 1);
                } catch (IllegalArgumentException ignored) {

                }

                soundFile = possibleSources.get(randomSpot);
                possibleSources.remove(soundFile);
            }

            if (soundFile != null) {
                try {
                    log("Copying sound file \"" + dest.getAbsolutePath() + "\"... (" + count + " of " + total + ")");
                    FileUtils.copyFile(soundFile,dest);
                } catch (Exception ignored) {

                }
            }
        }

        return true;
    }

    private static boolean addPackFiles() { // TODO
        return false;
    }

    private static boolean cleanup() { // TODO
        return false;
    }

    private static boolean zip() { // TODO
        return false;
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
