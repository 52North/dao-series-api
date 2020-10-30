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
package org.n52.series.db.da.mapper;

import org.hibernate.Session;
import org.n52.io.response.ParameterOutput;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.dao.DbQuery;

public interface OutputMapper<T extends ParameterOutput, S extends DescribableEntity> {

    T createCondensed(S entity, DbQuery query);

    default T createCondensed(T result, S entity, DbQuery query) {
        return condensed(result, entity, query);
    }

    default T condensed(T result, DescribableEntity entity, DbQuery query) {
        String id = Long.toString(entity.getId());
        String label = entity.getLabelFrom(query.getLocale());
        String domainId = entity.getIdentifier();
        String hrefBase = query.getHrefBase();

        result.setId(id);
        result.setValue(ParameterOutput.DOMAIN_ID, domainId, query.getParameters(), result::setDomainId);
        result.setValue(ParameterOutput.LABEL, label, query.getParameters(), result::setLabel);
        result.setValue(ParameterOutput.HREF_BASE, hrefBase, query.getParameters(), result::setHrefBase);
        return result;
    }

    T createExpanded(S entity, DbQuery query, Session session);

}
