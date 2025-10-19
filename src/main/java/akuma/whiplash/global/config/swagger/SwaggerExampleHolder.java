package akuma.whiplash.global.config.swagger;

import lombok.Builder;
import lombok.Getter;
import io.swagger.v3.oas.models.examples.Example;

@Getter
@Builder
public class SwaggerExampleHolder {

    private Example example;
    private String name;
    private int code;
}