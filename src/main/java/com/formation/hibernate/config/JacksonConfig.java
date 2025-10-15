package com.formation.hibernate.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson Configuration for Hibernate Integration
 *
 * This configuration handles Hibernate proxy objects and lazy-loaded entities
 * to prevent serialization errors when returning entities directly from controllers.
 *
 * ✅ BOA PRÁTICA: Configurar Jackson para lidar com proxies do Hibernate
 * ✅ BOA PRÁTICA: Não forçar carregamento de entidades lazy
 * ✅ BOA PRÁTICA: Usar FORCE_LAZY_LOADING = false para evitar N+1
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();

        // ✅ BOA PRÁTICA: Registrar módulo Hibernate6
        // VANTAGEM: Jackson sabe como serializar proxies do Hibernate
        // VANTAGEM: Evita erros "ByteBuddyInterceptor" e "HibernateLazyInitializer"
        Hibernate6Module hibernate6Module = new Hibernate6Module();

        // ✅ BOA PRÁTICA: NÃO forçar carregamento de entidades lazy
        // IMPORTANTE: Se FORCE_LAZY_LOADING = true, causaria N+1 queries!
        // RESULTADO: Propriedades lazy aparecem como null no JSON (comportamento desejado)
        hibernate6Module.configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, false);

        // ✅ BOA PRÁTICA: Usar inicializadores lazy ao serializar
        // VANTAGEM: Permite que @JsonIgnore funcione corretamente
        hibernate6Module.configure(Hibernate6Module.Feature.USE_TRANSIENT_ANNOTATION, false);

        objectMapper.registerModule(hibernate6Module);

        return objectMapper;
    }
}
