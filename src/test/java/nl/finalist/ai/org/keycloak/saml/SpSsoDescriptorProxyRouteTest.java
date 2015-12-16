package nl.finalist.ai.org.keycloak.saml;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Test;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletUnitClient;

public class SpSsoDescriptorProxyRouteTest extends ServletCamelRouterTestSupport {
	
	final static String ACCESS_TOKEN = "eyJhbGciOiJSUzI1NiJ9.eyJqdGkiOiJjMjY0M2I4ZC02NTk0LTQwMjItOTBjMy05ZjVjNGViMjI4NTEiLCJleHAiOjE0NDkwNTQ0MTMsIm5iZiI6MCwiaWF0IjoxNDQ5MDU0MzUzLCJpc3MiOiJodHRwOi8va2V5Y2xvYWsuYWkuZmluYWxpc3QubGNsL2F1dGgvcmVhbG1zL21hc3RlciIsImF1ZCI6Im1hc3RlciIsInN1YiI6IjlmYTIwZGE1LWRiNDItNDFmZi1hZjk3LTkwMTkyNjFkN2VhMyIsInR5cCI6IkJlYXJlciIsImF6cCI6Im1hc3RlciIsInNlc3Npb25fc3RhdGUiOiIwODBmMTE0NS04NGZhLTQyYzktYmIxOC1lMDY1MGQ4YTg4NTMiLCJjbGllbnRfc2Vzc2lvbiI6ImJkYjgyYzNmLTU3ODEtNGY0Ni04MjBhLWMwN2E2MDZmNjc3ZCIsImFsbG93ZWQtb3JpZ2lucyI6W10sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJhZG1pbiIsImNyZWF0ZS1yZWFsbSJdfSwicmVzb3VyY2VfYWNjZXNzIjp7Im1hc3Rlci1yZWFsbSI6eyJyb2xlcyI6WyJtYW5hZ2UtY2xpZW50cyIsIm1hbmFnZS1ldmVudHMiLCJ2aWV3LXJlYWxtIiwidmlldy1ldmVudHMiLCJtYW5hZ2UtaWRlbnRpdHktcHJvdmlkZXJzIiwidmlldy1pZGVudGl0eS1wcm92aWRlcnMiLCJ2aWV3LXVzZXJzIiwiY3JlYXRlLWNsaWVudCIsInZpZXctY2xpZW50cyIsImltcGVyc29uYXRpb24iLCJtYW5hZ2UtdXNlcnMiLCJtYW5hZ2UtcmVhbG0iXX0sImFjY291bnQiOnsicm9sZXMiOlsidmlldy1wcm9maWxlIiwibWFuYWdlLWFjY291bnQiXX19LCJuYW1lIjoiIiwicHJlZmVycmVkX3VzZXJuYW1lIjoiYWRtaW4ifQ.GGmPWfrzTVt4DSEFbZimMshjO5rd80jyi8-FqpAM0nGMkHztSeRx0gAaGEj16wnlHnB-c7VlyQv5ClycEu2WXD7KjOUu2rtV6mUCanlbBN60tt3vpgyKGJgN-qpJe5oDPmWlQvYMaVMHJ26aw5-NOGkl-f2YW-7Vba9-fsBkke8-yVxhvVj7r_MrF484cVodMke3AYMrnbhJaYSEO6WPAxAQJIK1VbbSv9nOnX3iR92_jcjm3gQuZ0FSoCxjbs7XU9qH0Op-k7PVLPJH-ao6AHHM4dDqS5DD8fCzF5UnXWgYc-hfXbBADurA0BrK49RALPYoP8VRb11jiN5RPuIJL";
	final static String MOCK_URI_TOKEN = "mock:token";
	final static String MOCK_URI_API = "mock:api";
	MockEndpoint mockToken;
	MockEndpoint mockApi;
	
	@Override
	protected void doPostSetup() throws Exception {
		super.doPostSetup();
		mockToken = getMockEndpoint(MOCK_URI_TOKEN);
		mockApi = getMockEndpoint(MOCK_URI_API);
	}
	
	@Test
	public void testGetAccessToken() throws Exception {
		
		setSystemProperties();
		interceptSendToTokenEndpoint();
		
		Exchange request = createExchangeWithBody(null);
		request.setProperty("realm", "master");
		request.setProperty("username", "admin");
		request.setProperty("password", "admin123!");
		
		Exchange response = template.send("direct:getAccessToken", request);
		
		assertEquals(ACCESS_TOKEN,response.getIn().getBody());		
		
		mockToken.expectedMessageCount(1);
		Message tokenRequest = mockToken.getExchanges().get(0).getIn();
		
		assertEquals("username=admin&password=admin123!&grant_type=password",tokenRequest.getBody());
		assertEquals("application/x-www-form-urlencoded",tokenRequest.getHeader(Exchange.CONTENT_TYPE));
		assertMockEndpointsSatisfied();
	}
	
	@Test
	public void testSetAuthorizationHeader() {

		Exchange request = createExchangeWithBody(null);
		request.setProperty("realm", "master");
		Exchange response = template.send("direct:setAuthorizationHeader", request);
		String authorizationHeader = ((Exchange) response).getIn().getHeader("Authorization", String.class);
		assertEquals("Encoded value of: 'master:password'","Basic bWFzdGVyOnBhc3N3b3Jk",authorizationHeader);
	}
	
	@Test
	public void testGetSpSsoDescriptoer() throws Exception {
		
		setSystemProperties();
		interceptSendToTokenEndpoint();
		interceptSendToApiEndpoint();
		
        WebRequest req = new GetMethodWebRequest(CONTEXT_URL + "/realms/master/identity-provider/SimSamIdp");
        ServletUnitClient client = newClient();
        WebResponse webResponse = client.getResponse(req);
		
		assertEquals("application/xml",webResponse.getContentType());
		assertEquals("OK",webResponse.getResponseMessage());
		assertEquals("xml",webResponse.getText());
	}

	private void interceptSendToApiEndpoint() throws Exception {
		context.getRouteDefinition("spssodescriptor").adviceWith(context, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint("http://localhost/auth/admin/realms/master/identity-provider/instances/SimSamIdp/export")
				.skipSendToOriginalEndpoint()
				.to(MOCK_URI_API)
				.setBody(constant("xml"))
				.setHeader(Exchange.CONTENT_TYPE, constant("application/xml"));
			}
		});
	}

	private void interceptSendToTokenEndpoint() throws Exception {
		context.getRouteDefinition("getAccessToken").adviceWith(context, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint("http://localhost/auth/realms/master/protocol/openid-connect/token")				
				.skipSendToOriginalEndpoint()
				.to(MOCK_URI_TOKEN)
				.setBody(constant("{\"access_token\":\""+ ACCESS_TOKEN +"\",\"token_type\":\"bearer\",\"id_token\":\"eyJhbGciOiJSUzI1NiJ9.eyJqdGkiOiI4MDI3Zjg3Ny0yNzk2LTQ1YTgtOTAwOS00M2MyYjY1OWNkN2UiLCJleHAiOjE0NDkwNTQ0MTMsIm5iZiI6MCwiaWF0IjoxNDQ5MDU0MzUzLCJpc3MiOiJodHRwOi8va2V5Y2xvYWsuYWkuZmluYWxpc3QubGNsL2F1dGgvcmVhbG1zL21hc3RlciIsImF1ZCI6Im1hc3RlciIsInN1YiI6IjlmYTIwZGE1LWRiNDItNDFmZi1hZjk3LTkwMTkyNjFkN2VhMyIsInR5cCI6IklEIiwiYXpwIjoibWFzdGVyIiwic2Vzc2lvbl9zdGF0ZSI6IjA4MGYxMTQ1LTg0ZmEtNDJjOS1iYjE4LWUwNjUwZDhhODg1MyIsIm5hbWUiOiIiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJhZG1pbiJ9.E6GCphj6OXWd5wSjA82Muc0ne6yDkeIKRjAiQEqokgqA2GTIvQvg1KZ8XGTrp3FHb6wHrXhDJW7fTmW1ErOylD3k1YUP80UK0sSvtHDYSIeRJ7wA7wR_4C5PslXj7MjVpaEWo122GyXnY5vkmBoMZWUYV01oST2FFi7J4ZT6TtB6vVwNegf_yyJl3mRps-sacoznOsctjogD2k8rQY-yWPVk2VJiFCC18d-M5nahISFNFWVElobJQGqGe36eSmv1ySXoluVIuJbsS_DM8Xn3_FPAX8BDxYrvHkzbWsJtmoFfpSYk129BqCIbZZy_BwNhD0q9rvLe_RiGv9vaExblIQ\",\"not-before-policy\":0,\"session-state\":\"080f1145-84fa-42c9-bb18-e0650d8a8853\"}"))
				.setHeader(Exchange.CONTENT_TYPE,constant("application/json"));				
			}
		});
	}
	
	private void setSystemProperties() {
		System.setProperty("spssodescriptor.username", "admin");
		System.setProperty("spssodescriptor.password", "admin123!");
		System.setProperty("spssodescriptor.baseUrl", "http://localhost");
	}
	
	@Override
	protected RouteBuilder createRouteBuilder() {
		return new SpSsoDescriptorProxyRoute();
	}
}
