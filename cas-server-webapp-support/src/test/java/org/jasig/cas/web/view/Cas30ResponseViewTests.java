/*
 * Licensed to Apereo under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Apereo licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.cas.web.view;

import org.jasig.cas.CasProtocolConstants;
import org.jasig.cas.TestUtils;
import org.jasig.cas.authentication.Authentication;
import org.jasig.cas.authentication.AuthenticationBuilder;
import org.jasig.cas.authentication.CacheCredentialsMetaDataPopulator;
import org.jasig.cas.authentication.UsernamePasswordCredential;
import org.jasig.cas.services.ServicesManager;
import org.jasig.cas.util.CompressionUtils;
import org.jasig.cas.web.AbstractServiceValidateControllerTests;
import org.jasig.cas.web.support.CasAttributeEncoder;
import org.jasig.cas.web.support.DefaultCasAttributeEncoder;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.view.JstlView;

import javax.crypto.Cipher;
import javax.servlet.http.HttpServletRequest;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.*;


/**
 * Unit tests for {@link org.jasig.cas.web.view.Cas20ResponseView}.
 * @author Misagh Moayyed
 * @since 4.0.0
 */
public class Cas30ResponseViewTests extends AbstractServiceValidateControllerTests {

    @Autowired
    @Qualifier("protocolCas3ViewResolver")
    private ViewResolver resolver;

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Autowired
    @Qualifier("servicesManager")
    private ServicesManager servicesManager;

    @Before
    public void setup() {
        final KeyPair pair = generateKeyPair();
        this.publicKey = pair.getPublic();
        this.privateKey = pair.getPrivate();
    }

    private Map<?, ?> renderView() throws Exception{
        final ModelAndView modelAndView = this.getModelAndViewUponServiceValidationWithSecurePgtUrl();
        final JstlView v = (JstlView) resolver.resolveViewName(modelAndView.getViewName(), Locale.getDefault());
        final MockHttpServletRequest req = new MockHttpServletRequest(new MockServletContext());
        v.setServletContext(req.getServletContext());
        req.setAttribute(RequestContext.WEB_APPLICATION_CONTEXT_ATTRIBUTE,
                new GenericWebApplicationContext(req.getServletContext()));

        final Cas30ResponseView view = new Cas30ResponseView(v);
        view.setServicesManager(this.servicesManager);
        view.setCasAttributeEncoder(new DefaultCasAttributeEncoder(this.servicesManager));

        final MockHttpServletResponse resp = new MockHttpServletResponse();
        view.render(modelAndView.getModel(), req, resp);
        return (Map<?, ?>) req.getAttribute(CasProtocolConstants.VALIDATION_CAS_MODEL_ATTRIBUTE_NAME_ATTRIBUTES);
    }

    @Test
    public void verifyViewAuthnAttributes() throws Exception {
        final Map<?, ?> attributes = renderView();
        assertTrue(attributes.containsKey(CasProtocolConstants.VALIDATION_CAS_MODEL_ATTRIBUTE_NAME_AUTHENTICATION_DATE));
        assertTrue(attributes.containsKey(CasProtocolConstants.VALIDATION_CAS_MODEL_ATTRIBUTE_NAME_FROM_NEW_LOGIN));
        assertTrue(attributes.containsKey(CasProtocolConstants.VALIDATION_REMEMBER_ME_ATTRIBUTE_NAME));

    }

    @Test
    public void verifyPasswordAsAuthenticationAttributeCanDecrypt() throws Exception {
        final Map<?, ?> attributes = renderView();
    }

    private static KeyPair generateKeyPair() {
        try {
            final KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                    CasAttributeEncoder.DEFAULT_CIPHER_ALGORITHM);
            kpg.initialize(2048);
            return kpg.genKeyPair();
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private String decryptCredential(final String cred) {
        try {
            final Cipher cipher = Cipher.getInstance(CasAttributeEncoder.DEFAULT_CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, this.privateKey);
            final byte[] cred64 = CompressionUtils.decodeBase64ToByteArray(cred);
            final byte[] cipherData = cipher.doFinal(cred64);
            return new String(cipherData);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

}
