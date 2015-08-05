package org.openhds.service.impl.update;

import org.openhds.domain.model.update.PregnancyResult;
import org.openhds.repository.concrete.update.PregnancyResultRepository;
import org.openhds.service.contract.AbstractAuditableCollectedService;
import org.openhds.service.impl.census.IndividualService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

/**
 * Created by Wolfe on 7/15/2015.
 */
@Service
public class PregnancyResultService extends AbstractAuditableCollectedService<PregnancyResult, PregnancyResultRepository>{

    @Autowired
    private IndividualService individualService;

    @Autowired
    private PregnancyOutcomeService pregnancyOutcomeService;

    @Autowired
    public PregnancyResultService(PregnancyResultRepository repository) {
        super(repository);
    }

    @Override
    public PregnancyResult makePlaceHolder(String id, String name) {
        PregnancyResult pregnancyResult = new PregnancyResult();
        pregnancyResult.setUuid(id);
        pregnancyResult.setPregnancyOutcome(pregnancyOutcomeService.getUnknownEntity());
        pregnancyResult.setType(name);
        pregnancyResult.setChild(individualService.getUnknownEntity());

        initPlaceHolderCollectedFields(pregnancyResult);

        return pregnancyResult;
    }

    public PregnancyResult recordPregnancyResult(PregnancyResult pregnancyResult,
                           String pregnancyOutcomeId,
                           String childId,
                           String fieldWorkerId){

        pregnancyResult.setPregnancyOutcome(pregnancyOutcomeService.findOrMakePlaceHolder(pregnancyOutcomeId));
        pregnancyResult.setChild(individualService.findOrMakePlaceHolder(childId));
        pregnancyResult.setCollectedBy(fieldWorkerService.findOrMakePlaceHolder(fieldWorkerId));

        return createOrUpdate(pregnancyResult);

    }
}
