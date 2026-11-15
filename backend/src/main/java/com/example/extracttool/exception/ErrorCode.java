package com.example.extracttool.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    VALIDATION_ERROR("LF-4000", HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không hợp lệ."),
    MALFORMED_REQUEST("LF-4001", HttpStatus.BAD_REQUEST, "Không thể đọc nội dung yêu cầu."),
    WEAK_PASSWORD("LF-4004", HttpStatus.BAD_REQUEST, "Mật khẩu phải có ít nhất 12 ký tự."),
    WORKSPACE_REQUIRED("LF-4002", HttpStatus.BAD_REQUEST, "Cần giữ lại ít nhất một workspace."),
    EMAIL_ALREADY_EXISTS("LF-4090", HttpStatus.CONFLICT, "Email đã tồn tại."),
    FEATURE_ALREADY_GRANTED("LF-4003", HttpStatus.BAD_REQUEST, "Tài khoản đã có quyền sử dụng tính năng này."),
    FEATURE_REQUEST_PENDING("LF-4091", HttpStatus.CONFLICT, "Yêu cầu cấp quyền đang chờ phê duyệt."),
    FEATURE_NOT_FOUND("LF-4044", HttpStatus.NOT_FOUND, "Không tìm thấy tính năng yêu cầu."),
    INVALID_CREDENTIALS("LF-4010", HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác."),
    INVALID_SESSION("LF-4011", HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ hoặc đã hết hạn."),
    ACCOUNT_NOT_APPROVED("LF-4030", HttpStatus.FORBIDDEN, "Tài khoản chưa được admin phê duyệt."),
    ACCESS_DENIED("LF-4031", HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này."),
    WORKSPACE_NOT_FOUND("LF-4040", HttpStatus.NOT_FOUND, "Không tìm thấy workspace."),
    LEAD_NOT_FOUND("LF-4041", HttpStatus.NOT_FOUND, "Không tìm thấy lead."),
    EXTRACTION_HISTORY_NOT_FOUND("LF-4046", HttpStatus.NOT_FOUND, "Không tìm thấy lịch sử trích xuất."),
    ACCESS_REQUEST_NOT_FOUND("LF-4042", HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu cấp quyền."),
    USER_NOT_FOUND("LF-4043", HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."),
    EXTRACTION_JOB_NOT_FOUND("LF-4045", HttpStatus.NOT_FOUND, "Không tìm thấy tác vụ trích xuất."),
    EXTRACTION_QUEUE_FULL("LF-4290", HttpStatus.TOO_MANY_REQUESTS, "Hàng đợi trích xuất đã đầy. Vui lòng thử lại sau."),
    BROWSER_BUSY("LF-4092", HttpStatus.CONFLICT, "Browser đang xử lý tác vụ. Hãy thử lại sau khi tác vụ hoàn tất."),
    BROWSER_UNAVAILABLE("LF-5030", HttpStatus.SERVICE_UNAVAILABLE, "Không thể mở phiên Facebook trên server. Kiểm tra trạng thái browser và thử lại."),
    INTERNAL_ERROR("LF-5000", HttpStatus.INTERNAL_SERVER_ERROR, "Hệ thống gặp lỗi không mong muốn.");

    private final String code;
    private final HttpStatus status;
    private final String message;
    ErrorCode(String code, HttpStatus status, String message) { this.code = code; this.status = status; this.message = message; }
}
