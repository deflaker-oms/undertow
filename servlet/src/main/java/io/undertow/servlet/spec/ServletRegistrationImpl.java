/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.undertow.servlet.spec;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.HttpMethodConstraintElement;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletSecurityElement;

import io.undertow.UndertowMessages;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.HttpMethodSecurityInfo;
import io.undertow.servlet.api.SecurityConstraint;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.api.ServletSecurityInfo;
import io.undertow.servlet.api.TransportGuaranteeType;
import io.undertow.servlet.api.WebResourceCollection;

import static javax.servlet.annotation.ServletSecurity.TransportGuarantee.CONFIDENTIAL;

/**
 * @author Stuart Douglas
 */
public class ServletRegistrationImpl implements ServletRegistration, ServletRegistration.Dynamic {

    private final ServletInfo servletInfo;
    private final DeploymentInfo deploymentInfo;

    public ServletRegistrationImpl(final ServletInfo servletInfo, final DeploymentInfo deploymentInfo) {
        this.servletInfo = servletInfo;
        this.deploymentInfo = deploymentInfo;
    }

    @Override
    public void setLoadOnStartup(final int loadOnStartup) {
        servletInfo.setLoadOnStartup(loadOnStartup);
    }

    @Override
    public Set<String> setServletSecurity(final ServletSecurityElement constraint) {
        if (constraint == null) {
            throw UndertowMessages.MESSAGES.argumentCannotBeNull("constraint");
        }

        //this is not super efficient, but it does not really matter
        final Set<String> urlPatterns = new HashSet<String>();
        for (SecurityConstraint sc : deploymentInfo.getSecurityConstraints()) {
            for (WebResourceCollection webResources : sc.getWebResourceCollections()) {
                urlPatterns.addAll(webResources.getUrlPatterns());
            }
        }
        final Set<String> ret = new HashSet<String>();
        for (String url : servletInfo.getMappings()) {
            if (urlPatterns.contains(url)) {
                ret.add(url);
            }
        }
        ServletSecurityInfo info = new ServletSecurityInfo();
        servletInfo.setServletSecurityInfo(info);
        info.setTransportGuaranteeType(constraint.getTransportGuarantee() == CONFIDENTIAL ? TransportGuaranteeType.CONFIDENTIAL : TransportGuaranteeType.NONE)
                .setEmptyRoleSemantic(constraint.getEmptyRoleSemantic())
                .addRolesAllowed(constraint.getRolesAllowed());

        for (final HttpMethodConstraintElement methodConstraint : constraint.getHttpMethodConstraints()) {
            info.addHttpMethodSecurityInfo(new HttpMethodSecurityInfo()
                    .setTransportGuaranteeType(methodConstraint.getTransportGuarantee() == CONFIDENTIAL ? TransportGuaranteeType.CONFIDENTIAL : TransportGuaranteeType.NONE)
                    .setMethod(methodConstraint.getMethodName())
                    .setEmptyRoleSemantic(methodConstraint.getEmptyRoleSemantic())
                    .addRolesAllowed(methodConstraint.getRolesAllowed()));
        }
        return ret;
    }

    @Override
    public void setMultipartConfig(final MultipartConfigElement multipartConfig) {
        servletInfo.setMultipartConfig(multipartConfig);
    }

    @Override
    public void setRunAsRole(final String roleName) {
        servletInfo.setRunAs(roleName);
    }

    @Override
    public void setAsyncSupported(final boolean isAsyncSupported) {
        servletInfo.setAsyncSupported(isAsyncSupported);
    }

    @Override
    public Set<String> addMapping(final String... urlPatterns) {
        final Set<String> ret = new HashSet<String>();
        final Set<String> existing = new HashSet<String>();
        for (ServletInfo s : deploymentInfo.getServlets().values()) {
            if (!s.getName().equals(servletInfo.getName())) {
                existing.addAll(s.getMappings());
            }
        }
        for (String pattern : urlPatterns) {
            if (existing.contains(pattern)) {
                ret.add(pattern);
            }
        }
        //only update if no changes have been made
        if (ret.isEmpty()) {
            for (String pattern : urlPatterns) {
                if (!servletInfo.getMappings().contains(pattern)) {
                    servletInfo.addMapping(pattern);
                }
            }
        }
        return ret;
    }

    @Override
    public Collection<String> getMappings() {
        return servletInfo.getMappings();
    }

    @Override
    public String getRunAsRole() {
        return servletInfo.getRunAs();
    }

    @Override
    public String getName() {
        return servletInfo.getName();
    }

    @Override
    public String getClassName() {
        return servletInfo.getServletClass().getName();
    }


    @Override
    public boolean setInitParameter(final String name, final String value) {
        if (servletInfo.getInitParams().containsKey(name)) {
            return false;
        }
        servletInfo.addInitParam(name, value);
        return true;
    }

    @Override
    public String getInitParameter(final String name) {
        return servletInfo.getInitParams().get(name);
    }

    @Override
    public Set<String> setInitParameters(final Map<String, String> initParameters) {
        final Set<String> ret = new HashSet<String>();
        for (Map.Entry<String, String> entry : initParameters.entrySet()) {
            if (!setInitParameter(entry.getKey(), entry.getValue())) {
                ret.add(entry.getKey());
            }
        }
        return ret;
    }

    @Override
    public Map<String, String> getInitParameters() {
        return servletInfo.getInitParams();
    }
}
