package com.utown.utownbackend.security;

import com.utown.utownbackend.entity.User;
import com.utown.utownbackend.repository.UserRepository;
import com.utown.utownbackend.util.PhoneUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        String normalizedPhone = PhoneUtil.normalizePhone(phone);
        if (normalizedPhone == null) {
            throw new UsernameNotFoundException("Invalid phone number: " + phone);
        }

        User user = userRepository.findByPhoneAndDeletedAtIsNull(normalizedPhone)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with phone: " + phone));

        return new CustomUserDetails(user);
    }
}
