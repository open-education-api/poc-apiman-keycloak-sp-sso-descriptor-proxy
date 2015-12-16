SAML - Service Provider SSO Descriptor Proxy
========================

This project contains a proxy for the SAML Service Provider SSO Descriptor of JBoss Keycloak. This proxy is needed to overcome the current limitations of JBoss Keycloak when it is configured as an Identity Broker. The current limitations are:  

* The SAML SP SSO Descriptor is secured by a JBoss Keycloak realm, but should be publicly available. [A JIRA ticket has been logged for this limitation.](https://issues.jboss.org/browse/KEYCLOAK-2189)

When JBoss Keycloak is configured as an Identity Broker for another Identity Provider both systems need to publish their SAML metadata on an URL which does not require authentication. JBoss Keycloak has the role of a service provider and should publish its SAML Service Provider SSO Descriptor. The Identity Provider should publish its Identity Provider SSO Descriptor.

Jboss Keycloak can be configured as an Identity Broker by defining one or more Identity Provider(s) within a realm.
See: [JBoss Keycloak - Identity Broker](http://keycloak.github.io/docs/userguide/keycloak-server/html/identity-broker.html)

For each configured Identity Broker within a realm JBoss Keycloak publishes a SAML Service Provider SSO Descriptor.

This proxy makes these descriptors available on a URL which does not require authentication.

# Installation

* Deploy the WAR on an application server (it has been tested with JBoss Wildfly 8.2). Can be the same server as JBoss Keycloak is running on or a different server.
* Create a user in Keycloak with the **view-identity-providers** role for the realm we want to query
* Configure the following system properties:
	* spssodescriptor.username : The username of a JBoss Keycloak user
	* spssodescriptor.password : The password of the JBoss Keycloak user
	* spssodescriptor.baseUrl : The url of the application server running JBoss Keycloak, but without the context. (e.g.: http://apiman.openonderwijsapi.nl:7443). Can be localhost when running on the same server. Make sure to use https when running from a different server.

# Run
To retrieve the SP SSO Descriptor for a configured IdP in a realm. Execute the following REST call:

    http(s)://<host>:<port>/spssodescriptor/realms/{realm}/identity-provider/{identity-provider}

where:

* realm : Is replaced by the name of the JBoss Keycloak realm containing the Identity Provider
* identity-provider : The name of the Identity Provider within the realm


# TODO
When using the SP SSO Descriptor we have come across two issues which should be further investigated:  

* The descriptor is missing the correct XML namespace. Should be fixed from v1.6.0 onwards.
* The descriptor is defines the services with HTTP-Redirect binding, but it looks like a HTTP-Post binding is a better match:
  
```<EntityDescriptor entityID="http://127.0.0.1:8080/auth/realms/apiman"><SPSSODescriptor AuthnRequestsSigned="false" protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol urn:oasis:names:tc:SAML:1.1:protocol http://schemas.xmlsoap.org/ws/2003/07/secext"><NameIDFormat>urn:oasis:names:tc:SAML:2.0:nameid-format:persistent
        </NameIDFormat><SingleLogoutService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="http://127.0.0.1:8080/auth/realms/apiman/broker/SurfConext/endpoint"/><AssertionConsumerService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="http://127.0.0.1:8080/auth/realms/apiman/broker/SurfConext/endpoint" index="1" isDefault="true"/></SPSSODescriptor></EntityDescriptor>```      


[The source code of the descriptor is available at Github.](https://github.com/keycloak/keycloak/commits/master/broker/saml/src/main/java/org/keycloak/broker/saml/SAMLIdentityProvider.java)