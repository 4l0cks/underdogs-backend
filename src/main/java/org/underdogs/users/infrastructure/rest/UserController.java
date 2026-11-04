package org.underdogs.users.infrastructure.rest;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.underdogs.users.application.models.CreateUserRequest;
import org.underdogs.users.application.models.CreditKibblesRequest;
import org.underdogs.users.application.models.UpdateUserRequest;
import org.underdogs.users.application.usecases.CreateUser;
import org.underdogs.users.application.usecases.CreditKibbles;
import org.underdogs.users.application.usecases.SearchUserById;
import org.underdogs.users.application.usecases.SearchUsers;
import org.underdogs.users.application.usecases.UpdateUser;
import org.underdogs.users.domain.UserId;
import org.underdogs.users.infrastructure.rest.dto.UserDetailDTO;
import org.underdogs.users.infrastructure.rest.dto.UserSummaryDTO;
import org.underdogs.users.infrastructure.rest.mapper.UserMapper;

@RestController
@RequestMapping("/api/v1/users")
class UserController {

  private final CreateUser createUser;
  private final SearchUserById searchUserById;
  private final SearchUsers searchUsers;
  private final UpdateUser updateUser;
  private final CreditKibbles creditKibbles;
  private final UserMapper userMapper;

  UserController(
      CreateUser createUser,
      SearchUserById searchUserById,
      SearchUsers searchUsers,
      UpdateUser updateUser,
      CreditKibbles creditKibbles,
      UserMapper userMapper) {
    this.createUser = createUser;
    this.searchUserById = searchUserById;
    this.searchUsers = searchUsers;
    this.updateUser = updateUser;
    this.creditKibbles = creditKibbles;
    this.userMapper = userMapper;
  }

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping
  ResponseEntity<List<UserSummaryDTO>> listUsers() {
    return ResponseEntity.ok(userMapper.toSummaryDTOList(searchUsers.handle()));
  }

  @GetMapping("/{id}")
  ResponseEntity<UserDetailDTO> getUser(@PathVariable UUID id) {
    return searchUserById
        .handle(new UserId(id.toString()))
        .map(userMapper::toDetailDTO)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping
  ResponseEntity<Void> createUser(@Valid @RequestBody CreateUserRequest request) {
    final var userId = createUser.handle(request);
    return ResponseEntity.created(URI.create("/api/v1/users/" + userId.value())).build();
  }

  @PutMapping("/{id}")
  ResponseEntity<Void> updateUser(
      @PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
    updateUser.handle(new UserId(id.toString()), request);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{id}/kibbles/credit")
  ResponseEntity<Void> creditKibbles(
      @PathVariable UUID id, @Valid @RequestBody CreditKibblesRequest request) {
    creditKibbles.handle(new UserId(id.toString()), request.amount());
    return ResponseEntity.ok().build();
  }
}
