package org.apache.mina.core.file;

import java.io.File;
import java.nio.channels.FileChannel;

public class FilenameFileRegion extends DefaultFileRegion {

	public FilenameFileRegion(File file, FileChannel channel, long position, long remainingBytes) {
		super(channel, position, remainingBytes);
	}
}
