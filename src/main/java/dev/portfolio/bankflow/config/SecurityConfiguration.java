package dev.portfolio.bankflow.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class SecurityConfiguration {
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
    @Configuration @Profile("!prod")
    static class Local {
        @Bean UserDetailsService users(@Value("${app.review.username}") String name,
                @Value("${app.review.password}") String password,PasswordEncoder encoder) {
            return new InMemoryUserDetailsManager(User.withUsername(name)
                .password(encoder.encode(password)).roles("REVIEWER").build());
        }
        @Bean SecurityFilterChain localSecurity(HttpSecurity http) throws Exception {
            // Localhost / sandbox only. Never expose default BASIC credentials on public networks.
            http.csrf(c->c.disable()).authorizeHttpRequests(a->a
                .requestMatchers(HttpMethod.POST,"/api/cases/*/decision").hasRole("REVIEWER")
                .requestMatchers("/api/cases/*/audit").hasRole("REVIEWER")
                .anyRequest().permitAll()).httpBasic(Customizer.withDefaults());
            return http.build();
        }
    }
    @Configuration @Profile("prod")
    static class Production {
        @Bean SecurityFilterChain prodSecurity(HttpSecurity http)throws Exception {
            http.csrf(c->c.disable()).authorizeHttpRequests(a->a
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/prometheus").denyAll()
                .requestMatchers(HttpMethod.POST,"/api/cases/*/decision").hasAuthority("SCOPE_bankflow.review")
                .requestMatchers(HttpMethod.POST,"/api/cases").hasAuthority("SCOPE_bankflow.write")
                .requestMatchers(HttpMethod.GET,"/api/**").hasAnyAuthority("SCOPE_bankflow.read","SCOPE_bankflow.review")
                .requestMatchers("/","/index.html").denyAll()
                .anyRequest().denyAll())
                .oauth2ResourceServer(o->o.jwt(Customizer.withDefaults()));
            return http.build();
        }
    }
}
