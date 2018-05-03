package test.locale.configuration.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Created by taesu on 2018-05-03.
 */
@Component
@ConfigurationProperties(prefix = "app")
@Setter
@Getter
public class ApplicationLocaleProperties {
    public static final Locale defaultLocale = Locale.ENGLISH;

    private List<String> languages;
}
