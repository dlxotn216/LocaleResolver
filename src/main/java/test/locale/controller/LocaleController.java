package test.locale.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import test.locale.model.LocaleMap;
import test.locale.util.AppLocaleContextHolder;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

/**
 * Created by taesu on 2018-05-03.
 */
@RestController
@Slf4j
public class LocaleController {

    @GetMapping(value = "/locales")
    public LocaleMap getLocale(HttpServletRequest request, Locale locale) {
        //아래 네 가지 방법으로 Locale을 읽을 수 있다
        log.info("check from request :" + request.getLocale());
        log.info("check from controller argument :" + locale);
        log.info("check from LocaleContextHolder:" + LocaleContextHolder.getLocale());
        log.info("check from AppLocaleContextHolder:" + AppLocaleContextHolder.getLocale());

        return new LocaleMap(request.getLocale(), locale, LocaleContextHolder.getLocale(), AppLocaleContextHolder.getLocale());
    }
}
