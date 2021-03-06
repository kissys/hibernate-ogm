/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013-2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.test.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.couchdb.CouchDB;
import org.hibernate.ogm.datastore.couchdb.impl.CouchDBDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.couchdb.backend.impl.CouchDBDatastore;
import org.hibernate.ogm.dialect.couchdb.backend.json.impl.EntityDocument;
import org.hibernate.ogm.dialect.couchdb.backend.json.impl.GenericResponse;
import org.hibernate.ogm.dialect.couchdb.model.impl.CouchDBTupleSnapshot;
import org.hibernate.ogm.dialect.couchdb.util.impl.Identifier;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.logging.couchdb.impl.Log;
import org.hibernate.ogm.logging.couchdb.impl.LoggerFactory;
import org.hibernate.ogm.options.generic.document.AssociationStorageType;
import org.hibernate.ogm.options.navigation.context.GlobalContext;
import org.hibernate.ogm.test.utils.backend.facade.DatabaseTestClient;
import org.hibernate.ogm.test.utils.backend.json.AssociationCountResponse;
import org.hibernate.ogm.test.utils.backend.json.EntityCountResponse;
import org.hibernate.ogm.test.utils.backend.json.designdocument.AssociationsDesignDocument;
import org.hibernate.ogm.test.utils.backend.json.designdocument.EntitiesDesignDocument;
import org.jboss.resteasy.client.exception.ResteasyClientException;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

/**
 * Testing infrastructure for CouchDB.
 *
 * @author Andrea Boriero <dreborier@gmail.com/>
 * @author Gunnar Morling
 */
public class CouchDBTestHelper implements TestableGridDialect {

	private static final Log logger = LoggerFactory.getLogger();

	@Override
	public long getNumberOfEntities(SessionFactory sessionFactory) {
		return getNumberOfEntities( getDataStore( sessionFactory ) );
	}

	public long getNumberOfEntities(CouchDBDatastore dataStore) {
		DatabaseTestClient databaseTestClient = getDatabaseTestClient( dataStore );

		Response response = null;

		try {
			response = databaseTestClient.getNumberOfEntities();
			if ( response.getStatus() == Response.Status.OK.getStatusCode() ) {
				return response.readEntity( EntityCountResponse.class ).getCount();
			}
			else {
				GenericResponse responseEntity = response.readEntity( GenericResponse.class );
				throw logger.unableToRetrieveTheNumberOfEntities( response.getStatus(), responseEntity.getError(), responseEntity.getReason() );
			}
		}
		catch (ResteasyClientException e) {
			throw logger.couchDBConnectionProblem( e );
		}
		finally {
			if ( response != null ) {
				response.close();
			}
		}
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory, AssociationStorageType type) {
		DatabaseTestClient databaseTestClient = getDatabaseTestClient( getDataStore( sessionFactory ) );
		Long count = getNumberOfAssociations( databaseTestClient ).get( type );
		return count != null ? count : 0;
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory) {
		DatabaseTestClient databaseTestClient = getDatabaseTestClient( getDataStore( sessionFactory ) );

		Map<AssociationStorageType, Long> associationCountByType = getNumberOfAssociations( databaseTestClient );
		long totalCount = 0;
		for ( long count : associationCountByType.values() ) {
			totalCount += count;
		}
		return totalCount;
	}

	/**
	 * Retrieves the number of associations stored in the database
	 *
	 * @return the number of associations stored in the database
	 */
	public Map<AssociationStorageType, Long> getNumberOfAssociations(DatabaseTestClient databaseTestClient) {
		Response response = null;
		try {
			response = databaseTestClient.getNumberOfAssociations( true );
			if ( response.getStatus() == Response.Status.OK.getStatusCode() ) {
				AssociationCountResponse countResponse = response.readEntity( AssociationCountResponse.class );

				Map<AssociationStorageType, Long> countsByType = new HashMap<AssociationStorageType, Long>( 2 );
				countsByType.put( AssociationStorageType.IN_ENTITY, countResponse.getInEntityAssociationCount() );
				countsByType.put( AssociationStorageType.ASSOCIATION_DOCUMENT, countResponse.getAssociationDocumentCount() );

				return countsByType;
			}
			else {
				GenericResponse responseEntity = response.readEntity( GenericResponse.class );
				throw logger.unableToRetrieveTheNumberOfAssociations( response.getStatus(), responseEntity.getError(), responseEntity.getReason() );
			}
		}
		catch (ResteasyClientException e) {
			throw logger.couchDBConnectionProblem( e );
		}
		finally {
			if ( response != null ) {
				response.close();
			}
		}
	}

	@Override
	public long getNumberOEmbeddedCollections(SessionFactory sessionFactory) {
		DatabaseTestClient databaseTestClient = getDatabaseTestClient( getDataStore( sessionFactory ) );

		Response response = null;
		try {
			response = databaseTestClient.getNumberOfAssociations( true );
			if ( response.getStatus() == Response.Status.OK.getStatusCode() ) {
				AssociationCountResponse countResponse = response.readEntity( AssociationCountResponse.class );
				return countResponse.getEmbeddedCollectionCount();
			}
			else {
				GenericResponse responseEntity = response.readEntity( GenericResponse.class );
				throw logger.unableToRetrieveTheNumberOfAssociations( response.getStatus(), responseEntity.getError(), responseEntity.getReason() );
			}
		}
		catch (ResteasyClientException e) {
			throw logger.couchDBConnectionProblem( e );
		}
		finally {
			if ( response != null ) {
				response.close();
			}
		}
	}

	@Override
	public Map<String, Object> extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		Map<String, Object> tupleMap = new HashMap<String, Object>();
		CouchDBDatastore dataStore = getDataStore( sessionFactory );
		EntityDocument entity = dataStore.getEntity( Identifier.createEntityId( key ) );
		CouchDBTupleSnapshot snapshot = new CouchDBTupleSnapshot( entity.getProperties() );
		Set<String> columnNames = snapshot.getColumnNames();
		for ( String columnName : columnNames ) {
			tupleMap.put( columnName, snapshot.get( columnName ) );
		}
		return tupleMap;
	}

	@Override
	public boolean backendSupportsTransactions() {
		return false;
	}

	@Override
	public void dropSchemaAndDatabase(SessionFactory sessionFactory) {
		getDataStore( sessionFactory ).dropDatabase();
	}

	@Override
	public Map<String, String> getEnvironmentProperties() {
		Map<String, String> envProps = new HashMap<String, String>( 2 );
		copyFromSystemPropertiesToLocalEnvironment( OgmProperties.HOST, envProps );
		copyFromSystemPropertiesToLocalEnvironment( OgmProperties.PORT, envProps );
		copyFromSystemPropertiesToLocalEnvironment( OgmProperties.DATABASE, envProps );
		copyFromSystemPropertiesToLocalEnvironment( OgmProperties.USERNAME, envProps );
		copyFromSystemPropertiesToLocalEnvironment( OgmProperties.PASSWORD, envProps );
		copyFromSystemPropertiesToLocalEnvironment( OgmProperties.CREATE_DATABASE, envProps );
		return envProps;
	}

	private void copyFromSystemPropertiesToLocalEnvironment(String environmentVariableName, Map<String, String> envProps) {
		String value = System.getProperties().getProperty( environmentVariableName );
		if ( value != null && value.length() > 0 ) {
			envProps.put( environmentVariableName, value );
		}
	}

	private CouchDBDatastore getDataStore(SessionFactory sessionFactory) {
		DatastoreProvider provider = ( (SessionFactoryImplementor) sessionFactory )
				.getServiceRegistry()
				.getService( DatastoreProvider.class );

		if ( !( provider instanceof CouchDBDatastoreProvider ) ) {
			throw new RuntimeException( "DatastoreProvider is not an instance of " + CouchDBDatastoreProvider.class );
		}

		return ( (CouchDBDatastoreProvider) provider ).getDataStore();
	}

	private DatabaseTestClient getDatabaseTestClient(CouchDBDatastore dataStore) {
		if ( !dataStore.exists( AssociationsDesignDocument.DOCUMENT_ID, true ) ) {
			dataStore.saveDocument( new AssociationsDesignDocument() );
		}
		if ( !dataStore.exists( EntitiesDesignDocument.DOCUMENT_ID, true ) ) {
			dataStore.saveDocument( new EntitiesDesignDocument() );
		}

		Client client = ClientBuilder.newBuilder().build();
		WebTarget target = client.target( dataStore.getDatabaseIdentifier().getDatabaseUri() );
		ResteasyWebTarget rtarget = (ResteasyWebTarget) target;

		return rtarget.proxy( DatabaseTestClient.class );
	}

	@Override
	public GlobalContext<?, ?> configureDatastore(OgmConfiguration configuration) {
		return configuration.configureOptionsFor( CouchDB.class );
	}
}
