package com.skmns.infra.zabbixtest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 아이템 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemDto {

    /**
     * 아이템 ID
     */
    private String itemId;

    /**
     * 호스트 ID
     */
    private String hostId;

    /**
     * 아이템 이름
     */
    private String name;

    /**
     * 마지막 값
     */
    private String lastValue;
}
