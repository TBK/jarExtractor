import java.io.*;
import java.lang.String.*;
import java.nio.file.FileSystems;
import java.util.*;
import java.util.jar.*;
import java.util.logging.*;
// Used by deflate
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class jarExtractor {
	public static void main(String args[]) {
		Boolean debug = false;

		// Ensure there is something to work on
		if (args.length == 0 || args.length == 3 || args.length > 4) {
			System.out.println("Proper use:");
			System.out.println("java jarExtractor <jarToExtract.jar>");
			System.out.println("java jarExtractor <jarToExtract.jar> <Cap>");
			System.out.println("java jarExtractor <jarToExtract.jar> <Cap> <Pattern> <PatternReplacement>");
			System.out.println("--------------------------------------------");
			System.out.println("<jarToExtract.jar>: relative or absolute file path.");
			System.out.println("By default the name will be capped at 80 chars and use deflate to create a new name.");
			System.out.println("-----------");
			System.out.println("The options below are an manual overwrite to the default cap/rename.");
			System.out.println("<Cap>: The cut of length for entries before using the pattern and replacement.");
			System.out.println("<Pattern>: The pattern will be cut out of mathing files and dirs names.");
			System.out.println("<PatternReplacementr>: Regex to generate a new shorter name.");
			System.out.println("--------------------------------------------");
			System.out.println("Get the pattern with: zipinfo jarToExtract.jar > files.txt");
			System.out.println("A pattern extracted from files.txt could be ^0OOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
			System.out.println("It is also possible to use more complex regex - e.g. ^.*O{100}.*");
			System.exit(0);
		}

		if (debug)
			System.out.println("Input jar: " + args[0]);

		int cap = 80;
		if (args.length == 2 || args.length == 4)
			cap = Integer.parseInt(args[1]);

		String pattern = "([a-zA-Z0-9])\1{30}";
		if (args.length == 4)
			pattern = args[2];

		String patternReplacement = "";
		if (args.length == 4)
			patternReplacement = args[3];

		if (debug) {
			System.out.println("Cap: " + cap);
			System.out.println("Pattern: " + pattern);
			System.out.println("Pattern replacement: " + patternReplacement);
		}

		try {
			JarFile jarFile = new JarFile(args[0]);
			Enumeration<JarEntry> jarEntries = jarFile.entries();

			while (jarEntries.hasMoreElements()) {
				JarEntry jarEntry = jarEntries.nextElement();
				File entry = new File(jarEntry.getName());
				System.out.println("Input entry: " + entry);

				String entryName = entry.getName();
				String entryPath = entry.getPath().replace(entryName, "");

				String fileExtensionPattern = "[\\w]+\\..*$";
				String entryFileExtension = entryName.replace(fileExtensionPattern, "");

				String entryExtension = "";
				String entryFile = "";
				String[] splitFileExtension = entryFileExtension.split("\\.");

				if (splitFileExtension.length == 2) {
					entryExtension = "." + splitFileExtension[1];
					entryFile = splitFileExtension[0];
				}

				// Cases where the entry is just a directory
				if (entryPath.length() == 0 && entryFile.length() == 0 && entryExtension.length() == 0) {
					entryPath = entry.getPath();
					System.out.println("Only a directory: " + entryPath);
				}

				if (debug) {
					System.out.println("Entry name: " + entryName);
					System.out.println("Entry file: " + entryFile);
					System.out.println("Entry extension: " + entryExtension);
					System.out.println("Entry path: " + entryPath);
					if (args.length == 4) {
						System.out.println("entryName match: " + entryName.matches(args[1]));
						System.out.println("entryPath match: " + entryPath.matches(args[1]));
					}
				}

				// New entryFile
				if (entryFile.length() > cap) {
					if (args.length == 4) {
						entryFile = entryFile.replaceAll(pattern, patternReplacement);
					} else {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						DeflaterOutputStream dos = new DeflaterOutputStream(baos);
						dos.write(entryFile.getBytes());
						dos.flush();
						dos.close();
						entryFile = baos.toByteArray().toString();
					}

					System.out.println("New entryFile: " + entryFile);

					if (entryName.length() == 0) {
						System.out.println("Pattern is too aggressive. There is nothing left!");
						System.exit(0);
					}

				}

				// New entryPath
				if (entryPath.length() > cap) {
					if (args.length == 4) {
						entryPath = entryPath.replaceAll(pattern, patternReplacement);
					} else {
						String entryPathFront = "";
						if (entryPath.endsWith("/")) {
							entryPathFront = entryPath.substring(0, entryPath.length() - 1);

							if (debug)
								System.out.println("entryPathFront remove last /: " + entryPathFront);
						}

						entryPathFront = String.join("", entryPathFront.split("[^\\/]+$"));

						String entryPathEnd = entryPath.replace(entryPathFront, "");
						if (entryPathEnd.endsWith("/")) {
							entryPathEnd = entryPathEnd.substring(0, entryPathEnd.length() - 1);

							if (debug)
								System.out.println("entryPathEnd remove last /: " + entryPathEnd);
						}

						if (debug) {
							System.out.println("entryPathFront: " + entryPathFront);
							System.out.println("entryPathEnd: " + entryPathEnd);
						}

						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						DeflaterOutputStream dos = new DeflaterOutputStream(baos);
						dos.write(entryPathEnd.getBytes());
						dos.flush();
						dos.close();

						entryPath = entryPathFront + baos.toByteArray().toString()
								+ System.getProperty("file.separator");
					}

					System.out.println("New entryPath: " + entryPath);

					if (entryPath.length() == 0) {
						System.out.println("Pattern is too aggressive. There is nothing left!");
						System.exit(0);
					}
				}

				Boolean entryFileEntryPathSame = entryFile.equals(entryPath);

				if (debug)
					System.out.println("File & Path same: " + entryFileEntryPathSame);

				if (!entryFileEntryPathSame) {
					if (debug)
						System.out.println("Making directory!");
					File directory = new File(entryPath);
					directory.mkdirs();
				}

				InputStream jarFileInputStream = jarFile.getInputStream(jarEntry);

				if (entryExtension.matches("^.*\\..*$")) {
					if (debug)
						System.out.println("Making file!");

					File outputEntry = new File(FileSystems.getDefault().getPath("").toAbsolutePath()
							+ System.getProperty("file.separator") + entryPath + entryFile + entryExtension);

					System.out.println("Output entry: " + outputEntry.toString());

					OutputStream jarOutputStream = new FileOutputStream(outputEntry);

					while (jarFileInputStream.available() > 0) {
						jarOutputStream.write(jarFileInputStream.read());
					}
					jarOutputStream.close();
				}
				jarFileInputStream.close();
				if (debug)
					System.out.println("\n");
			}
			jarFile.close();
		} catch (IOException ex) {
			Logger.getLogger(jarExtractor.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
