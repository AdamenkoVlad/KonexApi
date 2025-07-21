package com.example.konexapi.controller;

import com.example.konexapi.service.KonexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WebController {

    @Autowired
    private KonexService konexService;

    @GetMapping("/")
    public String home() {
        return "gallery";
    }

    @GetMapping("/gallery")
    public String gallery() {
        return "gallery";
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "Невірний логін або пароль");
        }
        return "login";
    }

    @GetMapping("/admin")
    public String admin(Model model, Authentication authentication) {
        try {

            model.addAttribute("username", authentication.getName());


            model.addAttribute("uploadHistory", konexService.getUploadHistory());


            int totalPhotos = konexService.getUploadHistory().size();
            model.addAttribute("totalPhotos", totalPhotos);

        } catch (Exception e) {
            model.addAttribute("error", "Помилка завантаження даних: " + e.getMessage());
        }
        return "admin";
    }

    @GetMapping("/logout")
    public String logout() {
        return "redirect:/";
    }
}