package org.openhds.resource;

import org.junit.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Created by Ben on 5/26/15.
 */
public class HateoasTraversalTest extends AbstractRestControllerTest {

    @Test
    @WithMockUser(username = username, password = password)
    public void followLinks() throws Exception {
        // find the locations controller from the home controller
        String locationsUrl = getAndExtractJsonPath("/", "$._links.locations.href");

        // find the first location from the list of locations
        String oneLocationUrl = getAndExtractJsonPath(locationsUrl, "$._embedded.locationList[0]._links.self.href");

        // follow the "external id" link to f the same location
        String externalLocationUrl = getAndExtractJsonPath(oneLocationUrl, "$._links.self-external.href");

        // find the user who inserted the location, from the location
        String insertByUrl = getAndExtractJsonPath(externalLocationUrl, "$._embedded.locationList[0]._links.insertBy.href");

        // find the user's "self"
        String insertBySelfUrl = getAndExtractJsonPath(insertByUrl, "$._links.self.href");

    }

    private String getAndExtractJsonPath(String url, String linkPath) throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(get(url)
                .contentType(regularJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(halJson))
                .andExpect(jsonPath(linkPath).exists())
                .andReturn();
        return extractJsonPath(mvcResult, linkPath);
    }
}