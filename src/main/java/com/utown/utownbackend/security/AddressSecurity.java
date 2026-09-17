package com.utown.utownbackend.security;

import com.utown.utownbackend.entity.UserRole;
import com.utown.utownbackend.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("addressSecurity")
@RequiredArgsConstructor
public class AddressSecurity {

    private final AddressRepository addressRepository;

    public boolean isOwner(Authentication authentication, Long addressId) {

        if (authentication == null || addressId == null) {
            return false;
        }

        if (!(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            return false;
        }

        // Only customers can use the "own address" permission
        if (principal.getRole() != UserRole.CUSTOMER) {
            return false;
        }

        Long userId = principal.getId();

        return addressRepository.findByIdAndDeletedAtIsNull(addressId)
                .map(address ->
                        address.getUser()
                                .getId()
                                .equals(userId)
                )
                .orElse(false);
    }
}