package utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipIterator implements FileIterator {

	ZipFile zipFile;
	List<? extends ZipEntry> entries;
	int index;

	public ZipIterator(String path) {

		try {
			zipFile = new ZipFile(path);
		} catch (IOException e) {
			e.printStackTrace();
		}

		entries = Collections.list(zipFile.entries());
		index = 0;
	}



	@Override
	public InputStream nextStream() {

		InputStream is = null;

		while(is == null && index < entries.size()){
			ZipEntry entry = entries.get(index);
			if(entry.getName().endsWith(".java") && !entry.getName().contains("test"))
				try {					
					is = zipFile.getInputStream(entry);
				} catch (IOException e) {
					e.printStackTrace();
				}
			index++;
		}
		return is;
	}

	@Override
	public boolean close() {

		entries.clear();
		entries = null;
		zipFile = null;
		return true;
	}

}
