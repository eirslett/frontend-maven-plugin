package com.github.eirslett.maven.plugins.frontend.lib;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

final class DownloadException extends Exception {
    DownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}

interface FileDownloader {
    void download(String downloadUrl, String destination) throws DownloadException;
}

final class DefaultFileDownloader implements FileDownloader {
    public void download(String downloadUrl, String destination) throws DownloadException {
        try {
            new File(FilenameUtils.getPath(destination)).mkdirs();
            URL link = new URL(downloadUrl);
            ReadableByteChannel rbc = Channels.newChannel(link.openStream());
            FileOutputStream fos = new FileOutputStream(destination);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
        } catch (IOException e) {
            throw new DownloadException("Could not download "+downloadUrl, e);
        }
    }
}
