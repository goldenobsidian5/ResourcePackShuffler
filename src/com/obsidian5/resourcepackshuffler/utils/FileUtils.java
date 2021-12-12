package com.obsidian5.resourcepackshuffler.utils;

import com.google.gson.Gson;
import com.obsidian5.resourcepackshuffler.ResourcePackShuffler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtils {

    private static File minecraftFolder;
    private static File packFolder;
    private static File tempFolder;
    private static File assetsFolder;
    private static File objectsFolder;
    private static File jarFile;
    private static File indexFile;

    public static File getMinecraftFolder() {
        return minecraftFolder;
    }
    public static File getPackFolder() {
        return packFolder;
    }
    public static File getTempFolder() {
        return tempFolder;
    }
    public static File getAssetsFolder() {
        return assetsFolder;
    }
    public static File getObjectsFolder() {
        return objectsFolder;
    }
    public static File getJarFile() {
        return jarFile;
    }
    public static File getIndexFile() {
        return indexFile;
    }

    public static boolean startup() {
        String version = "1." + ResourcePackShuffler.mcVersion;

        minecraftFolder = new File(System.getenv("APPDATA").concat("\\.minecraft"));

        packFolder = new File(minecraftFolder, "resourcepacks\\Shuffle");
        tempFolder = new File(packFolder, "temp");
        assetsFolder = new File(tempFolder, "assets");

        objectsFolder = new File(minecraftFolder, "assets\\objects");
        jarFile = new File(minecraftFolder, "versions\\" + version + "\\" + version + ".jar");
        indexFile = new File(minecraftFolder, "assets\\indexes\\" + version + ".json");

        if (!indexFile.exists() || !jarFile.exists() || !objectsFolder.exists()) {
            ResourcePackShuffler.quit("find required files. Make sure you start Minecraft " + version + " at least once before using this");
            return false;
        }

        if (!assetsFolder.exists() && !assetsFolder.mkdirs()) {
            ResourcePackShuffler.quit("make required files");
            return false;
        }

        return true;
    }

    public static Object getJsonObject(File file, Class objectClass) throws IOException {
        Reader reader = Files.newBufferedReader(file.toPath());
        Object object = new Gson().fromJson(reader, objectClass);
        reader.close();
        return object;
    }

    public static List<File> getPngFiles(File folder) { // TODO
        File[] fileList = folder.listFiles();

        if (fileList == null)
            return null;

        List<File> files = new CopyOnWriteArrayList<>();

        for (File file : fileList) {
            if (file.isDirectory()) {
                List<File> metaFiles = getPngFiles(file);

                if (metaFiles != null)
                    files.addAll(metaFiles);
            }

            if (!file.getName().endsWith(".png"))
                continue;

            files.add(file);
        }

        return files;
    }

    public static List<BufferedImage> getPngData(List<File> files) throws IOException {
        if (files == null)
            return null;

        List<BufferedImage> images = new CopyOnWriteArrayList<>();

        for (File file : files) {
            if (!file.getName().endsWith(".png"))
                continue;

            images.add(ImageIO.read(file));
        }

        return images;
    }

    public static void unzip(File archive, File destFolder) throws IOException {
        byte[] buffer = new byte[1024];
        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(archive));
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        while (zipEntry != null) {
            File newFile = newFile(destFolder, zipEntry);
            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
            } else {
                // fix for Windows-created archives
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }

                // write file content
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zipInputStream.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zipInputStream.getNextEntry();
        }
        zipInputStream.closeEntry();
        zipInputStream.close();
    }

    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }
}
