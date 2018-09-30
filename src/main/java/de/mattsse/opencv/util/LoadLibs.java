package de.mattsse.opencv.util;

import com.sun.jna.Platform;
import net.sourceforge.tess4j.util.LoggHelper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.opencv.core.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Helper Class to load libraries from JAR or project folder.
 */
public class LoadLibs {


    private static boolean INITIALIZED = false;
    private static final String VFS_PROTOCOL = "vfs";
    private static final String JAVA_LIBRARY_PATH = "java.library.path";
    public static final String OPENCV_TEMP_DIR = new File(System.getProperty("java.io.tmpdir"), "opencv").getPath();

    private static final Logger logger = LoggerFactory.getLogger(new LoggHelper().toString());

    static void prepareLibs() {
        File targetTempFolder = packOpencvResources(Platform.RESOURCE_PREFIX);
        if (targetTempFolder != null && targetTempFolder.exists()) {
            String userCustomizedPath = System.getProperty(JAVA_LIBRARY_PATH);
            if (null == userCustomizedPath || userCustomizedPath.isEmpty()) {
                System.setProperty(JAVA_LIBRARY_PATH, targetTempFolder.getPath());
            } else {
                System.setProperty(JAVA_LIBRARY_PATH, userCustomizedPath + File.pathSeparator + targetTempFolder.getPath());
            }
        }
    }

    /**
     * loads the native opencv library
     */
    public static void loadOpencvLib() {
        if (INITIALIZED) return;
        prepareLibs();
        try {
            Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
            fieldSysPath.setAccessible(true);
            fieldSysPath.set(null, null);
        } catch (Exception e) {
            logger.error("Failed to reset sys_paths");
        }
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        logger.info("Loaded opencv library.");
        INITIALIZED = true;
    }


    /**
     * packs the necessary opencv resources to temp folder
     */
    public static synchronized File packOpencvResources(String resourceName) {
        File targetPath = null;

        try {
            targetPath = new File(OPENCV_TEMP_DIR, resourceName);

            Enumeration<URL> resources = LoadLibs.class.getClassLoader()
                                                       .getResources(resourceName);
            while (resources.hasMoreElements()) {
                URL resourceUrl = resources.nextElement();
                copyResources(resourceUrl, targetPath);
            }
        } catch (IOException | URISyntaxException e) {
            logger.warn(e.getMessage(), e);
        }

        return targetPath;
    }

    /**
     * Copies resources to target folder.
     *
     * @param resourceUrl
     * @param targetPath
     * @return
     */
    static void copyResources(URL resourceUrl, File targetPath) throws IOException, URISyntaxException {
        if (resourceUrl == null) {
            return;
        }

        URLConnection urlConnection = resourceUrl.openConnection();

        /**
         * Copy resources either from inside jar or from project folder.
         */
        if (urlConnection instanceof JarURLConnection) {
            copyJarResourceToPath((JarURLConnection) urlConnection, targetPath);
        } else if (VFS_PROTOCOL.equals(resourceUrl.getProtocol())) {
            VirtualFile virtualFileOrFolder = VFS.getChild(resourceUrl.toURI());
            copyFromWarToFolder(virtualFileOrFolder, targetPath);
        } else {
            File file = new File(resourceUrl.getPath());
            if (file.isDirectory()) {
                FileUtils.copyDirectory(file, targetPath);
            } else {
                FileUtils.copyFile(file, targetPath);
            }
        }
    }

    /**
     * Copies resources from the jar file of the current thread and extract it
     * to the destination path.
     *
     * @param jarConnection
     * @param destPath      destination file or directory
     */
    static void copyJarResourceToPath(JarURLConnection jarConnection, File destPath) {
        try (JarFile jarFile = jarConnection.getJarFile()) {
            String jarConnectionEntryName = jarConnection.getEntryName();

            /**
             * Iterate all entries in the jar file.
             */
            for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements(); ) {
                JarEntry jarEntry = e.nextElement();
                String jarEntryName = jarEntry.getName();

                /**
                 * Extract files only if they match the path.
                 */
                if (jarEntryName.startsWith(jarConnectionEntryName + "/")) {
                    String filename = jarEntryName.substring(jarConnectionEntryName.length());
                    File targetFile = new File(destPath, filename);

                    if (jarEntry.isDirectory()) {
                        targetFile.mkdirs();
                    } else {
                        if (!targetFile.exists() || targetFile.length() != jarEntry.getSize()) {
                            try (InputStream is = jarFile.getInputStream(jarEntry);
                                 OutputStream out = FileUtils.openOutputStream(targetFile)) {
                                IOUtils.copy(is, out);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }
    }

    /**
     * Copies resources from WAR to target folder.
     *
     * @param virtualFileOrFolder
     * @param targetFolder
     * @throws IOException
     */
    static void copyFromWarToFolder(VirtualFile virtualFileOrFolder, File targetFolder) throws IOException {
        if (virtualFileOrFolder.isDirectory() && !virtualFileOrFolder.getName()
                                                                     .contains(".")) {
            if (targetFolder.getName()
                            .equalsIgnoreCase(virtualFileOrFolder.getName())) {
                for (VirtualFile innerFileOrFolder : virtualFileOrFolder.getChildren()) {
                    copyFromWarToFolder(innerFileOrFolder, targetFolder);
                }
            } else {
                File innerTargetFolder = new File(targetFolder, virtualFileOrFolder.getName());
                innerTargetFolder.mkdir();
                for (VirtualFile innerFileOrFolder : virtualFileOrFolder.getChildren()) {
                    copyFromWarToFolder(innerFileOrFolder, innerTargetFolder);
                }
            }
        } else {
            File targetFile = new File(targetFolder, virtualFileOrFolder.getName());
            if (!targetFile.exists() || targetFile.length() != virtualFileOrFolder.getSize()) {
                FileUtils.copyURLToFile(virtualFileOrFolder.asFileURL(), targetFile);
            }
        }
    }
}
