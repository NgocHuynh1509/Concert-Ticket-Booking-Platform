package com.example.concert_ticket_booking_platform.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/login")
    public String loginPage() {
        return "login"; // returns templates/login.html
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register"; // returns templates/register.html
    }

    @GetMapping({"/", "/home"})
    public String homePage() {
        return "home"; // returns templates/home.html
    }

    @GetMapping("/concert/{id}")
    public String concertDetail(){
        return "concert-detail";
    }

    @GetMapping("/checkout")
    public String checkoutPage() {
        return "checkout"; // Trả về checkout.html
    }

    // MỚI: trang mock thanh toán (2 nút thành công/thất bại) — trước đó payment.html
    // chưa có route nào trả về nên truy cập /payment sẽ bị 404.
    @GetMapping("/payment")
    public String paymentPage() {
        return "payment"; // Trả về payment.html
    }

    @GetMapping("/booking-success")
    public String bookingSuccessPage() {
        return "booking-success"; // Trả về booking-success.html
    }

    @GetMapping("/my-bookings")
    public String myBookings() {
        return "my-bookings";
    }

    @GetMapping("/booking/{id}")
    public String bookingDetail() {
        return "booking-detail";
    }

    @GetMapping("/ops/dashboard")
    public String opsDashboard() {
        return "ops-dashboard";
    }

    @GetMapping("/ops/bookings")
    public String opsBookings() {
        return "ops-bookings"; // Trả về ops-bookings.html
    }

    @GetMapping("/ops/bookings/{id}")
    public String opsBookingDetail() {
        return "op-booking-detail"; // Trả về file templates/ops-booking-detail.html (file đã viết ở bước trước)
    }

    // MỚI: trang tra cứu/lọc ticket categories theo concert hoặc loại vé,
    // gọi API GET /ops/ticket-categories (đã viết ở TicketCategoryController).
    @GetMapping("/ops/ticket-categories/view")
    public String opsTicketCategories() {
        return "op-ticker"; // Trả về templates/ops-ticket-categories.html
    }

    @GetMapping("/ops/vouchers")
    public String opsVoucher() {
        return "op-voucher"; // Trả về templates/ops-ticket-categories.html
    }



}