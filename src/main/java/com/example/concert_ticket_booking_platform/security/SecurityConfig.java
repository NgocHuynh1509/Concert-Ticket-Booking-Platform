package com.example.concert_ticket_booking_platform.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
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

import java.util.List;

/**
 * PHAN QUYEN (authorization) duoc cau hinh o 2 lop:
 * 1) Lop "coarse-grained" ngay trong SecurityFilterChain ben duoi: cong khai vs phai dang nhap.
 * 2) Lop "fine-grained" theo role: dung @PreAuthorize("hasRole('OPERATOR')") tren tung method
 *    controller (xem ConcertController#createConcert) - @EnableMethodSecurity bat tinh nang nay.
 *
 * Stateless JWT: khong dung session (SessionCreationPolicy.STATELESS), moi request tu mang JWT
 * trong header Authorization.
 */
@Configuration
@EnableWebSecurity
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. Static resources + h2-console: Công khai
                        .requestMatchers(
                                "/", "/home", "/login", "/register", "/*.html", "/css/**", "/js/**", "/assets/**", "/favicon.ico",
                                "/h2-console/**", "/concert/**","/checkout","/booking-success","/favicon.ico"
                        ).permitAll()

                        // 2. Auth & GET Concerts: Công khai
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/concerts", "/api/concerts/**").permitAll()

                        // -------------------------------------------------------------
                        // 3. PHÂN QUYỀN RIÊNG CHO ROLE CUSTOMER
                        // -------------------------------------------------------------
                        // Chỉ người dùng có Role CUSTOMER mới được thao tác đặt vé
                        .requestMatchers("/api/bookings/**").hasRole("CUSTOMER")

                        // Nếu bạn có API lấy thông tin ticket category cho trang checkout:
                        .requestMatchers(HttpMethod.GET, "/api/ticket-categories/**").hasAnyRole("CUSTOMER", "OPERATOR", "ADMIN")

                        // các request còn lại yêu cầu authenticated
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}