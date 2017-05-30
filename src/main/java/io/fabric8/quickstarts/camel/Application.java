/**
 * Copyright 2005-2016 Red Hat, Inc.
 * <p>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.quickstarts.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.idempotent.MemoryIdempotentRepository;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

import com.github.czgov.isds.Constants;

/**
 * A spring-boot application that includes a Camel route builder to setup the Camel routes
 */
@SpringBootApplication
@ImportResource({"classpath:spring/camel-context.xml"})
public class Application extends RouteBuilder {

    // must have a main method spring-boot can run
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void configure() throws Exception {
        from("isds:messages?environment={{isds.env}}&username={{isds.login}}&password={{isds.password}}&consumer.delay={{isds.delay}}")

                .idempotentConsumer(header(Constants.MSG_ID), MemoryIdempotentRepository.memoryIdempotentRepository())
                .log("new message: ${header.isdsSubject} from ${header.isdsFrom}")
                .setHeader("subject").header(Constants.MSG_SUBJECT)
                .setBody().constant("E-mail sent with Sample application using camel-isds components. See attached files.")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        for (String name : exchange.getIn().getAttachmentNames()) {
                            log.info("attachment name {}", name);
                            log.info("content type: {}", exchange.getIn().getAttachment(name).getContentType());
                        }
                    }
                })
                // prepare data for mustache template
                .process(e -> {
                    // need to pass entryset so we can iterate over Map entries in mustache
                    e.getIn().setHeader("attachments", e.getIn().getAttachments().entrySet());
                })
                .to("mustache:/mail-template.mustache")
                .setHeader(Exchange.CONTENT_TYPE).constant("text/html")
                .marshal().mimeMultipart()

                .to("smtps://smtp.gmail.com?username={{gmail.login}}" +
                        "&from={{gmail.login}}" +
                        "&to={{gmail.recipient}}" +
                        "&password={{gmail.password}}")
                .log("email sent");
    }
}
