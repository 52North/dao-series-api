/*
 * Copyright (C) 2015-2017 52°North Initiative for Geospatial Open Source
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

package org.n52.series.db.dao;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.ObservationConstellationEntity;
import org.n52.series.db.beans.PlatformEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.i18n.I18nPlatformEntity;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class PlatformDao extends ParameterDao<PlatformEntity, I18nPlatformEntity> {

    public PlatformDao(Session session) {
        super(session);
    }

    @Override
    public Integer getCount(DbQuery query) throws DataAccessException {
        String alias = "const";
        String procedure = QueryUtils.createAssociation(alias, ObservationConstellationEntity.PROCEDURE);
        DetachedCriteria mobile = QueryUtils.projectionOn(procedure, createMobileSubquery(alias, true));
        DetachedCriteria stationary = QueryUtils.projectionOn(DatasetEntity.PROPERTY_FEATURE,
                                                              createMobileSubquery(alias, false));

        FeatureDao featureDao = new FeatureDao(session);
        ProcedureDao procedureDao = new ProcedureDao(session);
        return (int) Long.sum(count(stationary, featureDao, query),
                              count(mobile, procedureDao, query));
    }

    private Long count(DetachedCriteria subquery, AbstractDao< ? > dao, DbQuery query) {
        Criteria criteria = dao.getDefaultCriteria(query);
        Criteria elements = criteria.add(Subqueries.propertyIn("pkid", subquery));
        return (Long) elements.setProjection(Projections.rowCount())
                              .uniqueResult();
    }

    private DetachedCriteria createMobileSubquery(String constellationAlias, boolean mobile) {
        DetachedCriteria criteria = DetachedCriteria.forClass(DatasetEntity.class);
        criteria.createCriteria(DatasetEntity.PROPERTY_OBSERVATION_CONSTELLATION, constellationAlias)
                .createCriteria(ObservationConstellationEntity.PROCEDURE)
                .add(Restrictions.eq(ProcedureEntity.PROPERTY_MOBILE, mobile));
        return criteria;
    }

    @Override
    protected String getDatasetProperty() {
        return DatasetEntity.PROPERTY_OBSERVATION_CONSTELLATION + "." + ObservationConstellationEntity.PROCEDURE;
    }

    @Override
    protected DetachedCriteria projectOnDatasetParameterId(DetachedCriteria subquery) {
        return subquery.createCriteria(DatasetEntity.PROPERTY_OBSERVATION_CONSTELLATION)
                       .createCriteria(ObservationConstellationEntity.PROCEDURE)
                       .setProjection(Projections.property(DescribableEntity.PROPERTY_PKID));
    }

    @Override
    protected Class<PlatformEntity> getEntityClass() {
        return PlatformEntity.class;
    }

    @Override
    protected Class<I18nPlatformEntity> getI18NEntityClass() {
        return I18nPlatformEntity.class;
    }

}
