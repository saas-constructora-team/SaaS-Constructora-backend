package com.SaaS.Constructora.identity.interfaces;

import com.SaaS.Constructora.identity.application.AccountProfileUseCase;
import com.SaaS.Constructora.identity.interfaces.dto.AccountProfileResponse;
import com.SaaS.Constructora.identity.interfaces.dto.CompanyResponse;
import com.SaaS.Constructora.identity.interfaces.dto.UpdateAccountProfileRequest;
import com.SaaS.Constructora.identity.interfaces.dto.UpdateCompanyRequest;
import com.SaaS.Constructora.shared.security.AuthenticatedUser;
import com.SaaS.Constructora.shared.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
class AccountController {

    private final AccountProfileUseCase accountProfileUseCase;

    AccountController(AccountProfileUseCase accountProfileUseCase) {
        this.accountProfileUseCase = accountProfileUseCase;
    }

    @GetMapping("/me")
    AccountProfileResponse me(@CurrentUser AuthenticatedUser user) {
        var profile = accountProfileUseCase.getProfile(user.userId(), user.tenantId());
        return AccountProfileResponse.from(profile.user(), profile.membership(), profile.tenant());
    }

    @PatchMapping("/profile")
    AccountProfileResponse updateProfile(@CurrentUser AuthenticatedUser user,
                                         @Valid @RequestBody UpdateAccountProfileRequest request) {
        var profile = accountProfileUseCase.updateProfile(user.userId(), user.tenantId(),
                request.fullName(), request.username());
        return AccountProfileResponse.from(profile.user(), profile.membership(), profile.tenant());
    }

    @PatchMapping("/company")
    CompanyResponse updateCompany(@CurrentUser AuthenticatedUser user,
                                  @Valid @RequestBody UpdateCompanyRequest request) {
        var profile = accountProfileUseCase.updateCompany(user.userId(), user.tenantId(),
                request.name(), request.tradeName(), request.taxId(),
                request.contactPhone(), request.address(), request.companyEmail());
        return CompanyResponse.from(profile.tenant());
    }
}
