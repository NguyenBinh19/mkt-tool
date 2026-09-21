package com.example.extracttool.service;

import com.example.extracttool.model.entity.FeatureAccessRequest;
import com.example.extracttool.model.entity.UserAccount;
import com.example.extracttool.repository.FeatureAccessRequestRepository;
import com.example.extracttool.repository.UserAccountRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccessControlService {
    private final UserAccountRepository users;
    private final FeatureAccessRequestRepository requests;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    public AccessControlService(UserAccountRepository users, FeatureAccessRequestRepository requests) { this.users = users; this.requests = requests; }
    public UserAccount register(String name, String email, String password) {
        if (users.findByEmailIgnoreCase(email).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã tồn tại");
        return users.save(new UserAccount(name, email.toLowerCase().trim(), encoder.encode(password)));
    }
    public UserAccount login(String email, String password) {
        UserAccount user = users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Thông tin đăng nhập không đúng"));
        if (!encoder.matches(password, user.getPasswordHash())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Thông tin đăng nhập không đúng");
        if (user.getStatus() != UserAccount.AccountStatus.APPROVED) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản chưa được admin phê duyệt");
        user.setAuthToken(UUID.randomUUID().toString()); return users.save(user);
    }
    public UserAccount require(String token) { return users.findByAuthToken(token).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ")); }
    public void requireAdmin(UserAccount user) { if (user.getRole() != UserAccount.Role.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ admin mới có quyền thao tác"); }
    public FeatureAccessRequest requestFeature(UserAccount user, String feature) {
        if (user.getRole() == UserAccount.Role.ADMIN || user.canUse(feature)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản đã có quyền sử dụng");
        if (requests.existsByUserIdAndFeatureKeyAndStatus(user.getId(), feature, FeatureAccessRequest.RequestStatus.PENDING)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Yêu cầu đang chờ duyệt");
        return requests.save(new FeatureAccessRequest(user, feature));
    }
    public List<FeatureAccessRequest> pendingRequests() { return requests.findByStatus(FeatureAccessRequest.RequestStatus.PENDING); }
    public FeatureAccessRequest decideRequest(Long id, boolean approve, UserAccount admin) {
        requireAdmin(admin); FeatureAccessRequest request = requests.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu"));
        request.setStatus(approve ? FeatureAccessRequest.RequestStatus.APPROVED : FeatureAccessRequest.RequestStatus.REJECTED);
        if (approve) request.getUser().getPermissions().add(request.getFeatureKey());
        return requests.save(request);
    }
    public List<UserAccount> users(UserAccount admin) { requireAdmin(admin); return users.findAll(); }
    public UserAccount decideUser(Long id, boolean approve, UserAccount admin) { requireAdmin(admin); UserAccount user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy user")); user.setStatus(approve ? UserAccount.AccountStatus.APPROVED : UserAccount.AccountStatus.REJECTED); return users.save(user); }
}
