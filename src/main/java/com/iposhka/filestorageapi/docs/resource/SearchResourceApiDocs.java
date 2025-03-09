package com.iposhka.filestorageapi.docs.resource;

import com.iposhka.filestorageapi.dto.responce.ErrorResponseDto;
import com.iposhka.filestorageapi.dto.responce.resourse.ResourceResponseDto;
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
        summary = "Search for resources",
        description = "Searches for files matching the given query",
        parameters = {
                @Parameter(
                        name = "query",
                        description = "Search query in URL-encoded format",
                        required = true,
                        example = "file.txt")
        },
        responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful search results",
                        content = @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ResourceResponseDto.class),
                                examples = @ExampleObject(
                                        value = """
                                                [
                                                  {
                                                    "path": "folder1/folder2/",
                                                    "name": "file.txt",
                                                    "size": 123,
                                                    "type": "FILE"
                                                  }
                                                ]
                                                """
                                )
                        )
                ),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid or missing search query",
                        content = @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ErrorResponseDto.class),
                                examples = @ExampleObject(
                                        value = """
                                                {
                                                    "message": "Invalid search query"
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
public @interface SearchResourceApiDocs {
}
