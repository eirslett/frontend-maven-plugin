package com.github.eirslett.maven.plugins.frontend;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.maven.plugin.MojoFailureException;

import java.io.*;

class ArchiveExtractor {
    private String destinationDirectory;
    private String archive;

    public ArchiveExtractor(String destinationDirectory, String archive) {
        this.destinationDirectory = destinationDirectory;
        this.archive = archive;
    }

    public void extract() throws MojoFailureException {
        try {
            File archiveFile = new File(archive);
            if (!archiveFile.exists()){
                throw new MojoFailureException("The archive you're trying to extract ("
                        + archive
                        + ") does not exist!");
            }
            if (!archiveFile.canRead()){
                throw new MojoFailureException("The archive you're trying to extract ("
                        + archive
                        + ") can not be read!");
            }
            FileInputStream fis = new FileInputStream(archiveFile);

            // TarArchiveInputStream can be constructed with a normal FileInputStream if
            // we ever need to extract regular '.tar' files.
            TarArchiveInputStream tarIn = new TarArchiveInputStream(new GzipCompressorInputStream(fis));

            TarArchiveEntry tarEntry = tarIn.getNextTarEntry();
            while (tarEntry != null) {
                // Create a file for this tarEntry
                File destPath = new File(destinationDirectory + File.separator + tarEntry.getName());
                if (tarEntry.isDirectory()) {
                    destPath.mkdirs();
                } else {
                    destPath.createNewFile();
                    //byte [] btoRead = new byte[(int)tarEntry.getSize()];
                    byte [] btoRead = new byte[8024];
                    BufferedOutputStream bout =
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

        } catch (FileNotFoundException e) {
            throw new MojoFailureException("Could not extract archive: '"
                    + archive
                    + "'", e);
        } catch (IOException e) {
            throw new MojoFailureException("Could not extract archive: '"
                    + archive
                    + "'", e);
        }
    }
}
