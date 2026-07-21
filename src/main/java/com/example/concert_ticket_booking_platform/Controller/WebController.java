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
}
