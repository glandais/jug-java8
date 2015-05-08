package org.glandais.jug.java8;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.ServiceCalendarDate;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.GtfsRelationalDao;

public class Exemple6 implements Cas, GtfsTan {

	public static void main(String[] args) {
		new Exemple6().execute();
	}

	private DateTimeFormatter timeFormatter;
	private DateTimeFormatter dateFormatter;

	@Override
	public void execute() {
		GtfsRelationalDao gtfsDao = getGtfsDao(new File("ARRETS_HORAIRES_CIRCUITS_TAN_GTFS_2015.zip"));
		List<Stop> allStops = new ArrayList<>(gtfsDao.getAllStops());

		timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);
		dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

		//		calculerItineraire(gtfsDao, LocalDate.of(2015, 05, 11), LocalTime.of(17, 12), "Polytech'", "Vertou");
		//		calculerItineraire(gtfsDao, LocalDate.of(2015, 06, 9), LocalTime.of(7, 43), "Californie", "Sautron");
		calculerItineraire(gtfsDao, LocalDate.of(2015, 06, 27), LocalTime.of(12, 37), "La Plée", "Mairie de Couëron");

		calculerItineraire(gtfsDao, LocalDate.of(2015, 3, 2), LocalTime.of(7, 47), "Bio Ouest Laënnec",
				"Gare de Chantenay");

		//		Random r = new Random(System.nanoTime());
		//		IntStream.range(0, 10).forEach(i -> calculerAleatoire(gtfsDao, allStops, r));
	}

	protected void calculerAleatoire(GtfsRelationalDao gtfsDao, List<Stop> allStops, Random r) {
		LocalDate date = LocalDate.of(2015, 1 + r.nextInt(5), 1 + r.nextInt(27));
		LocalTime time = LocalTime.of(5 + r.nextInt(12), r.nextInt(60));

		Stop s1 = allStops.get(r.nextInt(allStops.size()));
		Stop s2;
		do {
			s2 = allStops.get(r.nextInt(allStops.size()));
		} while (getStationId(s1).equals(getStationId(s2)));

		calculerItineraire(gtfsDao, date, time, s1.getName(), s2.getName());
	}

	protected void calculerItineraire(GtfsRelationalDao gtfsDao, LocalDate departJour, LocalTime departHeure,
			String departArret, String arriveeArret) {

		// 1 - recherche des services (ServiceCalendar) pour le jour du départ

		// 1a - jour de la semaine
		DayOfWeek dayOfWeek = departJour.getDayOfWeek();

		Predicate<ServiceCalendar> predicatJourSemaine;
		switch (dayOfWeek) {
		case MONDAY:
			predicatJourSemaine = (sc -> sc.getMonday() == 1);
			break;
		case TUESDAY:
			predicatJourSemaine = (sc -> sc.getTuesday() == 1);
			break;
		case WEDNESDAY:
			predicatJourSemaine = (sc -> sc.getWednesday() == 1);
			break;
		case THURSDAY:
			predicatJourSemaine = (sc -> sc.getThursday() == 1);
			break;
		case FRIDAY:
			predicatJourSemaine = (sc -> sc.getFriday() == 1);
			break;
		case SATURDAY:
			predicatJourSemaine = (sc -> sc.getSaturday() == 1);
			break;
		case SUNDAY:
			predicatJourSemaine = (sc -> sc.getSunday() == 1);
			break;
		default:
			throw new IllegalStateException();
		}

		// Converstion de la date d'un ServiceDate (utilisé pour le début/fin
		// d'un ServiceCalendar) en Date Java8
		Function<ServiceDate, LocalDate> conversionJour = (sd -> LocalDate.of(sd.getYear(), sd.getMonth(),
				sd.getDay()));
		// Récupération de la date de début
		Function<ServiceCalendar, LocalDate> jourDebut = (sc -> conversionJour.apply(sc.getStartDate()));
		// Récupération de la date de fin
		Function<ServiceCalendar, LocalDate> jourFin = (sc -> conversionJour.apply(sc.getEndDate()));

		// Valide si départ pendant la validité
		Predicate<ServiceCalendar> predicatJourDebut = (sc -> jourDebut.apply(sc).compareTo(departJour) <= 0);
		Predicate<ServiceCalendar> predicatJourFin = (sc -> jourFin.apply(sc).compareTo(departJour) >= 0);

		// récupération de tous les ids des ServiceCalendar
		Set<String> serviceIdsForDate = gtfsDao.getAllCalendars().parallelStream()
				.filter(predicatJourSemaine.and(predicatJourDebut.and(predicatJourFin)))
				.map(sc -> sc.getServiceId().getId()).collect(Collectors.toSet());

		// System.out.println("***************");
		// serviceIdsForDate.stream().forEach(s -> System.out.println(s));

		// Récupère les exceptions (ServiceCalendarDate) pour la date de départ
		Map<Integer, List<ServiceCalendarDate>> collect = gtfsDao.getAllCalendarDates().parallelStream()
				.filter(scd -> conversionJour.apply(scd.getDate()).isEqual(departJour))
				.collect(Collectors.groupingBy(scd -> scd.getExceptionType()));
		// Code 1 : service exceptionel
		collect.getOrDefault(1, Collections.emptyList()).stream()
				.forEach(scd -> serviceIdsForDate.add(scd.getServiceId().getId()));
		// Code 2 : service annulé
		collect.getOrDefault(2, Collections.emptyList()).stream()
				.forEach(scd -> serviceIdsForDate.remove(scd.getServiceId().getId()));

		// System.out.println("***************");
		// serviceIdsForDate.stream().forEach(s -> System.out.println(s));

		// Récupère tous les trajets (Trip) de la journée
		Set<String> tripsJour = gtfsDao.getAllTrips().parallelStream()
				.filter(t -> serviceIdsForDate.contains(t.getServiceId().getId())).map(t -> t.getId().getId())
				.collect(Collectors.toSet());
				// System.out.println("***************");
				// tripsJour.stream().forEach(t -> System.out.println(t));

		// Récupère tous les arrêts (StopTime) des trajets de la journée, après
		// l'heure de départ et trié par ordre chronologique
		Comparator<StopTime> stopTimeComparator = (st1, st2) -> Integer.compare(st1.getArrivalTime(),
				st2.getArrivalTime());
		List<StopTime> stopTimes = gtfsDao.getAllStopTimes().parallelStream()
				.filter(st -> tripsJour.contains(getTripId(st)))
				.filter(st -> st.getDepartureTime() >= departHeure.toSecondOfDay()).sorted(stopTimeComparator)
				.collect(Collectors.toList());

		// Là on où on est monté dans un trajet
		Map<String, StopTime> montees = new HashMap<>();
		// Liste des arrêts pour arriver à cette station
		Map<String, List<StopTime[]>> trajetsPourStation = new HashMap<>();
		// Heure de départ possible de la station
		Map<String, Integer> departsStation = new HashMap<>();

		// Tous les arrêts (Stop) correspondant à la station de départ
		List<Stop> stopsDepart = gtfsDao.getAllStops().parallelStream().filter(s -> s.getName().equals(departArret))
				.collect(Collectors.toList());
		stopsDepart.forEach(s -> departsStation.put(getStationId(s), departHeure.toSecondOfDay()));
		stopsDepart.forEach(s -> trajetsPourStation.put(getStationId(s), Collections.emptyList()));

		for (StopTime arret : stopTimes) {
			String stationId = getStationId(arret.getStop());

			String trajet = getTripId(arret);
			if (departsStation.containsKey(stationId) && departsStation.get(stationId) > arret.getDepartureTime()) {
				// impossible de changer
			} else if (departsStation.containsKey(stationId)
					&& departsStation.get(stationId) <= arret.getDepartureTime()) {
				// on monte dans un trajet si le découvre
				montees.putIfAbsent(trajet, arret);
			} else if (montees.containsKey(trajet)) {
				// arrêt possible car possibilité d'être monté sur ce trajet
				// auparavant

				// Stockage des changements
				StopTime derniereMontee = montees.get(trajet);
				// Précédentes montees
				List<StopTime[]> trajetsPourCetteStation = new ArrayList<>(
						trajetsPourStation.get(getStationId(derniereMontee.getStop())));

				trajetsPourCetteStation.add(new StopTime[] { derniereMontee, arret });
				trajetsPourStation.put(stationId, trajetsPourCetteStation);

				// on ne peut pas partir plus tôt de cet arrêt
				departsStation.put(stationId, arret.getArrivalTime() + 30);
				if (arriveeArret.equals(arret.getStop().getName())) {

					// tous les arrêts
					List<StopTime> stops = trajetsPourCetteStation.stream()
							.flatMap(sts -> stopTimes.stream()
									.filter(st -> getTripId(st).equals(getTripId(sts[0]))
											&& sts[0].getStopSequence() <= st.getStopSequence()
											&& st.getStopSequence() <= sts[1].getStopSequence()))
							.sorted(stopTimeComparator).collect(Collectors.toList());

					//					afficherResultat(departJour, departHeure, stops, false);

					List<StopTime> newStops = new ArrayList<>(stops);

					// si on passe plusieurs fois au premier arrêt
					stops.stream().collect(Collectors.groupingBy(st -> getStationId(st.getStop()))).values().stream()
							.filter(e -> e.size() > 1 && e.contains(stops.get(0)))
							.map(l -> l.stream().sorted(stopTimeComparator).collect(Collectors.toList()))
							.flatMapToInt(l -> IntStream.range(stops.indexOf(l.get(0)), stops.indexOf(l.get(1))))
							.forEach(i -> newStops.remove(stops.get(i)));

					// suppression des arrêts entre les arrêts plus qu'en
					// doubles
					stops.stream().collect(Collectors.groupingBy(st -> getStationId(st.getStop()))).values().stream()
							.filter(e -> e.size() > 2)
							.map(l -> l.stream().sorted(stopTimeComparator).collect(Collectors.toList()))
							.flatMapToInt(
									l -> IntStream.range(stops.indexOf(l.get(1)), stops.indexOf(l.get(l.size() - 1))))
							.forEach(i -> newStops.remove(stops.get(i)));

					//					afficherResultat(departJour, departHeure, newStops, false);

					// premier et dernier arrêt pour chaque trajet
					List<StopTime> okStops = newStops.stream().collect(Collectors.groupingBy(st -> getTripId(st)))
							.values().stream()
							.map(l -> l.stream().sorted(stopTimeComparator).collect(Collectors.toList()))
							.flatMap(l -> IntStream.range(0, 2).mapToObj(i -> l.get(i * (l.size() - 1))))
							.sorted(stopTimeComparator).collect(Collectors.toList());

					// affichage
					afficherResultat(departJour, departHeure, okStops, true);
					break;
				}
			}
		}
	}

	private String getTripId(StopTime st) {
		return st.getTrip().getId().getId();
	}

	private String getStationId(Stop stop) {
		String code;
		if (stop.getParentStation() != null) {
			code = stop.getParentStation();
		} else {
			code = stop.getId().getId();
		}
		return code;
	}

	private LocalDateTime getDateTime(StopTime st) {
		return LocalDate.now().atStartOfDay().plus(st.getDepartureTime(), ChronoUnit.SECONDS);
	}

	private void afficherResultat(LocalDate departJour, LocalTime departHeure, List<StopTime> stops,
			boolean resultatFinal) {
		System.out.println("**********************************");
		System.out.println("Départ : " + dateFormatter.format(departJour) + " " + timeFormatter.format(departHeure));
		for (int i = 0; i < stops.size(); i++) {
			StopTime st = stops.get(i);
			System.out.println(timeFormatter.format(getDateTime(st)) + " " + st.getStop().getName() + " ("
					+ getStationId(st.getStop()) + ")");
			if (!resultatFinal || (i % 2) == 0) {
				System.out.println(" | " + st.getTrip().getRoute().getShortName() + " (-> "
						+ st.getTrip().getTripHeadsign() + ")");
			}
		}
		System.out.println("**********************************");
	}
}
