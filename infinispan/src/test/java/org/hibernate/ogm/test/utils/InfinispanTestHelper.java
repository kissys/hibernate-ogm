/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011-2014 Red Hat Inc. and/or its affiliates and other contributors
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

import static org.hibernate.ogm.datastore.spi.DefaultDatastoreNames.ASSOCIATION_STORE;
import static org.hibernate.ogm.datastore.spi.DefaultDatastoreNames.ENTITY_STORE;

import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.datastore.infinispan.Infinispan;
import org.hibernate.ogm.datastore.infinispan.impl.InfinispanDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.options.generic.document.AssociationStorageType;
import org.hibernate.ogm.options.navigation.context.GlobalContext;
import org.infinispan.Cache;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class InfinispanTestHelper implements TestableGridDialect {

	@Override
	public long getNumberOfEntities(SessionFactory sessionFactory) {
		return getEntityCache( sessionFactory ).size();
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory) {
		return getAssociationCache( sessionFactory ).size();
	}

	@Override
	public Map<String, Object> extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		return (Map) getEntityCache( sessionFactory ).get( key );
	}

	private static Cache getEntityCache(SessionFactory sessionFactory) {
		InfinispanDatastoreProvider castProvider = getProvider( sessionFactory );
		return castProvider.getCache( ENTITY_STORE );
	}

	public static InfinispanDatastoreProvider getProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry().getService( DatastoreProvider.class );
		if ( !( InfinispanDatastoreProvider.class.isInstance( provider ) ) ) {
			throw new RuntimeException( "Not testing with Infinispan, cannot extract underlying cache" );
		}
		return InfinispanDatastoreProvider.class.cast( provider );
	}

	private static Cache getAssociationCache(SessionFactory sessionFactory) {
		InfinispanDatastoreProvider castProvider = getProvider( sessionFactory );
		return castProvider.getCache( ASSOCIATION_STORE );
	}

	@Override
	public boolean backendSupportsTransactions() {
		return true;
	}

	@Override
	public void dropSchemaAndDatabase(SessionFactory sessionFactory) {
		// Nothing to do
	}

	@Override
	public Map<String, String> getEnvironmentProperties() {
		return null;
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory, AssociationStorageType type) {
		throw new UnsupportedOperationException( "This datastore does not support different association storage strategies." );
	}

	@Override
	public long getNumberOEmbeddedCollections(SessionFactory sessionFactory) {
		throw new UnsupportedOperationException( "This datastore does not support storing collections embedded within entities." );
	}

	@Override
	public GlobalContext<?, ?> configureDatastore(OgmConfiguration configuration) {
		return configuration.configureOptionsFor( Infinispan.class );
	}
}
