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
package org.n52.series.db.assembler.value;

import java.util.ArrayList;
import java.util.List;

import org.n52.io.response.dataset.profile.ProfileDataItem;
import org.n52.io.response.dataset.profile.ProfileValue;
import org.n52.io.response.dataset.text.TextValue;
import org.n52.series.db.ValueAssemblerComponent;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.ProfileDataEntity;
import org.n52.series.db.beans.TextDataEntity;
import org.n52.series.db.old.dao.DbQuery;
import org.n52.series.db.repositories.core.DataRepository;
import org.n52.series.db.repositories.core.DatasetRepository;

@ValueAssemblerComponent(value = "text-quantity", datasetEntityType = DatasetEntity.class)
public class TextProfileValueAssembler extends ProfileValueAssembler<String, String> {

    public TextProfileValueAssembler(DataRepository<ProfileDataEntity> profileDataRepository,
            DatasetRepository datasetRepository) {
        super(profileDataRepository, datasetRepository);
    }

    @Override
    public ProfileValue<String> assembleDataValue(ProfileDataEntity observation, DatasetEntity datasetEntity,
            DbQuery query) {
        ProfileValue<String> profile = prepareValue(new ProfileValue<>(), observation, query);
        return assembleDataValue(observation, datasetEntity, query, profile);
    }

    private ProfileValue<String> assembleDataValue(ProfileDataEntity observation, DatasetEntity datasetEntity,
            DbQuery query, ProfileValue<String> profile) {
        profile.setValue(getDataValue(observation, datasetEntity, profile, query));
        return profile;
    }

    private List<ProfileDataItem<String>> getDataValue(ProfileDataEntity observation, DatasetEntity datasetEntity,
            ProfileValue<String> profile, DbQuery query) {
        List<ProfileDataItem<String>> dataItems = new ArrayList<>();
        for (DataEntity<?> dataEntity : observation.getValue()) {
            TextDataEntity quantityEntity = (TextDataEntity) dataEntity;
            TextValue valueItem = prepareValue(new TextValue(), dataEntity, query);

            // XXX still needed?
            addParameters(quantityEntity, valueItem, query);

            if (observation.hasVerticalFrom() || observation.hasVerticalTo()) {
                dataItems.add(assembleDataItem(quantityEntity, profile, observation, query));
            } else {
                dataItems.add(
                        assembleDataItem(quantityEntity, profile, valueItem.getParameters(), datasetEntity, query));
            }
        }
        return dataItems;
    }

}
