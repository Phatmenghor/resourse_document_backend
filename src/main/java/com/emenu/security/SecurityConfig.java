package com.emenu.security;

import com.emenu.security.jwt.JWTAuthenticationFilter;
import com.emenu.security.jwt.JwtAuthEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthEntryPoint authEntryPoint;
    private final JWTAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authEntryPoint))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ===== PUBLIC ENDPOINTS =====
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/public/**").permitAll()
                        .requestMatchers("/api/images/**").permitAll()

                        .requestMatchers("/api/v1/public/notifications/**").permitAll()

                        .requestMatchers("/api/v1/public/system-notifications/send").permitAll()
                        .requestMatchers("/api/v1/public/system-notifications/settings").hasAnyRole("PLATFORM_OWNER", "PLATFORM_ADMIN")

                        .requestMatchers("/api/v1/users/admin-token").permitAll()
                        .requestMatchers("/api/v1/users/api-key").permitAll()

                        // ===== STORAGE RESOURCE ENDPOINTS =====
                        // Public download endpoint (for shared resources)
                        .requestMatchers("/api/v1/storage/resources/public/**").permitAll()
                        // Storage API endpoints use X-Storage-Api-Key header for authentication
                        .requestMatchers("/api/v1/storage/resources/**").permitAll()
                        // Storage API key management requires ADMIN role (handled by @PreAuthorize)
                        .requestMatchers("/api/v1/storage/api-keys/**").authenticated()

                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/swagger-resources/**", "/webjars/**").permitAll()
                        .requestMatchers("/swagger-config", "/api-docs/**").permitAll()

                        // ===== ACTUATOR ENDPOINTS =====
                        .requestMatchers("/actuator/health/**").permitAll()

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", new CorsConfiguration() {{
            setAllowedOriginPatterns(List.of("*")); // matches all origins
            setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
            setAllowedHeaders(List.of("*"));
            setAllowCredentials(true); // allow cookies / auth headers
            setExposedHeaders(List.of("Authorization", "Content-Disposition", "Content-Type", "X-Storage-Api-Key"));
            setMaxAge(3600L);
        }});
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}