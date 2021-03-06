---
layout: section
title: Dispatcher Configuration
---

### Web Layer
The Web Layer can be configured by Spring MVC controllers served by a dispatcher servlet 
(configured in `web.xml`). Configuration includes content negotiation, serialization config, 
and Web controller injections.

#### Dispatcher Servlet

This describes an example configuration via Spring. There are lots of variants and 
alternatives which may end in the same result. This example splits Spring configuration 
files into two main files:

* `/WEB-INF/spring/dispatcher-servlet.xml` the dispatcher's config file
* `/WEB-INF/spring/api_mvc.xml` the Web controllers configuration
* `/WEB-INF/spring/application-context.xml` SPI configuration

However, everything starts by configuring Spring's `DispatcherServlet` within the `web.xml` 
and relate it to some context path like so:

{::options parse_block_html="true" /}
{: .n52-example-block}
<div>
<div class="btn n52-example-caption n52-example-toggler active" type="button" data-toggle="button">
Example configuration of the dispatcher servlet
</div>
```xml
<servlet>
  <servlet-name>api-dispatcher</servlet-name>
  <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
  <init-param>
     <param-name>contextConfigLocation</param-name>
     <param-value>/WEB-INF/spring/dispatcher-servlet.xml</param-value>
  </init-param>
  <load-on-startup>1</load-on-startup>
</servlet>

<servlet-mapping>
  <servlet-name>api-dispatcher</servlet-name>
  <url-pattern>/api/*</url-pattern>
</servlet-mapping>
```
</div>

#### Content Negotiation
Content Negotiation is configured by `org.n52.series.dao.spring.DefaultConfig` class 
and read by `<ctx:component-scan base-package="org.n52.series.dao.spring" />`. If you 
need to change config, replace `<ctx:component-scan ... />` with `<ctx:annotation-config />`
and add the following which you can modify depending on your needs.

{::options parse_block_html="true" /}
{: .n52-example-block}
<div>
<div class="btn n52-example-caption n52-example-toggler active" type="button" data-toggle="button">
Content negotiation configuration
</div>
```xml
<mvc:annotation-driven />
<ctx:annotation-config />

<bean id="objectMapper" class="com.fasterxml.jackson.databind.ObjectMapper">
    <property name="serializationInclusion" value="NON_NULL" />
</bean>

<bean id="jsonViewResolver" class="org.springframework.web.servlet.view.json.MappingJackson2JsonView">
    <property name="extractValueFromSingleKeyModel" value="true" />
    <property name="objectMapper" ref="objectMapper" />
</bean>

<mvc:annotation-driven content-negotiation-manager="contentNegotiationManager" />

<bean id="contentNegotiationManager" class="org.springframework.web.accept.ContentNegotiationManagerFactoryBean">
    <property name="defaultContentType" value="application/json" />
</bean>

<bean class="org.springframework.web.servlet.view.ContentNegotiatingViewResolver">
    <property name="defaultViews">
        <util:list>
            <ref bean="jsonViewResolver" />
        </util:list>
    </property>
</bean>
```
</div>

#### Web Controller injections
A Web controller serves an endpoint and behaves like described in the [Web API]({{site.baseurl}}/api.html). It 
performs [I/O operations]({{site.baseurl}}/io.html) (graph rendering, generalization, etc.) if neccessary.


{:.n52-callout .n52-callout-info}
A Web controller delegates data request to the actual SPI implementation so it has to be 
referenced here. SPI implementors have to use these references to make sure the right
backend service is called.

{::options parse_block_html="true" /}
{: .n52-example-block}
<div>
<div class="btn n52-example-caption n52-example-toggler active" type="button" data-toggle="button">
Example how to configure Web MVC controllers
</div>
```xml
<mvc:annotation-driven />
<ctx:annotation-config />

<!--
    This bean description file injects the Web binding layer. SPI implementation 
    beans have to match the ref-ids associated below.
-->

<bean class="org.n52.web.ctrl.ResourcesController">
    <property name="metadataService" ref="metadataService" />
</bean>

<bean class="org.n52.web.ctrl.SearchController">
    <property name="searchService" ref="searchService"/>
</bean>

<!-- a parent controller configuration -->
<bean class="org.n52.web.ctrl.ParameterController" id="parameterController" abstract="true">
    <property name="metadataExtensions">
        <list>
            <bean class="org.n52.io.response.extension.LicenseExtension" />
        </list>
    </property>
</bean>

<bean class="org.n52.web.ctrl.OfferingsParameterController" parent="parameterController">
    <property name="parameterService">
        <bean class="org.n52.web.ctrl.ParameterBackwardsCompatibilityAdapter">
            <constructor-arg index="0" ref="offeringParameterService" />
        </bean>
    </property>
</bean>

<bean class="org.n52.web.ctrl.ServicesParameterController" parent="parameterController">
    <property name="parameterService">
        <bean class="org.n52.web.ctrl.ParameterBackwardsCompatibilityAdapter">
            <constructor-arg index="0" ref="serviceParameterService" />
        </bean>
    </property>
</bean>


<bean class="org.n52.web.ctrl.DatasetController" parent="parameterController">
    <property name="parameterService" ref="datasetService" />
    <property name="metadataExtensions">
        <list merge="true">
            <bean class="org.n52.io.extension.RenderingHintsExtension" />
            <bean class="org.n52.io.extension.StatusIntervalsExtension" />
            <bean class="org.n52.io.extension.resulttime.ResultTimeExtension">
                <property name="service" ref="resultTimeService" />
            </bean>
            <!-- Using DatabaseMetadataExtension requires some preparation work. -->
            <!--<bean class="org.n52.io.extension.metadata.DatabaseMetadataExtension" />-->
        </list>
    </property>
</bean>

<bean class="org.n52.web.ctrl.DataController">
    <property name="dataService" ref="datasetService" />
    <property name="datasetService" ref="datasetService" />
    <property name="preRenderingTask" ref="preRenderingJob" />
    <property name="requestIntervalRestriction" value="${request.interval.restriction}" />
</bean>
```
</div>

{::options parse_block_html="true" /}
{:.n52-callout .n52-callout-info}
<div>
Known application properties are 
Some things to note:

* `${external.url}` and `${request.interval.restriction}` are property placeholders defined in 
an extra application properties file
* `org.n52.web.ctrl.ParameterBackwardsCompatibilityAdapter` is a backwards compatibility wrapper
* `metadataExtensions` list contains extensions which adds further metadata to `/<endpoint>/extras`

</div>

#### General Properties
Configurable properties are 

* `requestIntervalRestriction`: sets the maximum time period a clients can query data for, e.g. `P380D`
* `externalUrl`: sets the external URL under which the API can be accessed by clients, e.g. (`https://example.com/my-api/`)


