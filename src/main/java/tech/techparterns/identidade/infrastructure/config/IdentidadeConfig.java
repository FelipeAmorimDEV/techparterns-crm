package tech.techparterns.identidade.infrastructure.config;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/** Limites e parâmetros do módulo de identidade (DT-13: o perfil de teste pode reduzi-los). */
@ConfigMapping(prefix = "crm")
public interface IdentidadeConfig {

    @WithName("frontend-url")
    @WithDefault("http://localhost:4200")
    String frontendUrl();

    @WithName("bcrypt-cost")
    @WithDefault("12")
    int bcryptCost();

    @WithName("allowed-origins")
    @WithDefault("http://localhost:4200")
    List<String> allowedOrigins();

    Cookie cookie();
    Login login();
    Rate rate();
    Refresh refresh();
    Mail mail();
    Rotas rotas();
    Owner owner();

    interface Cookie {
        @WithDefault("true")
        boolean secure();
    }

    interface Login {
        @WithName("par-max-falhas") @WithDefault("5") int parMaxFalhas();
        @WithName("email-max-falhas") @WithDefault("20") int emailMaxFalhas();
        @WithName("ip-max-falhas") @WithDefault("50") int ipMaxFalhas();
        @WithDefault("PT15M") Duration janela();
        @WithDefault("PT15M") Duration bloqueio();
    }

    interface Rate {
        @WithName("login-por-minuto") @WithDefault("30") int loginPorMinuto();
        @WithName("refresh-por-minuto") @WithDefault("60") int refreshPorMinuto();
        @WithName("recuperacao-por-15-min") @WithDefault("5") int recuperacaoPor15Min();
        @WithName("cadastro-por-hora") @WithDefault("30") int cadastroPorHora();
        @WithName("reenvio-intervalo") @WithDefault("PT60S") Duration reenvioIntervalo();
    }

    interface Refresh {
        @WithName("tolerancia-reuso") @WithDefault("PT10S") Duration toleranciaReuso();
    }

    interface Mail {
        /** Atrasos entre tentativas automáticas: imediata, +1 min, +5 min. */
        @WithName("retry-delays") @WithDefault("PT1M,PT5M") List<Duration> retryDelays();
    }

    interface Rotas {
        @WithName("definir-senha") @WithDefault("/definir-senha") String definirSenha();
        @WithName("redefinir-senha") @WithDefault("/redefinir-senha") String redefinirSenha();
        @WithDefault("/convite") String convite();
    }

    interface Owner {
        Optional<String> email();
        Optional<String> nome();
        @WithDefault("true") boolean semear();
    }
}
