package com.skmns.infra.zabbixtest.controller;

import com.skmns.infra.zabbixtest.dto.HostDto;
import com.skmns.infra.zabbixtest.dto.ItemDto;
import com.skmns.infra.zabbixtest.service.ZabbixApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Zabbix API 컨트롤러
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ZabbixController {

    private final ZabbixApiService zabbixApiService;

    /**
     * 호스트 목록 조회
     * 
     * @return 호스트 목록
     */
    @GetMapping("/hosts")
    public ResponseEntity<List<HostDto>> getHosts() {
        log.info("호스트 목록 조회 요청");
        List<HostDto> hosts = zabbixApiService.getHosts();
        return ResponseEntity.ok(hosts);
    }

    /**
     * 호스트 목록 동기화 (최신 데이터 가져오기)
     * 
     * @return 호스트 목록
     */
    @GetMapping("/hosts/sync")
    public ResponseEntity<List<HostDto>> syncHosts() {
        log.info("호스트 목록 동기화 요청");
        // 재인증 후 최신 데이터 가져오기
        zabbixApiService.authenticate();
        List<HostDto> hosts = zabbixApiService.getHosts();
        return ResponseEntity.ok(hosts);
    }

    /**
     * 특정 호스트의 아이템 목록 조회 (INFRA. 로 시작하는 아이템만)
     * 
     * @param hostId 호스트 ID
     * @return 아이템 목록
     */
    @GetMapping("/hosts/{hostId}/items")
    public ResponseEntity<List<ItemDto>> getHostItems(@PathVariable String hostId) {
        log.info("호스트 {} 의 아이템 목록 조회 요청", hostId);
        List<ItemDto> items = zabbixApiService.getHostItems(hostId);
        return ResponseEntity.ok(items);
    }
}
