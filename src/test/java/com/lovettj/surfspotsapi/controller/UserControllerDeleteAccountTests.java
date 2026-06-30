package com.lovettj.surfspotsapi.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.service.UserService;
import com.lovettj.surfspotsapi.testutil.BaseControllerTest;
import com.lovettj.surfspotsapi.testutil.MockMvcDefaults;
import com.lovettj.surfspotsapi.testutil.SessionTestCookieFactory;

class UserControllerDeleteAccountTests extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void deleteAccountShouldReturnOkWhenAuthenticatedUserDeletesOwnAccount() throws Exception {
        String authenticatedUserId = "user-self";
        doNothing().when(userService).deleteAccount(eq(authenticatedUserId));

        mockMvc.perform(delete("/api/user/account/" + authenticatedUserId)
                .cookie(SessionTestCookieFactory.createSignedSessionCookie(authenticatedUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", Matchers.is("Account deleted successfully")));

        verify(userService).deleteAccount(authenticatedUserId);
    }

    @Test
    void deleteAccountShouldReturn403WhenPathUserIdDoesNotMatchSession() throws Exception {
        String sessionUserId = "user-self";
        String pathUserId = "other-user";

        mockMvc.perform(delete("/api/user/account/" + pathUserId)
                .cookie(SessionTestCookieFactory.createSignedSessionCookie(sessionUserId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", Matchers.is(ApiErrors.ACCOUNT_DELETE_NOT_PERMITTED)));

        verify(userService, never()).deleteAccount(anyString());
    }
}
