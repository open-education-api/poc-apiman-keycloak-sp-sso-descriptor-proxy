package nl.finalist.ai.org.keycloak.saml;

import java.io.UnsupportedEncodingException;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http.SSLContextParametersSecureProtocolSocketFactory;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

public class SpSsoDescriptorProxyRoute extends RouteBuilder {
	
	@Override
	public void configure() throws UnsupportedEncodingException {
		
		//Enable HTTPS for outgoing calls
		SSLContextParameters scp = new SSLContextParameters();		 
		ProtocolSocketFactory factory = new SSLContextParametersSecureProtocolSocketFactory(scp);		 
		Protocol.registerProtocol("https", new Protocol("https",factory,443));		
		
		restConfiguration().component("servlet");
		
		rest("/realms/{realm}/identity-provider/{idp}").get().route()
			.routeId("spssodescriptor")
			.setProperty("realm").header("realm")
			.setProperty("idp").header("idp")
			.to("direct:getAccessToken")
			.removeHeaders("*")
			.setHeader("Authorization").simple("Bearer ${body}")
			.setHeader(Exchange.HTTP_METHOD).constant("GET")
			.recipientList().simple("${sys.spssodescriptor.baseUrl}/auth/admin/realms/${property.realm}/identity-provider/instances/${property.idp}/export")
			//Don't expose any internal information
			.removeHeaders("*", Exchange.CONTENT_TYPE);
		
		from("direct:getAccessToken")
			.routeId("getAccessToken")
			.removeHeaders("*")
			.to("direct:setAuthorizationHeader")
			.setHeader(Exchange.CONTENT_TYPE, constant("application/x-www-form-urlencoded"))
			.setBody().simple("username=${sys.spssodescriptor.username}&password=${sys.spssodescriptor.password}&grant_type=password")			
			.recipientList().simple("${sys.spssodescriptor.baseUrl}/auth/realms/${property.realm}/protocol/openid-connect/token")
			.setBody().jsonpath("access_token");
		
		from("direct:setAuthorizationHeader")
			.routeId("setAuthorizationHeader")
			.setBody().simple("${property.realm}:password")
			.marshal().base64(76,"",false) //Remove the line separator. Leave other parameters at default
			.setHeader("Authorization").simple("Basic ${body}");
	}	
}