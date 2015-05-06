package org.glandais.jug.java8;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class Exemple3 {

	public static void main(String[] args) throws Exception {
		// Parcours fichiers récursif de fichiers

		File racine = new File(".");
		// Legacy
		scan(racine);

		System.out.println("**************");
		// Java8
		try (Stream<Path> files = Files.walk(racine.toPath())) {
			files.filter(p -> p.getFileName().toString().endsWith(".java")).forEach(System.out::println);
		}
		// Java8 : subtilité...
		System.out.println("**************");
		try (Stream<Path> files = Files.walk(racine.toPath())) {
			files.filter(p -> p.getFileName().endsWith(".java")).forEach(System.out::println);
		}
	}

	private static void scan(File file) {
		File[] files = file.listFiles();
		for (File child : files) {
			if (!child.getName().startsWith(".")) {
				if (child.isDirectory()) {
					scan(child);
				} else if (child.getName().endsWith(".java")) {
					System.out.println(child.getAbsolutePath());
				}
			}
		}
	}

}
