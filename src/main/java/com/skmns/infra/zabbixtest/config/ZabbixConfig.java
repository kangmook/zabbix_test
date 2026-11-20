package com.skmns.infra.zabbixtest.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Zabbix API 연동 설정
 */
@Configuration
@Getter
public class ZabbixConfig {

    @Value("${zabbix.api.url}")
    private String apiUrl;

    @Value("${zabbix.api.username}")
    private String username;

    @Value("${zabbix.api.password}")
    private String password;

    /**
     * RestTemplate Bean 등록
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
