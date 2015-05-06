package org.glandais.jug.java8;

import java.io.File;
import java.io.IOException;

import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.services.GtfsRelationalDao;

public interface GtfsTan {

	public default GtfsRelationalDao getGtfsDao(File fichierGtfs) {
		try {
			GtfsReader reader = new GtfsReader();
			reader.setDefaultAgencyId("TAN");
			reader.setInternStrings(false);

			reader.setInputLocation(fichierGtfs);

			GtfsRelationalDaoImpl entityStore = new GtfsRelationalDaoImpl();
			entityStore.setGenerateIds(true);
			reader.setEntityStore(entityStore);

			reader.run();
			return entityStore;
		} catch (IOException e) {
			throw new IllegalStateException();
		}
	}

}
