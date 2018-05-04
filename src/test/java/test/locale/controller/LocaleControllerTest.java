package test.locale.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import test.locale.model.LocaleMap;

import java.util.Arrays;
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
	 * request.getLocale에선 ko_KR로 시스템의 default locale을 반환 함
	 */
	@Test
	public void testForSetAcceptLanguageByHeaderName_acceptLanguageToEmptyString() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Accept-Language", "");
		
		ResponseEntity<LocaleMap> response
				= restTemplate.exchange("/locales", HttpMethod.GET, new HttpEntity<>(headers), LocaleMap.class);
		
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getAppLocaleContextHolderLocale()).isEqualTo(Locale.ENGLISH);
	}
	
	/**
	 * request.getLocale에선 ko_KR로 시스템의 default locale을 반환 함
	 */
	@Test
	public void testForSetAcceptLanguageByHeaderName_acceptLanguageToNull() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Accept-Language", null);
		
		ResponseEntity<LocaleMap> response
				= restTemplate.exchange("/locales", HttpMethod.GET, new HttpEntity<>(headers), LocaleMap.class);
		
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getAppLocaleContextHolderLocale()).isEqualTo(Locale.ENGLISH);
	}
	
	
	@Test
	public void testForChangeAcceptLanguage() {
		HttpHeaders headers = new HttpHeaders();
//        headers.add("Accept-Language", "ja_JP");
		headers.setAcceptLanguageAsLocales(Arrays.asList(Locale.JAPAN));    //ja-JP로 헤더 전송 됨
		
		ResponseEntity<LocaleMap> response
				= restTemplate.exchange("/locales", HttpMethod.GET, new HttpEntity<>(headers), LocaleMap.class);
		
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getAppLocaleContextHolderLocale()).isEqualTo(Locale.JAPAN);
		
		System.out.println(Locale.KOREA);                                	//ko_KR
		System.out.println(Locale.KOREAN);                                	//ko
		System.out.println(new Locale("ko"));                    //ko
		System.out.println(new Locale("ko_KR"));                	//ko_kr
		System.out.println(new Locale("ko", "KR"));    	//ko_KR
		System.out.println();
		System.out.println();
		System.out.println(Locale.JAPAN);                                 	//ja_JP
		System.out.println(Locale.JAPANESE);                              	//ja
		System.out.println(new Locale("ja"));                		//ja
		System.out.println(new Locale("ja_JP"));          		//ja_jp
		System.out.println(new Locale("ja", "JP")); 		//ja_JP
		System.out.println();
		System.out.println();
		System.out.println(Locale.CHINA);                                	//zh_CN
		System.out.println(Locale.CHINESE);                                	//zh
		System.out.println(Locale.SIMPLIFIED_CHINESE);                    	//zh_CN
		System.out.println(Locale.TRADITIONAL_CHINESE);                    	//zh_TW
		System.out.println(new Locale("zh"));                   	//zh
		System.out.println(new Locale("zh_CN"));                	//zh_cn
		System.out.println(new Locale("zh_TW"));                	//zh_tw
		System.out.println(new Locale("zh", "CN"));    	//zn_CN
		System.out.println(new Locale("zh", "TW"));    	//zn_TW
		
	}
	
	@Test
	public void testForSendQueryString() {
		ResponseEntity<LocaleMap> response
				= restTemplate.getForEntity("/locales?lang=en_CA", LocaleMap.class);
		
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getAppLocaleContextHolderLocale()).isEqualTo(Locale.ENGLISH);
	}
	
	/**
	 * Accept-Language로 보낸 Locale이 우선순위가 높다
	 * 다만 값이 롷지 않다면 Request parameter로 보낸 값이 설정된다.
	 */
	@Test
	public void testForChangeAcceptLanguageAndParam() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Accept-Language", "ja_JP");
		
		ResponseEntity<LocaleMap> response
				= restTemplate.exchange("/locales?lang=ko_KR", HttpMethod.GET, new HttpEntity<>(headers), LocaleMap.class);
		
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getAppLocaleContextHolderLocale()).isEqualTo(Locale.KOREA);
		assertThat(response.getBody().getRequestGetLocale()).isEqualTo(new Locale(""));        //request.getLocale 에선 아무런 결과가 없다
	}

	/**
	 * Accept-Language로 보낸 Locale이 우선순위가 높다
	 * 다만 값이 롷지 않다면 Request parameter로 보낸 값이 설정 되며 그마저 없다면 System의 default locale이 적용된다.
	 */
	@Test
	public void testForChangeAcceptLanguageAndNonParam() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Accept-Language", "ja_JP");

		ResponseEntity<LocaleMap> response
				= restTemplate.exchange("/locales", HttpMethod.GET, new HttpEntity<>(headers), LocaleMap.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getAppLocaleContextHolderLocale()).isEqualTo(Locale.ENGLISH);		//System의 default locale
		assertThat(response.getBody().getRequestGetLocale()).isEqualTo(new Locale(""));        //잘못 된 형식의 header value이므로 request.getLocale 에선 아무런 결과가 없다
	}
	
	/**
	 * Accept-Language로 보낸 Locale이 우선순위가 높다
	 */
	@Test
	public void testForChangeAcceptLanguageToTagAndParam() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Accept-Language", Locale.JAPAN.toLanguageTag());        //request header 전송 규약은 language tag로 보냄. _(Under bar)를 허용하지 않기 때문
		
		ResponseEntity<LocaleMap> response
				= restTemplate.exchange("/locales?lang=ko_KR", HttpMethod.GET, new HttpEntity<>(headers), LocaleMap.class);
		
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getAppLocaleContextHolderLocale()).isEqualTo(Locale.JAPAN);
		assertThat(response.getBody().getRequestGetLocale()).isEqualTo(Locale.JAPAN);
	}
	
	/**
	 * 지원하지 않는 local이면 ApplicationLocaleContextholder에서는 default locald을 반환할 것이다
	 */
	@Test
	public void testForAppLocaleContextHolder() {
		ResponseEntity<LocaleMap> response
				= restTemplate.getForEntity("/locales?lang=en_CA", LocaleMap.class);
		
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getAppLocaleContextHolderLocale()).isEqualTo(Locale.ENGLISH);
	}
	
}