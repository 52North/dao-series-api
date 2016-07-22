/*
 * Copyright (C) 2013-2016 52°North Initiative for Geospatial Open Source
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

import static java.lang.String.format;
import static org.hibernate.criterion.DetachedCriteria.forClass;
import static org.hibernate.criterion.Projections.projectionList;
import static org.hibernate.criterion.Projections.property;
import static org.hibernate.criterion.Restrictions.and;
import static org.hibernate.criterion.Restrictions.between;
import static org.hibernate.criterion.Restrictions.eq;
import static org.hibernate.criterion.Restrictions.isNull;
import static org.hibernate.criterion.Restrictions.like;
import static org.hibernate.criterion.Restrictions.or;
import static org.hibernate.criterion.Subqueries.propertyIn;
import static org.n52.series.db.DataModelUtil.isEntitySupported;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.LogicalExpression;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Restrictions;
import org.hibernate.spatial.criterion.SpatialRestrictions;
import org.hibernate.sql.JoinType;
import org.joda.time.Interval;
import org.n52.io.crs.BoundingBox;
import org.n52.io.crs.CRSUtils;
import org.n52.io.request.FilterResolver;
import org.n52.io.request.IoParameters;
import org.n52.io.request.Parameters;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.PlatformEntity;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;

public abstract class AbstractDbQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDbQuery.class);

    protected static final String COLUMN_KEY = "pkid";

    private static final String COLUMN_LOCALE = "locale";

    private static final String COLUMN_TIMESTAMP = "timestamp";

    private IoParameters parameters = IoParameters.createDefaults();

    private String sridAuthorityCode = "EPSG:4326"; // default

    protected AbstractDbQuery(IoParameters parameters) {
        if (parameters != null) {
            this.parameters = parameters;
        }
    }

    public abstract DetachedCriteria createDetachedFilterCriteria(String propertyName);

    public void setDatabaseAuthorityCode(String code) {
        this.sridAuthorityCode = code;
    }

    public String getHrefBase() {
        return parameters.getHrefBase();
    }

    public String getLocale() {
        return parameters.getLocale();
    }

    public String getSearchTerm() {
        return parameters.getAsString(Parameters.SEARCH_TERM);
    }

    public Interval getTimespan() {
        return parameters.getTimespan().toInterval();
    }

    public BoundingBox getSpatialFilter() {
        return parameters.getSpatialFilter();
    }

    public boolean isExpanded() {
        return parameters.isExpanded();
    }

    public Set<String> getDatasetTypes() {
        return parameters.getDatasetTypes();
    }

    public boolean isSetDatasetTypeFilter() {
        return !parameters.getDatasetTypes().isEmpty();
    }

    public boolean checkTranslationForLocale(Criteria criteria) {
        return !criteria.add(Restrictions.like(COLUMN_LOCALE, getCountryCode())).list().isEmpty();
    }

    public Criteria addLocaleTo(Criteria criteria, Class< ? > clazz) {
        if (getLocale() != null && isEntitySupported(clazz, criteria)) {
            Criteria translations = criteria.createCriteria("translations", JoinType.LEFT_OUTER_JOIN);
            criteria = translations.add(or(like(COLUMN_LOCALE, getCountryCode()), isNull(COLUMN_LOCALE)));
        }
        return criteria;
    }

    private String getCountryCode() {
        return getLocale().split("_")[0];
    }

    public Criteria addTimespanTo(Criteria criteria) {
        if (parameters.getTimespan() != null) {
            Interval interval = parameters.getTimespan().toInterval();
            Date start = interval.getStart().toDate();
            Date end = interval.getEnd().toDate();
            criteria.add(between(COLUMN_TIMESTAMP, start, end));
        }
        return criteria;
    }

    /**
     * Adds a external defined filters to the query.
     *
     * @param parameter parameters containing the filters.
     * @param criteria the criteria to add the filter to.
     * @return the criteria to chain.
     */
    Criteria addPlatformTypesFilter(String parameter, Criteria criteria) {
        FilterResolver filterResolver = getFilterResolver();
        if ( !filterResolver.shallIncludeAllPlatformTypes()) {
            if ("series".equalsIgnoreCase(parameter)) {
                criteria.createCriteria("platform")
                        .add(createMobileExpression(filterResolver))
                        .add(createInsituExpression(filterResolver));
            } else {
                DetachedCriteria c = forClass(DatasetEntity.class, "series")
                        .createCriteria("procedure")
                        .add(createMobileExpression(filterResolver))
                        .add(createInsituExpression(filterResolver));
                    c.setProjection(createSeriesProjectionWith(parameter));
                    criteria.add(propertyIn(format("%s.pkid", parameter), c));
            }
        }
        return criteria;
    }

    private LogicalExpression createMobileExpression(FilterResolver filterResolver) {
        boolean includeStationary = filterResolver.shallIncludeStationaryPlatformTypes();
        boolean includeMobile = filterResolver.shallIncludeMobilePlatformTypes();
        return Restrictions.or(
                 Restrictions.eq(PlatformEntity.MOBILE, !includeStationary), // inverse to match filter
                 Restrictions.eq(PlatformEntity.MOBILE, includeMobile));
    }


    private LogicalExpression createInsituExpression(FilterResolver filterResolver) {
        boolean includeInsitu = filterResolver.shallIncludeInsituPlatformTypes();
        boolean includeRemote = filterResolver.shallIncludeRemotePlatformTypes();
        return Restrictions.or(
                 Restrictions.eq(PlatformEntity.INSITU, includeInsitu),
                 Restrictions.eq(PlatformEntity.INSITU, !includeRemote)); // inverse to match filter
    }

    private Criteria filterMobileInsitu(String parameter, Criteria criteria, boolean mobile, boolean insitu) {
        if ("series".equalsIgnoreCase(parameter)) {
            criteria.createCriteria("platform")
                    .add(and(eq("mobile", mobile), eq("insitu", insitu)));
        } else {
            DetachedCriteria c = forClass(DatasetEntity.class, "series")
                .createCriteria("procedure", "p")
                .add(and(eq("p.mobile", mobile), eq("p.insitu", insitu)));
            c.setProjection(createSeriesProjectionWith(parameter));
            criteria.add(propertyIn(format("%s.pkid", parameter), c));
        }
        return criteria;
    }

    private ProjectionList createSeriesProjectionWith(String parameter) {
        final String filterProperty = format("series.%s.pkid", parameter);
        return projectionList().add(property(filterProperty));
    }

    /**
     * @param id
     *        the id string to parse.
     * @return the long value of given string or {@link Long#MIN_VALUE} if string could not be parsed to type
     *         long.
     */
    public Long parseToId(String id) {
        try {
            if (id.contains("/")) {
                return Long.parseLong(id.substring(id.lastIndexOf("/")+1));
            }
            return Long.parseLong(id);
        }
        catch (NumberFormatException e) {
            return Long.MIN_VALUE;
        }
    }

    public Set<Long> parseToIds(Set<String> ids) {
        Set<Long> parsedIds = new HashSet<>(ids.size());
        for (String id : ids) {
            parsedIds.add(parseToId(id));
        }
        return parsedIds;
    }

    public Criteria addSpatialFilterTo(Criteria criteria, AbstractDbQuery parameters) {
        BoundingBox spatialFilter = parameters.getSpatialFilter();
        if (spatialFilter != null) {
            try {
                CRSUtils crsUtils = CRSUtils.createEpsgForcedXYAxisOrder();
                int databaseSrid = crsUtils.getSrsIdFrom(sridAuthorityCode);
                Point ll = (Point) crsUtils.transformInnerToOuter(spatialFilter.getLowerLeft(), sridAuthorityCode);
                Point ur = (Point) crsUtils.transformInnerToOuter(spatialFilter.getUpperRight(), sridAuthorityCode);
                Envelope envelope = new Envelope(ll.getCoordinate(), ur.getCoordinate());
                criteria.add(SpatialRestrictions.filter("geometry.geometry", envelope, databaseSrid));

                // TODO intersect with linestring

            }
            catch (FactoryException e) {
                LOGGER.error("Could not create transformation facilities.", e);
            }
            catch (TransformException e) {
                LOGGER.error("Could not perform transformation.", e);
            }
        }
        return criteria;
    }

    public IoParameters getParameters() {
        return parameters;
    }

    public FilterResolver getFilterResolver() {
        return parameters.getFilterResolver();
    }

}
