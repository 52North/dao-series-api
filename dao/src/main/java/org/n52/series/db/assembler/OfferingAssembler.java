
package org.n52.series.db.assembler;

import org.n52.io.response.OfferingOutput;
import org.n52.series.db.DatasetRepository;
import org.n52.series.db.OfferingRepository;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.old.dao.DbQuery;
import org.n52.series.db.query.DatasetQuerySpecifications;
import org.n52.series.db.query.OfferingQuerySpecifications;
import org.springframework.stereotype.Component;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;

@Component
public class OfferingAssembler extends ParameterOutputAssembler<OfferingEntity, OfferingOutput> {

    public OfferingAssembler(OfferingRepository offeringRepository,
                             DatasetRepository<DatasetEntity> datasetRepository) {
        super(offeringRepository, datasetRepository);
    }

    @Override
    protected OfferingOutput prepareEmptyOutput() {
        return new OfferingOutput();
    }

    BooleanExpression createFilterPredicate(DbQuery query) {
        DatasetQuerySpecifications dsFilterSpec = DatasetQuerySpecifications.of(query);
        JPQLQuery<DatasetEntity> subQuery = dsFilterSpec.toSubquery(dsFilterSpec.matchFilters());

        OfferingQuerySpecifications oFilterSpec = OfferingQuerySpecifications.of(query);
        return oFilterSpec.selectFrom(subQuery);
    }

}
