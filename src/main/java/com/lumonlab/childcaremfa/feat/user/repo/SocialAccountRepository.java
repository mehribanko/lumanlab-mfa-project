package com.lumonlab.childcaremfa.feat.user.repo;

import com.lumonlab.childcaremfa.feat.user.entity.SocialAuthProvider;
import com.lumonlab.childcaremfa.feat.user.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderUserId(SocialAuthProvider provider, String providerUserId);

    List<SocialAccount> findByUserId(Long userId);

    boolean existsByProviderAndProviderUserId(SocialAuthProvider provider, String providerUserId);

    Optional<SocialAccount> findByProviderAndEmail(SocialAuthProvider provider, String email);
}
