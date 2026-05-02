package com.quyen.shoplite.domain.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ResStoreInvitationAcceptDTO {
    private ResMeDTO.StoreInfo currentStore;
    private List<ResPermissionDTO> permissions;
}
