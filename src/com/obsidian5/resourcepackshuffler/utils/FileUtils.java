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
    private static File assetsFolder;
    private static File tempFolder;
    private static File tempAssetsFolder;
    private static File objectsFolder;
    private static File jarFile;
    private static File indexFile;

    public static File getMinecraftFolder() {
        return minecraftFolder;
    }
    public static File getPackFolder() {
        return packFolder;
    }
    public static File getAssetsFolder() {
        return assetsFolder;
    }
    public static File getTempFolder() {
        return tempFolder;
    }
    public static File getTempAssetsFolder() {
        return tempAssetsFolder;
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
        assetsFolder = new File(packFolder, "assets");
        tempFolder = new File(packFolder, "temp");
        tempAssetsFolder = new File(tempFolder, "assets");

        objectsFolder = new File(minecraftFolder, "assets\\objects");
        jarFile = new File(minecraftFolder, "versions\\" + version + "\\" + version + ".jar");
        indexFile = new File(minecraftFolder, "assets\\indexes\\" + version + ".json");

        if (!indexFile.exists() || !jarFile.exists() || !objectsFolder.exists()) {
            ResourcePackShuffler.quit("find required files. Make sure you start Minecraft " + version + " at least once before using this");
            return false;
        }

        if (!tempAssetsFolder.exists() && !tempAssetsFolder.mkdirs()) {
            ResourcePackShuffler.quit("make required files");
            return false;
        }

        return true;
    }

    public static Object getJsonObject(File file, Class<com.obsidian5.resourcepackshuffler.utils.mojang.Index> objectClass) throws IOException { // remove "<com.obsidian5.resourcepackshuffler.utils.mojang.Index>" if needed for something else
        Reader reader = Files.newBufferedReader(file.toPath());
        Object object = new Gson().fromJson(reader, objectClass);
        reader.close();
        return object;
    }

    public static List<File> getFilesInFolder(File folder) {
        File[] fileList = folder.listFiles();

        if (fileList == null)
            return null;

        List<File> files = new CopyOnWriteArrayList<>();

        for (File file : fileList) {
            if (file.isDirectory()) {
                List<File> metaFiles = getFilesInFolder(file);

                if (metaFiles != null)
                    files.addAll(metaFiles);
            } else
                files.add(file);
        }

        return files;
    }

    public static List<BufferedImage> getPngData(List<File> pngFiles) throws IOException {
        if (pngFiles == null)
            return null;

        List<BufferedImage> images = new CopyOnWriteArrayList<>();

        for (File file : pngFiles)
            images.add(ImageIO.read(file));

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

    public static void copyFile(File source, File destination) throws IOException {
        destination.getParentFile().mkdirs();

        destination.createNewFile();

        try (InputStream is = new FileInputStream(source); OutputStream os = new FileOutputStream(destination)) {

            byte[] buffer = new byte[1024];

            int bytesRead;
            while ((bytesRead = is.read(buffer)) > 0) {
                os.write(buffer, 0, bytesRead);
            }
        }
    }

    public static boolean deleteFolder(File folder) {
        File[] files = folder.listFiles();

        if (files != null)
            for (File file : files)
                deleteFolder(file);

        return folder.delete();
    }
}
