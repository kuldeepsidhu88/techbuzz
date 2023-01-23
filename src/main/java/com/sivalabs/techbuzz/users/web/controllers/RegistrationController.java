package com.sivalabs.techbuzz.users.web.controllers;

import com.sivalabs.techbuzz.common.exceptions.ResourceAlreadyExistsException;
import com.sivalabs.techbuzz.notifications.EmailService;
import com.sivalabs.techbuzz.users.domain.User;
import com.sivalabs.techbuzz.users.usecases.registration.CreateUserHandler;
import com.sivalabs.techbuzz.users.usecases.registration.CreateUserRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Controller
@RequiredArgsConstructor
public class RegistrationController {
    private static final Logger logger = LoggerFactory.getLogger(RegistrationController.class);
    private static final String REGISTRATION_VIEW = "registration";

    private final CreateUserHandler createUserHandler;
    private final EmailService emailService;

    @GetMapping("/registration")
    public String registrationForm(Model model) {
        model.addAttribute("user", new CreateUserRequest("", "", ""));
        return REGISTRATION_VIEW;
    }

    @PostMapping("/registration")
    public String registerUser(
            HttpServletRequest request,
            @Valid @ModelAttribute("user") CreateUserRequest createUserRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return REGISTRATION_VIEW;
        }
        try {
            User user = createUserHandler.createUser(createUserRequest);
            this.sendVerificationEmail(request, user);
            redirectAttributes.addFlashAttribute("message", "Registration is successful.");
            return "redirect:/registrationStatus";
        } catch (ResourceAlreadyExistsException e) {
            logger.error("Registration err", e);
            bindingResult.rejectValue("email", "email.exists", e.getMessage());
            return REGISTRATION_VIEW;
        }
    }

    @GetMapping("/registrationStatus")
    public String registrationStatus(Model model) {
        return "registrationStatus";
    }

    private void sendVerificationEmail(HttpServletRequest request, User user) {
        String baseUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath(null)
                .build()
                .toUriString();
        String verificationUrl = baseUrl + "/verifyEmail?email="+user.getEmail()+"&token="+user.getVerificationToken();
        String to = user.getEmail();
        String subject = "TechBuzz - Email verification";
        String content = """
                Hi %s,
                <br/>
                <br/>
                Please click on the below link to verify your account.
                <br/>
                <br/>
                <a href="%s" target="_blank">Verify Email</a>
                <br/>
                <br/>
                Thanks,<br/>
                TechBuzz Team
                """.formatted(user.getName(), verificationUrl);
        emailService.sendEmail(to, subject, content);
    }
}