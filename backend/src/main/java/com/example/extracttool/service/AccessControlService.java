package com.example.extracttool.service;

import com.example.extracttool.model.entity.FeatureAccessRequest;
import com.example.extracttool.model.entity.UserAccount;
import java.util.List;

public interface AccessControlService {
    UserAccount register(String name, String email, String password);
    UserAccount login(String email, String password);
    UserAccount require(String token);
    void requireAdmin(UserAccount user);
    FeatureAccessRequest requestFeature(UserAccount user, String feature);
    List<FeatureAccessRequest> pendingRequests();
    FeatureAccessRequest decideRequest(Long id, boolean approve, UserAccount admin);
    List<UserAccount> users(UserAccount admin);
    UserAccount decideUser(Long id, boolean approve, UserAccount admin);
}
