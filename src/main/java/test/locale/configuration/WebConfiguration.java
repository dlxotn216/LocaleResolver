package test.locale.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import test.locale.configuration.interceptor.ApplicationLocaleChangeInterceptor;
import test.locale.configuration.resolver.AcceptHeaderThenCookieLocaleResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

/**
 * Created by taesu on 2018-05-03.
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    /**
     * AcceptHeaderLocaleResolver
     * Request Header의 Accept-Language로부터 locale 정보를 읽는다 (브라우저에서 기본으로 전송 함)
     *
     * Response에 Content-Language에는 적절히 language가 반환 된다
     *
     * 오직 Request Header로만 관리하기 때문에 브라우저의 여러 탭에서 별도의 Locale을 지정 할 수 있다
     * ex) 탭 1번은 En, 탭 2번은 Ko ...
     *
     * @return LocaleResolver
     */
//    @Bean
//    public LocaleResolver localeResolver(){
//        return new AcceptHeaderLocaleResolver();
//    }

    /**
     * CookieLocaleResolver
     * Locale 정보를 쿠키로 관리하는 리졸버이다.
     * 쿠키로 관리하기 때문에 브라우저의 여러 탭이 모두 동일한 Locale로 셋팅된다
     *
     * 아래는 세션을 통해 Locale을 관리하는 리졸버이다
     * @see org.springframework.web.servlet.i18n.SessionLocaleResolver
     *
     * @return LocaleResolver
     */
//    @Bean
//    public LocaleResolver localeResolver() {
//        CookieLocaleResolver localeResolver = new CookieLocaleResolver();
//        localeResolver.setDefaultLocale(Locale.ENGLISH);
//        localeResolver.setCookieName("test.locale.demo.locale");
//
//        return localeResolver;
//    }

    /**
     * CookieLocaleResolver
     *
     * @return
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderThenCookieLocaleResolver localeResolver = new AcceptHeaderThenCookieLocaleResolver();
        localeResolver.setDefaultLocale(Locale.ENGLISH);
        localeResolver.setCookieName("test.locale.demo.locale");

        return localeResolver;
    }


    /**
     * 파라미터로 전달 된 locale 정보를 자동으로 인터셉터에서 읽어와
     * 현재 컨텍스트에 등록 된 LocaleResolver에 setLocale을 호출하는 인터셉터
     * <p>
     * 만약 현재 등록된 localeResolver가 setLocale을 제대로 구현하고 있지 않다면 예외가 발생할 수 있으니 주의
     *
     * @param registry InterceptorRegistry
     * @see org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver#setLocale(HttpServletRequest, HttpServletResponse, Locale)
     * <p>
     * try {
     * localeResolver.setLocale(request, response, parseLocaleValue(newLocale));
     * }
     * catch (IllegalArgumentException ex) {
     * if (isIgnoreInvalidLocale()) {
     * logger.debug("Ignoring invalid locale value [" + newLocale + "]: " + ex.getMessage());
     * }
     * else {
     * throw ex;
     * }
     * }
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        ApplicationLocaleChangeInterceptor localeChangeInterceptor = new ApplicationLocaleChangeInterceptor();
        localeChangeInterceptor.setParamName("lang");
        registry.addInterceptor(localeChangeInterceptor);
    }

}
