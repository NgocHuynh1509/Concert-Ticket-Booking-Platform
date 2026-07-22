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
 *
 * LƯU Ý QUAN TRỌNG về page-route (không phải API):
 * Token JWT được lưu ở localStorage phía client và chỉ được đính kèm khi JS chủ động gọi
 * fetch()/XHR — trình duyệt KHÔNG tự gắn nó vào request điều hướng trang thông thường
 * (vd click <a href="/my-bookings">, gõ URL, load lại trang...). Vì vậy mọi route chỉ có
 * nhiệm vụ "trả về file HTML" (được ViewController forward, ví dụ /concert/{id},
 * /my-bookings, /booking/{id}) đều PHẢI permitAll ở đây, bất kể nội dung trang đó có yêu
 * cầu đăng nhập hay không — việc chặn truy cập dữ liệu thật sự nằm ở tầng API
 * (/api/bookings/**, /api/holds/**...), không nằm ở tầng route trang.
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
                        // 1. Static resources + page routes (chỉ trả HTML, xem javadoc ở trên): Công khai
                        //    FIX: thêm "/my-bookings" và "/booking/**" — 2 route bị thiếu, gây lỗi
                        //    401 ngay khi vừa điều hướng trang (trước khi JS kịp gắn Bearer token).
                        //    FIX: bỏ "/favicon.ico" bị khai trùng 2 lần trong bản gốc.
                        .requestMatchers(
                                "/", "/home", "/login", "/register", "/*.html", "/css/**", "/js/**", "/assets/**", "/favicon.ico",
                                "/h2-console/**", "/concert/**", "/checkout", "/payment", "/booking-success",
                                "/my-bookings", "/booking/**"
                        ).permitAll()

                        // 2. Auth & GET Concerts: Công khai
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/concerts", "/api/concerts/**").permitAll()

                        // -------------------------------------------------------------
                        // 3. PHÂN QUYỀN RIÊNG CHO ROLE CUSTOMER
                        // -------------------------------------------------------------
                        // Chỉ người dùng có Role CUSTOMER mới được thao tác đặt vé
                        .requestMatchers("/api/bookings/**").hasRole("CUSTOMER")

                        // Hold (giữ chỗ tạm) là hành động của customer, cùng nhóm quyền với booking
                        .requestMatchers("/api/holds/**").hasRole("CUSTOMER")

                        // Xác nhận kết quả thanh toán — chỉ chủ booking (customer) mới gọi được.
                        // PaymentService đã tự kiểm tra thêm booking.user.id == currentUser.id ở tầng service,
                        // đây chỉ là lớp chặn thô ở URL (coarse-grained) theo đúng quy ước của file này.
                        .requestMatchers("/api/payments/**").hasRole("CUSTOMER")

                        // Nếu bạn có API lấy thông tin ticket category cho trang checkout:
                        // LƯU Ý: enum UserRole hiện chỉ có CUSTOMER/OPERATOR — "ADMIN" ở đây sẽ
                        // KHÔNG BAO GIỜ match cho tới khi bạn thêm ADMIN vào UserRole. Không xoá vì
                        // rõ ràng là chủ đích của bạn, nhưng cần thêm ADMIN vào enum để có tác dụng.
                        .requestMatchers(HttpMethod.GET, "/api/ticket-categories/**").hasAnyRole("CUSTOMER", "OPERATOR", "ADMIN")

                        // Danh sách voucher đang áp dụng được — checkout.html gọi endpoint này để
                        // đổ vào dropdown. Cho phép mọi role đã đăng nhập xem, giống ticket-categories.
                        .requestMatchers(HttpMethod.GET, "/api/vouchers/available").hasAnyRole("CUSTOMER", "OPERATOR", "ADMIN")

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