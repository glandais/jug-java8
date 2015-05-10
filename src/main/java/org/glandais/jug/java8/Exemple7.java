package org.glandais.jug.java8;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.glandais.jug.java8.jts.LengthSubstring;
import org.glandais.jug.java8.jts.LengthToPoint;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.ServiceCalendarDate;
import org.onebusaway.gtfs.model.ShapePoint;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.GtfsRelationalDao;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.routing.util.EncodingManager;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class Exemple7 implements Cas, GtfsTan {

	public static void main(String[] args) {
		new Exemple7().execute();
	}

	private DateTimeFormatter timeFormatter;
	private GraphHopper hopper;
	private Map<String, Integer> tempsPieton = new HashMap<>();
	private Map<String, LineString> shapes;

	private NumberFormat nf;

	protected static class Trajet {

		public Stop stationDepart;

		public Stop stationArrivee;

		public int depart;

		public int arrivee;

		public Trip trip;

		public Trajet(Stop stationDepart, Stop stationArrivee, int depart, int arrivee, Trip trip) {
			super();
			this.stationDepart = stationDepart;
			this.stationArrivee = stationArrivee;
			this.depart = depart;
			this.arrivee = arrivee;
			this.trip = trip;
		}

	}

	@Override
	public void execute() {

		timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);
		nf = NumberFormat.getInstance(Locale.ENGLISH);
		nf.setMaximumFractionDigits(14);

		hopper = new GraphHopper().forServer();
		hopper.setOSMFile("./osm/nantes_france.osm.pbf");
		hopper.setGraphHopperLocation("./gh/");
		hopper.setEncodingManager(new EncodingManager("foot"));
		hopper.importOrLoad();

		GtfsRelationalDao gtfsDao = getGtfsDao(new File("ARRETS_HORAIRES_CIRCUITS_TAN_GTFS_2015.zip"));

		shapes = new HashMap<>();
		List<AgencyAndId> allShapeIds = gtfsDao.getAllShapeIds();
		GeometryFactory geometryFactory = new GeometryFactory();
		for (AgencyAndId shapeId : allShapeIds) {
			List<ShapePoint> points = gtfsDao.getShapePointsForShapeId(shapeId);
			Coordinate[] coordinates = points.stream().map(sp -> new Coordinate(sp.getLon(), sp.getLat()))
					.toArray(size -> new Coordinate[size]);
			LineString shape = geometryFactory.createLineString(coordinates);
			shapes.put(shapeId.getId(), shape);
		}

		calculerItineraire(gtfsDao, LocalDate.of(2015, 05, 11), LocalTime.of(17, 12), "Polytech'", "Vertou");
		calculerItineraire(gtfsDao, LocalDate.of(2015, 06, 9), LocalTime.of(7, 43), "Californie", "Sautron");
		calculerItineraire(gtfsDao, LocalDate.of(2015, 06, 27), LocalTime.of(12, 37), "La Plée", "Couëron");
		calculerItineraire(gtfsDao, LocalDate.of(2015, 3, 2), LocalTime.of(7, 47), "Bio Ouest Laënnec",
				"Gare de Chantenay");

		calculerItineraire(gtfsDao, LocalDate.of(2015, 05, 9), LocalTime.of(21, 37), "Dolto", "Mangin");
		calculerItineraire(gtfsDao, LocalDate.of(2015, 05, 9), LocalTime.of(21, 37), "Dolto", "Leygues");
		calculerItineraire(gtfsDao, LocalDate.of(2015, 05, 9), LocalTime.of(21, 37), "Dolto", "Monzie");
		calculerItineraire(gtfsDao, LocalDate.of(2015, 05, 9), LocalTime.of(21, 37), "Dolto", "Mandel");
		calculerItineraire(gtfsDao, LocalDate.of(2015, 05, 9), LocalTime.of(21, 37), "Dolto", "Beaulieu");

		List<Stop> allStops = new ArrayList<>(gtfsDao.getAllStops());
		Random r = new Random(System.nanoTime());
		IntStream.range(0, 100).forEach(i -> calculerAleatoire(gtfsDao, allStops, r));
	}

	private void calculerAleatoire(GtfsRelationalDao gtfsDao, List<Stop> allStops, Random r) {
		LocalDate date = LocalDate.of(2015, 1 + r.nextInt(5), 1 + r.nextInt(27));
		LocalTime time = LocalTime.of(5 + r.nextInt(12), r.nextInt(60));

		Stop s1 = allStops.get(r.nextInt(allStops.size()));
		Stop s2;
		do {
			s2 = allStops.get(r.nextInt(allStops.size()));
		} while (getStationId(s1).equals(getStationId(s2)));

		calculerItineraire(gtfsDao, date, time, s1.getName(), s2.getName());
	}

	private void calculerItineraire(GtfsRelationalDao gtfsDao, LocalDate departJour, LocalTime departHeure,
			String departArret, String arriveeArret) {
		Comparator<StopTime> stopTimeComparator = (st1, st2) -> Integer.compare(st1.getArrivalTime(),
				st2.getArrivalTime());

		List<StopTime> stopTimes = calculerArretsPotentiels(gtfsDao, departJour, departHeure, stopTimeComparator);

		// Liste des trips ids empruntables
		Map<String, StopTime> tripIds = new HashMap<String, StopTime>();
		// Dernier trajet de la station
		Map<String, Trajet> trajetParStation = new TreeMap<>();

		// Premier arrêt correspondant à la station de départ
		Stop stopDepart = gtfsDao.getAllStops().parallelStream().filter(s -> s.getName().equals(departArret))
				.findFirst().get();
		// ids des stations d'arrivée
		Set<String> stopsArrivees = gtfsDao.getAllStops().parallelStream().filter(s -> s.getName().equals(arriveeArret))
				.map(s -> s.getId().getId()).collect(Collectors.toSet());

		// initialisation
		trajetParStation.put(getStationId(stopDepart),
				new Trajet(null, stopDepart, departHeure.toSecondOfDay(), departHeure.toSecondOfDay(), null));

		// ajout des stations accessibles à pied
		Integer meilleurTempsPieton = calculerPieton(gtfsDao, departHeure.toSecondOfDay(), trajetParStation, stopDepart,
				stopsArrivees);

		for (StopTime arret : stopTimes) {
			String stationId = getStationId(arret.getStop());
			String tripId = getTripId(arret);
			Trajet trajetPrecedent = trajetParStation.get(stationId);
			StopTime stationDepart = null;

			if (tripIds.containsKey(tripId)) {
				// sur un trajet que l'on connait deja

				// si on y a accédé via un autre chemin
				if (trajetPrecedent != null) {
					// si on le fait dans un meilleur temps
					if (trajetPrecedent.arrivee > arret.getArrivalTime()) {
						stationDepart = tripIds.get(tripId);
					}
				} else {
					// premier temps -> meilleur temps
					stationDepart = tripIds.get(tripId);
				}
			} else if (trajetPrecedent != null && trajetPrecedent.arrivee < arret.getDepartureTime()) {
				// on ne connait pas ce trajet et on peut prendre ce trajet
				tripIds.put(tripId, arret);
			}

			if (stationDepart != null) {
				trajetPrecedent = new Trajet(stationDepart.getStop(), arret.getStop(), stationDepart.getDepartureTime(),
						arret.getArrivalTime(), arret.getTrip());
				// on ne pourra pas partir plus tôt de cet arrêt
				trajetParStation.put(stationId, trajetPrecedent);
				meilleurTempsPieton = calculerPieton(gtfsDao, arret.getArrivalTime(), trajetParStation, arret.getStop(),
						stopsArrivees);
			}

			String arriveeId = null;
			if (arret.getArrivalTime() > meilleurTempsPieton) {
				final int innerMeilleurTempsPieton = meilleurTempsPieton;
				arriveeId = trajetParStation.entrySet().stream().filter(e -> stopsArrivees.contains(e.getKey()))
						.filter(e -> e.getValue().arrivee == innerMeilleurTempsPieton).findFirst().get().getKey();
			} else if (arriveeArret.equals(arret.getStop().getName()) && trajetPrecedent != null
					&& trajetPrecedent.trip != null) {
				arriveeId = getStationId(arret.getStop());
			}

			if (arriveeId != null) {
				String nom = departJour.getDayOfYear() + "-" + departHeure.toSecondOfDay() + "-"
						+ departArret.replaceAll("[^a-zA-Z0-9.-]", "_") + "-"
						+ arriveeArret.replaceAll("[^a-zA-Z0-9.-]", "_");
				afficherParcours(trajetParStation, stopDepart, arriveeId, nom);
				break;
			}

		}

	}

	private List<StopTime> calculerArretsPotentiels(GtfsRelationalDao gtfsDao, LocalDate departJour,
			LocalTime departHeure, Comparator<StopTime> stopTimeComparator) {
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
		List<StopTime> stopTimes = gtfsDao.getAllStopTimes().parallelStream()
				.filter(st -> tripsJour.contains(getTripId(st)))
				.filter(st -> st.getDepartureTime() >= departHeure.toSecondOfDay()).sorted(stopTimeComparator)
				.collect(Collectors.toList());
		return stopTimes;
	}

	private Integer calculerPieton(GtfsRelationalDao gtfsDao, int heureDepart, Map<String, Trajet> heuresArrivee,
			Stop stopDepart, Set<String> stopsArrivees) {
		gtfsDao.getAllStops().parallelStream().filter(otherStop -> calculerDistance(stopDepart, otherStop) < 1000)
				.forEach(stopPieton -> {
					String stationId = getStationId(stopPieton);
					int arrivee = heureDepart + calculerTempsPieton(stopDepart, stopPieton);
					if (heuresArrivee.get(stationId) == null || heuresArrivee.get(stationId).arrivee > arrivee) {
						heuresArrivee.put(stationId, new Trajet(stopDepart, stopPieton, heureDepart, arrivee, null));
					}
				} );

		return heuresArrivee.entrySet().stream().filter(e -> stopsArrivees.contains(e.getKey()))
				.map(e -> e.getValue().arrivee).min(Integer::min).orElse(Integer.MAX_VALUE);
	}

	private int calculerDistance(Stop depart, Stop arrivee) {
		double latFrom = depart.getLat();
		double lonFrom = depart.getLon();
		double latTo = arrivee.getLat();
		double lonTo = arrivee.getLon();

		double dist = Math.sin(deg2rad(latFrom)) * Math.sin(deg2rad(latTo))
				+ Math.cos(deg2rad(latFrom)) * Math.cos(deg2rad(latTo)) * Math.cos(deg2rad(lonFrom - lonTo));
		dist = Math.acos(Math.max(-1.0, Math.min(1.0, dist))) * 6371000;
		return (int) dist;
	}

	private double deg2rad(double deg) {
		return (deg * Math.PI / 180.0);
	}

	private int calculerTempsPieton(Stop depart, Stop arrivee) {
		String key = getStationId(depart) + "-" + getStationId(arrivee);
		if (tempsPieton.containsKey(key)) {
			return tempsPieton.get(key);
		}

		double latFrom = depart.getLat();
		double lonFrom = depart.getLon();
		double latTo = arrivee.getLat();
		double lonTo = arrivee.getLon();
		GHRequest req = new GHRequest(latFrom, lonFrom, latTo, lonTo);
		GHResponse rsp = hopper.route(req);

		if (rsp.hasErrors()) {
			rsp.getErrors().forEach(e -> e.printStackTrace(System.err));
			return Integer.MAX_VALUE;
		}
		int resultat = (int) (rsp.getMillis() / 1000);
		tempsPieton.put(key, resultat);
		tempsPieton.put(getStationId(arrivee) + "-" + getStationId(depart), resultat);
		return resultat;
	}

	private String getTripId(StopTime st) {
		return st.getTrip().getId().getId();
	}

	private String getStationId(Stop stop) {
		return stop.getId().getId();
	}

	private void afficherParcours(Map<String, Trajet> heuresArrivee, Stop stopDepart, String stopArriveeId,
			String nom) {
		List<Trajet> trajets = new ArrayList<>();
		Trajet heureArrivee = heuresArrivee.get(stopArriveeId);
		do {
			trajets.add(heureArrivee);
			String stationId = getStationId(heureArrivee.stationDepart);
			heureArrivee = heuresArrivee.get(stationId);
		} while (heureArrivee != null && heureArrivee.stationDepart != null);

		Collections.reverse(trajets);

		System.out.println("*****************");
		trajets.forEach(
				a -> System.out
						.println(timeFormatter.format(LocalTime.ofSecondOfDay(a.depart % 86400)) + " "
								+ a.stationDepart.getName() + " -> "
								+ timeFormatter.format(LocalTime.ofSecondOfDay(a.arrivee % 86400)) + " "
								+ a.stationArrivee.getName() + " ("
								+ (a.trip == null ? "à pied"
										: a.trip.getRoute().getShortName() + " (-> " + a.trip.getTripHeadsign() + ")")
								+ ")"));
		System.out.println("*****************");

		StringBuilder sb = new StringBuilder();
		sb.append(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><kml xmlns=\"http://earth.google.com/kml/2.0\"><Document><Placemark><MultiGeometry>\r\n");

		for (Trajet trajet : trajets) {
			if (trajet.trip == null) {
				ajouterPoints(sb, trajet.stationDepart, trajet.stationArrivee);
			} else {
				LineString shape = shapes.get(trajet.trip.getShapeId().getId());
				if (shape.getNumPoints() > 0) {
					Coordinate debut = new Coordinate(trajet.stationDepart.getLon(), trajet.stationDepart.getLat());
					Coordinate fin = new Coordinate(trajet.stationArrivee.getLon(), trajet.stationArrivee.getLat());
					LengthToPoint ldebut = new LengthToPoint(shape, debut);
					LengthToPoint lfin = new LengthToPoint(shape, fin);

					LengthSubstring lengthSubstring = new LengthSubstring(shape);
					LineString substring = lengthSubstring.getSubstring(ldebut.getLength(), lfin.getLength());
					sb.append("<LineString><coordinates>\r\n");
					Arrays.asList(substring.getCoordinates())
							.forEach(c -> sb.append(nf.format(c.x) + "," + nf.format(c.y) + ",0\r\n"));
					sb.append("</coordinates></LineString>\r\n");
				} else {
					System.err.println("Trajet manquant... : " + trajet.trip.getShapeId().getId());
				}
			}
		}
		sb.append("</MultiGeometry></Placemark></Document></kml>");
		try {
			Files.write(Paths.get("kml/" + nom + ".kml"), sb.toString().getBytes());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private void ajouterPoints(StringBuilder sb, Stop depart, Stop arrivee) {
		double latFrom = depart.getLat();
		double lonFrom = depart.getLon();
		double latTo = arrivee.getLat();
		double lonTo = arrivee.getLon();
		GHRequest req = new GHRequest(latFrom, lonFrom, latTo, lonTo);
		GHResponse rsp = hopper.route(req);
		sb.append("<LineString><coordinates>\r\n");
		rsp.getPoints().forEach(p -> sb.append(nf.format(p.getLon()) + "," + nf.format(p.getLat()) + ",0\r\n"));
		sb.append("</coordinates></LineString>\r\n");
	}

}
