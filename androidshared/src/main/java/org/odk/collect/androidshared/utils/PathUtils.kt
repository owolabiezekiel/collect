package org.odk.collect.androidshared.utils

import timber.log.Timber
import java.io.File

object PathUtils {
    @JvmStatic
    fun getAbsoluteFilePath(dirPath: String, filePath: String): String {
        val absoluteFilePath =
            if (filePath.startsWith(dirPath)) filePath else dirPath + File.separator + filePath

        val canonicalAbsoluteFilePath = File(absoluteFilePath).canonicalPath
        val canonicalDirPath = File(dirPath).canonicalPath
        if (!canonicalAbsoluteFilePath.startsWith(canonicalDirPath)) {
            Timber.e(
                "Attempt to access file outside of Collect directory:\n" +
                    "dirPath: $dirPath\n" +
                    "filePath: $filePath\n" +
                    "absoluteFilePath: $absoluteFilePath\n" +
                    "canonicalAbsoluteFilePath: $canonicalAbsoluteFilePath\n" +
                    "canonicalDirPath: $canonicalDirPath"
            )
        }
        return absoluteFilePath
    }
}
