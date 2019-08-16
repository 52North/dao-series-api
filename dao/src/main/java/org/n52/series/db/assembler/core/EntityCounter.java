/*
 * Copyright (C) 2015-2019 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public License
 * version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package org.n52.series.db.old.da;

import org.hibernate.Session;
import org.n52.io.request.IoParameters;
import org.n52.series.db.DataRepositoryTypeFactory;
import org.n52.series.db.old.HibernateSessionStore;
import org.n52.series.db.old.dao.AbstractDao;
import org.n52.series.db.old.dao.CategoryDao;
import org.n52.series.db.old.dao.DatasetDao;
import org.n52.series.db.old.dao.DbQuery;
import org.n52.series.db.old.dao.DbQueryFactory;
import org.n52.series.db.old.dao.FeatureDao;
import org.n52.series.db.old.dao.MeasuringProgramDao;
import org.n52.series.db.old.dao.OfferingDao;
import org.n52.series.db.old.dao.PhenomenonDao;
import org.n52.series.db.old.dao.PlatformDao;
import org.n52.series.db.old.dao.ProcedureDao;
import org.n52.series.db.old.dao.SamplingDao;
import org.springframework.stereotype.Component;

@Component
public class EntityCounter {

    private final HibernateSessionStore sessionStore;

    private final DbQueryFactory dbQueryFactory;

    private final DataRepositoryTypeFactory dataRepositoryFactory;

    public EntityCounter(HibernateSessionStore sessionStore,
                         DbQueryFactory dbQueryFactory,
                         DataRepositoryTypeFactory dataRepositoryFactory) {
        this.sessionStore = sessionStore;
        this.dbQueryFactory = dbQueryFactory;
        this.dataRepositoryFactory = dataRepositoryFactory;
    }

    public Integer countFeatures(DbQuery query) {
        Session session = sessionStore.getSession();
        try {
            return getCount(new FeatureDao(session), query);
        } finally {
            sessionStore.returnSession(session);
        }
    }

    public Integer countOfferings(DbQuery query) {
        Session session = sessionStore.getSession();
        try {
            return getCount(new OfferingDao(session), query);
        } finally {
            sessionStore.returnSession(session);
        }
    }

    public Integer countProcedures(DbQuery query) {
        Session session = sessionStore.getSession();
        try {
            return getCount(new ProcedureDao(session), query);
        } finally {
            sessionStore.returnSession(session);
        }
    }

    public Integer countPhenomena(DbQuery query) {
        Session session = sessionStore.getSession();
        try {
            return getCount(new PhenomenonDao(session), query);
        } finally {
            sessionStore.returnSession(session);
        }
    }

    public Integer countCategories(DbQuery query) {
        Session session = sessionStore.getSession();
        try {
            return getCount(new CategoryDao(session), query);
        } finally {
            sessionStore.returnSession(session);
        }
    }

    public Integer countPlatforms(DbQuery query) {
        Session session = sessionStore.getSession();
        try {
            return getCount(new PlatformDao(session), query);
        } finally {
            sessionStore.returnSession(session);
        }
    }

    public Integer countDatasets(DbQuery query) {
        Session session = sessionStore.getSession();
        try {
            IoParameters parameters = query.getParameters();
            if (parameters.getValueTypes().isEmpty()) {
                parameters = parameters.extendWith(
                        "valueTypes",
                        dataRepositoryFactory.getKnownTypes().toArray(new String[0])
                );
                return getCount(new DatasetDao<>(session),
                                dbQueryFactory.createFrom(parameters));
            }
            return getCount(new DatasetDao<>(session), query);
        } finally {
            sessionStore.returnSession(session);
        }
    }

    public int countSamplings(DbQuery query) {
        Session session = sessionStore.getSession();
        try {
            return getCount(new SamplingDao(session), query);
        } finally {
            sessionStore.returnSession(session);
        }
    }

    public int countMeasuringPrograms(DbQuery query) {
        Session session = sessionStore.getSession();
        try {
            return getCount(new MeasuringProgramDao(session), query);
        } finally {
            sessionStore.returnSession(session);
        }
    }

    public Integer countStations() {
        Session session = sessionStore.getSession();
        try {
            DbQuery query = createBackwardsCompatibleQuery();
            return countFeatures(query);
        } finally {
            sessionStore.returnSession(session);
        }
    }


    @Deprecated
    public Integer countTimeseries() {
        Session session = sessionStore.getSession();
        try {
            DbQuery query = createBackwardsCompatibleQuery();
            return countDatasets(query);
        } finally {
            sessionStore.returnSession(session);
        }
    }

    public Integer countTimeseries(DbQuery query) {
        return countDataset(query, "timeseries");
    }

    public Integer countIndividualObservations(DbQuery query) {
        return countDataset(query, "individualObservation");
    }

    public Integer countTrajectories(DbQuery query) {
        return countDataset(query, "trajectory");
    }

    public Integer countProfiles(DbQuery query) {
        return countDataset(query, "profile");
    }

    private Integer countDataset(DbQuery query, String datasetType) {
        Session session = sessionStore.getSession();
        try {
            IoParameters parameters = query.getParameters();
            parameters = parameters.extendWith("datasetTypes", datasetType);
            return getCount(new DatasetDao<>(session), dbQueryFactory.createFrom(parameters));
        } finally {
            sessionStore.returnSession(session);
        }
    }

    public Integer getCount(AbstractDao< ? > dao, DbQuery query) {
        return dao.getCount(query);
    }

    private DbQuery createBackwardsCompatibleQuery() {
        IoParameters parameters = IoParameters.createDefaults();
        // parameters = parameters.extendWith(Parameters.FILTER_PLATFORM_TYPES,
        // "stationary", "insitu")
        //     .extendWith(Parameters.FILTER_VALUE_TYPES, ValueType.DEFAULT_VALUE_TYPE);
        return dbQueryFactory.createFrom(parameters);
    }

}