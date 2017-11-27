package com.github.eirslett.maven.plugins.frontend.lib;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;


public class UtilsIT{
	private File srcDirectory;
	private File destDirectory;
	
	@Before
	public void setup() {
		srcDirectory = new File((String)System.getProperties().get("srcDirectory"));
		destDirectory = new File((String)System.getProperties().get("destDirectory"));
		
		System.out.println("Project Build Directory set to " + srcDirectory.getAbsolutePath());
		System.out.println("Project Build Directory set to " + destDirectory.getAbsolutePath());
	}
	
	@Test
	public void testSourceFolderAttributeContent() {
		System.out.println("Testing source Folder file permissions at: " + srcDirectory.getAbsolutePath());
		
		traverseDirectoryForAllFiles(srcDirectory);
	}
	
	@Test
	public void testDestinationFolderAttributeContent() {
		System.out.println("Testing Destination Folder file permissions at: " + destDirectory.getAbsolutePath());
		
		Utils.copyDirectoryContents(srcDirectory, destDirectory);
	}
	
	private void traverseDirectoryForAllFiles(File sourceDirectory) {
		File[] files = sourceDirectory.listFiles();
		
		for(File file: files) {
			if(file.isDirectory()) {
				traverseDirectoryForAllFiles(file);
			}
			else {
				assertTrue(FilePermissions.isPermissionCorrect(file));
			}
		}
	}
	
	/**
	 * This Enum will be used as a convenience for this test class to assert that permissions remain intact after calling Utils.copyDirectoryContents
	 * Note that we expect that all files have at least READ permission, otherwise the ability to copy a file is pointless.
	 * @author mjimenez
	 *
	 */
	enum FilePermissions{
		READONLY("readonly", true, false, false),
		READWRITE("readwrite", true, true, false),
		READEXECUTE("readexecute", true, false, true),
		READWRITEEXEUCTE("readwriteexecute", true, true, true);
		
		private String fileName;
		private boolean canRead;
		private boolean canWrite;
		private boolean canExecute;
		
		private static Map<String, FilePermissions> filePermissionsMap = new HashMap<String, FilePermissions>();
		
		static {
			for(FilePermissions filePermission: FilePermissions.values()) {
				filePermissionsMap.put(filePermission.fileName, filePermission);
			}
		}
		
		private FilePermissions(String filename, boolean canRead, boolean canWrite, boolean canExecute) {
			this.fileName = filename;
			this.canRead = canRead;
			this.canWrite = canWrite;
			this.canExecute = canExecute;
		}
		
		public String getFileName() {
			return fileName;
		}
		
		public boolean canRead() {
			return canRead;
		}
		
		public boolean canWrite() {
			return canWrite;
		}
		
		public boolean canExecute() {
			return canExecute;
		}
		
		public static boolean isPermissionCorrect(File file) {
			boolean isCorrectPermission = false;
			if(filePermissionsMap.containsKey(file.getName())) {
				isCorrectPermission = (filePermissionsMap.get(file.getName()).canRead() == file.canRead() &&
										filePermissionsMap.get(file.getName()).canWrite() == file.canWrite() &&
										filePermissionsMap.get(file.getName()).canExecute() == file.canExecute()) ? true:false;
			}
			else {
				System.out.println("Could not determine expected file attributes for file: " + file.getName());
			}
			return isCorrectPermission;
		}
	}
}
