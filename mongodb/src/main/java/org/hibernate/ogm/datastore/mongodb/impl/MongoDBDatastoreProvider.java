/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012-2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.mongodb.impl;

import java.net.UnknownHostException;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.ogm.datastore.mongodb.AssociationDocumentType;
import org.hibernate.ogm.datastore.mongodb.impl.configuration.MongoDBConfiguration;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.mongodb.MongoDBDialect;
import org.hibernate.ogm.dialect.mongodb.query.parsing.MongoDBBasedQueryParserService;
import org.hibernate.ogm.logging.mongodb.impl.Log;
import org.hibernate.ogm.logging.mongodb.impl.LoggerFactory;
import org.hibernate.ogm.options.generic.document.AssociationStorageType;
import org.hibernate.ogm.service.impl.QueryParserService;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;

/**
 * Provides access to MongoDB system
 *
 * @author Guillaume Scheibel<guillaume.scheibel@gmail.com>
 */
public class MongoDBDatastoreProvider implements DatastoreProvider, Startable, Stoppable, Configurable {

	private static final Log log = LoggerFactory.getLogger();

	private boolean isCacheStarted;
	private MongoClient mongo;
	private DB mongoDb;
	private MongoDBConfiguration config;

	@Override
	public void configure(Map configurationValues) {
		this.config = new MongoDBConfiguration( configurationValues );
	}

	public AssociationStorageType getAssociationStorage() {
		return config.getAssociationStorageStrategy();
	}

	public AssociationDocumentType getAssociationDocumentStorage() {
		return config.getAssociationDocumentStorage();
	}

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return MongoDBDialect.class;
	}

	@Override
	public Class<? extends QueryParserService> getDefaultQueryParserServiceType() {
		return MongoDBBasedQueryParserService.class;
	}

	@Override
	public void start() {
		if ( !isCacheStarted ) {
			try {
				ServerAddress serverAddress = new ServerAddress( config.getHost(), config.getPort() );
				MongoClientOptions clientOptions = config.buildOptions();

				log.connectingToMongo( config.getHost(), config.getPort(), clientOptions.getConnectTimeout() );

				this.mongo = new MongoClient( serverAddress, clientOptions );
				this.isCacheStarted = true;
			}
			catch ( UnknownHostException e ) {
				throw log.mongoOnUnknownHost( config.getHost() );
			}
			catch ( RuntimeException e ) {
				throw log.unableToInitializeMongoDB( e );
			}
			mongoDb = extractDatabase();
		}
	}

	@Override
	public void stop() {
		log.disconnectingFromMongo();
		this.mongo.close();
	}

	public DB getDatabase() {
		return mongoDb;
	}

	private DB extractDatabase() {
		try {
			if ( config.getUsername() != null ) {
				DB admin = this.mongo.getDB( "admin" );
				boolean auth = admin.authenticate( config.getUsername(), config.getPassword().toCharArray() );
				if ( !auth ) {
					throw log.authenticationFailed( config.getUsername() );
				}
			}
			String databaseName = config.getDatabaseName();
			log.connectingToMongoDatabase( databaseName );

			if ( !this.mongo.getDatabaseNames().contains( databaseName ) ) {
				log.creatingDatabase( databaseName );
			}
			return this.mongo.getDB( databaseName );
		}
		catch ( HibernateException e ) {
			throw e;
		}
		catch ( Exception e ) {
			throw log.unableToConnectToDatastore( this.config.getHost(), this.config.getPort(), e );
		}
	}

}
