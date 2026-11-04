package org.underdogs.users.infrastructure.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.underdogs.config.KeycloakJwtRolesConverter;
import org.underdogs.config.SecurityConfig;
import org.underdogs.shared.TimeProvider;
import org.underdogs.users.application.gateways.UserRepository;
import org.underdogs.users.application.models.CreateUserRequest;
import org.underdogs.users.application.usecases.CreateUser;
import org.underdogs.users.application.usecases.CreditKibbles;
import org.underdogs.users.application.usecases.SearchUserById;
import org.underdogs.users.application.usecases.SearchUsers;
import org.underdogs.users.application.usecases.UpdateUser;
import org.underdogs.users.domain.UserId;
import org.underdogs.users.infrastructure.rest.mapper.UserMapper;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, UserMapper.class})
class UserControllerTest {

  private static final String USER_ID = "7f3c1b6e-2a4d-4c8e-9b1f-0d2e3a4b5c6d";

  private static final String VALID_CREATE_USER_BODY =
      """
      {
        "externalAuthId": "keycloak-new-user",
        "username": "newuser",
        "email": "new.user@underdogs.org",
        "firstName": "New",
        "lastName": "User",
        "birthDate": "1990-01-01"
      }
      """;

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CreateUser createUser;
  @MockitoBean private SearchUserById searchUserById;
  @MockitoBean private SearchUsers searchUsers;
  @MockitoBean private UpdateUser updateUser;
  @MockitoBean private CreditKibbles creditKibbles;
  @MockitoBean private UserRepository userRepository;
  @MockitoBean private TimeProvider timeProvider;
  @MockitoBean private JwtDecoder jwtDecoder;

  @BeforeEach
  void setUp() {
    when(userRepository.findByExternalAuthId(anyString())).thenReturn(Optional.empty());
  }

  @Test
  void createUser_returns403_whenCallerHasUserRole() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/users")
                .with(jwtWithRealmRole("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_CREATE_USER_BODY))
        .andExpect(status().isForbidden());

    verify(createUser, never()).handle(any());
  }

  @Test
  void createUser_returns201WithLocation_whenCallerHasAdminRole() throws Exception {
    when(createUser.handle(any(CreateUserRequest.class))).thenReturn(new UserId(USER_ID));

    mockMvc
        .perform(
            post("/api/v1/users")
                .with(jwtWithRealmRole("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_CREATE_USER_BODY))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/users/" + USER_ID));
  }

  private static JwtRequestPostProcessor jwtWithRealmRole(String role) {
    return jwt()
        .jwt(
            token ->
                token
                    .subject("keycloak-caller")
                    .claim("realm_access", Map.of("roles", List.of(role))))
        .authorities(new KeycloakJwtRolesConverter());
  }
}
