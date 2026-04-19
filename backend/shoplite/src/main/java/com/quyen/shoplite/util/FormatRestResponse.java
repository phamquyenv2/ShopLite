package com.quyen.shoplite.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyen.shoplite.util.annotation.ApiMessage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class FormatRestResponse implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {

        HttpServletRequest servletRequest =
                ((ServletServerHttpRequest) request).getServletRequest();
        String uri = servletRequest.getRequestURI();
        if (uri.startsWith("/v3/api-docs") || uri.startsWith("/swagger-ui")
                || uri.startsWith("/swagger-resources") || uri.startsWith("/webjars")) {
            return body;
        }

        HttpServletResponse servletResponse =
                ((ServletServerHttpResponse) response).getServletResponse();
        int statusCode = servletResponse.getStatus();

        // Không bọc nếu đã là Map lỗi (từ GlobalException)
        if (body instanceof Map<?, ?> map && map.containsKey("statusCode")) {
            return body;
        }

        // Đọc @ApiMessage nếu có
        String message = "";
        ApiMessage apiMessage = returnType.getMethodAnnotation(ApiMessage.class);
        if (apiMessage != null) {
            message = apiMessage.value();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("statusCode", statusCode);
        result.put("message", message);
        result.put("data", body);

        if (body instanceof String) {
            try {
                return new ObjectMapper().writeValueAsString(result);
            } catch (JsonProcessingException e) {
                return result.toString();
            }
        }

        return result;
    }
}
