package top.kircute.texas.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Slf4j
public class ErrorForwardInterceptor implements HandlerInterceptor {
    private String baseUrl;
    private String baseUrlError;

    @Value("${error-redirect}")
    private String errorRedirect;

    @Value("${spring.mvc.servlet.path}")
    private void setBaseUrl(String rawBaseUrl) {
        baseUrl = rawBaseUrl.endsWith("/") ? rawBaseUrl.substring(0, rawBaseUrl.length() - 1) : rawBaseUrl;
        baseUrlError = baseUrl + "/error";
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (request.getRequestURI().equals(baseUrl)) {
            response.sendRedirect(baseUrl + "/#/");
            return false;
        }
        if (request.getRequestURI().equals(baseUrlError) && !errorRedirect.isEmpty()) {
            response.sendRedirect(errorRedirect);
            return false;
        }
        return true;
    }
}
