package org.openhds.repository.generator;

import org.openhds.domain.contract.AuditableCollectedEntity;
import org.openhds.domain.contract.AuditableEntity;
import org.openhds.domain.model.FieldWorker;
import org.openhds.domain.model.census.Location;
import org.openhds.domain.model.census.LocationHierarchy;
import org.openhds.domain.model.census.LocationHierarchyLevel;
import org.openhds.repository.concrete.census.LocationHierarchyLevelRepository;
import org.openhds.repository.concrete.census.LocationHierarchyRepository;
import org.openhds.repository.concrete.census.LocationRepository;
import org.openhds.security.model.User;
import org.openhds.service.impl.FieldWorkerService;
import org.openhds.service.impl.UserService;
import org.openhds.service.impl.census.LocationHierarchyLevelService;
import org.openhds.service.impl.census.LocationHierarchyService;
import org.openhds.service.impl.census.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Created by bsh on 7/21/15.
 * <p>
 * Generates sample location data, including
 * LocationHierarchyLevel, LocationHierarchy, and Location.
 * <p>
 * For each entity, only inserts sample records if there are no records yet.
 * This behavior should support testing without messing up existing project data.
 * <p>
 * Takes a parameter h which determines how many sample records to create.
 * <p>
 * The sample location hierarchy will always be a 10-way tree with 10 locations
 * at each leaf of the location hierarchy tree.
 * <p>
 * The number of LocationHierarchyLevel records will be equal to h.
 * <p>
 * The number of LocationHierarchy records will be the number of nodes in an a
 * 10-ary tree with h levels: (10^h - 1) / 9.
 * <p>
 * The number of Location records will be the number of leaf nodes in an a
 * 10-ary tree with h levels, times 10: 10^h.
 */
@Component
public class LocationDataGenerator implements DataGenerator {

    public static final int ARITY = 10;

    private final LocationHierarchyLevelService locationHierarchyLevelService;
    private final LocationHierarchyLevelRepository locationHierarchyLevelRepository;

    private final LocationHierarchyService locationHierarchyService;
    private final LocationHierarchyRepository locationHierarchyRepository;

    private final LocationService locationService;
    private final LocationRepository locationRepository;

    private final User user;

    private final FieldWorker fieldWorker;

    @Autowired
    public LocationDataGenerator(LocationHierarchyLevelService locationHierarchyLevelService,
                                 LocationHierarchyLevelRepository locationHierarchyLevelRepository,
                                 LocationHierarchyService locationHierarchyService,
                                 LocationHierarchyRepository locationHierarchyRepository,
                                 LocationService locationService,
                                 LocationRepository locationRepository,
                                 FieldWorkerService fieldWorkerService,
                                 UserService userService) {

        this.locationHierarchyLevelService = locationHierarchyLevelService;
        this.locationHierarchyLevelRepository = locationHierarchyLevelRepository;
        this.locationHierarchyService = locationHierarchyService;
        this.locationHierarchyRepository = locationHierarchyRepository;
        this.locationService = locationService;
        this.locationRepository = locationRepository;

        this.user = userService.getUnknownEntity();
        this.fieldWorker = fieldWorkerService.getUnknownEntity();
    }

    @Override
    public void generateData() {
        generateData(0);
    }

    @Override
    public void generateData(int size) {
        generateLevels(size);
        generateHierarchies(size);
        generateLocations(size);
        generateUnknowns();
    }

    @Override
    public void clearData() {
        locationRepository.deleteAllInBatch();
        locationHierarchyRepository.deleteAllInBatch();
        locationHierarchyLevelRepository.deleteAllInBatch();
    }

    // trigger services to create unknown entities ahead of time, for predictable entity counts
    private void generateUnknowns() {
        locationHierarchyLevelService.getUnknownEntity();
        locationHierarchyService.getUnknownEntity();
        locationService.getUnknownEntity();
    }


    private void generateLevels(int h) {
        if (locationHierarchyLevelService.hasRecords()) {
            return;
        }

        // always create the 0-level, then work down to h
        for (int i = 0; i <= h; i++) {
            LocationHierarchyLevel locationHierarchyLevel = new LocationHierarchyLevel();
            locationHierarchyLevel.setKeyIdentifier(i);
            locationHierarchyLevel.setName(String.format("location-hierarchy-level-%d", i));
            locationHierarchyLevelService.recordLocationHierarchyLevel(locationHierarchyLevel);
        }
    }

    // define h levels
    private void generateHierarchies(int h) {

        // always create one hierarchy at level 0, then recur down to the bottom
        LocationHierarchy root = locationHierarchyService.getHierarchyRoot();
        LocationHierarchyLevel levelZero = locationHierarchyLevelService.findByKeyIdentifier(0);
        LocationHierarchy top = generateHierarchy(root, levelZero, "hierarchy-0");

        if (h > 0) {
            LocationHierarchyLevel levelOne = locationHierarchyLevelService.findByKeyIdentifier(1);
            generateChildHierarchies(top, levelOne);
        }
    }

    // recursively build the ARITY-ary tree
    private void generateChildHierarchies(LocationHierarchy parent, LocationHierarchyLevel level) {

        int levelId = level.getKeyIdentifier();

        LocationHierarchyLevel nextLevel = null;
        if (locationHierarchyLevelService.levelKeyIdentifierExists(levelId + 1)) {
            nextLevel = locationHierarchyLevelService.findByKeyIdentifier(levelId + 1);
        }

        // add ARITY children
        for (int i = 1; i <= ARITY; i++) {
            String extId = parent.getExtId() + "-" + i;
            LocationHierarchy child = generateHierarchy(parent, level, extId);

            // base case: bottom of the tree
            if (null == nextLevel) {
                continue;
            }

            // keep adding grandchildren
            generateChildHierarchies(child, nextLevel);
        }

    }

    private LocationHierarchy generateHierarchy(LocationHierarchy parent, LocationHierarchyLevel level, String extId) {
        LocationHierarchy child = new LocationHierarchy();
        setAuditableFields(child);
        setCollectedFields(child);

        child.setExtId(extId);
        child.setName(extId);
        child.setParent(parent);
        child.setLevel(level);

        // save using repository, not service, for performance
        return locationHierarchyRepository.save(child);
    }

    // add ARITY locations at each leaf of the hierarchy
    private void generateLocations(int h) {

        // always create one location at level 0, then add to the leaves
        LocationHierarchyLevel zeroLevel = locationHierarchyLevelService.findByKeyIdentifier(0);
        List<LocationHierarchy> zeroNodes = locationHierarchyService.findByLevel(zeroLevel);
        generateLocation(zeroNodes.get(0), "location-0");

        if (h < 1) {
            return;
        }

        LocationHierarchyLevel bottomLevel = locationHierarchyLevelService.findByKeyIdentifier(h);
        List<LocationHierarchy> leafNodes = locationHierarchyService.findByLevel(bottomLevel);
        for (LocationHierarchy leaf : leafNodes) {
            for (int i = 1; i <= ARITY; i++) {
                String extId = String.format("location-%d", i);
                generateLocation(leaf, extId);
            }
        }
    }

    private void generateLocation(LocationHierarchy locationHierarchy, String extId) {
        Location location = new Location();
        setAuditableFields(location);
        setCollectedFields(location);

        location.setExtId(extId);
        location.setName(extId);
        location.setDescription("sample location");
        location.setType("RURAL");
        location.setLocationHierarchy(locationHierarchy);

        locationRepository.save(location);
    }

    private void setAuditableFields(AuditableEntity entity) {
        ZonedDateTime now = ZonedDateTime.now();

        //Check to see if we're creating or updating the entity
        if (null == entity.getInsertDate()) {
            entity.setInsertDate(now);
        }

        if (null == entity.getInsertBy()) {
            entity.setInsertBy(user);
        }

        entity.setLastModifiedDate(now);
        entity.setLastModifiedBy(user);
    }

    private void setCollectedFields(AuditableCollectedEntity entity) {
        entity.setCollectedBy(fieldWorker);
        entity.setCollectionDateTime(ZonedDateTime.now());
    }

}
