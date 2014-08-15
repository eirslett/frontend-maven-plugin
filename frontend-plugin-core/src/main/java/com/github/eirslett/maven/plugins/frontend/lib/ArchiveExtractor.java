package com.github.eirslett.maven.plugins.frontend.lib;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

class ArchiveExtractionException extends Exception {
    ArchiveExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}

interface ArchiveExtractor {
    public void extract(String archive, String destinationDirectory) throws ArchiveExtractionException;
}

final class DefaultArchiveExtractor implements ArchiveExtractor {
    public void extract(String archive, String destinationDirectory) throws ArchiveExtractionException {
        try {
            final File archiveFile = new File(archive);
            FileInputStream fis = new FileInputStream(archiveFile);

            // TarArchiveInputStream can be constructed with a normal FileInputStream if
            // we ever need to extract regular '.tar' files.
            final TarArchiveInputStream tarIn = new TarArchiveInputStream(new GzipCompressorInputStream(fis));

            TarArchiveEntry tarEntry = tarIn.getNextTarEntry();
            while (tarEntry != null) {
                // Create a file for this tarEntry
                final File destPath = new File(destinationDirectory + File.separator + tarEntry.getName());
                if (tarEntry.isDirectory()) {
                    destPath.mkdirs();
                } else {
                    if (!destPath.getParentFile().exists()) {
                        destPath.getParentFile().mkdirs();
                    }
                    destPath.createNewFile();
                    boolean isExecutable = (tarEntry.getMode() & 0100) > 0;
                    destPath.setExecutable(isExecutable);

                    //byte [] btoRead = new byte[(int)tarEntry.getSize()];
                    byte [] btoRead = new byte[8024];
                    final BufferedOutputStream bout =
                        new BufferedOutputStream(new FileOutputStream(destPath));
                    int len = 0;

                    while((len = tarIn.read(btoRead)) != -1)
                    {
                        bout.write(btoRead,0,len);
                    }

                    bout.close();
                }
                tarEntry = tarIn.getNextTarEntry();
            }
            tarIn.close();
        } catch (IOException e) {
            throw new ArchiveExtractionException("Could not extract archive: '"
                    + archive
                    + "'", e);
        }
    }
}
