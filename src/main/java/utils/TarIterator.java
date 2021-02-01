package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

public class TarIterator implements FileIterator {

	TarArchiveInputStream tar;
	String decompressed_path;

	public TarIterator(String path){

		if(path.endsWith(".tar.gz"))
			try {
				tar = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(path)));
			} catch (IOException e) {
				e.printStackTrace();
			}
		else
			try {
				tar = new TarArchiveInputStream(new FileInputStream(path));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

		decompressed_path = new File(path).getParent()+"_decompressed";

	}

	@Override
	public InputStream nextStream() {

		InputStream is = null;
		TarArchiveEntry entry;
		try {
			while(is == null && (entry = tar.getNextTarEntry()) != null){
				if(entry.isFile() && entry.getName().endsWith(".java") && !entry.getName().contains("test")){
					File curfile = new File(decompressed_path, entry.getName());
					File parent = curfile.getParentFile();
					if (!parent.exists()) 
						parent.mkdirs();	

					OutputStream aux = new FileOutputStream(curfile);
					IOUtils.copy(tar, aux); 
					aux.close();
					is = new FileInputStream(curfile);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return is;
	}

	@Override
	public boolean close() {
		try {
			tar.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		tar = null;

		try {
			Files.walkFileTree(Paths.get(decompressed_path), new SimpleFileVisitor<Path>()
			{
				@Override
				public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException
				{
					Files.delete(path);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path directory, IOException ioException) throws IOException
				{
					System.out.println("deleting... "+directory.toString());
					Files.delete(directory);
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return true;
	}

}
