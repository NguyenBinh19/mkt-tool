package com.example.extracttool.controller;

import com.example.extracttool.model.entity.FeatureAccessRequest;
import com.example.extracttool.model.entity.UserAccount;
import com.example.extracttool.service.AccessControlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AccessControlController {
    private final AccessControlService access;

    public AccessControlController(AccessControlService access) {
        this.access = access;
    }

    @PostMapping("/auth/register")
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest body) {
        UserAccount user = access.register(body.name(), body.email(), body.password());
        return Map.of("id", user.getId(), "status", user.getStatus(), "message", "Yêu cầu đăng ký đã được gửi");
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest body) {
        return userMap(access.login(body.email(), body.password()));
    }

    @GetMapping("/auth/me")
    public Map<String, Object> me(HttpServletRequest request) {
        return userMap(current(request));
    }

    @PostMapping("/access-requests")
    public Map<String, Object> requestAccess(@Valid @RequestBody FeatureRequest body, HttpServletRequest request) {
        FeatureAccessRequest item = access.requestFeature(current(request), body.feature());
        return requestMap(item);
    }

    @GetMapping("/access-requests/mine")
    public List<Map<String, Object>> myRequests(HttpServletRequest request) {
        return access.requestsFor(current(request)).stream().map(AccessControlController::requestMapWithUser).toList();
    }

    @GetMapping("/admin/users")
    public List<Map<String, Object>> users(HttpServletRequest request) {
        return access.users(current(request)).stream().map(AccessControlController::userMap).toList();
    }

    @PostMapping("/admin/users/{id}/approve")
    public Map<String, Object> approveUser(@PathVariable Long id, HttpServletRequest request) {
        return userMap(access.decideUser(id, true, current(request)));
    }

    @PostMapping("/admin/users/{id}/reject")
    public Map<String, Object> rejectUser(@PathVariable Long id, HttpServletRequest request) {
        return userMap(access.decideUser(id, false, current(request)));
    }

    @GetMapping("/admin/access-requests")
    public List<Map<String, Object>> pendingRequests(HttpServletRequest request) {
        access.requireAdmin(current(request));
        return access.pendingRequests().stream().map(item -> Map.<String, Object>of(
                "id", item.getId(),
                "userId", item.getUser().getId(),
                "userName", item.getUser().getName(),
                "feature", item.getFeatureKey(),
                "status", item.getStatus()
        )).toList();
    }

    @PostMapping("/admin/access-requests/{id}/approve")
    public Map<String, Object> approveRequest(@PathVariable Long id, HttpServletRequest request) {
        return requestMap(access.decideRequest(id, true, current(request)));
    }

    @PostMapping("/admin/access-requests/{id}/reject")
    public Map<String, Object> rejectRequest(@PathVariable Long id, HttpServletRequest request) {
        return requestMap(access.decideRequest(id, false, current(request)));
    }

    private UserAccount current(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        String token = header == null ? "" : header.replace("Bearer ", "");
        return access.require(token);
    }

    private static Map<String, Object> userMap(UserAccount user) {
        return Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "status", user.getStatus(),
                "permissions", user.getPermissions(),
                "token", user.getAuthToken() == null ? "" : user.getAuthToken()
        );
    }

    private static Map<String, Object> requestMap(FeatureAccessRequest item) {
        return Map.of("id", item.getId(), "feature", item.getFeatureKey(), "status", item.getStatus());
    }

    private static Map<String, Object> requestMapWithUser(FeatureAccessRequest item) {
        return Map.of(
                "id", item.getId(),
                "userId", item.getUser().getId(),
                "userName", item.getUser().getName(),
                "feature", item.getFeatureKey(),
                "status", item.getStatus()
        );
    }

    public record RegisterRequest(@NotBlank @Size(max = 120) String name, @Email @NotBlank @Size(max = 254) String email, @NotBlank @Size(min = 12, max = 72) String password) { }
    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) { }
    public record FeatureRequest(@NotBlank @Size(max = 80) String feature) { }
}
