package org.glandais.jug.java8;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.services.GtfsRelationalDao;

public class Exemple5 implements Cas, GtfsTan {

	public static void main(String[] args) {
		new Exemple5().execute();
	}

	public void execute() {
		GtfsRelationalDao gtfsDao = getGtfsDao(new File("ARRETS_HORAIRES_CIRCUITS_TAN_GTFS_2015.zip"));

		// Legacy
		Map<String, List<StopTime>> stopsParStationLegacy = new HashMap<String, List<StopTime>>();
		for (StopTime stopTime : gtfsDao.getAllStopTimes()) {
			String stopName = stopTime.getStop().getName();
			List<StopTime> stops = stopsParStationLegacy.get(stopName);
			if (stops == null) {
				stops = new ArrayList<StopTime>();
				stopsParStationLegacy.put(stopName, stops);
			}
			stops.add(stopTime);
		}

		Comparator<StopTime> comparateurLegacy = new Comparator<StopTime>() {
			@Override
			public int compare(StopTime o1, StopTime o2) {
				return Integer.valueOf(o1.getDepartureTime()).compareTo(o2.getDepartureTime());
			}
		};

		List<StopTime> premiersArret = new ArrayList<>();
		for (Entry<String, List<StopTime>> entry : stopsParStationLegacy.entrySet()) {
			List<StopTime> stops = entry.getValue();
			Collections.sort(stops, comparateurLegacy);
			premiersArret.add(stops.get(0));
		}
		Collections.sort(premiersArret, comparateurLegacy);
		for (StopTime st : premiersArret) {
			System.out.println(st.getStop().getName() + " " + st.getDepartureTime());
		}

		// Java 8
		Comparator<StopTime> comparateur = (st1, st2) -> Integer.compare(st1.getDepartureTime(),
				st2.getDepartureTime());

		Map<String, Optional<StopTime>> stopsMap = gtfsDao.getAllStopTimes().stream()
				.collect(Collectors.groupingBy(st -> st.getStop().getName(), Collectors.minBy(comparateur)));

		List<StopTime> premierStops = stopsMap.values().stream().map(ost -> ost.get()).sorted(comparateur)
				.collect(Collectors.toList());

		System.out.println("**************");
		premierStops.forEach(st -> System.out.println(st.getStop().getName() + " " + st.getDepartureTime()));

	}

}
