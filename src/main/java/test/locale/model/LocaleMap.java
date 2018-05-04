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
    private Locale requestGetLocale;
    private Locale controllerArgLocale;
    private Locale localeContextHolderLocale;
    private Locale appLocaleContextHolderLocale;

    public LocaleMap() {
    }

    public LocaleMap(Locale requestGetLocale, Locale controllerArgLocale, Locale localeContextHolderLocale, Locale appLocaleContextHolderLocale) {
        this.requestGetLocale = requestGetLocale;
        this.controllerArgLocale = controllerArgLocale;
        this.localeContextHolderLocale = localeContextHolderLocale;
        this.appLocaleContextHolderLocale = appLocaleContextHolderLocale;
    }
}
