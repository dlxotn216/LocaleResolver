package test.locale.configuration.resolver;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

/**
 * Created by taesu on 2018-05-03.
 *
 * Accept Header가 있다면 우선으로 Accept-Language로 넘어온 Locale 값을 처리하고
 * 그 외엔 CookieLocaleResolver로 위임한다.
 *
 * Browser의 Tab 별 별도의 locale을 처리할 수 잇도록 하기 위해 사용 된다.
 */
public class AcceptHeaderThenCookieLocaleResolver extends CookieLocaleResolver {
    @Nullable
    protected Locale determineDefaultLocale(HttpServletRequest request) {
        if(!StringUtils.isEmpty(request.getHeader("Accept-Language"))){				//headers.setAcceptLanguageAsLocales(Arrays.asList(Locale.JAPAN)) ja_JP 이지만 setAcceptLanguageAsLocales 내부에서 ja-JP로 바꿈 (toLanguageTag 메소드 호출 함)
            return Locale.forLanguageTag(request.getHeader("Accept-Language"));		//Accept header에서 ja-JP, ko-KR로 전송되는 경우가 있다 -> _(Under bar)를 허용하지 않기 때문이다
        }																				//https://docs.oracle.com/javase/tutorial/i18n/locale/matching.html
        else{	
            return super.determineDefaultLocale(request);
        }
    }
}
