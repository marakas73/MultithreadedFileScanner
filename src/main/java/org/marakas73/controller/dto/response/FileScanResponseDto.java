package org.marakas73.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "File scan response object")
public record FileScanResponseDto(

        @Schema(description = "Providing token of alive task")
        String token,

        @Schema(description = "Status of task", allowableValues = {"true", "false"})
        boolean completed,

        @Schema(description = "Founded and filtered files by task")
        List<String> result
) {
}
