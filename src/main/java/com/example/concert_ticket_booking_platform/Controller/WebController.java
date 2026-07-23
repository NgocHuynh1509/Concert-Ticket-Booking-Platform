package com.example.concert_ticket_booking_platform.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping({"/", "/home"})
    public String homePage() {
        return "home";
    }

    @GetMapping("/concert/{id}")
    public String concertDetail(){
        return "concert-detail";
    }

    @GetMapping("/checkout")
    public String checkoutPage() {
        return "checkout";
    }

    @GetMapping("/payment")
    public String paymentPage() {
        return "payment";
    }

    @GetMapping("/booking-success")
    public String bookingSuccessPage() {
        return "booking-success";
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
        return "ops-bookings";
    }

    @GetMapping("/ops/bookings/{id}")
    public String opsBookingDetail() {
        return "op-booking-detail";
    }

    @GetMapping("/ops/ticket-categories/view")
    public String opsTicketCategories() {
        return "op-ticker";
    }

    @GetMapping("/ops/vouchers")
    public String opsVoucher() {
        return "op-voucher";
    }



}