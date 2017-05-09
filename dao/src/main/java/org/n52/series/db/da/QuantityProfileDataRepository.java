
package org.n52.series.db.da;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.n52.io.request.IoParameters;
import org.n52.io.response.dataset.profile.ProfileData;
import org.n52.io.response.dataset.profile.ProfileValue;
import org.n52.io.response.dataset.quantity.QuantityValue;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.ProfileDataEntity;
import org.n52.series.db.beans.ProfileDatasetEntity;
import org.n52.series.db.beans.QuantityDataEntity;
import org.n52.series.db.dao.DataDao;
import org.n52.series.db.dao.DbQuery;

public class QuantityProfileDataRepository
        extends AbstractDataRepository<ProfileData, ProfileDatasetEntity, ProfileDataEntity, ProfileValue> {

    private final QuantityDataRepository quantityRepository;

    public QuantityProfileDataRepository() {
        this.quantityRepository = new QuantityDataRepository();
    }

    @Override
    public ProfileValue getFirstValue(ProfileDatasetEntity dataset, Session session, DbQuery query) {
        query.setComplexParent(true);
        return super.getFirstValue(dataset, session, query);
    }

    @Override
    public ProfileValue getLastValue(ProfileDatasetEntity dataset, Session session, DbQuery query) {
        query.setComplexParent(true);
        return super.getLastValue(dataset, session, query);
    }

    @Override
    public Class<ProfileDatasetEntity> getDatasetEntityType() {
        return ProfileDatasetEntity.class;
    }

    private boolean isVertical(Map<String, Object> parameterObject, String verticalName) {
        return parameterObject.containsKey("name")
                && ((String) parameterObject.get("name")).equalsIgnoreCase(verticalName);
    }

    @Override
    protected ProfileValue createSeriesValueFor(ProfileDataEntity valueEntity,
                                                ProfileDatasetEntity datasetEntity,
                                                DbQuery query) {
        Date timeend = valueEntity.getTimeend();
        Date timestart = valueEntity.getTimestart();
        long end = timeend.getTime();
        long start = timestart.getTime();
        IoParameters parameters = query.getParameters();
        ProfileValue profile = parameters.isShowTimeIntervals()
                ? new ProfileValue(start, end, null)
                : new ProfileValue(end, null);

        List<QuantityProfileDataItem> dataItems = new ArrayList<>();
        for (DataEntity< ? > dataEntity : valueEntity.getValue()) {
            QuantityDataEntity quantityEntity = (QuantityDataEntity) dataEntity;
            QuantityValue valueItem = quantityRepository.createValue(quantityEntity.getValue(), quantityEntity, query);
            addParameters(quantityEntity, valueItem, query);
            for (Map<String, Object> parameterObject : valueItem.getParameters()) {
                String verticalName = datasetEntity.getVerticalParameterName();
                if (isVertical(parameterObject, verticalName)) {
                    // TODO vertical unit is missing for now
                    QuantityProfileDataItem dataItem = new QuantityProfileDataItem();
                    dataItem.setValue(quantityEntity.getValue());
                    // set vertical's value
                    dataItem.setVertical((double) parameterObject.get("value"));
                    String verticalUnit = (String) parameterObject.get("unit");
                    if (profile.getVerticalUnit() == null) {
                        profile.setVerticalUnit(verticalUnit);
                    }
                    if (profile.getVerticalUnit() == null
                            || !profile.getVerticalUnit()
                                       .equals(verticalUnit)) {
                        dataItem.setVerticalUnit(verticalUnit);
                    }
                    dataItems.add(dataItem);
                }
            }
        }

        profile.setValue(dataItems);
        return profile;
    }

    @Override
    protected ProfileData assembleData(ProfileDatasetEntity datasetEntity, DbQuery query, Session session)
            throws DataAccessException {
        query.setComplexParent(true);
        ProfileData result = new ProfileData();
        DataDao<ProfileDataEntity> dao = createDataDao(session);
        List<ProfileDataEntity> observations = dao.getAllInstancesFor(datasetEntity, query);
        for (ProfileDataEntity observation : observations) {
            if (observation != null) {
                result.addValues(createSeriesValueFor(observation, datasetEntity, query));
            }
        }
        return result;
    }

    @Override
    protected ProfileData assembleDataWithReferenceValues(ProfileDatasetEntity datasetEntity,
                                                          DbQuery dbQuery,
                                                          Session session)
            throws DataAccessException {
        
        // TODO handle reference values
        
        return  assembleData(datasetEntity, dbQuery, session);
    }
    

    public static class QuantityProfileDataItem {
        private String verticalUnit;
        private Double vertical;
        private Double value;

        public String getVerticalUnit() {
            return verticalUnit;
        }

        public void setVerticalUnit(String verticalUnit) {
            this.verticalUnit = verticalUnit;
        }

        public Double getVertical() {
            return vertical;
        }

        public void setVertical(Double vertical) {
            this.vertical = vertical;
        }

        public Double getValue() {
            return value;
        }

        public void setValue(Double value) {
            this.value = value;
        }

    }

}
