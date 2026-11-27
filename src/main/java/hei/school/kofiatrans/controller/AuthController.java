package hei.school.kofiatrans.controller;

import hei.school.kofiatrans.controller.dto.LoginRequestDto;
import hei.school.kofiatrans.controller.dto.LoginResponseDto;
import hei.school.kofiatrans.controller.dto.UserDto;
import hei.school.kofiatrans.exception.ClientException;
import hei.school.kofiatrans.model.AppUser;
import hei.school.kofiatrans.security.TokenService;
import hei.school.kofiatrans.service.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;

    public AuthController(AuthService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public LoginResponseDto login(@RequestBody LoginRequestDto request) {
        if (request == null || isBlank(request.username()) || isBlank(request.password())) {
            throw new ClientException("Les champs 'username' et 'password' sont obligatoires");
        }
        AppUser user = authService.authenticate(request.username().trim(), request.password());
        TokenService.IssuedToken token = tokenService.issue(user);
        return new LoginResponseDto(
                token.value(),
                "Bearer",
                token.expiresInSeconds(),
                token.expiresAt(),
                new UserDto(user.username(), user.role().name())
        );
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal Jwt jwt) {
        return new UserDto(jwt.getSubject(), jwt.getClaimAsString(TokenService.ROLE_CLAIM));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
