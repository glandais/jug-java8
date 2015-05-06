package org.glandais.jug.java8;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Exemple1 {

	public static void main(String[] args) {
		List<String> strings = Arrays.asList(args);

		// Join String

		// Legacy
		boolean first = true;
		String resultat = "";
		for (String string : strings) {
			if (!first) {
				resultat = resultat + ", ";
			}
			resultat = resultat + string;
			first = false;
		}
		System.out.println(resultat);

		// Java 8
		resultat = strings.stream().collect(Collectors.joining(", "));
		System.out.println(resultat);
	}

}
