package test.locale.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import test.locale.model.LocaleMap;

import java.util.Locale;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Created by taesu on 2018-05-03.
 */
@RunWith(value = SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LocaleControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     *
     */
    @Test
    public void testForDefaultAcceptLanguage(){
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept-Language", "");

        ResponseEntity<LocaleMap> response
                = restTemplate.exchange("/locales", HttpMethod.GET, new HttpEntity<>(headers), LocaleMap.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getLocaleContextHolderLocale()).isEqualTo(Locale.ENGLISH);
    }

    @Test
    public void testForChangeAcceptLanguage() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept-Language", "ja_JP");

        ResponseEntity<LocaleMap> response
                = restTemplate.exchange("/locales", HttpMethod.GET, new HttpEntity<>(headers), LocaleMap.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getLocaleContextHolderLocale()).isEqualTo(Locale.JAPAN);
    }

    @Test
    public void testForChangeAcceptLanguageAndParam() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept-Language", "ja_JP");

        ResponseEntity<LocaleMap> response
                = restTemplate.exchange("/locales?lang=ko_KR", HttpMethod.GET, new HttpEntity<>(headers), LocaleMap.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getLocaleContextHolderLocale()).isEqualTo(Locale.KOREA);
    }

    @Test
    public void testForSendQueryString(){
        ResponseEntity<LocaleMap> response
                = restTemplate.getForEntity("/locales?lang=en_CA", LocaleMap.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getLocaleContextHolderLocale()).isEqualTo(Locale.CANADA);
    }

    /**
     * 지원하지 않는 local이면 default locald을 반환할 것이다
     */
    @Test
    public void testForAppLocaleContextHolder(){
        ResponseEntity<LocaleMap> response
                = restTemplate.getForEntity("/locales?lang=en_CA", LocaleMap.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getAppLocaleContextHolderLocale()).isEqualTo(Locale.ENGLISH);
    }

}