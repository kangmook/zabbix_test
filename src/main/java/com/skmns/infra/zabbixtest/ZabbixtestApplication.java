package com.skmns.infra.zabbixtest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Zabbix 서버 현황 조회 시스템 메인 애플리케이션
 */
@SpringBootApplication
public class ZabbixtestApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZabbixtestApplication.class, args);
    }
}
