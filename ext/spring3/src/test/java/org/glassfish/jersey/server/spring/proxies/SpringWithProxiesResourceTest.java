/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2016 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.jersey.server.spring.proxies;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import static java.util.Collections.singletonList;

public class SpringWithProxiesResourceTest extends JerseyTest {

    @Override
    protected Application configure() {
        ApplicationContext context = new AnnotationConfigApplicationContext(SpringWithProxyConfiguration.class);
        return new ResourceConfig()
                .register(RequestContextFilter.class)
                .register(LoggingFeature.class)
                .register(context.getBean(JaxRsResource.class))
                .property("contextConfig", context);
    }

    @Test
    public void shouldUseDefaultComponent() {
        final String result = target("spring-resource").request().get(String.class);
        Assert.assertEquals("spring-resource", result);
    }

    @Configuration
    @EnableCaching
    public static class SpringWithProxyConfiguration {
        @Bean
        public JaxRsResource jaxRsResource() {
            return new SpringWithProxiesResourceImpl();
        }

        @Bean
        public CacheManager cacheManager() {
            SimpleCacheManager cm = new SimpleCacheManager();
            cm.setCaches(singletonList(new ConcurrentMapCache("default")));
            return cm;
        }
    }

    @Service
    public static class SpringWithProxiesResourceImpl implements JaxRsResource {
        @Context
        private UriInfo uriInfo;

        @Cacheable("default")
        @Override
        public String doSomething() {
            return uriInfo == null ? "fail" : uriInfo.getPath();
        }

    }

    @Path("spring-resource")
    public interface JaxRsResource {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        String doSomething();
    }
}
