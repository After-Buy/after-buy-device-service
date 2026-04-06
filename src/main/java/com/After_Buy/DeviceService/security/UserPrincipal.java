package com.After_Buy.DeviceService.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 스프링 시큐리티 유저 컨텍스트 모델
 * JWT 검증 필터를 통과한 정상 사용자의 인증 정보를 시큐리티 컨텍스트에
 * 체류시키기 위한 UserDetails 구현체입니다. user_id만 보유합니다.
 *
 * @since : 2026.04.06
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@RequiredArgsConstructor
public class UserPrincipal implements UserDetails {

	// JWT 페이로드에서 추출된 사용자 DB PK
	private final Long userId;

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of();
	}

	@Override
	public String getPassword() {
		return null;
	}

	@Override
	public String getUsername() {
		return String.valueOf(this.userId);
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
