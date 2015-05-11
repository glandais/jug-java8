package org.glandais.jug.java8;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.services.GtfsRelationalDao;

public class Exemple4 implements Cas, GtfsTan {

	public static void main(String[] args) {
		new Exemple4().execute();
	}

	@Override
	public void execute() {
		GtfsRelationalDao gtfsDao = getGtfsDao(new File("ARRETS_HORAIRES_CIRCUITS_TAN_GTFS_2015.zip"));

		// groupBy

		// Legacy
		Map<String, List<Stop>> stopsLegacy = new HashMap<String, List<Stop>>();
		for (Stop stop : gtfsDao.getAllStops()) {
			String name = stop.getName();
			List<Stop> stops = stopsLegacy.get(name);
			if (stops == null) {
				stops = new ArrayList<Stop>();
				stopsLegacy.put(name, stops);
			}
			stops.add(stop);
		}
		List<Entry<String, List<Stop>>> entries = new ArrayList<>(stopsLegacy.entrySet());
		Collections.sort(entries, new Comparator<Entry<String, List<Stop>>>() {
			@Override
			public int compare(Entry<String, List<Stop>> o1, Entry<String, List<Stop>> o2) {
				if (o1.getValue().size() == o2.getValue().size()) {
					return o1.getKey().compareTo(o2.getKey());
				} else {
					return -Integer.valueOf(o1.getValue().size()).compareTo(o2.getValue().size());
				}
			}
		});
		for (Entry<String, List<Stop>> e : entries) {
			System.out.println(e.getKey() + " " + e.getValue().size());
		}

		System.out.println("********************");
		// Java 8
		Map<String, List<Stop>> stops = gtfsDao.getAllStops().stream()
				.collect(Collectors.groupingBy(stop -> stop.getName()));

		Comparator<Entry<String, List<Stop>>> comparator1 = (e1,
				e2) -> -Integer.compare(e1.getValue().size(), e2.getValue().size());
		Collator collator = Collator.getInstance();
		collator.setStrength(Collator.PRIMARY);
		Comparator<Entry<String, List<Stop>>> comparator2 = (e1, e2) -> collator.compare(e1.getKey(), e2.getKey());

		stops.entrySet().stream().sorted(comparator1.thenComparing(comparator2))
				.forEach(e -> System.out.println(e.getKey() + " " + e.getValue().size()));
	}

}
