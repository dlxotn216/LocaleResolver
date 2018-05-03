package test.locale.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Locale;

/**
 * Created by taesu on 2018-05-03.
 */
@Getter
@Setter
public class LocaleMap {
    private Locale requestHeaderLocale;
    private Locale controllerArgLocale;
    private Locale localeContextHolderLocale;
    private Locale appLocaleContextHolderLocale;

    public LocaleMap() {
    }

    public LocaleMap(Locale requestHeaderLocale, Locale controllerArgLocale, Locale localeContextHolderLocale, Locale appLocaleContextHolderLocale) {
        this.requestHeaderLocale = requestHeaderLocale;
        this.controllerArgLocale = controllerArgLocale;
        this.localeContextHolderLocale = localeContextHolderLocale;
        this.appLocaleContextHolderLocale = appLocaleContextHolderLocale;
    }
}
