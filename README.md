## 1. 다국어 프로젝트를 마주하며

처음 다국어 프로젝트를 진행했을 때 요구사항은 아래와 같았다.
- 다국어 값에 대한 관리는 DB로 진행 할 수 있도록 할 것.
- 기존 솔루션의 경우 properties 파일을 통해 다국어를 관리했는데 번역이 잘못된 부분이 있을 때마다 다시 릴리즈를 해야 했고
운영중인 서비스에 다국어로 인한 재배포는 부담이 있다고 한다.

- 서비스 중 다국어로 인해 오류가 나는 일이 없도록 할 것.
- 기존 솔루션에 다국어 처리 중 새로운 언어 셋이나 새로이추가 된 다국어 값이 있을 때 문제가 많았기 때문

프로젝트를 진행하면서 다국어에 대한 설계는 i18n 테이블에 화면에서 사용할 다국어 라벨 id 값 및 api에서 사용할 다국어 메시지 값 id 등에 따른
언어 코드마다 실제 label value가 입력되는 형태로 설계 되었다.
ex)
en / documentName / Document name
ko / documentName / 문서 명

성능을 위해 최초 Container에 Bean이 담길 때 모든 다국어 값을 EhCache에 Load하였고
당시 Angular를 사용하면서 각 컴포넌트 별로 필요한 다국어 set을 요청하도록 설계하였기 때문에
요청시 반환되는 다국어 set을 Run-time에 EhCache에 Load 하도록 처리하였고
다국어가 변경 되는 API에서 Transaction 종료 후 Cache를 expire 하는 처리를 적용하여 실시간성도 확보하엿다

또한 RESTful API 형태로 구현하면서 사요자의 token에 사용자 관련 정보를 저장했는데 대표적으로 타임존, 사용자 아이디, 언어셋 등을 암호화 된
claim으로 넣었다. (이런식 구현이 불편한 것이 사용자가 언어셋 변경하면 토큰을 재발해야 하기 때문에 client와 약속 되어있어야 한다는 것,
그리고 만약 admin이 설정한 언어 셋이 강제로 적용되어야 한다는 조건이 있으면 복잡해진다)

따라서 매번 요청 때마다 Interceptor에서 토큰을 파싱하여 현재 사용자의 정보를 Thread local에 담아두고 
각 layer에서 getLanguage()를 통해 사용자의 언어셋을 가져오며 없는경우엔 default language를 반환하는 형태로 유용하게 사용했다.

Thread local을 사용하는 부분이 나름 좋았기 때문에 그 당시 다국어 처리에 대한 설계에 큰 개선요건은 느끼지 못했다
(Token에 담지 않고 Request parameter나 Request Header를 사용하는 것이 좋을 것 같다는 생각은 했다)

프로젝트가 어느정도 마무리 된 후 든 생각이
i18n 테이블에서 사용하는 en, ko와 같은 언어 코드는 프로젝트에서 별도로 지정한 값들이었는데 과연 재사용이 가능할까? 이었다.

## 2. 좀 더 우아하게

표준화 된 방법은 재사용성도 높을 뿐 아니라 유지보수성도 높아진다. 따라서 Framework를 이용하는 방법을 위주로 찾아봤다.
Spring에서는 LocaleResolver라는 인터페이스가 존재하는데 웹 요청으로부터 Locale을 추출하는 기능을 제공하며 대표적 구현체는 아래와 같다

![Alt text](https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTD6o9fPMiUEZ4CyPnYV1QBnGG4kEBxkCzshucv7YR090UlyoUy-A)

**AcceptHeaderLocaleResolver**
- LocaleResolver를 별도로 설정하지 않은 경우 기본적으로 bean으로 등록 된다.
- Request header 중 Accept-Language 헤더에 전달 된 값으로부터 Locale을 반환한다.
- setLocale 메소드를 지원하지 않는다. (UnsupportedOperationException 발생)
- 대부분 요청에서 브라우저가 사용자 브라우저의 Locale 값을 전달하도록 약속되어 잇다. 만약 존재하지 않는 경우 defaultLocale 값을 반환한다

**CookieLocaleResolver**
- LocaleContextResolver 인터페이스의 구현체
- 쿠키를 통해 Locale 정보를 관리한다. cookieName을 지정 할 수 있으며 브라우저의 여러 탭이 같은 Locale을 공유하게 된다.
- defaultLocale 정보를 설정 할 수 있다.
  
 **FixedLocaleResolver**
 - 고정된 Locale 값을 지정한다
 - setLocaleContext 호출 시 UnsupportedOperationException 발생
  
 **SessionLocaleResolver**
 - CookieLocaleResolver와 구현체가 매우 닮았으며 저장소의 차이가 있다.
 - Spring Session과 같이 외부 세션 관리 메커니즘을 사용하는 것과 직접적인 연관이 없단다
 - 단순히 HttpServletRequest로부터 HttpSession 속성을 통해 Locale 값을 계산하고 변경한다


각 Resolver들은 어떻게 사용되며 Locale은 LocaleContextHolder에 저장 될까?
<pre><code>
FrameworkServlet의 processRequest 메소드에서 DispatcherServlet의 buildLocaleContext를 호출하면 아래와 같은 일이 벌어진다
	@Override
	protected LocaleContext buildLocaleContext(final HttpServletRequest request) {
		LocaleResolver lr = this.localeResolver;
		if (lr instanceof LocaleContextResolver) {
			return ((LocaleContextResolver) lr).resolveLocaleContext(request);
		}
		else {
			return () -> (lr != null ? lr.resolveLocale(request) : request.getLocale());
		}
	}
</code>	
</pre>
  
  각 resolver에 의해 반환된 LocaleContext는 FrameworkServlet의 initContextholders 메소드를 통
  <pre><code>
  private void initContextHolders(HttpServletRequest request,
			@Nullable LocaleContext localeContext, @Nullable RequestAttributes requestAttributes) {

		if (localeContext != null) {
			LocaleContextHolder.setLocaleContext(localeContext, this.threadContextInheritable);
		}
		if (requestAttributes != null) {
			RequestContextHolder.setRequestAttributes(requestAttributes, this.threadContextInheritable);
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Bound request context to thread: " + request);
		}
	}
  </code></pre>
LocaleContextHolder의 setLocaleContext메소드를 통해 설정 된다.
  
이러한 Resolver들이 매 요청마다 Locale을 판단 해주면 Application에선 LocaleContextHolder.getLocale() 메소드를 통해
현재 요청의 Locale 정보를 얻을 수 있다. 이는 static 메소드로 지원되며 내부적으로 Thread local을 사용하기 때문에 Thread safe하다.


## 3. LocaleResolver를 활용하여 다국어 처리 기반 제작

내가 원하는 Locale API의 동작은 아래와 같앗다.
- default로 en의 Locale을 
- query string이나 request parameter로 ?lang=ko와 같이 Locale을 전달하면 그 값을 유지할 것.
- 브라우저의 탭마다 다른 Locale 정보를 볼 수 있을 것
- RESTful API 규격에 맞게 Accpet-Language 헤더로 전달 한 Locale 값을 지원 할 것

**(1) default locale**
CookieLocaleResolver에 defalt locale을 설정 할 수 있는 부분이 잇어 요구사항이 충족 되었다.

**(2) query string이나 request parameter로 ?lang=ko와 같이 Locale을 전달하면 그 값을 유지할 것.**
CookieLocaleResolver를 사용하기로 하였고 두 번째 요구 사항을 충족 시키기 위해 LocaleChangeInterceptor를 사용하였다.

<pre><code>
LocaleChangeResolver의 preHandle 메소드는 아래와 같은데
String newLocale = request.getParameter(getParamName());
		if (newLocale != null) {
			if (checkHttpMethod(request.getMethod())) {
				LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
				if (localeResolver == null) {
					throw new IllegalStateException(
							"No LocaleResolver found: not in a DispatcherServlet request?");
				}
				try {
					localeResolver.setLocale(request, response, parseLocaleValue(newLocale));
				}
				...
			}
		}
</code></pre>

전달된 Request parameter로부터 locale 정보를 읽어 LocaleResolver의 setLocale을 호출 해준다
따라서 CookieLocaleResolver의 setLocale -> setLocaleContext 메소드를 타고 아래 구문에서 
LocaleContextHolder의 locale 갑이 설정된다.

<pre><code>
request.setAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME,
				(locale != null ? locale : determineDefaultLocale(request)));
</code></pre>

여기서의 request는 스프링 5 버전 기준 RequestFacade인데 setAttribute 내부를 보면
<pre><code>
   @Override
    public void setAttribute(String name, Object o) {
        if (request == null) {
            throw new IllegalStateException(sm.getString("requestFacade.nullRequest"));
        }
        request.setAttribute(name, o);
    }
</code></pre>
Apache connector의 Request 객체가 구현한 setAttribute를 호출하고 그 내부에서 아래 구무에서 LocaleContextHolder의 Locale 값이 바뀐다.
Object oldValue = attributes.put(name, value);
(내부 구현은 따라 갈 수 가 없었음 ㅜㅜ)

이로써 1번 요구사항은 처리가 완료이다.

**(3) 브라우저의 탭마다 다른 Locale 정보를 볼 수 있을 것**

CookieLocaleResolver를 사용하므로 브라우저의 모든 탭이 같은 Locale을 공유한다.
따라서 요구사항 충족을 위해서는 Accept-Header의 사용이 필수이다. (parameter로 넘기면 쿠키 값이 바뀌므로)

4번과도 연계된 내용인데 일단 Accept-Language 헤더로 값을 실어 보냈지만 제대로 처리되지 않는다.

그 이유를 보니 CookieLocaleResolver의 Locale을 판단하는 로직의 핵심인 아래 메서를 보면
<pre><code>
@Nullable
	protected Locale determineDefaultLocale(HttpServletRequest request) {
		Locale defaultLocale = getDefaultLocale();
		if (defaultLocale == null) {
			defaultLocale = request.getLocale();
		}
		return defaultLocale;
	}
</code></pre>

default locale이 있는 경우엔 우선시 사용된다. request.getLocale의 구현 스페을 보면 Accept-Header 등을 적절히 파악하여
request 객체로부터 Locale을 반환한다. (https://docs.oracle.com/javaee/6/api/javax/servlet/ServletRequest.html#getLocale())

따라서 CookieLocaleResolver를 확장해야 한다
난 아래 Resolver를 구현했다.
<pre><code>
public class AcceptHeaderThenCookieLocaleResolver extends CookieLocaleResolver {
    @Nullable
    protected Locale determineDefaultLocale(HttpServletRequest request) {
        if(!StringUtils.isEmpty(request.getHeader("Accept-Language"))){			
            return new Locale(request.getHeader("Accept-Language"));		
        }																			
        else{	
            return super.determineDefaultLocale(request);
        }
    }
}
</code></pre>
이로써 Accept-Language로 넘어온 Locale을 우선 처리 할 수 있도록 하여 3, 4번 요구사항이 충족 되었다.

## 4. 테스트 그리고 테스트...

테스트 케이스를 작성하며 많은 우여곡절이 있었다.
그 중 하나가 java.util.Locale 관련인데

<pre><code>
System.out.println(Locale.KOREA);              	//ko_KR
System.out.println(Locale.KOREAN);             	//ko
System.out.println(new Locale("ko"));           //ko
System.out.println(new Locale("ko_KR"));       	//ko_kr
System.out.println(new Locale("ko", "KR"));    	//ko_KR
System.out.println();
System.out.println();
System.out.println(Locale.JAPAN);                   	//ja_JP
System.out.println(Locale.JAPANESE);                	//ja
System.out.println(new Locale("ja"));              		//ja
System.out.println(new Locale("ja_JP"));          		//ja_jp
System.out.println(new Locale("ja", "JP"));       		//ja_JP
System.out.println();
System.out.println();
System.out.println(Locale.CHINA);                 //zh_CN
System.out.println(Locale.CHINESE);               //zh
System.out.println(Locale.SIMPLIFIED_CHINESE);    //zh_CN
System.out.println(Locale.TRADITIONAL_CHINESE);   //zh_TW
System.out.println(new Locale("zh"));             //zh
System.out.println(new Locale("zh_CN"));          //zh_cn
System.out.println(new Locale("zh_TW"));          //zh_tw
System.out.println(new Locale("zh", "CN"));    	  //zn_CN
System.out.println(new Locale("zh", "TW"));    	  //zn_TW
</code></pre>

하나같이 제 각각이다....
게다가 headers.setAcceptLanguageAsLocales(Arrays.asList(Locale.JAPAN));    //ja-JP로 헤더 전송 됨
이렇게 헤더를 보내면 분명 Locale.JAPAN은 ja_JP임에도 불구하고 ja-JP로 Accept-Language가 날아간다.
물론 header.addHeader('Accpet-Language', 'ja_JP')로 보내면 잘 날아간다.
하지만 HttpServletRequest의 getLocale에선 아무런 값이 찍히지 앟았다.

물론 LocaleContextHolder를 이용하면 되지만 난 통일성이 있어야 다른 개발자들이 사용할 때 버그로 인해 시간을 낭비하는 일이 없다고 생각한다.
Controller 단에서 request 객체를 이용하여 request.getLocale()을 쓰던, Locale 객체를 직접 인자로 받던 LocaleContextHolder를 사용하던 말이다.
<pre><code>
@GetMapping(value = "/locales")
public LocaleMap getLocale(HttpServletRequest request, Locale locale) {
    log.info("check from request :" + request.getLocale());
    log.info("check from controller argument :" + locale);
    log.info("check from LocaleContextHolder:" + LocaleContextHolder.getLocale());

    return new LocaleMap(request.getLocale(), locale, LocaleContextHolder.getLocale(), AppLocaleContextHolder.getLocale());
}
</code></pre>

이유를 찾던 중 http request header의 value에 _(under score)가 보안상의 이유로 Web server 같은 곳에서 차단되는 사실을 알아냈고
스펙 상 대부분 '_'의 사용을 지양하는 것을 알아냈다.

사실 setAcceptLanguageAsLocales 메소드 내에선 locale.toLanguageTag라는 메소드를 통해 Locale 값을 바꾸는데
여기서 Locale.JAPAN의 ja_JP 값이 ja-JP로 날아가는 것이었다.

그에 따라서 AcceptHeaderThenCookieLocaleResolver의 구현을 아래처럼 바꾸었다.
<pre><code>
 @Nullable
    protected Locale determineDefaultLocale(HttpServletRequest request) {
        if(!StringUtils.isEmpty(request.getHeader("Accept-Language"))){				    //headers.setAcceptLanguageAsLocales(Arrays.asList(Locale.JAPAN)) ja_JP 이지만 setAcceptLanguageAsLocales 내부에서 ja-JP로 바꿈 (toLanguageTag 메소드 호출 함)
            return Locale.forLanguageTag(request.getHeader("Accept-Language"));		//Accept header에서 ja-JP, ko-KR로 전송되는 경우가 있다 -> _(Under bar)를 허용하지 않기 때문이다
        }																				                                  //https://docs.oracle.com/javase/tutorial/i18n/locale/matching.html
        else{	
            return super.determineDefaultLocale(request);
        }
    }
</code></pre>

또한 테스트 중 한 가지 사실을 더 알아냈다. 단순히 궁금증에 의한 테스트였는데
<pre><code>
HttpHeaders headers = new HttpHeaders();
headers.add("Accept-Language", Locale.JAPAN.toLanguageTag());        //request header 전송 규약은 language tag로 보냄. _(Under bar)를 허용하지 않기 때문
		
ResponseEntity<LocaleMap> response
    = restTemplate.exchange("/locales?lang=ko_KR", HttpMethod.GET, new HttpEntity<>(headers), LocaleMap.class);
		
assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
assertThat(response.getBody().getLocaleContextHolderLocale()).isEqualTo(Locale.KOREA);
assertThat(response.getBody().getRequestGetLocale()).isEqualTo(Locale.JAPAN);
</code></pre>
        
위와 같은 테스트에서 보면 참 결과가 난감하다.
request.getLocale로 얻은 값은 Accept-Language로 보낸 값은 ja_JP이고 
?lang=ko_KR로 보낸 값은 query parameter로 보낸 ko_KR 값이다

원인을 파악해보면 LocaleChangeInterceptor의 호출 순서에 잇다.
분명 처음엔 AcceptHeaderThenCookieLocaleResolver를 통해 Accept-Language로 보낸 값 ja_JP를 
buildLocaleContext의 로직을 타면서 LocaleContextHolder에 Locale을 저장 할 것이다
또한 request.getLocale은 request 객체로부터 값을 얻어오므로 ja_JP를 반환 할 것이다.

그런데 LocaleChangeInterceptor의 preHandle는 언제 호출되는가?
위 과정이 FrameworkServlet -> DispatcherServlet을 통해 진행 된 후
doService -> doDisptch -> mappedHandler.applyPreHandle 호출 순으로 처리 될 때
바로 applyPreHandle 메소드가 호출 될 때 모든 Interceptor의 preHandle이 호출된다.

즉 request의 parameter로 전달 된 값이 최종적으로 LocaleContextHolder에 설정 된다.

통일성 유지를 위해 아래 인터셉터를 구현했다.
<pre><code>
public class ApplicationLocaleChangeInterceptor extends LocaleChangeInterceptor {
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws ServletException {
      if(!StringUtils.isEmpty(request.getHeader("Accept-Language"))){
          return true;
      }
      else{
          return super.preHandle(request, response, handler);
      }
  }
}
</code></pre>
Accept-Language 헤더가 있다면 아무렁 일을 하지 않고 없는 경우에만
request parameter로부터 Locale값을 추출하여 LocaleContextHolder에 설정하도록 했다.

사실 위 구현에서 Accept-Language가 존재 하는지 비교가 아니라 LocaleContextHolder에 값이 있는지 비교해도 되지만
다른 개발자들이 호출 순서를 파악해야 이해할 수 있다고 생각하여 명시적으로 Accept-Languge 헤더를 비교했다.

## 5. Trade-off 상황을 

자 이제 마무리인가? 아니다 아래 테스트가 깨진다
<pre><code>
HttpHeaders headers = new HttpHeaders();
headers.add("Accept-Language", "ja_JP");
		
ResponseEntity<LocaleMap> response
    = restTemplate.exchange("/locales?lang=ko_KR", HttpMethod.GET, new HttpEntity<>(headers), LocaleMap.class);
		
assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
assertThat(response.getBody().getLocaleContextHolderLocale()).isEqualTo(Locale.KOREA);
assertThat(response.getBody().getRequestGetLocale()).isEqualTo(new Locale(""));        //request.getLocale 에선 아무런 결과가 없다
</code></pre>

좀 전 requestHeader에 _ 값을 사용했을 때 문제이다.
ja_JP가 올바른 값이 아니므로 ko_KR을 받아들어야 할 것 같지만 그렇지 않다.

사실 스프링이 동작하는 매커니즘이 1선후보, 2선후보, 3선후보... 처럼 값이 없거나 제대로 들어오지 않은 경우에 알아서 
차선책을 셋팅 해주는데 그런 동작을 여기서도 기대해야 하나 싶다.

Accept-Language가 우선순위를 가지는 것은 명확한 결과 일 것이다. 하지만 Accept-Language로 보낸 값이 옳지 않다면?
parameter로 넘어온 값이 있느냐를 조사해서 처리하기로 한다. 관련 내용은 문서화를 위해 Javadoc으로 남긴다

<pre><code>
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
</code></pre>

## 6. 마지막 예외 상황
항상 궁금증은...

적용된 우선순위는 아래와 같다.

Accept-Language에 설정 된 값
없거나 옳지 않다면 Request Paramter에 설정 된 값

**Request Parameter에 설정 된 값이 없다면?**

당연히 논리적으론 system의 default locale을 택해야 할 것 같지만
테스트를 해보면 아무런 값이 찍히지 않는다.
<pre><code>
@Test
public void testForChangeAcceptLanguageAndNonParam() {
  HttpHeaders headers = new HttpHeaders();
  headers.add("Accept-Language", "ja_JP");

  ResponseEntity<LocaleMap> response
      = restTemplate.exchange("/locales", HttpMethod.GET, new HttpEntity<>(headers), LocaleMap.class);

  assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  assertThat(response.getBody().getLocaleContextHolderLocale()).isEqualTo(Locale.ENGLISH);		//System의 default locale
  assertThat(response.getBody().getRequestGetLocale()).isEqualTo(new Locale(""));        //잘못 된 형식의 header value이므로 request.getLocale 에선 아무런 결과가 없다
}
</code></pre>

여기서 AcceptHeaderThenCookieLocaleResolver의 구현을 바꿀까?
Locale.forLanguageTag(request.getHeader("Accept-Language"));을 통해 생성된 Locale이 옳지 않으면 default locale로 설정하도록?

그렇다면 Interceptor에서 구현한 내용은 무의미해진다. preHandle 실행 이전 언제나 옳은 값으로 Locale이 설정되어
Request Parameter로 전달된 값이 빛을 발할 기회는 항상 없다. 그렇다고 우선순위를 Request Parameter로 높이기는 싫다.

따라서 LocaleContextHolder를 확장한 구현체를 사용하기로 한다.

## 7. 커스터마이징 한 LocaleContextHolder

일반적으로 시스템에서 지원하는 다국어는 제한되어있다.
ko, ja, en을 지원하는데 만약 외부 api 사용자가 중국어를 요청했다고 하자.
지금까지의 구현으로는 전혀 대비가 없다. 즉 지원하지 않는 다국어에 대해서 default locale 처리가 없다.

6번에서 마주한 문제와 지원하지 않늠 다국어의 방어코드 처리를 위해 LocaleContextHolder를 데코레이터로 사용하는 구현체를 구현했다.

**(1) properties 준비**
시스템에서 지원하는 Locale을 정의하기위한 properties를 준비한다.
ConfigurationProperteis 스펙을 사용한다.

<pre><code>
application.properties

app.languages[0]=en
app.countries[0]=
app.languages[1]=ko
app.countries[1]=KR
app.languages[2]=ja
app.countries[2]=JP
app.languages[3]=zn
app.countries[3]=CN

@Component
@ConfigurationProperties(prefix = "app")
@Setter
@Getter
public class ApplicationLocaleProperties {
    public static final Locale defaultLocale = Locale.ENGLISH;    //Default locale

    private List<String> languages;

    private List<String> countries;

    public Locale getLocale(int index) {
        return new Locale(languages.get(index), countries.get(index));
    }
}
</code></pre>

**(2) 커스터마이징**

LocaleContextHolder가 static 메소드 기반이므로 강제저으로 아래 클랫를 사용하게 할 수는 없다.
(Bean처럼 등록하는 것이면 커스터마이징 구현체를 등록하면 되지만..)

따라서 상속하는 형태가 아니라 데코레이터로 사용하는 형태로 정의했다.
(상속 할 경우 LocaleContextHolder의 구현 내용이 IDE의 인텔리센스 기능에서 노출되어 잘못 된 사용을 야기할 것 같아서이다)

setLocale을 개발자가 비즈니스 로직을 처리하면서 호출해야 할 경우가 있나 했지만 혹시나 해서 구현은 해두었다. (단, 시스템에서 지원하는 Locale 만)
getLocale의 경우 요청받은 Locale이 시스템에서 지원하는 Locale인지 확인하며 그렇지 않다면 System의 Default locale을 반환하도록 했다.

<pre><code>
@Component
@Slf4j
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

    public static void setLocale(Locale locale) {
        if (isPossibleLocale(locale)) {
            log.info("Change system locale " + locale);
            LocaleContextHolder.setLocale(locale);
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
        return acceptableLocales.contains(currentLocale); //Locale 객체거 equals, hashCode 메소드를 잘 구현하여 가능
    }
}
</code></pre>

모든 테스트에서 LocaleContextHolder를 통해 비교했던 것을 ApplicationLocaleContextHolder로 변경한 후 
다시 테스트를 진행한다.

좀 전 깨졌던 테스트를 포함하여 모든 테스트가 정상 동작한다.
<pre><code>
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
</code></pre>


# 
# 

* * *

### 회고 

> LocaleResolver를 뜯어보면서 관련 구현체들을 보고 직접 구현도 해보았다.
> 이렇게 많은 내용을 다룰 줄 몰랏는데 사실 오늘 다뤘던 문제는 AcceptHeaderLocaleResolver를 사용하면 대부분이 처리 될 수있다.
> 
> Accept-Langauge 헤더로부터 값을 읽어 LocaleContextHolder에 설정 하고 지원히자 않는 Locale인 경우
> Default locale로 설정하는 Interceptor만 있으면 더욱 편리하게 다국어 환경을 처리할 수 있을 것이다.
> 
> 하지만 RESTful API만 지원하는 형태가아니라 좀 더 범용적인 환경에서 사용할 수 있는 구현을 생각하고 싶었다.
> 그렇기에 CookieLocaleResolver의 구현부가 괜찮게 느껴졌고 둘을 합칠 수 없을까 하는 생각에 이곳저곳 확장을 하게 된 것 같다.
> 게다가 우선순위를 저용하는 부분을 처리하면서 아무런 값이 없을 때 default locale을 처리하는 부분까지 다양한 케스르를 다루어 
> 다소 내용도 복잡해진 것 같다.
>
> 두어가지 느낀 것이 있는데 첫번째 느낀것은 테스트케이스를 작성할 때 다양하게 작성하는 것이 중요하다는 것이다.
> 다른 비즈니스 로직을 오늘처럼 테스트 케이스를 다양하게 작성하여 테스트 하면 훨씬 여러 방향에서 생각하게 되고
> 설계상 놓치는 허점을 쉽게 발견하여 생상성도 높아지지 않을까 싶다
> 
> 두번째는 프레임워크의 구현을 따라가면서 구현부를 보고 왜 그렇게 동작하는지 직접 눈으로 확인하는게 제일 좋은 것 같다는 부분이다
> 구글링 하면 물론 쉽게 찾고 복붙하면 금방이다. 하지만 왜 돌아가는지 모르고 일단 되네? 그럼 넘어가자 할 수 있을 것이고
> 오늘 테스트 처럼 다양한 방면에 대해서 문제가 발생하면 왜 그런지 제대로 모르니... 원인도 제대로 설명 못할 뿐더러 해결도 못 할 것이다
> 
> Request header에 옳지 않은 형식으로 발생하는 문제를 마주했을 때를 가정한다면
> 그냥 Front-end에 헤더를 제대로 안보낸거 아니에요? 그럼 그냥 query string으로 보내세요 등의 대응이나 했을 것이다
> 혹여 운 좋게 원인이라도 파악했다고 하더라도 어떤식으로 구현 변경을 할 지 똑부러지게 이야기도 못 꺼냈을 것이다. 개발자로서 끔찍한 일이 아닐까

Github 
https://github.com/dlxotn216/LocaleResolver


  
  
  




