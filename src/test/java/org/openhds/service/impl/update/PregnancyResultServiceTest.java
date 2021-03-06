package org.openhds.service.impl.update;

import org.junit.Test;
import org.openhds.domain.model.FieldWorker;
import org.openhds.domain.model.census.Individual;
import org.openhds.domain.model.update.PregnancyOutcome;
import org.openhds.domain.model.update.PregnancyResult;
import org.openhds.service.AuditableCollectedServiceTest;
import org.openhds.service.impl.census.IndividualService;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZonedDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Wolfe on 7/15/2015.
 */
public class PregnancyResultServiceTest extends AuditableCollectedServiceTest<PregnancyResult, PregnancyResultService> {

    @Autowired
    private IndividualService individualService;

    @Autowired
    private PregnancyOutcomeService pregnancyOutcomeService;

    @Override
    protected PregnancyResult makeInvalidEntity() {
        return new PregnancyResult();
    }

    @Override
    @Autowired
    protected void initialize(PregnancyResultService service) {
        this.service = service;
    }

    @Test
    public void recordWithExistingReferences() {

        //Grab a valid entity
        PregnancyResult pregnancyResult = makeValidEntity("validName", "validId");

        FieldWorker fieldWorker = pregnancyResult.getCollectedBy();
        pregnancyResult.setCollectedBy(null);

        PregnancyOutcome pregnancyOutcome = pregnancyResult.getPregnancyOutcome();
        pregnancyResult.setPregnancyOutcome(null);

        Individual child = pregnancyResult.getChild();
        pregnancyResult.setChild(null);

        // pass it all into the record method
        pregnancyResult = service.recordPregnancyResult(pregnancyResult,
                pregnancyOutcome.getUuid(),
                child.getUuid(),
                fieldWorker.getUuid());


        //Check that the originals match the ones pulled out from findOrMakePlaceholder()
        assertNotNull(pregnancyResult.getCollectedBy());
        assertEquals(pregnancyResult.getCollectedBy(), fieldWorker);

        assertNotNull(pregnancyResult.getPregnancyOutcome());
        assertEquals(pregnancyResult.getPregnancyOutcome(), pregnancyOutcome);

        assertNotNull(pregnancyResult.getChild());
        assertEquals(pregnancyResult.getChild(), child);

    }

    @Test
    public void recordWithNonexistentReferences(){

        //Make a new entity with no references
        PregnancyResult pregnancyResult = makeValidEntity("validName", "validId");
        pregnancyResult.setCollectedBy(null);
        pregnancyResult.setPregnancyOutcome(null);
        pregnancyResult.setChild(null);

        //Pass it in with new reference uuids
        pregnancyResult = service.recordPregnancyResult(pregnancyResult,
                "pregnuncyOotkum",
                "chuld",
                "feldwarker");

        //check that they were persisted
        assertNotNull(pregnancyResult.getCollectedBy());
        assertEquals(pregnancyResult.getCollectedBy().getUuid(), "feldwarker");

        assertNotNull(pregnancyResult.getPregnancyOutcome());
        assertEquals(pregnancyResult.getPregnancyOutcome().getUuid(), "pregnuncyOotkum");

        assertNotNull(pregnancyResult.getChild());
        assertEquals(pregnancyResult.getChild().getUuid(), "chuld");

    }

    @Test
    public void correctEntityReferenceOnUpdate(){

        //Make a new entity with no references
        PregnancyResult pregnancyResult = makeValidEntity("validName", "validId");
        pregnancyResult.setCollectedBy(null);
        pregnancyResult.setChild(null);
        pregnancyResult.setPregnancyOutcome(null);

        //Pass it in with new reference uuids
        pregnancyResult = service.recordPregnancyResult(pregnancyResult, "prugnuncyOotkum", "chuld", "feldwarker");

        //make the "real" entity to overwrite the old one.
        PregnancyOutcome pregnancyOutcome = pregnancyOutcomeService.makePlaceHolder("prugnuncyOotkum");
        pregnancyOutcome.setVoidReason("blegh");

        pregnancyOutcomeService.createOrUpdate(pregnancyOutcome);

        //Get the entity with the updated reference
        pregnancyResult = service.findOne("validId");

        //cheggerout
        assertEquals(pregnancyResult.getPregnancyOutcome().getVoidReason(), "blegh");

    }

    @Test
    public void complexRecordValid(){
        PregnancyResult pregnancyResult = makeValidEntity("validName", "validId");
        ZonedDateTime recordTime = ZonedDateTime.now().minusHours(1);

        service.recordPregnancyResult(pregnancyResult, recordTime, PREGNANCY_OUTCOME_ID, INDIVIDUAL_ID, FIELDWORKER_ID, MEMBERSHIP_ID, RESIDENCY_ID);
    }

}
