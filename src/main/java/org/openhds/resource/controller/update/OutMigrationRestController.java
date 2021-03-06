package org.openhds.resource.controller.update;

import org.openhds.domain.model.update.OutMigration;
import org.openhds.resource.contract.AuditableCollectedRestController;
import org.openhds.resource.registration.update.OutMigrationRegistration;
import org.openhds.service.contract.AbstractUuidService;
import org.openhds.service.impl.FieldWorkerService;
import org.openhds.service.impl.census.IndividualService;
import org.openhds.service.impl.census.ResidencyService;
import org.openhds.service.impl.update.OutMigrationService;
import org.openhds.service.impl.update.VisitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Created by Ben on 5/18/15.
 */
@RestController
@RequestMapping("/outMigrations")
@ExposesResourceFor(OutMigration.class)
public class OutMigrationRestController extends AuditableCollectedRestController<
        OutMigration,
        OutMigrationRegistration,
        OutMigrationService> {

    private OutMigrationService outMigrationService;

    private final VisitService visitService;

    private final IndividualService individualService;

    private final ResidencyService residencyService;

    private final FieldWorkerService fieldWorkerService;

    @Autowired
    public OutMigrationRestController(OutMigrationService outMigrationService,
                                      VisitService visitService,
                                      IndividualService individualService,
                                      ResidencyService residencyService,
                                      FieldWorkerService fieldWorkerService) {
        super(outMigrationService);
        this.outMigrationService = outMigrationService;
        this.visitService = visitService;
        this.individualService = individualService;
        this.residencyService = residencyService;
        this.fieldWorkerService = fieldWorkerService;
    }

    @Override
    protected OutMigrationRegistration makeSampleRegistration(OutMigration entity) {
        OutMigrationRegistration registration = new OutMigrationRegistration();
        registration.setOutMigration(entity);
        registration.setVisitUuid(AbstractUuidService.UNKNOWN_ENTITY_UUID);
        registration.setIndividualUuid(AbstractUuidService.UNKNOWN_ENTITY_UUID);
        registration.setResidencyUuid(AbstractUuidService.UNKNOWN_ENTITY_UUID);
        return registration;
    }

    @Override
    protected OutMigration register(OutMigrationRegistration registration) {
        checkRegistrationFields(registration.getOutMigration(), registration);
        return outMigrationService.recordOutMigration(registration.getOutMigration(), registration.getIndividualUuid(),
                registration.getResidencyUuid(),
                registration.getVisitUuid(),
                registration.getCollectedByUuid());
    }

    @Override
    protected OutMigration register(OutMigrationRegistration registration, String id) {
        registration.getOutMigration().setUuid(id);
        return register(registration);
    }

    @RequestMapping(value = "/submitEdited/{id}", method = RequestMethod.PUT)
    public ResponseEntity editInMigration(@PathVariable String id, @RequestBody Map<String,String> outMigrationStub) {

        OutMigration outMig = outMigrationService.findOne(id);
        if(outMig == null){
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        if(outMigrationStub.containsKey("destination")){
            outMig.setDestination(outMigrationStub.get("destination"));
        }

        if(outMigrationStub.containsKey("reason")){
            outMig.setReason(outMigrationStub.get("reason"));
        }



        outMigrationService.recordOutMigration(outMig, outMig.getIndividual().getUuid(), outMig.getResidency().getUuid(),
                outMig.getVisit().getUuid(), outMig.getCollectedBy().getFieldWorkerId());
        return new ResponseEntity(HttpStatus.CREATED);
    }

}


