package me.kitkas1412.ticketbooking.security;

import me.kitkas1412.ticketbooking.entity.User;
import me.kitkas1412.ticketbooking.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * username ở đây là email. Phải normalize giống lúc ghi (User.normalizeEmail)
     * vì findByEmail so khớp chính xác — nếu không, đăng nhập bằng "A@X.com"
     * sẽ không tìm thấy bản ghi đã lưu dưới dạng "a@x.com".
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Email must not be empty");
        }

        User user = userRepository.findByEmail(username.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return CustomUserDetails.from(user);
    }
}
