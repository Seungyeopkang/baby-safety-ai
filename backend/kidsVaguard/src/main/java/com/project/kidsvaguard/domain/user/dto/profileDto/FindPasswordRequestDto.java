package com.project.kidsvaguard.domain.user.dto.profileDto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FindPasswordRequestDto {
    private String userId;
    private String email;
}