package test.locale.configuration.interceptor;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

/**
 * Created by taesu on 2018-05-04.
 *
 * Request Parameter로부터 전달 된 Locale 값을 LocaleContextHolder의 저장하는 LocaleChangeInterceptor의 확장
 * @see LocaleChangeInterceptor#preHandle(HttpServletRequest, HttpServletResponse, Object)
 *
 * 단 기존 구현체에선 Accept-Language에 대한 고려가 없음을 주의하세요.
 *
 * <code>ApplicationLocalChangeInterceptor</code>에선 Accept-Language에 값이 존재하는 경우 해당 Locale을 우선적으로 반영합니다.
 * 이때 유효하지 않은 Accept-Language 값이 전달되어 <code>AcceptHeaderThenCookieLocaleResolver</code>에서 설정한 Locale이 옳지 않은 경우
 * request parameter로 전달 된 Locale을 적용 합니다.
 * 그 값도 없는 경우엔 default locale 값을 사용 합니다.
 *
 * @see test.locale.configuration.resolver.AcceptHeaderThenCookieLocaleResolver#determineDefaultLocale(HttpServletRequest)
 */
public class ApplicationLocaleChangeInterceptor extends LocaleChangeInterceptor {

    private static final Locale wrongLocale = new Locale("");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws ServletException {
        if(!StringUtils.isEmpty(request.getHeader("Accept-Language"))){
            if(LocaleContextHolder.getLocale().equals(wrongLocale)){
                return super.preHandle(request, response, handler);
            }
            else {
                return true;
            }
        }
        else{
            return super.preHandle(request, response, handler);
        }
    }
}
