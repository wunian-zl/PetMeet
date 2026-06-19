package org.petmeet.support;

import java.io.File;

/**
 * 统一按整个工作区根目录解析路径。
 */
public final class UploadPathResolver {

    private static final String BACKEND_DIR_SUFFIX = "-backend";
    private static final String UPLOADS_DIR_NAME = "uploads";

    private UploadPathResolver() {
    }

    public static File resolveProjectRootDir() {
        File userDir = new File(System.getProperty("user.dir"));
        if (looksLikeWorkspaceRoot(userDir)) {
            return userDir;
        }

        File parentDir = userDir.getParentFile();
        if (looksLikeBackendModule(userDir) && parentDir != null) {
            return parentDir;
        }
        return userDir;
    }

    public static File resolveUploadsRootDir() {
        String configuredPath = System.getProperty("petmeet.upload.path");
        if (isBlank(configuredPath)) {
            configuredPath = System.getenv("PETMEET_UPLOAD_PATH");
        }
        if (!isBlank(configuredPath)) {
            File configuredDir = new File(configuredPath.trim());
            return configuredDir.isAbsolute() ? configuredDir : new File(resolveProjectRootDir(), configuredPath.trim());
        }
        return new File(resolveProjectRootDir(), UPLOADS_DIR_NAME);
    }

    public static String toResourceLocation(File dir) {
        String path = dir.getAbsolutePath();
        if (!path.endsWith(File.separator)) {
            path += File.separator;
        }
        return "file:" + path;
    }

    private static boolean looksLikeWorkspaceRoot(File dir) {
        return new File(dir, "PetMeet-backend").isDirectory();
    }

    private static boolean looksLikeBackendModule(File dir) {
        return dir.getName().endsWith(BACKEND_DIR_SUFFIX)
                && new File(dir, "pom.xml").isFile()
                && new File(dir, "src").isDirectory();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
