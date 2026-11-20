package com.skmns.infra.zabbixtest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 호스트 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HostDto {

    /**
     * 호스트 ID
     */
    private String hostId;

    /**
     * IP 주소
     */
    private String ip;

    /**
     * 호스트명
     */
    private String hostname;

    /**
     * 서비스 코드
     */
    private String serviceCode;

    /**
     * 벤더
     */
    private String vendor;

    /**
     * 아키텍처
     */
    private String arch;

    /**
     * 커널
     */
    private String kernel;

    /**
     * OS 버전
     */
    private String osVersion;

    /**
     * OS 버전 상세
     */
    private String osVersionDetail;
}
