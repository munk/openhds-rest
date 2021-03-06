package org.openhds.resource.contract;

import org.junit.Test;
import org.openhds.domain.contract.AuditableEntity;
import org.openhds.repository.contract.AuditableRepository;
import org.openhds.resource.registration.Registration;
import org.openhds.service.contract.AbstractAuditableService;
import org.openhds.service.contract.AbstractUuidService;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MvcResult;

import java.time.ZonedDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Created by Ben on 6/16/15.
 */
public abstract class AuditableRestControllerTest<
        T extends AuditableEntity,
        U extends AbstractAuditableService<T, ? extends AuditableRepository<T>>,
        V extends AuditableRestController<T, ? extends Registration<T>, U>>
        extends UuidIdentifiableRestControllerTest<T, U, V> {

    protected String getByDatePagedUrl() {
        return getResourceUrl() + "bydate/";
    }

    protected String getByDateBulkUrl() {
        return getByDatePagedUrl() + "bulk/";
    }

    protected String getByLocationHierarchyPagedUrl() {
        return getResourceUrl() + "bylocationhierarchy/";
    }

    protected String getByLocationHierarchyBulkdUrl() {
        return getByLocationHierarchyPagedUrl() + "bulk/";
    }

    protected String getVoidedResourceUrl() {
        return getResourceUrl() + "voided/";
    }

    @Test
    @WithUserDetails
    public void getByDate() throws Exception {

        int preexistingRecords = (int) service.countAll();

        ZonedDateTime preInsert = ZonedDateTime.now();

        T entity = insertNewAndReturn();
        ZonedDateTime modifiedDate = entity.getLastModifiedDate();

        ZonedDateTime postInsert = ZonedDateTime.now();

        // sanity check time stamps
        assertTrue(preInsert.compareTo(modifiedDate) < 0);
        assertTrue(modifiedDate.compareTo(postInsert) < 0);

        mockMvc.perform(get(getByDatePagedUrl())
                .param("beforeDate", preInsert.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(halJson))
                .andExpect(jsonPath("$.page.totalElements", is(preexistingRecords)));
        mockMvc.perform(get(getByDateBulkUrl())
                .param("beforeDate", preInsert.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(regularJson))
                .andExpect(jsonPath("$", hasSize(preexistingRecords)));


        int newRecords = 1;
        mockMvc.perform(get(getByDatePagedUrl())
                .param("afterDate", preInsert.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(halJson))
                .andExpect(jsonPath("$.page.totalElements", is(newRecords)));
        mockMvc.perform(get(getByDateBulkUrl())
                .param("afterDate", preInsert.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(regularJson))
                .andExpect(jsonPath("$", hasSize(newRecords)));

        mockMvc.perform(get(getByDatePagedUrl())
                .param("afterDate", preInsert.toString())
                .param("beforeDate", postInsert.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(halJson))
                .andExpect(jsonPath("$.page.totalElements", is(newRecords)));
        mockMvc.perform(get(getByDateBulkUrl())
                .param("afterDate", preInsert.toString())
                .param("beforeDate", postInsert.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(regularJson))
                .andExpect(jsonPath("$", hasSize(newRecords)));

        int allRecords = (int) service.countAll();
        mockMvc.perform(get(getByDatePagedUrl())
                .param("beforeDate", postInsert.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(halJson))
                .andExpect(jsonPath("$.page.totalElements", is(allRecords)));
        mockMvc.perform(get(getByDateBulkUrl())
                .param("beforeDate", postInsert.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(regularJson))
                .andExpect(jsonPath("$", hasSize(allRecords)));

        // no future records
        mockMvc.perform(get(getByDatePagedUrl())
                .param("afterDate", postInsert.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(halJson))
                .andExpect(jsonPath("$.page.totalElements", is(0)));
        mockMvc.perform(get(getByDateBulkUrl())
                .param("afterDate", postInsert.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(regularJson))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithUserDetails
    public void getVoided() throws Exception {
        // make one
        T entity = insertNewAndReturn();
        mockMvc.perform(get(getResourceUrl() + entity.getUuid()))
                .andExpect(status().isOk());

        // nothing voided yet
        mockMvc.perform(get(getVoidedResourceUrl()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(halJson))
                .andExpect(jsonPath("$.page.totalElements", is(0)));

        // void it
        mockMvc.perform(delete(getResourceUrl() + entity.getUuid()))
                .andExpect(status().isNoContent());

        // one voided
        mockMvc.perform(get(getVoidedResourceUrl()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(halJson))
                .andExpect(jsonPath("$.page.totalElements", is(1)));

        // can't get it anymore
        mockMvc.perform(get(getResourceUrl() + entity.getUuid()))
                .andExpect(status().isNotFound());
    }

    private T insertNewAndReturn() throws Exception {
        T entity = makeValidEntity("test registration", "test id");

        MvcResult mvcResult = this.mockMvc.perform(post(getResourceUrl())
                .contentType(regularJson)
                .content(toJson(makeRegistration(entity))))
                .andExpect(status().isCreated())
                .andReturn();

        T created = fromJson(controller.getEntityClass(), mvcResult.getResponse().getContentAsString());
        return created;
    }

    @Test
    @WithUserDetails
    public void getByLocationHierarchy() throws Exception {
        String locationHierarchyUuid = AbstractUuidService.UNKNOWN_ENTITY_UUID;
        ZonedDateTime beforeDate = ZonedDateTime.now().plusYears(1);
        ZonedDateTime afterDate = ZonedDateTime.now().minusYears(1);

        // test status and content type, leave details up to corresponding service test
        MvcResult mvcResult = mockMvc.perform(get(getByLocationHierarchyPagedUrl())
                .param("locationHierarchyUuid", locationHierarchyUuid)
                .param("afterDate", afterDate.toString())
                .param("beforeDate", beforeDate.toString()))
                .andReturn();

        assertThat(mvcResult.getResponse().getStatus(),
                isOneOf(HttpStatus.OK.value(), HttpStatus.NOT_FOUND.value()));

        if (mvcResult.getResponse().getStatus() == HttpStatus.OK.value()) {
            assertEquals(halJson.toString(), mvcResult.getResponse().getContentType());
        }

        mvcResult = mockMvc.perform(get(getByLocationHierarchyBulkdUrl())
                .param("locationHierarchyUuid", locationHierarchyUuid)
                .param("afterDate", afterDate.toString())
                .param("beforeDate", beforeDate.toString()))
                .andReturn();

        assertThat(mvcResult.getResponse().getStatus(),
                isOneOf(HttpStatus.OK.value(), HttpStatus.NOT_FOUND.value()));

        if (mvcResult.getResponse().getStatus() == HttpStatus.OK.value()) {
            assertEquals(regularJson.toString(), mvcResult.getResponse().getContentType());
        }
    }

}
