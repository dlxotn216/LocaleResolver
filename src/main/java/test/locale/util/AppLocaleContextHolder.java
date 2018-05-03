package test.locale.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import test.locale.configuration.properties.ApplicationLocaleProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by taesu on 2018-05-03.
 */
@Component
public class AppLocaleContextHolder {

    private static final List<Locale> acceptableLocales = new ArrayList<>();

    @Autowired
    public AppLocaleContextHolder(ApplicationLocaleProperties applicationLocaleProperties) {
        List<String> acceptableLanguages = applicationLocaleProperties.getLanguages();
        if (!CollectionUtils.isEmpty(acceptableLanguages)) {
            for (int i = 0, length = applicationLocaleProperties.getLanguages().size(); i < length; i++) {
                acceptableLocales.add(applicationLocaleProperties.getLocale(i));
            }
        }
    }

    public static Locale getLocale() {
        if (isPossibleLocale(LocaleContextHolder.getLocale())) {
            return LocaleContextHolder.getLocale();
        } else {
            return ApplicationLocaleProperties.defaultLocale;
        }
    }

    /**
     * 현재 locale이 System에서지원하는 다국어 목록에 존재하는지 여부를 반환한다
     *
     * @param currentLocale 요청 받은 locale
     * @return boolean
     */
    private static boolean isPossibleLocale(Locale currentLocale) {
        return acceptableLocales.contains(currentLocale);
    }
}
