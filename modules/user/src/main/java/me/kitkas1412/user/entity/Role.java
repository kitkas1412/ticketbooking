package me.kitkas1412.user.entity;

/**
 * Các role USER và ADMIN dùng để phân quyền truy cập.
 */
public enum Role {

    USER,
    ADMIN;

    /**
     * Tên authority Spring Security dùng cho hasRole()/hasAuthority().
     * Spring tự thêm tiền tố "ROLE_" khi dùng hasRole(), nên authority lưu trong
     * SecurityContext phải mang sẵn tiền tố này.
     */
    public String getAuthority() {
        return "ROLE_" + name();
    }
}
