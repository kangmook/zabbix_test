package com.skmns.infra.zabbixtest.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skmns.infra.zabbixtest.config.ZabbixConfig;
import com.skmns.infra.zabbixtest.dto.HostDto;
import com.skmns.infra.zabbixtest.dto.ItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Zabbix API 연동 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ZabbixApiService {

    private final RestTemplate restTemplate;
    private final ZabbixConfig zabbixConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String authToken;

    /**
     * Zabbix API 인증
     * 
     * @return 인증 토큰
     */
    public String authenticate() {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("method", "user.login");

            Map<String, String> params = new HashMap<>();
            params.put("username", zabbixConfig.getUsername());
            params.put("password", zabbixConfig.getPassword());
            request.put("params", params);

            request.put("id", 1);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            String response = restTemplate.postForObject(zabbixConfig.getApiUrl(), entity, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);

            authToken = jsonNode.get("result").asText();
            log.info("Zabbix API 인증 성공");
            return authToken;

        } catch (Exception e) {
            log.error("Zabbix API 인증 실패", e);
            throw new RuntimeException("Zabbix API 인증 실패", e);
        }
    }

    /**
     * 호스트 목록 조회
     * 
     * @return 호스트 목록
     */
    public List<HostDto> getHosts() {
        try {
            // 인증 토큰이 없으면 먼저 인증
            if (authToken == null || authToken.isEmpty()) {
                authenticate();
            }

            Map<String, Object> request = new HashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("method", "host.get");

            Map<String, Object> params = new HashMap<>();
            params.put("output", List.of("hostid", "host", "name"));
            params.put("selectInterfaces", List.of("ip"));
            params.put("selectInventory", "extend");
            params.put("selectTags", "extend"); // tags 정보 조회 추가
            request.put("params", params);

            request.put("auth", authToken);
            request.put("id", 1);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            String response = restTemplate.postForObject(zabbixConfig.getApiUrl(), entity, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);

            // 에러 체크
            if (jsonNode.has("error")) {
                log.warn("인증 토큰 만료, 재인증 시도");
                authenticate();
                return getHosts(); // 재귀 호출
            }

            JsonNode result = jsonNode.get("result");
            List<HostDto> hosts = new ArrayList<>();

            for (JsonNode hostNode : result) {
                HostDto host = HostDto.builder()
                        .hostId(hostNode.get("hostid").asText())
                        .hostname(hostNode.has("name") ? hostNode.get("name").asText() : "")
                        .build();

                // IP 주소 추출
                if (hostNode.has("interfaces") && hostNode.get("interfaces").isArray()
                        && hostNode.get("interfaces").size() > 0) {
                    JsonNode interfaceNode = hostNode.get("interfaces").get(0);
                    if (interfaceNode.has("ip")) {
                        host.setIp(interfaceNode.get("ip").asText());
                    }
                }

                // Tags에서 serviceCode 추출
                if (hostNode.has("tags") && hostNode.get("tags").isArray()) {
                    for (JsonNode tagNode : hostNode.get("tags")) {
                        if (tagNode.has("tag") && "serviceCode".equals(tagNode.get("tag").asText())) {
                            if (tagNode.has("value")) {
                                host.setServiceCode(tagNode.get("value").asText());
                                break;
                            }
                        }
                    }
                }

                // 인벤토리 정보 추출
                if (hostNode.has("inventory") && !hostNode.get("inventory").isEmpty()) {
                    JsonNode inventory = hostNode.get("inventory");

                    // tags에서 serviceCode를 못 찾았을 경우에만 inventory에서 가져오기
                    if (host.getServiceCode() == null && inventory.has("tag")) {
                        host.setServiceCode(inventory.get("tag").asText());
                    }
                    if (inventory.has("vendor")) {
                        host.setVendor(inventory.get("vendor").asText());
                    }
                    if (inventory.has("hardware")) {
                        host.setArch(inventory.get("hardware").asText());
                    }
                    if (inventory.has("os")) {
                        host.setOsVersion(inventory.get("os").asText());
                    }
                    if (inventory.has("os_full")) {
                        host.setOsVersionDetail(inventory.get("os_full").asText());
                    }
                    if (inventory.has("software")) {
                        host.setKernel(inventory.get("software").asText());
                    }
                }

                hosts.add(host);
            }

            log.info("호스트 {} 개 조회 완료", hosts.size());
            return hosts;

        } catch (Exception e) {
            log.error("호스트 목록 조회 실패", e);
            throw new RuntimeException("호스트 목록 조회 실패", e);
        }
    }

    /**
     * 특정 호스트의 아이템 목록 조회 (INFRA. 로 시작하는 아이템만)
     * 
     * @param hostId 호스트 ID
     * @return 아이템 목록
     */
    public List<ItemDto> getHostItems(String hostId) {
        try {
            // 인증 토큰이 없으면 먼저 인증
            if (authToken == null || authToken.isEmpty()) {
                authenticate();
            }

            Map<String, Object> request = new HashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("method", "item.get");

            Map<String, Object> params = new HashMap<>();
            params.put("output", new String[] { "itemid", "hostid", "name", "lastvalue" });
            params.put("hostids", hostId);

            // INFRA. 로 시작하는 아이템만 검색
            Map<String, String> searchParam = new HashMap<>();
            searchParam.put("name", "INFRA.");
            params.put("search", searchParam);

            request.put("params", params);
            request.put("auth", authToken);
            request.put("id", 1);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            String response = restTemplate.postForObject(zabbixConfig.getApiUrl(), entity, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);

            // 에러 체크
            if (jsonNode.has("error")) {
                log.warn("인증 토큰 만료, 재인증 시도");
                authenticate();
                return getHostItems(hostId); // 재귀 호출
            }

            JsonNode result = jsonNode.get("result");
            List<ItemDto> items = new ArrayList<>();

            for (JsonNode itemNode : result) {
                ItemDto item = ItemDto.builder()
                        .itemId(itemNode.has("itemid") ? itemNode.get("itemid").asText() : "")
                        .hostId(itemNode.has("hostid") ? itemNode.get("hostid").asText() : "")
                        .name(itemNode.has("name") ? itemNode.get("name").asText() : "")
                        .lastValue(itemNode.has("lastvalue") ? itemNode.get("lastvalue").asText() : "")
                        .build();
                items.add(item);
            }

            log.info("호스트 {} 의 아이템 {} 개 조회 완료", hostId, items.size());
            return items;

        } catch (Exception e) {
            log.error("아이템 목록 조회 실패", e);
            throw new RuntimeException("아이템 목록 조회 실패", e);
        }
    }
}
