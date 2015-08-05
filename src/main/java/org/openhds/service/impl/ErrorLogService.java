package org.openhds.service.impl;

import org.openhds.domain.model.FieldWorker;
import org.openhds.errors.model.ErrorLog;
import org.openhds.errors.model.ErrorLogException;
import org.openhds.repository.concrete.ErrorLogRepository;
import org.openhds.repository.results.EntityIterator;
import org.openhds.service.contract.AbstractAuditableCollectedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

@Service
public class ErrorLogService extends AbstractAuditableCollectedService<ErrorLog, ErrorLogRepository> {

    @Autowired
    public ErrorLogService(ErrorLogRepository repository) {
        super(repository);
    }

    @Override
    public ErrorLog makePlaceHolder(String id, String name) {
        ErrorLog errorLog = new ErrorLog();
        errorLog.setUuid(id);
        errorLog.appendError(name);

        initPlaceHolderCollectedFields(errorLog);

        return errorLog;
    }

    @Override
    public ErrorLog createOrUpdate(ErrorLog errorLog) {

        if (null == errorLog.getCollectedBy()) {
            errorLog.setCollectedBy(fieldWorkerService.getUnknownEntity());
        }

        if (null == errorLog.getCollectionDateTime()) {
            errorLog.setCollectionDateTime(ZonedDateTime.now());
        }
        setAuditableFields(errorLog);

        ErrorLog errorLogforErrorLog = new ErrorLog();
        validate(errorLog, errorLogforErrorLog);
        if (!errorLogforErrorLog.getErrors().isEmpty()) {
            throw new ErrorLogException(errorLogforErrorLog);
        }

        return repository.save(errorLog);

    }

    public EntityIterator<ErrorLog> findAllErrorsByEntityType(String entityType, Sort sort) {
        return iteratorFromPageable(pageable -> repository.findByEntityType(entityType, pageable), sort);
    }


    public EntityIterator<ErrorLog> findAllErrorsByResolutionStatus(String resolutionStatus, Sort sort) {
        return iteratorFromPageable(pageable -> repository.findByResolutionStatus(resolutionStatus, pageable), sort);
    }

    public EntityIterator<ErrorLog> findAllErrorsByAssignment(String assignedTo, Sort sort) {
        return iteratorFromPageable(pageable -> repository.findByAssignedTo(assignedTo, pageable), sort);
    }

    public EntityIterator<ErrorLog> findAllErrorsByFieldWorker(FieldWorker fieldWorker, Sort sort) {
        return iteratorFromPageable(pageable -> repository.findByDeletedFalseAndCollectedBy(fieldWorker, pageable), sort);
    }


}
