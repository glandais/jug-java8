package org.glandais.jug.java8;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Exemple2 {

	public static void main(String[] args) throws Exception {
		File csvFile = new File("tan/routes.txt");

		// Lecture CSV

		Map<String, String> lignes = new TreeMap<>();
		// Legacy
		BufferedReader br = new BufferedReader(new FileReader(csvFile));
		String line;
		boolean first = true;
		while ((line = br.readLine()) != null) {
			if (!first) {
				String[] split = line.split(",");
				lignes.put(split[0], split[2]);
			}
			first = false;
		}
		br.close();
		for (Entry<String, String> entry : lignes.entrySet()) {
			System.out.println(entry.getKey() + " : " + entry.getValue());
		}

		// Java8
		System.out.println("************");
		try (Stream<String> lines = Files.lines(csvFile.toPath())) {
			Map<String, String> resultat = lines.skip(1).map(s -> s.split(","))
					.collect(Collectors.toMap(s -> s[0], s -> s[2], (a, b) -> a, TreeMap::new));
			resultat.forEach((r, n) -> System.out.println(r + " : " + n));
		}

	}

}
