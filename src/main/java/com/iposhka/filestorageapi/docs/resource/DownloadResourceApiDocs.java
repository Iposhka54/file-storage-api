package com.iposhka.filestorageapi.docs.resource;

import com.iposhka.filestorageapi.dto.responce.ErrorResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Operation(
        summary = "Download a resource",
        description = "Downloads a file or a zipped directory from the specified path",
        parameters = {
                @Parameter(
                        name = "path",
                        description = "Full path to the resource (URL-encoded). Directories must end with '/'",
                        required = true,
                        example = "/folder1/file.txt")
        },
        responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful file download",
                        content = @Content(
                                mediaType = "application/octet-stream",
                                schema = @Schema(type = "string", format = "binary")
                        )
                ),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid or missing path",
                        content = @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ErrorResponseDto.class),
                                examples = @ExampleObject(
                                        value = """
                                                {
                                                    "message": "Invalid path format"
                                                }
                                                """
                                )
                        )
                ),
                @ApiResponse(
                        responseCode = "401",
                        description = "User is not authenticated",
                        content = @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ErrorResponseDto.class),
                                examples = @ExampleObject(
                                        value = """
                                                {
                                                    "message": "Unauthorized"
                                                }
                                                """
                                )
                        )
                ),
                @ApiResponse(
                        responseCode = "404",
                        description = "Resource not found",
                        content = @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ErrorResponseDto.class),
                                examples = @ExampleObject(
                                        value = """
                                                {
                                                    "message": "Resource not found"
                                                }
                                                """
                                )
                        )
                ),
                @ApiResponse(
                        responseCode = "500",
                        description = "Unknown server error",
                        content = @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ErrorResponseDto.class),
                                examples = @ExampleObject(
                                        value = """
                                                {
                                                    "message": "Unexpected server error"
                                                }
                                                """
                                )
                        )
                )
        }
)
public @interface DownloadResourceApiDocs {
}
