/*
 * Copyright (C) 2015-2020 52°North Initiative for Geospatial Open Source
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
package org.n52.series.db.assembler.core;

import java.util.Optional;

import javax.inject.Inject;

import org.n52.io.request.IoParameters;
import org.n52.series.db.DataRepositoryTypeFactory;
import org.n52.series.db.assembler.sampling.MeasuringProgramAssembler;
import org.n52.series.db.assembler.sampling.SamplingAssembler;
import org.n52.series.db.old.dao.DbQuery;
import org.n52.series.db.old.dao.DbQueryFactory;
import org.springframework.stereotype.Component;

@Component
public class EntityCounter {

    @Inject
    private CategoryAssembler categoryAssembler;

    @Inject
    private FeatureAssembler featureAssembler;

    @Inject
    private OfferingAssembler offeringAssembler;

    @Inject
    private PhenomenonAssembler phenomenonAssembler;

    @Inject
    private ProcedureAssembler procedureAssembler;

    @Inject
    private PlatformAssembler platformAssembler;

    @Inject
    private Optional<MeasuringProgramAssembler> measuringProgramAssembler;

    @Inject
    private Optional<SamplingAssembler> samplingAssembler;

    private final DbQueryFactory dbQueryFactory;

    private final DataRepositoryTypeFactory dataRepositoryFactory;

    public EntityCounter(DbQueryFactory dbQueryFactory, DataRepositoryTypeFactory dataRepositoryFactory) {
        this.dbQueryFactory = dbQueryFactory;
        this.dataRepositoryFactory = dataRepositoryFactory;
    }

    public Long countFeatures(DbQuery query) {
        return featureAssembler.count(query);
    }

    public Long countOfferings(DbQuery query) {
        return offeringAssembler.count(query);
    }

    public Long countProcedures(DbQuery query) {
        return procedureAssembler.count(query);
    }

    public Long countPhenomena(DbQuery query) {
        return phenomenonAssembler.count(query);
    }

    public Long countCategories(DbQuery query) {
        return categoryAssembler.count(query);
    }

    public Long countPlatforms(DbQuery query) {
        return platformAssembler.count(query);
    }

    public Long countDatasets(DbQuery query) {
        IoParameters parameters = query.getParameters();
        if (parameters.getValueTypes().isEmpty()) {
            parameters =
                    parameters.extendWith("valueTypes", dataRepositoryFactory.getKnownTypes().toArray(new String[0]));
            return platformAssembler.count(dbQueryFactory.createFrom(parameters));
        }
        return platformAssembler.count(query);
    }

    public Long countSamplings(DbQuery query) {
        return measuringProgramAssembler.isPresent() ? measuringProgramAssembler.get().count(query) : null;
    }

    public Long countMeasuringPrograms(DbQuery query) {
        return samplingAssembler.isPresent() ? samplingAssembler.get().count(query) : null;
    }

    public Long countTimeseries(DbQuery query) {
        return countDataset(query, "timeseries");
    }

    public Long countIndividualObservations(DbQuery query) {
        return countDataset(query, "individualObservation");
    }

    public Long countTrajectories(DbQuery query) {
        return countDataset(query, "trajectory");
    }

    public Long countProfiles(DbQuery query) {
        return countDataset(query, "profile");
    }

    private Long countDataset(DbQuery query, String datasetType) {
        IoParameters parameters = query.getParameters();
        parameters = parameters.extendWith("datasetTypes", datasetType);
        return platformAssembler.count(dbQueryFactory.createFrom(parameters));
    }

}