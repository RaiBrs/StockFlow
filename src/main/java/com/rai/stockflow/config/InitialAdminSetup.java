package com.rai.stockflow.config;

import com.rai.stockflow.repository.UserRepository;
import com.rai.stockflow.security.Role;
import com.rai.stockflow.service.UserRegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Cria o primeiro administrador somente quando o modo de configuração é ativado. */
@Component
@ConditionalOnProperty(name = "stockflow.setup-admin.enabled", havingValue = "true")
public class InitialAdminSetup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialAdminSetup.class);

    private final UserRepository userRepository;
    private final UserRegistrationService registrationService;
    private final String email;
    private final String password;

    public InitialAdminSetup(UserRepository userRepository,
                             UserRegistrationService registrationService,
                             @Value("${STOCKFLOW_SETUP_ADMIN_EMAIL:}") String email,
                             @Value("${STOCKFLOW_SETUP_ADMIN_PASSWORD:}") String password) {
        this.userRepository = userRepository;
        this.registrationService = registrationService;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (email.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Informe o e-mail e a senha do administrador inicial.");
        }
        if (userRepository.count() != 0) {
            throw new IllegalStateException("A configuração inicial exige uma tabela de usuários vazia.");
        }

        registrationService.register(email, password, Role.ADMIN);
        log.info("Administrador inicial criado. Desative STOCKFLOW_SETUP_ADMIN_ENABLED nas próximas execuções.");
    }
}
